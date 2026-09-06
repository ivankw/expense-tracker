package com.example.pengeluaran

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pengeluaran.data.Expense
import com.example.pengeluaran.data.RecurringBill
import com.example.pengeluaran.util.ApkDownloader
import com.example.pengeluaran.util.UpdateChecker
import com.example.pengeluaran.util.UpdateResult
import com.example.pengeluaran.util.UpdateStatus
import com.example.pengeluaran.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Mengunci tema selalu Light Theme
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0 = Catat Pengeluaran, 1 = Tagihan Rutin, 2 = Dashboard Spreadsheet
    var selectedTab by remember { mutableIntStateOf(2) }

    val expenseList by viewModel.expenses.collectAsState()
    val billsList by viewModel.recurringBills.collectAsState()

    // State Auto Update
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showUpToDateDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val currentVersion = remember { UpdateChecker.getCurrentVersion(context) }

    // Budget Plan & Income Spreadsheet
    var paycheckAmount by remember { mutableStateOf("4750000") }
    var savingAmount by remember { mutableStateOf("4750000") }

    val categories = listOf(
        "Debt",
        "Food",
        "Gifts",
        "Home",
        "Transportation/gas",
        "Electricity",
        "Ecommerce",
        "Investment"
    )

    val plannedBudget = remember {
        mutableStateMapOf(
            "Debt" to 0.0,
            "Food" to 658000.0,
            "Gifts" to 0.0,
            "Home" to 0.0,
            "Transportation/gas" to 493500.0,
            "Electricity" to 200000.0,
            "Ecommerce" to 0.0,
            "Investment" to 0.0
        )
    }

    LaunchedEffect(Unit) {
        val res = UpdateChecker.checkRelease(context)
        updateResult = res
        if (res.status == UpdateStatus.HAS_UPDATE) {
            showUpdateDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pencatat Keuangan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Versi: $currentVersion", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cek Pembaruan") },
                            leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Memeriksa versi di repo...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    val res = UpdateChecker.checkRelease(context)
                                    updateResult = res
                                    when (res.status) {
                                        UpdateStatus.HAS_UPDATE -> showUpdateDialog = true
                                        UpdateStatus.UP_TO_DATE -> showUpToDateDialog = true
                                        UpdateStatus.ERROR -> {
                                            Toast.makeText(context, "Error: ${res.errorMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                    label = { Text("Pengeluaran") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Tagihan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> {
                    RecordExpenseTab(
                        categoryList = categories,
                        onSaveExpense = { title, amount, cat ->
                            viewModel.addExpense(title, amount, cat)
                            Toast.makeText(context, "Pengeluaran tersimpan!", Toast.LENGTH_SHORT).show()
                            selectedTab = 2
                        }
                    )
                }
                1 -> {
                    BillsTab(
                        bills = billsList,
                        categoryList = categories,
                        onAddBill = { name, amount, dueDay, cat ->
                            viewModel.addRecurringBill(name, amount, dueDay, cat)
                            Toast.makeText(context, "Tagihan ditambahkan!", Toast.LENGTH_SHORT).show()
                        },
                        onTogglePaid = { bill ->
                            viewModel.toggleBillPaidStatus(bill)
                        },
                        onDeleteBill = { bill ->
                            viewModel.deleteRecurringBill(bill)
                        }
                    )
                }
                2 -> {
                    DashboardSpreadsheetTab(
                        categories = categories,
                        expenses = expenseList,
                        paycheckAmount = paycheckAmount,
                        onPaycheckChange = { paycheckAmount = it },
                        savingAmount = savingAmount,
                        onSavingChange = { savingAmount = it },
                        plannedBudget = plannedBudget,
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                }
            }
        }

        // Dialog Pembaruan Tersedia
        if (showUpdateDialog && updateResult != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("Pembaruan Tersedia") },
                text = {
                    Column {
                        Text("Versi Terpasang: ${updateResult?.currentVersion}")
                        Text("Versi di Repo: ${updateResult?.latestVersion}\n", fontWeight = FontWeight.Bold)
                        Text("Catatan Pembaruan:")
                        Text(updateResult?.releaseNotes ?: "-", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUpdateDialog = false
                            val downloadUrl = updateResult?.downloadUrl
                            if (!downloadUrl.isNullOrBlank()) {
                                ApkDownloader.downloadAndInstall(
                                    context = context,
                                    downloadUrl = downloadUrl,
                                    fileName = "ExpenseTracker-${updateResult?.latestVersion}.apk"
                                )
                            }
                        }
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Nanti Saja")
                    }
                }
            )
        }

        // Dialog Versi Terbaru
        if (showUpToDateDialog && updateResult != null) {
            AlertDialog(
                onDismissRequest = { showUpToDateDialog = false },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Versi app sudah paling baru.") },
                text = {
                    Column {
                        Text("Versi aplikasi saat ini: ${updateResult?.currentVersion}")
                        Text("Versi rilis di repository: ${updateResult?.latestVersion}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showUpToDateDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. TAB PENGELUARAN HARIAN
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordExpenseTab(
    categoryList: List<String>,
    onSaveExpense: (String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categoryList.firstOrNull() ?: "Food") }
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Catat Pengeluaran Baru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Nama Pengeluaran (mis. Makan Siang)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) amount = input
            },
            label = { Text("Jumlah (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = VisualTransformation.None,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategori") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                categoryList.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            selectedCategory = cat
                            isExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val parsed = amount.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && parsed > 0) {
                    onSaveExpense(title, parsed, selectedCategory)
                    title = ""
                    amount = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Simpan Pengeluaran", fontWeight = FontWeight.Bold)
        }
    }
}

// -------------------------------------------------------------------------------------
// 2. TAB PENCATAT TAGIHAN RUTIN (RECURRING BILLS)
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsTab(
    bills: List<RecurringBill>,
    categoryList: List<String>,
    onAddBill: (String, Double, Int, String) -> Unit,
    onTogglePaid: (RecurringBill) -> Unit,
    onDeleteBill: (RecurringBill) -> Unit
) {
    var billName by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Electricity") }
    var isExpanded by remember { mutableStateOf(false) }

    val totalUnpaid = bills.filter { !it.isPaidThisMonth }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Pencatat Tagihan Rutin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Tagihan Belum Dibayar", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatRupiah(totalUnpaid),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = billName,
            onValueChange = { billName = it },
            label = { Text("Nama Tagihan (mis. Wifi Indihome)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = billAmount,
                onValueChange = { if (it.all { c -> c.isDigit() }) billAmount = it },
                label = { Text("Nominal (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = dueDay,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && (input.toIntOrNull() ?: 0) <= 31) dueDay = input
                },
                label = { Text("Tgl Jatuh Tempo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategori Alokasi") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                categoryList.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            selectedCategory = cat
                            isExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val parsedAmount = billAmount.toDoubleOrNull() ?: 0.0
                val parsedDay = dueDay.toIntOrNull() ?: 1
                if (billName.isNotBlank() && parsedAmount > 0) {
                    onAddBill(billName, parsedAmount, parsedDay, selectedCategory)
                    billName = ""
                    billAmount = ""
                    dueDay = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tambah Tagihan")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Daftar Tagihan (${bills.size})", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (bills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("Belum ada tagihan terdaftar", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bills, key = { it.id }) { bill ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (bill.isPaidThisMonth) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Checkbox(
                                checked = bill.isPaidThisMonth,
                                onCheckedChange = { onTogglePaid(bill) }
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(bill.name, fontWeight = FontWeight.Bold)
                                Text("${bill.category} • Jatuh Tempo tgl ${bill.dueDay}", fontSize = 12.sp, color = Color.DarkGray)
                                Text(
                                    formatRupiah(bill.amount),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (bill.isPaidThisMonth) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            IconButton(onClick = { onDeleteBill(bill) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus")
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3. TAB DASHBOARD SPREADSHEET (PERSONAL EXPENSES TRACKER)
// -------------------------------------------------------------------------------------
@Composable
fun DashboardSpreadsheetTab(
    categories: List<String>,
    expenses: List<Expense>,
    paycheckAmount: String,
    onPaycheckChange: (String) -> Unit,
    savingAmount: String,
    onSavingChange: (String) -> Unit,
    plannedBudget: MutableMap<String, Double>,
    onDeleteExpense: (Expense) -> Unit
) {
    val totalActual = expenses.sumOf { it.amount }
    val totalPlanned = plannedBudget.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "PERSONAL EXPENSES TRACKER",
            color = Color(0xFFD35400),
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text("Income:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Paycheck", fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
            OutlinedTextField(
                value = paycheckAmount,
                onValueChange = { if (it.all { c -> c.isDigit() }) onPaycheckChange(it) },
                prefix = { Text("Rp ", fontSize = 12.sp) },
                modifier = Modifier.width(130.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("Saving", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    formatRupiah(savingAmount.toDoubleOrNull() ?: 0.0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Expenses:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Planned ${formatRupiah(totalPlanned)}", fontSize = 13.sp, color = Color.DarkGray)
            Text("Actual ${formatRupiah(totalActual)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Expenses Table",
            color = Color(0xFFD35400),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Spreadsheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F0F0))
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Category", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
            Text("Planned", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
            Text("Actual", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
            Text("%", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        }

        // Spreadsheet Data Rows
        categories.forEach { cat ->
            val actualCat = expenses.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
            val plannedCat = plannedBudget[cat] ?: 0.0
            val percentage = if (actualCat > 0) 100 else 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color(0xFFE0E0E0))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(cat, fontSize = 12.sp, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Medium)
                Text(formatRupiah(plannedCat), fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                Text(
                    if (actualCat > 0) formatRupiah(actualCat) else "-",
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1.3f),
                    textAlign = TextAlign.End,
                    fontWeight = if (actualCat > 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (actualCat > 0) Color(0xFFB71C1C) else Color.Gray
                )
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (percentage > 0) {
                        Text(
                            "$percentage%",
                            fontSize = 10.sp,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier
                                .background(Color(0xFFC8E6C9), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    } else {
                        Text("-", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Histori Transaksi (${expenses.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Text("Belum ada pengeluaran yang dicatat.", fontSize = 12.sp, color = Color.Gray)
        } else {
            expenses.forEach { item ->
                ExpenseItemRow(expense = item, onDelete = { onDeleteExpense(item) })
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun ExpenseItemRow(expense: Expense, onDelete: () -> Unit) {
    val dateString = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(expense.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${expense.category} • $dateString", fontSize = 11.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatRupiah(expense.amount),
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun formatRupiah(number: Double): String {
    val symbols = DecimalFormatSymbols(Locale("in", "ID")).apply {
        currencySymbol = "Rp "
        groupingSeparator = '.'
    }
    val formatter = DecimalFormat("Rp #,###", symbols)
    return if (number == 0.0) "Rp 0" else formatter.format(number)
}
