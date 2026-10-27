package com.custom.usbwriter

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private lateinit var statusText: TextView
    private val ACTION_USB_PERMISSION = "com.custom.usbwriter.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        statusText = TextView(this).apply { 
            text = "Подключи флешку с Ventoy..."
            textSize = 18f
            setPadding(0, 0, 0, 40)
        }
        
        val btnCopyIso = Button(this).apply { 
            text = "Выбрать и записать ISO-образ" 
        }

        btnCopyIso.setOnClickListener { 
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, 100)
        }

        // Собираем всё в вертикальный контейнер, чтобы убрать белый экран
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
            addView(statusText)
            addView(btnCopyIso)
        }

        setContentView(layout)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                checkAndCopyIso(uri)
            }
        }
    }

    private fun checkAndCopyIso(isoUri: Uri) {
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = UsbMassStorageDevice.getMassStorageDevices(this)

        if (devices.isEmpty()) {
            statusText.text = "Флешка не найдена. Проверь OTG-переходник."
            return
        }

        val device = devices[0]

        if (!usbManager.hasPermission(device.usbDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            statusText.text = "Подтверди доступ к USB на экране..."
            return
        }

        copyIsoDirectlyToSecondPartition(isoUri, device)
    }

    // Твой рабочий метод обхода ограничений Android для записи ISO на основной раздел
    private fun copyIsoDirectlyToSecondPartition(isoUri: Uri, device: UsbMassStorageDevice) {
        try {
            device.init()
            statusText.text = "Копирую ISO на флешку..."

            // partitions[1] — это основной раздел флешки с Ventoy
            val mainPartitionFs = device.partitions[1].fileSystem
            val rootDir = mainPartitionFs.rootDirectory

            val inputStream = contentResolver.openInputStream(isoUri) ?: return
            
            // Получаем имя файла из URI или задаем дефолтное
            val newIsoFile = rootDir.createFile("os_image.iso")

            val buffer = ByteArray(1024 * 64) // Буфер 64 КБ для высокой скорости
            var offset = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                newIsoFile.write(offset, ByteBuffer.wrap(buffer, 0, bytesRead))
                offset += bytesRead
            }

            newIsoFile.flush()
            inputStream.close()
            device.close()
            
            runOnUiThread { 
                statusText.text = "Готово! Образ успешно записан на флешку." 
            }
        } catch (e: Exception) {
            runOnUiThread { 
                statusText.text = "Ошибка записи: ${e.message}" 
            }
        }
    }
}
