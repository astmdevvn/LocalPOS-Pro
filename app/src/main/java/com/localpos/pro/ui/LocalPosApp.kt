@file:OptIn(ExperimentalMaterial3Api::class)

package com.localpos.pro.ui

import android.Manifest
import android.content.Intent
import android.content.ContentResolver
import android.net.Uri
import android.widget.ImageView
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.localpos.pro.BuildConfig
import com.localpos.pro.R
import com.localpos.pro.data.*
import com.localpos.pro.integration.BluetoothPrinter
import com.localpos.pro.integration.DataExporter
import com.localpos.pro.integration.StockNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private enum class Tab(val title: String, val icon: ImageVector) {
    SELL("Bán hàng", Icons.Default.ShoppingCart),
    PRODUCTS("Sản phẩm", Icons.Default.Inventory2),
    REPORTS("Báo cáo", Icons.Default.BarChart),
    MORE("Thêm", Icons.Default.MoreHoriz)
}

@Composable
fun LocalPosApp(vm: PosViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by vm.products.collectAsState(); val sales by vm.sales.collectAsState(); val debts by vm.debts.collectAsState()
    var tab by remember { mutableStateOf(Tab.SELL) }
    var showAbout by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { w -> w.write(pendingBackup.orEmpty()) }; notice = "Đã backup dữ liệu. Bạn có thể chọn Google Drive trong trình lưu tệp." } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { scope.launch { runCatching { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> vm.restoreJson(r.readText()) } }.onSuccess { notice = "Khôi phục dữ liệu thành công" }.onFailure { e -> notice = "Không thể khôi phục: ${e.message}" } } } }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { DataExporter.writeCsv(context, it, products, sales, debts); notice = "Đã xuất file Excel/CSV" } }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> uri?.let { DataExporter.writePdf(context, it, sales, debts); notice = "Đã xuất báo cáo PDF" } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("LocalPOS Pro", fontWeight = FontWeight.Bold); Text(tab.title, style = MaterialTheme.typography.labelMedium) } },
                actions = {
                    Image(
                        painter = painterResource(R.drawable.asl_logo),
                        contentDescription = "ASLDEV.VN",
                        modifier = Modifier.width(108.dp).height(42.dp).padding(horizontal = 6.dp),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(onClick = { showAbout = true }) { Icon(Icons.Default.Info, "About") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(item.icon, item.title) }, label = { Text(item.title) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.SELL -> SellScreen(vm)
                Tab.PRODUCTS -> ProductsScreen(vm)
                Tab.REPORTS -> ReportsScreen(vm)
                Tab.MORE -> MoreScreen(vm, onAbout = { showAbout = true }, onBackup = { scope.launch { pendingBackup = vm.backupJson(); backup.launch("LocalPOS-backup.json") } }, onRestore = { restore.launch(arrayOf("application/json", "text/plain")) }, onCsv = { csv.launch("LocalPOS-data.csv") }, onPdf = { pdf.launch("LocalPOS-report.pdf") }, onNotice = { notice = it })
            }
        }
    }
    AslAboutDialog(visible = showAbout, onClose = { showAbout = false })
    notice?.let { AlertDialog(onDismissRequest = { notice = null }, confirmButton = { TextButton(onClick = { notice = null }) { Text("Đóng") } }, text = { Text(it) }) }
}

@Composable
private fun SellScreen(vm: PosViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    var query by remember { mutableStateOf("") }
    var checkout by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var scanner by remember { mutableStateOf(false) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) scanner = true else message = "Cần quyền camera để quét mã vạch" }
    val filtered = products.filter { it.name.contains(query, true) || it.barcode.contains(query) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Tên hoặc mã vạch") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { IconButton(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) { Icon(Icons.Default.QrCodeScanner, "Quét mã") } }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { product -> ProductRow(product, onAdd = { vm.addToCart(product) }) }
        }
        if (cart.isNotEmpty()) {
            val total = cart.sumOf { it.product.price * it.quantity }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("${cart.sumOf { it.quantity }} sản phẩm"); Text(money(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    Button(onClick = { checkout = true }) { Text("Thanh toán") }
                }
            }
        }
    }
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("Đã hiểu") } }, title = { Text("Quét mã") }, text = { Text(it) }) }
    if (checkout) CheckoutDialog(cart, onDismiss = { checkout = false }, onPay = { customer ->
        val receipt = cart.toList(); vm.checkout(customer) {
            checkout = false
            val address = context.getSharedPreferences("localpos", 0).getString("printer", null)
            if (address != null) scope.launch(Dispatchers.IO) { runCatching { BluetoothPrinter.print(context, address, receipt.map { "${it.quantity}x ${it.product.name}" to money(it.quantity * it.product.price) }, money(receipt.sumOf { it.quantity * it.product.price })) } }
        }
    })
    if (scanner) BarcodeScannerDialog(onDismiss = { scanner = false }) { code -> scanner = false; query = code; products.firstOrNull { it.barcode == code }?.let(vm::addToCart) ?: run { message = "Không tìm thấy sản phẩm có mã $code" } }
}

