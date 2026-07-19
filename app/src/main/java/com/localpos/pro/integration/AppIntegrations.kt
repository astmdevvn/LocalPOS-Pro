package com.localpos.pro.integration

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.os.Build
import com.localpos.pro.data.*
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

object DataExporter {
    fun writeCsv(context: Context, uri: Uri, products: List<ProductEntity>, sales: List<SaleEntity>, debts: List<DebtEntity>) {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { out ->
            out.write("\uFEFFLOẠI,TÊN/MÃ,GIÁ/TRỊ GIÁ,TỒN KHO/TRẠNG THÁI,THỜI GIAN\n")
            products.forEach { out.write(csv("Sản phẩm", it.name, it.price, it.stock, it.barcode)) }
            sales.forEach { out.write(csv("Bán hàng", "Đơn #${it.id}", it.total, it.paymentMethod, date(it.createdAt))) }
            debts.forEach { out.write(csv("Công nợ", it.customerName, it.amount, if (it.settled) "Đã thu" else "Chưa thu", date(it.createdAt))) }
        }
    }

    fun writePdf(context: Context, uri: Uri, sales: List<SaleEntity>, debts: List<DebtEntity>) {
        val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; isFakeBoldText = true }
        canvas.drawText("LocalPOS Pro - Báo cáo bán hàng", 42f, 54f, paint)
        paint.textSize = 12f; paint.isFakeBoldText = false
        var y = 84f
        canvas.drawText("Ngày xuất: ${date(System.currentTimeMillis())}", 42f, y, paint); y += 28
        paint.isFakeBoldText = true; canvas.drawText("Tổng doanh thu: ${money(sales.sumOf { it.total })}", 42f, y, paint); y += 25
        canvas.drawText("Công nợ chưa thu: ${money(debts.filterNot { it.settled }.sumOf { it.amount })}", 42f, y, paint); y += 34
        paint.isFakeBoldText = false
        sales.take(28).forEach { canvas.drawText("#${it.id}   ${date(it.createdAt)}   ${it.paymentMethod}   ${money(it.total)}", 42f, y, paint); y += 22 }
        doc.finishPage(page); context.contentResolver.openOutputStream(uri)?.use(doc::writeTo); doc.close()
    }

    private fun csv(vararg values: Any) = values.joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" } + "\n"
    private fun date(time: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(Date(time))
    private fun money(v: Long) = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(v)
}

object BluetoothPrinter {
    data class Printer(val name: String, val address: String)
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun paired(context: Context): List<Printer> {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        return adapter.bondedDevices.orEmpty().map { Printer(it.name ?: "Máy in Bluetooth", it.address) }.sortedBy { it.name }
    }

    @SuppressLint("MissingPermission")
    fun print(context: Context, address: String, lines: List<Pair<String, String>>, total: String) {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: error("Bluetooth không khả dụng")
        val device = adapter.getRemoteDevice(address)
        device.createRfcommSocketToServiceRecord(spp).use { socket ->
            adapter.cancelDiscovery(); socket.connect(); socket.outputStream.use { out ->
                out.write(byteArrayOf(0x1B, 0x40)); center(out); out.write("LOCALPOS PRO\n".toByteArray())
                out.write("Scan. Sell. Print.\n\n".toByteArray()); left(out)
                lines.forEach { out.write("${it.first}  ${it.second}\n".toByteArray()) }
                out.write("-------------------------------\n".toByteArray()); out.write("TONG: $total\n\nCam on quy khach!\n\n\n".toByteArray())
                out.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10)); out.flush()
            }
        }
    }
    private fun center(out: OutputStream) = out.write(byteArrayOf(0x1B, 0x61, 0x01))
    private fun left(out: OutputStream) = out.write(byteArrayOf(0x1B, 0x61, 0x00))
}

object StockNotifier {
    fun show(context: Context, products: List<ProductEntity>) {
        val low = products.filter { it.stock <= it.lowStockAt }
        if (low.isEmpty()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("stock", "Cảnh báo tồn kho", NotificationManager.IMPORTANCE_DEFAULT))
        val text = low.take(3).joinToString { "${it.name} (${it.stock})" }
        manager.notify(1001, NotificationCompat.Builder(context,"stock").setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("${low.size} sản phẩm sắp hết hàng").setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).build())
    }
}
