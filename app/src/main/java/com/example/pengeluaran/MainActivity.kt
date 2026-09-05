package com.example.pengeluaran

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.pengeluaran.data.Expense
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTab == 0) {
                RecordExpenseTab(
                    onSaveExpense = { title, amount, category ->
                        viewModel.addExpense(title, amount, category)
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
    onSaveExpense: (String, Double, String) -> Unit
) {
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
            label = { Text("Nama Pengeluaran (mis. Beli Token Listrik)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) {
                    amount = input
                }
            },
            label = { Text("Jumlah (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = VisualTransformation.None,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = isCategoryExpanded,
            onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kategori") },
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
                    DropdownMenuItem(
                        text = { Text(categoryItem) },
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
            onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && parsedAmount > 0) {
                    onSaveExpense(title, parsedAmount, selectedCategory)
                    title = ""
                    amount = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Simpan Transaksi", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardTab(
    expenses: List<Expense>,
    income: Double,
    onSaveIncome: (Double) -> Unit,
    onDelete: (Expense) -> Unit
) {
    val totalExpense = expenses.sumOf { it.amount }
    val saving = income - totalExpense

    var showIncomeDialog by remember { mutableStateOf(false) }
    var incomeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pemasukan / Gaji", style = MaterialTheme.typography.labelMedium)
                        Text(formatRupiah(income), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = {
                        incomeInput = if (income > 0) income.toLong().toString() else ""
                        showIncomeDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Pemasukan")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Pengeluaran", style = MaterialTheme.typography.labelSmall)
                        Text(formatRupiah(totalExpense), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sisa Tabungan", style = MaterialTheme.typography.labelSmall)
                        Text(formatRupiah(saving), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Histori Transaksi (${expenses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada pengeluaran dicatat", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(expenses, key = { it.id }) { item ->
                    ExpenseItem(expense = item, onDelete = { onDelete(item) })
                }
            }
        }

        if (showIncomeDialog) {
            AlertDialog(
                onDismissRequest = { showIncomeDialog = false },
                title = { Text("Atur Pemasukan / Gaji") },
                text = {
                    OutlinedTextField(
                        value = incomeInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) incomeInput = it },
                        label = { Text("Nominal Gaji (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
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
fun ExpenseItem(expense: Expense, onDelete: () -> Unit) {
    val dateString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(expense.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${expense.category} • $dateString", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRupiah(expense.amount),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus Transaksi")
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
