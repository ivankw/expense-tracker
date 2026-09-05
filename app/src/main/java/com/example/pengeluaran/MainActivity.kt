package com.example.pengeluaran

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.pengeluaran.data.Expense
import com.example.pengeluaran.util.ApkDownloader
import com.example.pengeluaran.util.UpdateChecker
import com.example.pengeluaran.util.UpdateResult
import com.example.pengeluaran.util.UpdateStatus
import com.example.pengeluaran.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

enum class TimeFilter(val label: String) {
    THIS_MONTH("Bulan Ini"),
    LAST_7_DAYS("7 Hari Terakhir"),
    ALL("Semua")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val expenseList by viewModel.expenses.collectAsState()
    val income by viewModel.incomeFlow.collectAsState()

    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showUpToDateDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val currentVersion = remember { UpdateChecker.getCurrentVersion(context) }

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
                        Text("Pencatat Keuangan", fontWeight = FontWeight.Bold)
                        Text("Versi: $currentVersion", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu Opsi")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ekspor ke CSV (Excel)") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                exportExpensesToCsv(context, expenseList)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cek Pembaruan") },
                            leadingIcon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Memeriksa pembaruan...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    val res = UpdateChecker.checkRelease(context)
                                    updateResult = res
                                    when (res.status) {
                                        UpdateStatus.HAS_UPDATE -> showUpdateDialog = true
                                        UpdateStatus.UP_TO_DATE -> showUpToDateDialog = true
                                        UpdateStatus.ERROR -> {
                                            Toast.makeText(
                                                context,
                                                "Error: ${res.errorMessage}",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
                    label = { Text("Catat Transaksi") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { selectedTab = 0 },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTab == 0) {
                RecordExpenseTab(
                    onSaveExpense = { title, amount, category, dateMillis ->
                        viewModel.addExpense(title, amount, category, dateMillis)
                        Toast.makeText(context, "Transaksi tersimpan!", Toast.LENGTH_SHORT).show()
                        selectedTab = 1
                    }
                )
            } else {
                DashboardTab(
                    expenses = expenseList,
                    income = income,
                    onSaveIncome = { newIncome -> viewModel.saveIncome(newIncome) },
                    onDelete = { viewModel.deleteExpense(it) }
                )
            }
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordExpenseTab(
    onSaveExpense: (String, Double, String, Long) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }

    val categoryList = listOf(
        "Debt",
        "Food",
        "Gifts",
        "Home",
        "Transportation/gas",
        "Electricity",
        "Ecommerce",
        "Investment"
    )

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categoryList[1]) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Dialog Kalender
    val dateCalendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            selectedDateMillis = newCal.timeInMillis
        },
        dateCalendar.get(Calendar.YEAR),
        dateCalendar.get(Calendar.MONTH),
        dateCalendar.get(Calendar.DAY_OF_MONTH)
    )