@Composable
private fun ProductRow(product: ProductEntity, onAdd: () -> Unit, onDelete: (() -> Unit)? = null) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(52.dp)) { Box(contentAlignment = Alignment.Center) {
                if (product.imageUri != null) AndroidView(factory={ ctx -> ImageView(ctx).apply { scaleType=ImageView.ScaleType.CENTER_CROP } },update={ runCatching { it.setImageURI(Uri.parse(product.imageUri)) }.onFailure { _ -> it.setImageResource(android.R.drawable.ic_menu_gallery) } },modifier=Modifier.fillMaxSize()) else Icon(Icons.Default.Inventory2, null)
            } }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text("${money(product.price)} · Tồn ${product.stock}", style = MaterialTheme.typography.bodySmall, color = if (product.stock <= product.lowStockAt) MaterialTheme.colorScheme.error else LocalContentColor.current)
            }
            if (onDelete != null) IconButton(onClick=onDelete) { Icon(Icons.Default.Delete,"Xoá",tint=MaterialTheme.colorScheme.error) } else FilledTonalIconButton(onClick = onAdd, enabled = product.stock > 0) { Icon(Icons.Default.Add, "Thêm") }
        }
    }
}

@Composable
private fun CheckoutDialog(cart: List<CartLine>, onDismiss: () -> Unit, onPay: (String?) -> Unit) {
    var payLater by remember { mutableStateOf(false) }
    var customer by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Thanh toán") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cart.forEach { line -> Row(Modifier.fillMaxWidth()) { Text("${line.quantity}× ${line.product.name}", Modifier.weight(1f)); Text(money(line.product.price * line.quantity)) } }
            HorizontalDivider()
            Text("Tổng cộng: ${money(cart.sumOf { it.product.price * it.quantity })}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(payLater, { payLater = it }); Text("Ghi nợ / Pay Later") }
            if (payLater) OutlinedTextField(customer, { customer = it }, label = { Text("Tên khách hàng") }, singleLine = true)
        }
    }, confirmButton = { Button(onClick = { onPay(customer.takeIf { payLater }) }, enabled = !payLater || customer.isNotBlank()) { Icon(Icons.Default.Print, null); Spacer(Modifier.width(6.dp)); Text("Lưu & in") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable
private fun ProductsScreen(vm: PosViewModel) {
    val products by vm.products.collectAsState()
    var add by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProductEntity?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Kho hàng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("${products.size} sản phẩm · ${products.count { it.stock <= it.lowStockAt }} sắp hết") }; ExtendedFloatingActionButton(onClick = { add = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Thêm") }) }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(products, key = { it.id }) { product -> ProductRow(product, onAdd={}, onDelete={deleting=product}) } }
    }
    if (add) AddProductDialog(onDismiss = { add = false }) { n, b, p, s, image -> vm.addProduct(n, b, p, s, image); add = false }
    deleting?.let { product -> AlertDialog(onDismissRequest={deleting=null},title={Text("Xoá sản phẩm?")},text={Text("${product.name} sẽ bị xoá khỏi danh sách kho. Các giao dịch cũ vẫn được giữ lại.")},confirmButton={Button(onClick={vm.deleteProduct(product);deleting=null},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)){Text("Xoá")}},dismissButton={TextButton(onClick={deleting=null}){Text("Hủy")}}) }
}

@Composable
private fun AddProductDialog(onDismiss: () -> Unit, onAdd: (String, String, Long, Int, String?) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }; var barcode by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var stock by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; imageUri=it.toString() } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Thêm sản phẩm") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={imagePicker.launch(arrayOf("image/*"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.AddPhotoAlternate,null);Spacer(Modifier.width(8.dp));Text(if(imageUri==null) "Chọn ảnh sản phẩm" else "Đổi ảnh sản phẩm")}
        imageUri?.let { value -> AndroidView(factory={ctx->ImageView(ctx).apply{scaleType=ImageView.ScaleType.CENTER_CROP}},update={it.setImageURI(Uri.parse(value))},modifier=Modifier.fillMaxWidth().height(120.dp)) }
        OutlinedTextField(name, { name = it }, label = { Text("Tên sản phẩm") }); OutlinedTextField(barcode, { barcode = it }, label = { Text("Mã vạch") }); OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Giá bán") }); OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, label = { Text("Tồn kho") })
    } }, confirmButton = { Button(onClick = { onAdd(name, barcode.ifBlank { System.currentTimeMillis().toString() }, price.toLongOrNull() ?: 0, stock.toIntOrNull() ?: 0, imageUri) }, enabled = name.isNotBlank() && price.isNotBlank()) { Text("Lưu") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable
private fun ReportsScreen(vm: PosViewModel) {
    val sales by vm.sales.collectAsState()
    var period by remember { mutableIntStateOf(0) }
    val now = System.currentTimeMillis(); val day = 86_400_000L
    val selected = when(period) { 0 -> sales.filter { sameDay(it.createdAt, now) }; 1 -> sales.filter { it.createdAt >= now - 7*day }; else -> sales.filter { it.createdAt >= now - 30*day } }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Báo cáo bán hàng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Hôm nay","7 ngày","30 ngày").forEachIndexed { i, label -> FilterChip(selected=period==i,onClick={period=i},label={Text(label)}) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Doanh thu", money(selected.sumOf { it.total }), Modifier.weight(1f)); MetricCard("Đơn hàng", selected.size.toString(), Modifier.weight(1f)) } }
        item { Text("Giao dịch gần đây", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (selected.isEmpty()) item { EmptyState("Chưa có giao dịch", "Các đơn hàng trong kỳ sẽ xuất hiện ở đây.") }
        items(selected, key = { it.id }) { sale -> ListItem(headlineContent = { Text(money(sale.total), fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("${sale.paymentMethod} · ${SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN")).format(Date(sale.createdAt))}") }, leadingContent = { Icon(Icons.Default.ReceiptLong, null) }) }
    }
}

@Composable private fun RowScope.MetricCard(label: String, value: String, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(16.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } } }

@Composable
private fun MoreScreen(vm: PosViewModel, onAbout: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, onCsv: () -> Unit, onPdf: () -> Unit, onNotice: (String) -> Unit) {
    val debts by vm.debts.collectAsState()
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var printers by remember { mutableStateOf<List<BluetoothPrinter.Printer>?>(null) }
    var woo by remember { mutableStateOf(false) }
    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) printers = BluetoothPrinter.paired(context) else onNotice("Cần quyền Bluetooth để tìm máy in đã ghép đôi") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) { StockNotifier.show(context, vm.products.value); onNotice("Đã bật cảnh báo tồn kho") } else onNotice("Chưa cấp quyền thông báo") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Công nợ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (debts.none { !it.settled }) item { EmptyState("Không có công nợ", "Các đơn Pay Later sẽ xuất hiện ở đây.") }
        items(debts.filterNot { it.settled }, key = { it.id }) { debt -> ListItem(headlineContent = { Text(debt.customerName) }, supportingContent = { Text(money(debt.amount)) }, trailingContent = { TextButton(onClick = { vm.settle(debt) }) { Text("Đã thu") } }) }
        item { Spacer(Modifier.height(12.dp)); Text("Tiện ích & cài đặt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { SettingsRow(Icons.Default.CloudUpload, "Backup lên Google Drive", "Chọn Drive trong trình lưu tệp", onBackup) }
        item { SettingsRow(Icons.Default.Restore, "Restore dữ liệu", "Chọn bản backup từ máy hoặc Drive", onRestore) }
        item { SettingsRow(Icons.Default.Print, "Máy in Bluetooth", "Tìm máy in ESC/POS đã ghép đôi") { if (Build.VERSION.SDK_INT >= 31) btPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) else printers = BluetoothPrinter.paired(context) } }
        item { SettingsRow(Icons.Default.TableView, "Xuất Excel", "File CSV mở được bằng Excel", onCsv) }
        item { SettingsRow(Icons.Default.PictureAsPdf, "Xuất PDF", "Báo cáo bán hàng", onPdf) }
        item { SettingsRow(Icons.Default.Sync, "Import WooCommerce", "Kết nối REST API cửa hàng") { woo = true } }
        item { SettingsRow(Icons.Default.Notifications, "Cảnh báo tồn kho", "Thông báo sản phẩm sắp hết") { if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else { StockNotifier.show(context, vm.products.value); onNotice("Đã bật cảnh báo tồn kho") } } }
        item { SettingsRow(Icons.Default.Info, "About", "Giới thiệu LocalPOS Pro", onAbout) }
    }
    printers?.let { list -> PrinterDialog(list, onDismiss = { printers = null }, onSelect = { p -> context.getSharedPreferences("localpos",0).edit().putString("printer",p.address).apply(); printers = null; onNotice("Đã chọn ${p.name}. Máy in này sẽ tự in sau khi thanh toán.") }) }
    if (woo) WooDialog(onDismiss = { woo = false }) { url, key, secret -> scope.launch { runCatching { withContext(Dispatchers.IO) { vm.importWooCommerce(url,key,secret) } }.onSuccess { onNotice("Đã import $it sản phẩm từ WooCommerce"); woo = false }.onFailure { onNotice("Không thể kết nối WooCommerce: ${it.message}") } } }
}

@Composable private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) { Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }, leadingContent = { Icon(icon, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }) } }

@Composable private fun PrinterDialog(printers: List<BluetoothPrinter.Printer>, onDismiss: () -> Unit, onSelect: (BluetoothPrinter.Printer) -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Máy in đã ghép đôi") }, text = { Column { if (printers.isEmpty()) Text("Chưa có máy in. Hãy ghép đôi trong Cài đặt Bluetooth của điện thoại trước.") else printers.forEach { p -> ListItem(headlineContent={Text(p.name)}, supportingContent={Text(p.address)}, modifier=Modifier.fillMaxWidth()); TextButton(onClick={onSelect(p)}){Text("Chọn ${p.name}")} } } }, confirmButton = { TextButton(onClick=onDismiss){Text("Đóng")} }) }

@Composable private fun WooDialog(onDismiss: () -> Unit, onImport: (String,String,String) -> Unit) { var url by remember{mutableStateOf("")}; var key by remember{mutableStateOf("")}; var secret by remember{mutableStateOf("")}; AlertDialog(onDismissRequest=onDismiss,title={Text("WooCommerce")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(url,{url=it},label={Text("URL cửa hàng")});OutlinedTextField(key,{key=it},label={Text("Consumer key")});OutlinedTextField(secret,{secret=it},label={Text("Consumer secret")})}},confirmButton={Button(onClick={onImport(url,key,secret)},enabled=url.startsWith("http")&&key.isNotBlank()&&secret.isNotBlank()){Text("Import")}},dismissButton={TextButton(onClick=onDismiss){Text("Hủy")}}) }

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable private fun BarcodeScannerDialog(onDismiss: () -> Unit, onCode: (String) -> Unit) {
    val context = LocalContext.current; val owner = LocalLifecycleOwner.current
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().height(520.dp)) { Column {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment=Alignment.CenterVertically) { Text("Quét mã sản phẩm",Modifier.weight(1f),fontWeight=FontWeight.Bold); IconButton(onClick=onDismiss){Icon(Icons.Default.Close,"Đóng")} }
            AndroidView(factory = { ctx ->
                val view = PreviewView(ctx); val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get(); val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                    val scanner = BarcodeScanning.getClient(); var found = false
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                        val media = proxy.image
                        if (media == null || found) { proxy.close(); return@setAnalyzer }
                        scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)).addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let { found=true; onCode(it) } }.addOnCompleteListener { proxy.close() }
                    }
                    provider.unbindAll(); provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(ctx)); view
            }, modifier=Modifier.fillMaxSize())
        } }
    }
}

@Composable private fun EmptyState(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Inbox, null, Modifier.size(40.dp)); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold); Text(subtitle, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall) } }

private fun money(value: Long): String = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(value)
private fun sameDay(a: Long, b: Long): Boolean { val f = SimpleDateFormat("yyyyMMdd", Locale.US); return f.format(Date(a)) == f.format(Date(b)) }
