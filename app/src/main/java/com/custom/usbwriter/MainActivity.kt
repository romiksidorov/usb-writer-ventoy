package com.custom.usbwriter

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.io.InputStream
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private lateinit var statusText: TextView
    private val ACTION_USB_PERMISSION = "com.custom.usbwriter.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Простейший интерфейс: текст статуса и две кнопки
        statusText = TextView(this).apply { text = "Ожидание флешки..." }
        val btnPrepare = Button(this).apply { text = "1. Записать загрузчик (Prepare USB)" }
        val btnCopyIso = Button(this).apply { text = "2. Скинуть ISO в обход проводника" }

        btnPrepare.setOnClickListener { detectAndPrepareUsb() }
        
        // Тут вызывается системное окно выбора твоего .iso файла
        btnCopyIso.setOnClickListener { 
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, 100)
        }
    }

    private fun detectAndPrepareUsb() {
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = UsbMassStorageDevice.getMassStorageDevices(this)

        if (devices.isEmpty()) {
            statusText.text = "Флешка не найдена. Проверь OTG-переходник."
            return
        }

        val device = devices[0] // Берем первую подключенную флешку

        // Запрашиваем у Android разрешение на прямой доступ к USB
        if (!usbManager.hasPermission(device.usbDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            statusText.text = "Подтверди доступ к USB на экране..."
            return
        }

        writeBootloaderToUsb(device)
    }

private fun writeBootloaderToUsb(device: UsbMassStorageDevice) {
    try {
        device.init() // Инициализируем прямой доступ к секторам флешки
        
        // Исправлено: получаем блочное устройство правильно для текущей версии libaums
        val blockDevice = device.partitions[0].blockDevice

        // Открываем вшитый в приложение архив загрузчика Ventoy
        val zipStream = java.util.zip.ZipInputStream(assets.open("ventoy_core.zip"))
        zipStream.nextEntry
        val ventoyImageStream: InputStream = zipStream

        val buffer = ByteArray(4096) // Пишем блоками по 4 КБ
        var currentSectorOffset = 0L

        runOnUiThread { statusText.text = "Разметка GPT и запись загрузчика..." }

        while (ventoyImageStream.read(buffer) != -1) {
            val byteBuffer = ByteBuffer.wrap(buffer)
            blockDevice.write(currentSectorOffset, byteBuffer)
            currentSectorOffset += (buffer.size / blockDevice.blockSize)
        }

        ventoyImageStream.close()
        device.close()
        runOnUiThread { statusText.text = "Готово! Загрузчик записан." }
    } catch (e: Exception) {
        runOnUiThread { statusText.text = "Ошибка записи: ${e.message}" }
    }
}

    // Метод для прямой переброски ISO-файла во второй раздел флешки
    fun copyIsoDirectlyToSecondPartition(isoUri: Uri) {
        val devices = UsbMassStorageDevice.getMassStorageDevices(this)
        if (devices.isEmpty()) return

        val device = devices[0]
        device.init()

        // Хитрость против бага в 34 МБ: 
        // partitions[0] — это маленький VTOYEFI (34 МБ), который видит Android.
        // partitions[1] — это наш основной раздел на 29 ГБ! Обращаемся сразу к нему:
        val mainPartitionFs = device.partitions[1].fileSystem
        val rootDir = mainPartitionFs.rootDirectory

        val inputStream = contentResolver.openInputStream(isoUri) ?: return
        val newIsoFile = rootDir.createFile("Windows11.iso")

        // Перекачиваем гигабайты напрямую с памяти телефона в обход лимитов системы
        val buffer = ByteArray(1024 * 64) // Буфер 64 КБ для быстрой скорости
        var offset = 0L
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            newIsoFile.write(offset, ByteBuffer.wrap(buffer, 0, bytesRead))
            offset += bytesRead
        }

        newIsoFile.flush()
        inputStream.close()
        device.close()
        statusText.text = "Образ Windows11.iso успешно записан на 29 ГБ раздел!"
    }
}
