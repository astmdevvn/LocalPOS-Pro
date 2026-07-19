package com.localpos.pro.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PosRepository(private val db: LocalPosDatabase) {
    val products: Flow<List<ProductEntity>> = db.products().observeAll()
    val sales: Flow<List<SaleEntity>> = db.sales().observeAll()
    val debts: Flow<List<DebtEntity>> = db.debts().observeAll()

    suspend fun seed() {
        if (db.products().count() == 0) {
            listOf(
                ProductEntity(name = "Cà phê sữa", barcode = "8938505974192", price = 25000, stock = 18),
                ProductEntity(name = "Trà đào", barcode = "8938505974208", price = 30000, stock = 4),
                ProductEntity(name = "Nước suối", barcode = "8934588012228", price = 10000, stock = 24),
                ProductEntity(name = "Bánh mì", barcode = "8930000001234", price = 20000, stock = 3)
            ).forEach { db.products().insert(it) }
        }
    }

    suspend fun addProduct(name: String, barcode: String, price: Long, stock: Int, imageUri: String?) {
        db.products().insert(ProductEntity(name = name, barcode = barcode, price = price, stock = stock, imageUri = imageUri))
    }

    suspend fun deleteProduct(product: ProductEntity) = db.products().delete(product)

    suspend fun checkout(lines: List<Pair<ProductEntity, Int>>, payLaterName: String?) = db.withTransaction {
        val total = lines.sumOf { it.first.price * it.second }
        lines.forEach { (product, quantity) ->
            db.products().update(product.copy(stock = (product.stock - quantity).coerceAtLeast(0)))
        }
        val method = if (payLaterName.isNullOrBlank()) "Tiền mặt" else "Ghi nợ"
        db.sales().insert(SaleEntity(total = total, paymentMethod = method))
        if (!payLaterName.isNullOrBlank()) db.debts().insert(DebtEntity(customerName = payLaterName, amount = total))
    }

    suspend fun settle(debt: DebtEntity) = db.debts().update(debt.copy(settled = true))

    suspend fun backupJson(): String {
        val root = JSONObject().put("format", 1)
        root.put("products", JSONArray().apply { products.first().forEach { p -> put(JSONObject().put("id",p.id).put("name",p.name).put("barcode",p.barcode).put("price",p.price).put("stock",p.stock).put("lowStockAt",p.lowStockAt).put("imageUri",p.imageUri)) } })
        root.put("sales", JSONArray().apply { sales.first().forEach { s -> put(JSONObject().put("id",s.id).put("total",s.total).put("paymentMethod",s.paymentMethod).put("createdAt",s.createdAt)) } })
        root.put("debts", JSONArray().apply { debts.first().forEach { d -> put(JSONObject().put("id",d.id).put("customerName",d.customerName).put("amount",d.amount).put("note",d.note).put("settled",d.settled).put("createdAt",d.createdAt)) } })
        return root.toString(2)
    }

    suspend fun restoreJson(text: String) = db.withTransaction {
        val root = JSONObject(text); require(root.getInt("format") == 1) { "Định dạng backup không hỗ trợ" }
        db.products().clear(); db.sales().clear(); db.debts().clear()
        root.getJSONArray("products").forEachObject { o -> db.products().insert(ProductEntity(o.getLong("id"),o.getString("name"),o.getString("barcode"),o.getLong("price"),o.getInt("stock"),o.getInt("lowStockAt"),o.optString("imageUri").takeIf { it.isNotBlank() && it != "null" })) }
        root.getJSONArray("sales").forEachObject { o -> db.sales().insert(SaleEntity(o.getLong("id"),o.getLong("total"),o.getString("paymentMethod"),o.getLong("createdAt"))) }
        root.getJSONArray("debts").forEachObject { o -> db.debts().insert(DebtEntity(o.getLong("id"),o.getString("customerName"),o.getLong("amount"),o.optString("note"),o.getBoolean("settled"),o.getLong("createdAt"))) }
    }

    suspend fun importWooCommerce(baseUrl: String, key: String, secret: String): Int {
        val endpoint = baseUrl.trimEnd('/') + "/wp-json/wc/v3/products?per_page=100&consumer_key=" + java.net.URLEncoder.encode(key,"UTF-8") + "&consumer_secret=" + java.net.URLEncoder.encode(secret,"UTF-8")
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000; connection.readTimeout = 30_000
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val array = JSONArray(body)
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i); val barcode = o.optString("sku").ifBlank { "woo-${o.getLong("id")}" }
            val price = o.optDouble("price", 0.0).toLong()
            db.products().insert(ProductEntity(name=o.getString("name"),barcode=barcode,price=price,stock=o.optInt("stock_quantity",0)))
        }
        return array.length()
    }

    private suspend inline fun JSONArray.forEachObject(block: suspend (JSONObject) -> Unit) { for (i in 0 until length()) block(getJSONObject(i)) }
}
