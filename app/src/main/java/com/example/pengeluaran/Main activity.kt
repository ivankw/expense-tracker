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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.pengeluaran.data.CategoryBudget
import com.example.pengeluaran.data.Expense
import com.example.pengeluaran.data.RecurringBill
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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFF0F9FF),
    secondary = Color(0xFF818CF8),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF6366F1),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF1E293B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8)
)

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemInDark) }

            MaterialTheme(
                colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { isDarkMode = !isDarkMode }
                    )
                }
            }
        }
    }
}

enum class TimeFilter(val label: String) {
    MONTHLY("Bulanan"),
    LAST_7_DAYS("7 Hari"),
    ALL("Semua")
}

val defaultCategories = listOf(
    "Debt",
    "Food",
    "Gifts",
    "Home",
    "Transportation/gas",
    "Electricity",
    "Ecommerce",
    "Investment",
    "Traveling"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    val expenseList by viewModel.expenses.collectAsState()
    val recurringBills by viewModel.recurringBills.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val income by viewModel.incomeFlow.collectAsState()

    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showUpToDateDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti Tema"
                        )
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu Opsi")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isDarkMode) "Tema Terang" else "Tema Gelap") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleDarkMode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor Laporan (CSV)") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showExportDialog = true
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
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Catat") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Autorenew, contentDescription = null) },
                    label = { Text("Tagihan") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    DashboardReportsTab(
                        expenses = expenseList,
                        income = income,
                        budgets = categoryBudgets,
                        onSaveIncome = { newIncome -> viewModel.saveIncome(newIncome) },
                        onSetCategoryBudget = { cat, limit -> viewModel.setCategoryBudget(cat, limit) },
                        onNavigateToRecord = { selectedTab = 1 }
                    )
                }
                1 -> {
                    RecordAndHistoryTab(
                        expenses = expenseList,
                        onSaveExpense = { title, amount, category, dateMillis ->
                            viewModel.addExpense(title, amount, category, dateMillis)
                            Toast.makeText(context, "Transaksi tersimpan!", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateExpense = { updatedExpense ->
                            viewModel.updateExpense(updatedExpense)
                            Toast.makeText(context, "Perubahan transaksi tersimpan!", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                }
                2 -> {
                    RecurringBillsTab(
                        recurringBills = recurringBills,
                        onAddBill = { title, amount, category, dueDay ->
                            viewModel.addRecurringBill(title, amount, category, dueDay)
                            Toast.makeText(context, "Tagihan rutin ditambahkan!", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateBill = { viewModel.updateRecurringBill(it) },
                        onDeleteBill = { viewModel.deleteRecurringBill(it) },
                        onPayBill = { bill ->
                            viewModel.payRecurringBill(bill)
                            Toast.makeText(context, "'${bill.title}' telah dicatat ke pengeluaran!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Ekspor Laporan CSV") },
                text = { Text("Pilih data transaksi yang ingin Anda unduh dan bagikan:") },
                confirmButton = {
                    Button(onClick = {
                        showExportDialog = false
                        exportExpensesToCsv(context, expenseList, "Semua_Transaksi")
                    }) {
                        Text("Ekspor Semua")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        val currentCal = Calendar.getInstance()
                        val thisMonthExpenses = expenseList.filter {
                            val c = Calendar.getInstance().apply { timeInMillis = it.date }
                            c.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                                    c.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)
                        }
                        exportExpensesToCsv(context, thisMonthExpenses, "Bulan_Ini")
                    }) {
                        Text("Bulan Ini Saja")
                    }
                }
            )
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
fun DashboardReportsTab(
    expenses: List<Expense>,
    income: Double,
    budgets: List<CategoryBudget>,
    onSaveIncome: (Double) -> Unit,
    onSetCategoryBudget: (String, Double) -> Unit,
    onNavigateToRecord: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(TimeFilter.MONTHLY) }
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var isPrivacyMode by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var incomeInput by remember { mutableStateOf("") }
    var selectedCategoryForDetail by remember { mutableStateOf<String?>(null) }
    var categoryForBudgetDialog by remember { mutableStateOf<String?>(null) }

    val now = Calendar.getInstance()
    val isCurrentMonth = selectedCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            selectedCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)

    val budgetMap = remember(budgets) { budgets.associate { it.category to it.budgetAmount } }

    val filteredExpenses = remember(expenses, selectedFilter, selectedCalendar) {
        expenses.filter { item ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.date }
            when (selectedFilter) {
                TimeFilter.MONTHLY -> {
                    itemCal.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                            itemCal.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)
                }
                TimeFilter.LAST_7_DAYS -> {
                    val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                    item.date >= sevenDaysAgo
                }
                TimeFilter.ALL -> true
            }
        }
    }

    val totalExpense = filteredExpenses.sumOf { it.amount }
    val saving = income - totalExpense
    val isDeficit = saving < 0

    val expenseRatio = if (income > 0) (totalExpense / income).toFloat().coerceIn(0f, 1f) else 0f
    val budgetPercent = if (income > 0) ((totalExpense / income) * 100).toInt() else 0
    val budgetBarColor = when {
        budgetPercent > 85 || isDeficit -> MaterialTheme.colorScheme.error
        budgetPercent > 60 -> Color(0xFFFFA000)
        else -> Color(0xFF4CAF50)
    }

    val daysInMonth = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val remainingDaysInMonth = (daysInMonth - now.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
    val safeToSpendDaily = if (isCurrentMonth && saving > 0) saving / remainingDaysInMonth else 0.0

    val distinctDays = filteredExpenses.map {
        val c = Calendar.getInstance().apply { timeInMillis = it.date }
        "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }.distinct().size.coerceAtLeast(1)
    val dailyAverage = totalExpense / distinctDays

    val categoryTotals = remember(filteredExpenses) {
        filteredExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val needsCategories = setOf("Food", "Electricity", "Transportation/gas", "Home", "Debt")
    val wantsCategories = setOf("Ecommerce", "Gifts", "Traveling")
    val totalNeeds = filteredExpenses.filter { it.category in needsCategories }.sumOf { it.amount }
    val totalWants = filteredExpenses.filter { it.category in wantsCategories }.sumOf { it.amount }
    val totalInvestSavings = filteredExpenses.filter { it.category == "Investment" }.sumOf { it.amount } + if (saving > 0) saving else 0.0

    val denominator = if (income > 0) income else totalExpense.coerceAtLeast(1.0)
    val needsPercent = ((totalNeeds / denominator) * 100).toInt()
    val wantsPercent = ((totalWants / denominator) * 100).toInt()
    val investPercent = ((totalInvestSavings / denominator) * 100).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Pemasukan / Gaji", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = formatRupiahWithPrivacy(income, isPrivacyMode),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    income <= 0 -> Color(0xFF334155)
                                    isDeficit -> Color(0xFF7F1D1D)
                                    budgetPercent > 80 -> Color(0xFF78350F)
                                    else -> Color(0xFF14532D)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isDeficit -> Icons.Default.Warning
                                            budgetPercent > 80 -> Icons.Default.Info
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            income <= 0 -> Color(0xFF94A3B8)
                                            isDeficit -> Color(0xFFFCA5A5)
                                            budgetPercent > 80 -> Color(0xFFFDE68A)
                                            else -> Color(0xFF86EFAC)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            income <= 0 -> "Belum Diatur"
                                            isDeficit -> "Defisit"
                                            budgetPercent > 80 -> "Waspada"
                                            else -> "Aman"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            income <= 0 -> Color(0xFF94A3B8)
                                            isDeficit -> Color(0xFFFCA5A5)
                                            budgetPercent > 80 -> Color(0xFFFDE68A)
                                            else -> Color(0xFF86EFAC)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                    text = if (isDeficit) "Total Defisit" else "Sisa Tabungan",
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

                        Spacer(modifier = Modifier.height(14.dp))

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (income > 0) "Terpakai $budgetPercent% dari gaji" else "Pemasukan belum disetel",
                                    color = Color(0xFFCBD5E1),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = when {
                                        selectedFilter == TimeFilter.MONTHLY && isCurrentMonth && income > 0 -> {
                                            "Batas aman: ${formatRupiahWithPrivacy(safeToSpendDaily, isPrivacyMode)}/hari"
                                        }
                                        else -> "Rata-rata: ${formatRupiahWithPrivacy(dailyAverage, isPrivacyMode)}/hari"
                                    },
                                    color = if (isDeficit) Color(0xFFF87171) else Color(0xFF94A3B8),
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isPrivacyMode = !isPrivacyMode },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                            ) {
                                Icon(
                                    imageVector = if (isPrivacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPrivacyMode) "Tampilkan" else "Sembunyikan Saldo", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = {
                                    incomeInput = if (income > 0) income.toLong().toString() else ""
                                    showIncomeDialog = true
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ubah Gaji", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }

                if (selectedFilter == TimeFilter.MONTHLY) {
                    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(selectedCalendar.time)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val c = selectedCalendar.clone() as Calendar
                                c.add(Calendar.MONTH, -1)
                                selectedCalendar = c
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Sebelumnya")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }

                            Row {
                                IconButton(onClick = {
                                    val c = selectedCalendar.clone() as Calendar
                                    c.add(Calendar.MONTH, 1)
                                    selectedCalendar = c
                                }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Berikutnya")
                                }
                                if (!isCurrentMonth) {
                                    IconButton(onClick = { selectedCalendar = Calendar.getInstance() }) {
                                        Icon(Icons.Default.Today, contentDescription = "Kembali ke Hari Ini", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alokasi Pos Finansial (Aturan 70 / 20 / 10)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Evaluasi terhadap target pemasukan bulanan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(14.dp))

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kebutuhan & Tagihan (Target ≤70%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text("${formatRupiah(totalNeeds)} ($needsPercent%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (needsPercent > 70) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (needsPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (needsPercent > 70) MaterialTheme.colorScheme.error else Color(0xFF1E88E5),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gaya Hidup & Traveling (Target ≤20%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text("${formatRupiah(totalWants)} ($wantsPercent%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (wantsPercent > 20) MaterialTheme.colorScheme.error else Color(0xFFFFA000))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (wantsPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (wantsPercent > 20) MaterialTheme.colorScheme.error else Color(0xFFFFA000),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tabungan & Investasi (Target ≥10%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text("${formatRupiah(totalInvestSavings)} ($investPercent%)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (investPercent >= 10) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (investPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF2E7D32),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Belum ada transaksi pada periode ini", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Mulai catat transaksi untuk melihat laporan dan diagram.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToRecord, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Catat Pengeluaran Sekarang")
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Proporsi Pengeluaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                            Canvas(modifier = Modifier.size(150.dp)) {
                                val strokeWidth = 24.dp.toPx()
                                var startAngle = -90f

                                if (totalExpense == 0.0) {
                                    drawArc(
                                        color = Color(0xFF334155),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth)
                                    )
                                } else {
                                    categoryTotals.forEach { (catName, catAmount) ->
                                        val sweep = ((catAmount / totalExpense) * 360f).toFloat()
                                        val color = getCategoryInfo(catName).color
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth)
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = formatRupiahWithPrivacy(totalExpense, isPrivacyMode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plafon Anggaran per Kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ketuk untuk atur batas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            items(categoryTotals) { (catName, catAmount) ->
                val info = getCategoryInfo(catName)
                val budgetLimit = budgetMap[catName] ?: 0.0
                val hasBudget = budgetLimit > 0
                val ratio = if (hasBudget) (catAmount / budgetLimit).toFloat() else if (totalExpense > 0) (catAmount / totalExpense).toFloat() else 0f
                val isOverbudget = hasBudget && catAmount > budgetLimit

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategoryForDetail = catName },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(info.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(info.icon, contentDescription = null, tint = info.color, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(catName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                if (hasBudget) {
                                    Text(
                                        text = "Terpakai: ${formatRupiah(catAmount)} / ${formatRupiah(budgetLimit)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverbudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    Text("Belum ada plafon anggaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { categoryForBudgetDialog = catName }) {
                                    Icon(Icons.Default.Tune, contentDescription = "Atur Plafon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (isOverbudget) MaterialTheme.colorScheme.error else if (hasBudget && ratio > 0.8f) Color(0xFFFFA000) else info.color,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        if (isOverbudget) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Overbudget sebesar ${formatRupiah(catAmount - budgetLimit)}!",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedCategoryForDetail != null) {
        val categoryName = selectedCategoryForDetail!!
        val categoryInfo = getCategoryInfo(categoryName)
        val categoryItems = filteredExpenses.filter { it.category == categoryName }
        val totalCatAmount = categoryItems.sumOf { it.amount }

        ModalBottomSheet(
            onDismissRequest = { selectedCategoryForDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(categoryInfo.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryInfo.icon, contentDescription = null, tint = categoryInfo.color, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${categoryItems.size} Transaksi • Total: ${formatRupiah(totalCatAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    TextButton(onClick = {
                        categoryForBudgetDialog = categoryName
                        selectedCategoryForDetail = null
                    }) {
                        Text("Atur Plafon")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryItems) { item ->
                        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(item.date))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(
                                formatRupiah(item.amount),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (categoryForBudgetDialog != null) {
        val cat = categoryForBudgetDialog!!
        var budgetInput by remember(cat) {
            val existing = budgetMap[cat] ?: 0.0
            mutableStateOf(if (existing > 0) existing.toLong().toString() else "")
        }

        AlertDialog(
            onDismissRequest = { categoryForBudgetDialog = null },
            title = { Text("Plafon Anggaran: $cat") },
            text = {
                Column {
                    Text("Tentukan batas maksimal pengeluaran bulanan untuk kategori ini:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 12) budgetInput = digits
                        },
                        label = { Text("Batas Anggaran (Rp)") },
                        placeholder = { Text("0 = Tanpa batas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = budgetInput.toDoubleOrNull() ?: 0.0
                    onSetCategoryBudget(cat, parsed)
                    categoryForBudgetDialog = null
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryForBudgetDialog = null }) {
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
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 12) incomeInput = digits
                    },
                    label = { Text("Nominal Gaji (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordAndHistoryTab(
    expenses: List<Expense>,
    onSaveExpense: (String, Double, String, Long) -> Unit,
    onUpdateExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(defaultCategories[1]) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var searchQuery by remember { mutableStateOf("") }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

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

    val filteredExpenses = remember(expenses, searchQuery) {
        if (searchQuery.isBlank()) {
            expenses
        } else {
            expenses.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses.groupBy { getDateGroupLabel(it.date) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Catat Transaksi Pengeluaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
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
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama Pengeluaran (mis. Tiket Kereta / Hotel)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { amountFocusRequester.requestFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 12) amount = digits
                },
                label = { Text("Jumlah (Rp)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitExpense() }),
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
            )
        }

        item {
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
                    defaultCategories.forEach { categoryItem ->
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
        }

        item {
            Button(
                onClick = { submitExpense() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Simpan Transaksi", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Riwayat Transaksi (${expenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari transaksi...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Belum ada riwayat transaksi", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
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
                            text = "Total: ${formatRupiah(dailyTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                items(itemsInDate, key = { it.id }) { item ->
                    ExpenseItem(
                        expense = item,
                        onEdit = { expenseToEdit = item },
                        onDelete = { expenseToDelete = item }
                    )
                }
            }
        }
    }

    if (expenseToEdit != null) {
        val editing = expenseToEdit!!
        var editTitle by remember(editing) { mutableStateOf(editing.title) }
        var editAmount by remember(editing) { mutableStateOf(editing.amount.toLong().toString()) }
        var editCategory by remember(editing) { mutableStateOf(editing.category) }
        var editDateMillis by remember(editing) { mutableLongStateOf(editing.date) }
        var isEditCatExpanded by remember { mutableStateOf(false) }

        val editCal = Calendar.getInstance().apply { timeInMillis = editDateMillis }
        val editDatePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val c = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                editDateMillis = c.timeInMillis
            },
            editCal.get(Calendar.YEAR),
            editCal.get(Calendar.MONTH),
            editCal.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { expenseToEdit = null },
            title = { Text("Edit Transaksi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(editDateMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tanggal") },
                        trailingIcon = {
                            TextButton(onClick = { editDatePicker.show() }) { Text("Ubah") }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { editDatePicker.show() }
                    )
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Nama Pengeluaran") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 12) editAmount = digits
                        },
                        label = { Text("Jumlah (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = isEditCatExpanded,
                        onExpandedChange = { isEditCatExpanded = !isEditCatExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEditCatExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isEditCatExpanded,
                            onDismissRequest = { isEditCatExpanded = false }
                        ) {
                            defaultCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        editCategory = cat
                                        isEditCatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = editAmount.toDoubleOrNull() ?: 0.0
                    if (editTitle.isNotBlank() && parsed > 0) {
                        onUpdateExpense(
                            editing.copy(
                                title = editTitle.trim(),
                                amount = parsed,
                                category = editCategory,
                                date = editDateMillis
                            )
                        )
                        expenseToEdit = null
                    }
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToEdit = null }) {
                    Text("Batal")
                }
            }
        )
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
                        expenseToDelete?.let { onDeleteExpense(it) }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsTab(
    recurringBills: List<RecurringBill>,
    onAddBill: (String, Double, String, Int) -> Unit,
    onUpdateBill: (RecurringBill) -> Unit,
    onDeleteBill: (RecurringBill) -> Unit,
    onPayBill: (RecurringBill) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<RecurringBill?>(null) }
    var billToDelete by remember { mutableStateOf<RecurringBill?>(null) }

    val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val totalObligation = recurringBills.sumOf { it.amount }
    val paidObligation = recurringBills.filter { it.lastPaidMonthYear == currentMonthYear }.sumOf { it.amount }
    val remainingObligation = totalObligation - paidObligation

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Tagihan Rutin")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Tagihan & Pengeluaran Pasti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Pantau jatuh tempo langganan, cicilan, dan utilitas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Tagihan Bulanan", style = MaterialTheme.typography.labelSmall)
                                Text(formatRupiah(totalObligation), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Sisa Belum Dibayar", style = MaterialTheme.typography.labelSmall)
                                Text(formatRupiah(remainingObligation), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (remainingObligation > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }

            if (recurringBills.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada daftar tagihan rutin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Tambahkan internet, listrik PLN, ShopeePayLater, atau kos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                items(recurringBills, key = { it.id }) { bill ->
                    val isPaidThisMonth = bill.lastPaidMonthYear == currentMonthYear
                    val catInfo = getCategoryInfo(bill.category)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(catInfo.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(catInfo.icon, contentDescription = null, tint = catInfo.color, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bill.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Jatuh tempo setiap tgl ${bill.dueDay} • ${bill.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Text(
                                    text = formatRupiah(bill.amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPaidThisMonth) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Lunas Bulan Ini", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { onPayBill(bill) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Bayar & Catat", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Row {
                                    IconButton(onClick = { billToEdit = bill }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { billToDelete = bill }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            RecurringBillDialog(
                title = "Tambah Tagihan Rutin",
                initialTitle = "",
                initialAmount = "",
                initialCategory = defaultCategories[0],
                initialDueDay = "5",
                onDismiss = { showAddDialog = false },
                onConfirm = { name, amt, cat, day ->
                    onAddBill(name, amt, cat, day)
                    showAddDialog = false
                }
            )
        }

        if (billToEdit != null) {
            val b = billToEdit!!
            RecurringBillDialog(
                title = "Edit Tagihan Rutin",
                initialTitle = b.title,
                initialAmount = b.amount.toLong().toString(),
                initialCategory = b.category,
                initialDueDay = b.dueDay.toString(),
                onDismiss = { billToEdit = null },
                onConfirm = { name, amt, cat, day ->
                    onUpdateBill(b.copy(title = name, amount = amt, category = cat, dueDay = day))
                    billToEdit = null
                }
            )
        }

        if (billToDelete != null) {
            AlertDialog(
                onDismissRequest = { billToDelete = null },
                title = { Text("Hapus Tagihan Rutin?") },
                text = { Text("Hapus '${billToDelete?.title}' dari daftar tagihan berkala?") },
                confirmButton = {
                    Button(
                        onClick = {
                            billToDelete?.let { onDeleteBill(it) }
                            billToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { billToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillDialog(
    title: String,
    initialTitle: String,
    initialAmount: String,
    initialCategory: String,
    initialDueDay: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialTitle) }
    var amount by remember { mutableStateOf(initialAmount) }
    var category by remember { mutableStateOf(initialCategory) }
    var dueDay by remember { mutableStateOf(initialDueDay) }
    var isCatExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Tagihan (mis. WiFi, ShopeePayLater)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 12) amount = digits
                    },
                    label = { Text("Nominal Tagihan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.toIntOrNull() in 1..31 || digits.isEmpty()) dueDay = digits
                    },
                    label = { Text("Tanggal Jatuh Tempo (1 - 31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = isCatExpanded,
                    onExpandedChange = { isCatExpanded = !isCatExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false }
                    ) {
                        defaultCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isCatExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val day = dueDay.toIntOrNull() ?: 1
                if (name.isNotBlank() && amt > 0) {
                    onConfirm(name.trim(), amt, category, day)
                }
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ExpenseItem(expense: Expense, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryInfo.icon,
                    contentDescription = null,
                    tint = categoryInfo.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${expense.category} • $timeString", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatRupiah(expense.amount),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Transaksi", tint = MaterialTheme.colorScheme.outline)
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
        "Traveling" -> CategoryInfo(category, Icons.Default.Flight, Color(0xFF00897B))
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

fun exportExpensesToCsv(context: Context, expenses: List<Expense>, prefixName: String = "Laporan") {
    if (expenses.isEmpty()) {
        Toast.makeText(context, "Tidak ada data pengeluaran untuk diekspor", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val fileName = "${prefixName}_${System.currentTimeMillis()}.csv"
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
