package com.example.pengeluaran

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

    fun submitExpense() {
        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
        if (title.isBlank()) {
            Toast.makeText(context, "Nama pengeluaran tidak boleh kosong!", Toast.LENGTH_SHORT).show()
        } else if (parsedAmount <= 0) {
            Toast.makeText(context, "Nominal pengeluaran harus lebih dari 0!", Toast.LENGTH_SHORT).show()
        } else {
            keyboardController?.hide()
            onSaveExpense(title.trim(), parsedAmount, selectedCategory)
            title = ""
            amount = ""
        }
    }

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

@Composable
fun DashboardTab(
    expenses: List<Expense>,
    income: Double,
    onSaveIncome: (Double) -> Unit,
    onDelete: (Expense) -> Unit
) {
    val totalExpense = expenses.sumOf { it.amount }
    val saving = income - totalExpense
    val isDeficit = saving < 0

    var showIncomeDialog by remember { mutableStateOf(false) }
    var incomeInput by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

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
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Pengeluaran", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = formatRupiah(totalExpense),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isDeficit) "Status: Defisit" else "Sisa Tabungan",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDeficit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = formatRupiah(saving),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeficit) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada riwayat transaksi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mulai catat transaksi untuk memantau keuanganmu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(expenses, key = { it.id }) { item ->
                    ExpenseItem(expense = item, onDelete = { expenseToDelete = item })
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
fun ExpenseItem(expense: Expense, onDelete: () -> Unit) {
    val dateString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(expense.date))
    val categoryInfo = getCategoryInfo(expense.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text("${expense.category} • $dateString", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatRupiah(expense.amount),
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
