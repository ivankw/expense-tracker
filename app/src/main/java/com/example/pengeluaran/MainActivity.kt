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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    ExpenseScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Daftar Kategori lengkap dengan Traveling
    val categoryList = remember {
        listOf(
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
    }

    // State untuk form input transaksi
    val expenseList by viewModel.expenses.collectAsState()
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categoryList[1]) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // State untuk Dashboard Keuangan
    var paycheckInput by remember { mutableStateOf("4000000") }
    val plannedBudgets = remember {
        mutableStateMapOf<String, String>().apply {
            put("Debt", "2000000")
            put("Food", "658000")
            put("Gifts", "0")
            put("Home", "0")
            put("Transportation/gas", "493500")
            put("Electricity", "200000")
            put("Ecommerce", "0")
            put("Investment", "0")
            put("Traveling", "0")
        }
    }

    // Kalkulasi Data Dashboard
    val paycheck = paycheckInput.toDoubleOrNull() ?: 0.0
    val totalPlanned = categoryList.sumOf { plannedBudgets[it]?.toDoubleOrNull() ?: 0.0 }
    val totalActual = expenseList.sumOf { it.amount }
    val totalSaving = paycheck - totalActual
    val totalRemain = totalPlanned - totalActual

    // State update aplikasi
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
                                Toast.makeText(context, "Memeriksa versi di repo...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    val res = UpdateChecker.checkRelease(context)
                                    updateResult = res
                                    when (res.status) {
                                        UpdateStatus.HAS_UPDATE -> showUpdateDialog = true
                                        UpdateStatus.UP_TO_DATE -> showUpToDateDialog = true
                                        UpdateStatus.ERROR -> Toast.makeText(
                                            context,
                                            "Error: ${res.errorMessage}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= DASHBOARD HEADER =================
            item {
                Text(
                    text = "PERSONAL EXPENSES TRACKER",
                    color = Color(0xFFE65100),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic
                )
            }

            // ================= SEKSI INCOME =================
            item {
                Column {
                    Text(
                        "Income:",
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A365D),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Paycheck  ", fontWeight = FontWeight.Bold, color = Color(0xFF1A365D))
                            BasicNumberField(
                                value = paycheckInput,
                                onValueChange = { paycheckInput = it },
                                modifier = Modifier.width(130.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Saving  ", fontWeight = FontWeight.Bold, color = Color(0xFF1A365D))
                            Text(formatRupiah(totalSaving), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ================= SEKSI SUMMARY BAR =================
            item {
                Column {
                    Divider(color = Color(0xFF1A365D), thickness = 2.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Expenses:",
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A365D),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Planned vs Actual
                    val maxProgress = maxOf(totalPlanned, totalActual, 1.0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Planned", modifier = Modifier.width(70.dp), fontWeight = FontWeight.SemiBold)
                        Text(formatRupiah(totalPlanned), modifier = Modifier.width(110.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(Color.LightGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((totalPlanned / maxProgress).toFloat().coerceIn(0f, 1f))
                                    .background(Color(0xFF90A4AE))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Actual", modifier = Modifier.width(70.dp), fontWeight = FontWeight.SemiBold)
                        Text(formatRupiah(totalActual), modifier = Modifier.width(110.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(Color.LightGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((totalActual / maxProgress).toFloat().coerceIn(0f, 1f))
                                    .background(Color(0xFF263238))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFF1A365D), thickness = 2.dp)
                }
            }

            // ================= TABEL RINCIAN EXPENSES =================
            item {
                Column {
                    Text(
                        "Expenses",
                        color = Color(0xFFE65100),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = Color(0xFF90A4AE), thickness = 1.dp)

                    // Header Tabel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Planned (Edit)", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Actual", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Remain", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("%", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Baris Total
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Totals", fontStyle = FontStyle.Italic, modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(formatRupiah(totalPlanned), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(formatRupiah(totalActual), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(formatRupiah(totalRemain), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val totalPct = if (totalPlanned > 0) ((totalRemain / totalPlanned) * 100).toInt() else 0
                        Text("$totalPct%", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Divider(color = Color.LightGray, thickness = 1.dp)

                    // Baris Tiap Kategori
                    categoryList.forEach { category ->
                        val plannedVal = plannedBudgets[category]?.toDoubleOrNull() ?: 0.0
                        val actualVal = expenseList.filter { it.category.equals(category, ignoreCase = true) }.sumOf { it.amount }
                        val remainVal = plannedVal - actualVal
                        val percentage = if (plannedVal > 0) ((remainVal / plannedVal) * 100).toInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF8F6))
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.weight(1.3f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Editable Planned TextField
                            Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.CenterEnd) {
                                BasicNumberField(
                                    value = plannedBudgets[category] ?: "0",
                                    onValueChange = { plannedBudgets[category] = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Actual (Otomatis dari Database)
                            Text(
                                text = if (actualVal > 0) formatRupiah(actualVal) else "-",
                                modifier = Modifier.weight(1.1f),
                                textAlign = TextAlign.End,
                                fontSize = 11.sp
                            )

                            // Remain
                            Text(
                                text = if (plannedVal > 0) formatRupiah(remainVal) else "-",
                                modifier = Modifier.weight(1.1f),
                                textAlign = TextAlign.End,
                                fontSize = 11.sp
                            )

                            // Badge Persentase
                            Box(
                                modifier = Modifier.weight(0.7f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (plannedVal > 0) {
                                    val badgeColor = if (percentage < 50) Color(0xFFB71C1C) else Color(0xFFA5D6A7)
                                    val textColor = if (percentage < 50) Color.White else Color.Black
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(badgeColor)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$percentage%",
                                            color = textColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text("-", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ================= FORM CATAT PENGELUARAN =================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Catat Transaksi Pengeluaran", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Nama Pengeluaran (mis. Hotel, Tiket)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) amount = input
                            },
                            label = { Text("Jumlah (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = isCategoryDropdownExpanded,
                            onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategori") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isCategoryDropdownExpanded,
                                onDismissRequest = { isCategoryDropdownExpanded = false }
                            ) {
                                categoryList.forEach { categoryItem ->
                                    DropdownMenuItem(
                                        text = { Text(categoryItem) },
                                        onClick = {
                                            selectedCategory = categoryItem
                                            isCategoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                                if (title.isNotBlank() && parsedAmount > 0) {
                                    viewModel.addExpense(title, parsedAmount, selectedCategory)
                                    title = ""
                                    amount = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simpan Transaksi")
                        }
                    }
                }
            }

            // ================= HISTORI PENGELUARAN =================
            item {
                Text("Histori Pengeluaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(expenseList, key = { it.id }) { item ->
                ExpenseItem(expense = item, onDelete = { viewModel.deleteExpense(item) })
            }
        }

        // ================= DIALOG PEMBARUAN =================
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

// Widget Input Angka Ringkas untuk Kolom Planned dan Paycheck
@Composable
fun BasicNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { input ->
            if (input.all { it.isDigit() }) onValueChange(input)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        ),
        modifier = modifier
            .border(0.5.dp, Color.Gray, RoundedCornerShape(2.dp))
            .background(Color.White)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text("Rp ", fontSize = 10.sp, color = Color.Gray)
                innerTextField()
            }
        }
    )
}

@Composable
fun ExpenseItem(expense: Expense, onDelete: () -> Unit) {
    val dateString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(expense.date))
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("${expense.category} • $dateString", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(formatRupiah(expense.amount), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
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
