package com.custom.usbwriter

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private lateinit var statusText: TextView
    private lateinit var subStatusText: TextView
    private lateinit var btnCopyIso: Button
    private lateinit var otgStatusCard: LinearLayout
    private lateinit var otgIndicatorDot: View

    private val ACTION_USB_PERMISSION = "com.custom.usbwriter.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Контейнер с глубоким темным фоном
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0F19"))
            setPadding(64, 80, 64, 64)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Заголовок в стиле Cyber/Ventoy
        val titleText = TextView(this).apply {
            text = "MyVentoy"
            textSize = 32f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.parseColor("#22D3EE"))
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 10)
        }
        mainLayout.addView(titleText)

        val subtitleText = TextView(this).apply {
            text = "MOBILE MULTIBOOT ENGINE"
            textSize = 11f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(Color.parseColor("#475569"))
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(subtitleText)

        // Карточка статуса подключения OTG
        otgStatusCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_VERTICAL
            elevation = 16f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 36f
                setColor(Color.parseColor("#141E30"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 80) }
        }

        otgIndicatorDot = View(this).apply {
            val size = (14 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 0, 32, 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EF4444")) // Красный (по умолчанию)
            }
        }
        otgStatusCard.addView(otgIndicatorDot)

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusText = TextView(this).apply {
            text = "Ожидание USB OTG..."
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.parseColor("#F8FAFC"))
        }
        textContainer.addView(statusText)

        subStatusText = TextView(this).apply {
            text = "Подключите флешку через переходник"
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 8, 0, 0)
        }
        textContainer.addView(subStatusText)

        otgStatusCard.addView(textContainer)
        mainLayout.addView(otgStatusCard)

        // Кнопка записи ISO-образа на флешку
        val normalBtnBg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#2563EB"), Color.parseColor("#06B6D4"))
        ).apply { cornerRadius = 32f }

        btnCopyIso = Button(this).apply {
            text = "ЗАПИСАТЬ ISO-ОБРАЗ"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            elevation = 12f
            letterSpacing = 0.05f
            background = normalBtnBg
            setPadding(40, 48, 40, 48)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        btnCopyIso.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, 100)
        }
        mainLayout.addView(btnCopyIso)

        setContentView(mainLayout)
        
        startOtgMonitoring()
    }

    private fun startOtgMonitoring() {
        thread {
            while (!isFinishing) {
                val devices = UsbMassStorageDevice.getMassStorageDevices(this)
                runOnUiThread {
                    if (devices.isNotEmpty()) {
                        statusText.text = "Накопитель обнаружен"
                        subStatusText.text = "Готов к интеграции ISO"
                        otgIndicatorDot.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#10B981")) // Зеленый
                        }
                    } else {
                        statusText.text = "Ожидание USB OTG..."
                        subStatusText.text = "Подключите флешку через переходник"
                        otgIndicatorDot.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#EF4444")) // Красный
                        }
                    }
                }
                Thread.sleep(1500)
            }
        }
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
            statusText.text = "Флешка отключена!"
            subStatusText.text = "Проверьте OTG-коннектор."
            return
        }

        val device = devices[0]

        if (!usbManager.hasPermission(device.usbDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            statusText.text = "Запрос прав USB"
            subStatusText.text = "Подтвердите доступ во всплывающем окне..."
            return
        }

        copyIsoToVentoyPartition(isoUri, device)
    }

    private fun copyIsoToVentoyPartition(isoUri: Uri, device: UsbMassStorageDevice) {
        thread {
            try {
                runOnUiThread {
                    statusText.text = "Инициализация диска..."
                    subStatusText.text = "Сканирование файловой системы"
                }
                device.init()

                if (device.partitions.isEmpty()) {
                    runOnUiThread {
                        statusText.text = "Ошибка структуры"
                        subStatusText.text = "Разделы накопителя не найдены"
                    }
                    device.close()
                    return@thread
                }

                // Выбираем целевой раздел (по логике Ventoy это основной exFAT раздел для образов)
                val targetPartition = if (device.partitions.size > 1) {
                    device.partitions[1]
                } else {
                    device.partitions[0]
                }

                val mainPartitionFs = targetPartition.fileSystem
                val rootDir = mainPartitionFs.rootDirectory

                val inputStream = contentResolver.openInputStream(isoUri) ?: return@thread
                val totalBytes = inputStream.available().toLong()
                
                // Создаем файл образа на флешке
                val newIsoFile = rootDir.createFile("ventoy_boot_image.iso")

                val buffer = ByteArray(1024 * 64) // Буфер 64 КБ для молниеносной записи
                var offset = 0L
                var bytesRead: Int
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    newIsoFile.write(offset, ByteBuffer.wrap(buffer, 0, bytesRead))
                    offset += bytesRead.toLong()

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastProgressUpdate > 200L) {
                        val progressPercent = if (totalBytes > 0L) (offset * 100L / totalBytes) else 0L
                        runOnUiThread {
                            statusText.text = "Перенос ISO: $progressPercent%"
                            subStatusText.text = "${offset / 1024L / 1024L} MB записано"
                        }
                        lastProgressUpdate = currentTime
                    }
                }

                newIsoFile.flush()
                inputStream.close()
                device.close()

                runOnUiThread {
                    statusText.text = "Успешно записано! 🎉"
                    subStatusText.text = "Флешка готова к загрузке на ПК"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Ошибка передачи"
                    subStatusText.text = e.localizedMessage ?: "Сбой записи потока"
                }
            }
        }
    }
}
