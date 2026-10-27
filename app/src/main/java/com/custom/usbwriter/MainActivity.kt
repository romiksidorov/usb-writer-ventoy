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
    private lateinit var btnFormatVentoy: Button
    private lateinit var otgStatusCard: LinearLayout
    private lateinit var otgIndicatorDot: View

    private val ACTION_USB_PERMISSION = "com.custom.usbwriter.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            text = "USB BOOTABLE WRITER"
            textSize = 11f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(Color.parseColor("#475569"))
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(subtitleText)

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
                setColor(Color.parseColor("#EF4444"))
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
            text = "Подключи флешку через переходник"
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 8, 0, 0)
        }
        textContainer.addView(subStatusText)

        otgStatusCard.addView(textContainer)
        mainLayout.addView(otgStatusCard)

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

        val formatBtnBg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#EA580C"), Color.parseColor("#DC2626"))
        ).apply { cornerRadius = 32f }

        btnFormatVentoy = Button(this).apply {
            text = "ФОРМАТИРОВАТЬ В VENTOY"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            elevation = 12f
            letterSpacing = 0.05f
            background = formatBtnBg
            setPadding(40, 48, 40, 48)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 40, 0, 0) }
        }

        btnFormatVentoy.setOnClickListener {
            checkAndFormatVentoy()
        }
        mainLayout.addView(btnFormatVentoy)

        setContentView(mainLayout)
        
        startOtgMonitoring()
    }

    private fun startOtgMonitoring() {
        thread {
            while (!isFinishing) {
                val devices = UsbMassStorageDevice.getMassStorageDevices(this)
                runOnUiThread {
                    if (devices.isNotEmpty()) {
                        statusText.text = "Флешка обнаружена!"
                        subStatusText.text = "Готова к работе"
                        otgIndicatorDot.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#10B981"))
                        }
                    } else {
                        statusText.text = "Ожидание USB OTG..."
                        subStatusText.text = "Подключи флешку через переходник"
                        otgIndicatorDot.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#EF4444"))
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

    private fun checkAndFormatVentoy() {
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = UsbMassStorageDevice.getMassStorageDevices(this)

        if (devices.isEmpty()) {
            statusText.text = "Флешка не найдена!"
            subStatusText.text = "Подключи накопитель по OTG."
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

        formatDriveWithVentoy(device)
    }

    private fun formatDriveWithVentoy(device: UsbMassStorageDevice) {
        thread {
            try {
                runOnUiThread {
                    statusText.text = "Разметка диска..."
                    subStatusText.text = "Не извлекай флешку!"
                }
                device.init()

                val blockDevice = device.blockDevices[0]
                val blockSize = blockDevice.blockSize
                val totalBlocks = blockDevice.size

                val assetList = assets.list("") ?: emptyArray()
                if (assetList.contains("ventoy_disk.img")) {
                    val imgStream = assets.open("ventoy_disk.img")
                    val buffer = ByteArray(blockSize * 128)
                    var currentBlockOffset = 0L
                    var bytesRead: Int

                    while (imgStream.read(buffer).also { bytesRead = it } != -1) {
                        val alignedSize = if (bytesRead % blockSize == 0) bytesRead else ((bytesRead / blockSize) + 1) * blockSize
                        val writeBuf = ByteBuffer.allocate(alignedSize)
                        writeBuf.put(buffer, 0, bytesRead)
                        writeBuf.position(0)

                        blockDevice.write(currentBlockOffset, writeBuf)
                        currentBlockOffset += (alignedSize / blockSize)
                    }
                    imgStream.close()
                }

                val mbrBuffer = ByteBuffer.allocate(blockSize)
                blockDevice.read(0, mbrBuffer)
                val mbr = mbrBuffer.array()

                val vtoyEfiSectors = 65536L
                val part1Start = 2048L
                val part2Start = totalBlocks - vtoyEfiSectors
                val part1Size = part2Start - part1Start

                writeMbrPartitionEntry(mbr, 446, 0x00.toByte(), 0x0C.toByte(), part1Start.toInt(), part1Size.toInt())
                writeMbrPartitionEntry(mbr, 462, 0x80.toByte(), 0xEF.toByte(), part2Start.toInt(), vtoyEfiSectors.toInt())

                mbr[510] = 0x55.toByte()
                mbr[511] = 0xAA.toByte()

                mbrBuffer.position(0)
                blockDevice.write(0, mbrBuffer)

                device.close()

                runOnUiThread {
                    statusText.text = "Разметка Ventoy готова!"
                    subStatusText.text = "Переподключи флешку по OTG"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Ошибка форматирования"
                    subStatusText.text = e.localizedMessage ?: "Сбой записи секторов"
                }
            }
        }
        
