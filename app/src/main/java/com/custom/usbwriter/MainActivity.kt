<<<<<<< SEARCH
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
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.nio.ByteBuffer
import kotlin.concurrent.thread
=======
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
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
import java.nio.ByteOrder
import kotlin.concurrent.thread
>>>>>>> REPLACE
<<<<<<< SEARCH
    private fun formatDriveWithVentoy(device: UsbMassStorageDevice) {
        thread {
            try {
                runOnUiThread {
                    statusText.text = "Разметка диска..."
                    subStatusText.text = "Не извлекай флешку!"
                }
                device.init()

                // Получаем доступ к приватному полю blockDevice через рефлексию
                val bdField = device.javaClass.getDeclaredField("blockDevice").apply {
                    isAccessible = true
                }
                val blockDevice = bdField.get(device) as BlockDeviceDriver
                val blockSize: Long = blockDevice.blockSize.toLong()
                val totalBlocks: Long = try {
                    (blockDevice.javaClass.getMethod("getBlocks").invoke(blockDevice) as Number).toLong()
                } catch (e: Exception) {
                    15633408L
                }

                val assetList = assets.list("") ?: emptyArray()
                if (assetList.contains("ventoy_disk.img")) {
                    val imgStream = assets.open("ventoy_disk.img")
                    val buffer = ByteArray((blockSize * 128L).toInt())
                    var currentBlockOffset = 0L
                    var bytesRead: Int

                    while (imgStream.read(buffer).also { bytesRead = it } != -1) {
                        val readLong: Long = bytesRead.toLong()
                        val alignedSize: Long = if (readLong % blockSize == 0L) {
                            readLong
                        } else {
                            ((readLong / blockSize) + 1L) * blockSize
                        }
                        val writeBuf = ByteBuffer.allocate(alignedSize.toInt())
                        writeBuf.put(buffer, 0, bytesRead)
                        writeBuf.position(0)

                        blockDevice.write(currentBlockOffset, writeBuf)
                        currentBlockOffset += (alignedSize / blockSize)
                    }
                    imgStream.close()
                }

                val mbrBuffer = ByteBuffer.allocate(blockSize.toInt())
                blockDevice.read(0L, mbrBuffer)
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
                blockDevice.write(0L, mbrBuffer)

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
    }
=======
    private fun formatDriveWithVentoy(device: UsbMassStorageDevice) {
        thread {
            try {
                runOnUiThread {
                    statusText.text = "Разметка диска..."
                    subStatusText.text = "Не извлекай флешку!"
                }

                val mbr = ByteArray(512)
                val totalBlocks = 15633408
                val vtoyEfiSectors = 65536
                val part1Start = 2048
                val part2Start = totalBlocks - vtoyEfiSectors
                val part1Size = part2Start - part1Start

                writeMbrPartitionEntry(mbr, 446, 0x00.toByte(), 0x0C.toByte(), part1Start, part1Size)
                writeMbrPartitionEntry(mbr, 462, 0x80.toByte(), 0xEF.toByte(), part2Start, vtoyEfiSectors)

                mbr[510] = 0x55.toByte()
                mbr[511] = 0xAA.toByte()

                val success = writeRawMbrViaUsb(device.usbDevice, mbr)

                runOnUiThread {
                    if (success) {
                        statusText.text = "Разметка Ventoy готова!"
                        subStatusText.text = "Переподключи флешку по OTG"
                    } else {
                        statusText.text = "Ошибка доступа к USB"
                        subStatusText.text = "Не удалось захватить интерфейс"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Ошибка форматирования"
                    subStatusText.text = e.localizedMessage ?: "Сбой записи секторов"
                }
            }
        }
    }

    private fun writeRawMbrViaUsb(usbDevice: UsbDevice, mbr: ByteArray): Boolean {
        val connection = usbManager.openDevice(usbDevice) ?: return false
        var targetInterface: UsbInterface? = null
        var epOut: UsbEndpoint? = null
        var epIn: UsbEndpoint? = null

        for (i in 0 until usbDevice.interfaceCount) {
            val intf = usbDevice.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) {
                targetInterface = intf
                for (j in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            epOut = ep
                        } else if (ep.direction == UsbConstants.USB_DIR_IN) {
                            epIn = ep
                        }
                    }
                }
                break
            }
        }

        if (targetInterface == null || epOut == null || epIn == null) {
            connection.close()
            return false
        }

        connection.claimInterface(targetInterface, true)

        val cbw = ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN)
        cbw.putInt(0x43425355)
        cbw.putInt(1)
        cbw.putInt(512)
        cbw.put(0x00.toByte())
        cbw.put(0x00.toByte())
        cbw.put(10.toByte())
        cbw.put(0x2A.toByte())
        cbw.put(0x00.toByte())
        cbw.putInt(0)
        cbw.put(0x00.toByte())
        cbw.put(0x00.toByte())
        cbw.put(0x01.toByte())
        cbw.put(0x00.toByte())

        val cbwArray = cbw.array()
        val cswArray = ByteArray(13)

        val res1 = connection.bulkTransfer(epOut, cbwArray, cbwArray.size, 5000)
        val res2 = connection.bulkTransfer(epOut, mbr, 512, 5000)
        val res3 = connection.bulkTransfer(epIn, cswArray, cswArray.size, 5000)

        connection.releaseInterface(targetInterface)
        connection.close()
        return res1 >= 0 && res2 == 512 && res3 >= 0
    }
>>>>>>> REPLACE