    fun submitExpense() {
        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
        if (title.isBlank()) {
            Toast.makeText(context, "Nama pengeluaran tidak boleh kosong!", Toast.LENGTH_SHORT).show()
        } else if (parsedAmount <= 0) {
            Toast.makeText(context, "Nominal pengeluaran harus lebih dari 0!", Toast.LENGTH_SHORT).show()
        } else {
            keyboardController?.hide()
            onSaveExpense(title.trim(), parsedAmount, selectedCategory, selectedDateMillis)
            title = ""
            amount = ""
            selectedDateMillis = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Catat Pengeluaran Baru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Pilihan Tanggal Manual (DatePicker)
        OutlinedTextField(
            value = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(selectedDateMillis)),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tanggal Transaksi") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            trailingIcon = {
                TextButton(onClick = { datePickerDialog.show() }) {
                    Text("Ubah")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() }
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Nama Pengeluaran (mis. Beli Token Listrik)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { amountFocusRequester.requestFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                if (input.length <= 12 && input.all { it.isDigit() }) {
                    amount = input
                }
            },
            label = { Text("Jumlah (Rp)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submitExpense() }),
            visualTransformation = ThousandsSeparatorVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocusRequester)
        )

        ExposedDropdownMenuBox(
            expanded = isCategoryExpanded,
            onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            val selectedInfo = getCategoryInfo(selectedCategory)
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategori") },
                leadingIcon = {
                    Icon(
                        imageVector = selectedInfo.icon,
                        contentDescription = null,
                        tint = selectedInfo.color
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isCategoryExpanded,
                onDismissRequest = { isCategoryExpanded = false }
            ) {
                categoryList.forEach { categoryItem ->
                    val info = getCategoryInfo(categoryItem)
                    DropdownMenuItem(
                        text = { Text(categoryItem) },
                        leadingIcon = {
                            Icon(info.icon, contentDescription = null, tint = info.color)
                        },
                        onClick = {
                            selectedCategory = categoryItem
                            isCategoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { submitExpense() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Simpan Transaksi", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTab(
    expenses: List<Expense>,
    income: Double,
    onSaveIncome: (Double) -> Unit,
    onDelete: (Expense) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(TimeFilter.THIS_MONTH) }
    var isPrivacyMode by remember { mutableStateOf(false) } // Sensor Saldo
    var searchQuery by remember { mutableStateOf("") } // Pencarian Transaksi
    var showIncomeDialog by remember { mutableStateOf(false) }
    var incomeInput by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // 1. Filter Rentang Waktu
    val now = Calendar.getInstance()
    val timeFilteredExpenses = remember(expenses, selectedFilter) {
        expenses.filter { item ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.date }
            when (selectedFilter) {
                TimeFilter.THIS_MONTH -> {
                    itemCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            itemCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
                TimeFilter.LAST_7_DAYS -> {
                    val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                    item.date >= sevenDaysAgo
                }
                TimeFilter.ALL -> true
            }
        }
    }

    // 2. Filter Pencarian Teks
    val finalExpenses = remember(timeFilteredExpenses, searchQuery) {
        if (searchQuery.isBlank()) {
            timeFilteredExpenses
        } else {
            timeFilteredExpenses.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalExpense = finalExpenses.sumOf { it.amount }
    val saving = income - totalExpense
    val isDeficit = saving < 0

    val expenseRatio = if (income > 0) (totalExpense / income).toFloat().coerceIn(0f, 1f) else 0f
    val budgetPercent = if (income > 0) ((totalExpense / income) * 100).toInt() else 0
    val budgetBarColor = when {
        budgetPercent > 85 || isDeficit -> MaterialTheme.colorScheme.error
        budgetPercent > 60 -> Color(0xFFFFA000)
        else -> Color(0xFF4CAF50)
    }

    val distinctDays = finalExpenses.map {
        val c = Calendar.getInstance().apply { timeInMillis = it.date }
        "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }.distinct().size.coerceAtLeast(1)
    val dailyAverage = totalExpense / distinctDays

    val categoryTotals = remember(finalExpenses) {
        finalExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val groupedExpenses = remember(finalExpenses) {
        finalExpenses.groupBy { getDateGroupLabel(it.date) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- KARTU UTAMA GRADASI DENGAN TOGGLE SENSOR SALDO ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Pemasukan", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatRupiahWithPrivacy(income, isPrivacyMode),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            // Tombol Sensor Saldo (Eye Icon)
                            IconButton(onClick = { isPrivacyMode = !isPrivacyMode }) {
                                Icon(
                                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Sensor Saldo",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = {
                                incomeInput = if (income > 0) income.toLong().toString() else ""
                                showIncomeDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Pemasukan", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Pengeluaran", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatRupiahWithPrivacy(totalExpense, isPrivacyMode),
                                color = Color(0xFFF87171),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isDeficit) "Defisit" else "Sisa Tabungan",
                                color = if (isDeficit) Color(0xFFF87171) else Color(0xFF94A3B8),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = formatRupiahWithPrivacy(saving, isPrivacyMode),
                                color = if (isDeficit) Color(0xFFF87171) else Color(0xFF4ADE80),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Budget Health Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (income > 0) "Terpakai $budgetPercent% dari pemasukan" else "Belum menentukan pemasukan",
                                color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "Rata-rata: ${formatRupiahWithPrivacy(dailyAverage, isPrivacyMode)}/hari",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { expenseRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = budgetBarColor,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- FILTER WAKTU (CHIPS) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    leadingIcon = if (selectedFilter == filter) {
                        { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // --- KOLOM PENCARIAN RIWAYAT (SEARCH BAR) ---
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari nama transaksi atau kategori...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus Pencarian")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        // --- BREAKDOWN PENGELUARAN PER KATEGORI ---
        if (categoryTotals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categoryTotals) { (catName, catAmount) ->
                    val info = getCategoryInfo(catName)
                    val catPercent = if (totalExpense > 0) ((catAmount / totalExpense) * 100).toInt() else 0
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(info.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(info.icon, contentDescription = null, tint = info.color, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(catName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("${formatRupiahWithPrivacy(catAmount, isPrivacyMode)} ($catPercent%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- HISTORI TRANSAKSI DENGAN STICKY HEADER TANGGAL ---
        if (finalExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tidak ada transaksi ditemukan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Coba ubah kata kunci atau filter waktu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                groupedExpenses.forEach { (dateHeader, itemsInDate) ->
                    val dailyTotal = itemsInDate.sumOf { it.amount }
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total: ${formatRupiahWithPrivacy(dailyTotal, isPrivacyMode)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    items(itemsInDate, key = { it.id }) { item ->
                        ExpenseItem(
                            expense = item,
                            isPrivacyMode = isPrivacyMode,
                            onDelete = { expenseToDelete = item }
                        )
                    }
                }
            }
        }

        if (expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = { expenseToDelete = null },
                title = { Text("Hapus Transaksi?") },
                text = {
                    Text("Apakah Anda yakin ingin menghapus '${expenseToDelete?.title}' sebesar ${formatRupiah(expenseToDelete?.amount ?: 0.0)}?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            expenseToDelete?.let { onDelete(it) }
                            expenseToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expenseToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showIncomeDialog) {
            AlertDialog(
                onDismissRequest = { showIncomeDialog = false },
                title = { Text("Atur Pemasukan / Gaji") },
                text = {
                    OutlinedTextField(
                        value = incomeInput,
                        onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) incomeInput = it },
                        label = { Text("Nominal Gaji (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = ThousandsSeparatorVisualTransformation()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val parsed = incomeInput.toDoubleOrNull() ?: 0.0
                        onSaveIncome(parsed)
                        showIncomeDialog = false
                    }) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIncomeDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, isPrivacyMode: Boolean, onDelete: () -> Unit) {
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(expense.date))
    val categoryInfo = getCategoryInfo(expense.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryInfo.icon,
                    contentDescription = null,
                    tint = categoryInfo.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${expense.category} • $timeString", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatRupiahWithPrivacy(expense.amount, isPrivacyMode),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus Transaksi", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

data class CategoryInfo(val name: String, val icon: ImageVector, val color: Color)

fun getCategoryInfo(category: String): CategoryInfo {
    return when (category) {
        "Food" -> CategoryInfo(category, Icons.Default.Restaurant, Color(0xFFF57C00))
        "Transportation/gas" -> CategoryInfo(category, Icons.Default.DirectionsCar, Color(0xFF1976D2))
        "Electricity" -> CategoryInfo(category, Icons.Default.Bolt, Color(0xFFFFA000))
        "Home" -> CategoryInfo(category, Icons.Default.Home, Color(0xFF5D4037))
        "Ecommerce" -> CategoryInfo(category, Icons.Default.ShoppingCart, Color(0xFF7B1FA2))
        "Investment" -> CategoryInfo(category, Icons.Default.TrendingUp, Color(0xFF2E7D32))
        "Debt" -> CategoryInfo(category, Icons.Default.CreditCard, Color(0xFFD32F2F))
        "Gifts" -> CategoryInfo(category, Icons.Default.CardGiftcard, Color(0xFFE91E63))
        else -> CategoryInfo(category, Icons.Default.ReceiptLong, Color(0xFF607D8B))
    }
}

fun getDateGroupLabel(dateMillis: Long): String {
    val now = Calendar.getInstance()
    val itemCal = Calendar.getInstance().apply { timeInMillis = dateMillis }

    val isSameYear = now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR)

    return when {
        isSameYear && dayDiff == 0 -> "Hari Ini"
        isSameYear && dayDiff == 1 -> "Kemarin"
        else -> SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(dateMillis))
    }
}

// Fungsi Utilitas Ekspor ke Berkas CSV dan Share Sheet
fun exportExpensesToCsv(context: Context, expenses: List<Expense>) {
    if (expenses.isEmpty()) {
        Toast.makeText(context, "Tidak ada data pengeluaran untuk diekspor", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val fileName = "Laporan_Pengeluaran_${System.currentTimeMillis()}.csv"
        val csvFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        csvFile.bufferedWriter().use { writer ->
            writer.write("ID,Tanggal,Waktu,Judul Pengeluaran,Kategori,Nominal (Rp)\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            expenses.forEach { item ->
                val date = Date(item.date)
                val cleanTitle = item.title.replace("\"", "\"\"")
                val cleanCat = item.category.replace("\"", "\"\"")
                writer.write("${item.id},${dateFormat.format(date)},${timeFormat.format(date)},\"$cleanTitle\",\"$cleanCat\",${item.amount.toLong()}\n")
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            csvFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal mengekspor data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val out = StringBuilder()
        val n = raw.length
        val originalToTransformed = IntArray(n + 1)

        originalToTransformed[0] = 0
        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) {
                out.append('.')
            }
            out.append(raw[i])
            originalToTransformed[i + 1] = out.length
        }

        val transformedLen = out.length
        val transformedToOriginal = IntArray(transformedLen + 1)
        for (i in 0..n) {
            val transOffset = originalToTransformed[i]
            if (transOffset in 0..transformedLen) {
                transformedToOriginal[transOffset] = i
            }
        }
        for (j in 1..transformedLen) {
            if (transformedToOriginal[j] == 0 && j > 0) {
                transformedToOriginal[j] = transformedToOriginal[j - 1]
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, n)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, transformedLen)]
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
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

fun formatRupiahWithPrivacy(number: Double, hide: Boolean): String {
    return if (hide) "Rp •••••••" else formatRupiah(number)
}
