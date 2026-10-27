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
import androidx.cardview.widget.CardView
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private lateinit var statusText: TextView
    private lateinit var subStatusText: TextView
    private lateinit var btnCopyIso: Button
    private lateinit var otgStatusCard: CardView
    private lateinit var otgIndicatorDot: View

    private val ACTION_USB_PERMISSION = "com.custom.usbwriter.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Главный контейнер (Тёмный глубокий фон)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            backgroundColor = Color.parseColor("#0B0F19")
            setPadding(64, 80, 64, 64)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Заголовок приложения (Кибер-стиль)
        val titleText = TextView(this).apply {
            text = "MyVentoy"
            textSize = 32f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.parseColor("#22D3EE")) // Неоново-бирюзовый
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 10)
        }
        mainLayout.addView(titleText)

        val subtitleText = TextView(this).apply {
            text = "USB BOOTABLE WRITER"
            textSize = 11f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(Color.parseColor("#475569")) // Темный slate
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(subtitleText)

        // КАРТОЧКА СТАТУСА (CardView с внутренним градиентом)
        otgStatusCard = CardView(this).apply {
            radius = 36f
            cardElevation = 16f
            setCardBackgroundColor(Color.parseColor("#141E30")) // Темно-синий
            preventCornerOverlap = true
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 80) }
        }

        val cardContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Неоновая точка-индикатор
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
        cardContent.addView(otgIndicatorDot)

        // Текстовый блок статуса внутри карточки
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusText = TextView(this).apply {
            text = "Ожидание USB OTG..."
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.parseColor("#F8FAFC")) // Снежно-белый
        }
        textContainer.addView(statusText)

        subStatusText = TextView(this).apply {
            text = "Подключи флешку через переходник"
            textSize = 12f
            setTextColor(Color.parseColor("#64748B")) // Серый сапфир
            setPadding(0, 8, 0, 0)
        }
        textContainer.addView(subStatusText)

        cardContent.addView(textContainer)
        otgStatusCard.addView(cardContent)
        mainLayout.addView(otgStatusCard)

        // КНОПКА ЗАПИСИ (Кастомная, с градиентом и закруглениями)
        val normalBtnBg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#2563EB"), Color.parseColor("#06B6D4")) // Синий -> Бирюзовый
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
        
        // Запускаем постоянный мониторинг флешки, чтобы UI реагировал "на лету"
        startOtgMonitoring()
    }

    private fun startOtgMonitoring() {
        thread {
            while (!isFinishing) {
                val devices = UsbMassStorageDevice.getMassStorageDevices(this)
                runOnUiThread {
                    if (devices.isNotEmpty()) {
                        statusText.text = "Флешка обнаружена!"
                        subStatusText.text = "Готова к записи ISO"
                        otgIndicatorDot.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#10B981")) // Зеленый
                        }
                    } else {
                        statusText.text = "Ожидание USB OTG..."
                        subStatusText.text = "Подключи флешку через переходник"
                        otgIndicatorDot.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#EF4444")) // Красный
                        }
                    }
                }
                Thread.sleep(1500) // Опрос раз в 1.5 секунды
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
            statusText.text = "Флешка потерялась!"
            subStatusText.text = "Проверь OTG-коннектор."
            return
        }

        val device = devices[0]

        if (!usbManager.hasPermission(device.usbDevice)) {
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device.usbDevice, permissionIntent)
            statusText.text = "Требуется разрешение"
            subStatusText.text = "Подтверди доступ к USB на экране..."
            return
        }

        copyIsoDirectlyToSecondPartition(isoUri, device)
    }

    private fun copyIsoDirectlyToSecondPartition(isoUri: Uri, device: UsbMassStorageDevice) {
        // Логику переноса переводим в фоновый поток, чтобы UI не зависал
        thread {
            try {
                runOnUiThread {
                    statusText.text = "Инициализация..."
                    subStatusText.text = "Подготовка диска"
                }
                device.init()

                val mainPartitionFs = device.partitions[1].fileSystem
                val rootDir = mainPartitionFs.rootDirectory

                val inputStream = contentResolver.openInputStream(isoUri) ?: return@thread
                val totalBytes = inputStream.available().toLong()
                
                // Извлекаем имя ISO
                val newIsoFile = rootDir.createFile("ventoy_image.iso")

                val buffer = ByteArray(1024 * 64) // 64 КБ буфер для макс. скорости
                var offset = 0L
                var bytesRead: Int
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    newIsoFile.write(offset, ByteBuffer.wrap(buffer, 0, bytesRead))
                    offset += bytesRead

                    // Обновляем прогресс раз в 200 мс (чтобы не перегружать UI поток)
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastProgressUpdate > 200) {
                        val progressPercent = if (totalBytes > 0) (offset * 100 / totalBytes) else 0
                        runOnUiThread {
                            statusText.text = "Идет запись: $progressPercent%"
                            subStatusText.text = "${offset / 1024 / 1024} MB записано"
                        }
                        lastProgressUpdate = currentTime
                    }
                }

                newIsoFile.flush()
                inputStream.close()
                device.close()

                runOnUiThread {
                    statusText.text = "Запись завершена! 🎉"
                    subStatusText.text = "Можно извлекать флешку"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Ошибка записи"
                    subStatusText.text = e.localizedMessage ?: "Неизвестная ошибка"
                }
            }
        }
    }
}
