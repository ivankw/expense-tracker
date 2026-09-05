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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pengeluaran.data.Expense
import com.example.pengeluaran.util.ApkDownloader
import com.example.pengeluaran.util.UpdateChecker
import com.example.pengeluaran.util.UpdateInfo
import com.example.pengeluaran.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
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

    // State data input & transaksi
    val expenseList by viewModel.expenses.collectAsState()
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    val totalExpense = expenseList.sumOf { it.amount }

    // State pembaruan aplikasi
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val currentVersion = remember { UpdateChecker.getCurrentVersion(context) }

    // Cek pembaruan otomatis saat aplikasi pertama kali dijalankan
    LaunchedEffect(Unit) {
        val result = UpdateChecker.checkLatestRelease(context)
        if (result.hasUpdate) {
            updateInfo = result
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
                    // Tombol menu titik tiga
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
                                    val result = UpdateChecker.checkLatestRelease(context)
                                    if (result.hasUpdate) {
                                        updateInfo = result
                                        showUpdateDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Aplikasi sudah versi terbaru ($currentVersion)",
                                            Toast.LENGTH_LONG
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Card Total Pengeluaran
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Pengeluaran", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatRupiah(totalExpense),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Input Pengeluaran
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama Pengeluaran (mis. Makan Siang)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Jumlah (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategori (mis. Makanan, Transport)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && parsedAmount > 0) {
                        viewModel.addExpense(title, parsedAmount, category.ifBlank { "Umum" })
                        title = ""
                        amount = ""
                        category = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Transaksi")
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Histori Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // List Riwayat Transaksi
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(expenseList, key = { it.id }) { item ->
                    ExpenseItem(expense = item, onDelete = { viewModel.deleteExpense(item) })
                }
            }
        }

        // Dialog Notifikasi Update & Eksekusi Download
        if (showUpdateDialog && updateInfo != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("Pembaruan Tersedia (${updateInfo?.latestVersion})") },
                text = {
                    Column {
                        Text("Versi Anda saat ini: $currentVersion\n")
                        Text("Catatan Pembaruan:")
                        Text(
                            updateInfo?.releaseNotes ?: "-",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUpdateDialog = false
                            val downloadUrl = updateInfo?.downloadUrl
                            if (!downloadUrl.isNullOrBlank()) {
                                // Pemanggilan DownloadManager dan auto-install (Langkah No. 4)
                                ApkDownloader.downloadAndInstall(
                                    context = context,
                                    downloadUrl = downloadUrl,
                                    fileName = "ExpenseTracker-${updateInfo?.latestVersion}.apk"
                                )
                            } else {
                                Toast.makeText(context, "URL unduhan tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Update Sekarang")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Nanti Saja")
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
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${expense.category} • $dateString", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatRupiah(expense.amount),
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
    val localeID = Locale("in", "ID")
    val format = NumberFormat.getCurrencyInstance(localeID)
    return format.format(number)
}
