package com.example.pengeluaran

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pengeluaran.util.UpdateChecker
import com.example.pengeluaran.util.UpdateInfo
import com.example.pengeluaran.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ExpenseScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val currentVersion = remember { UpdateChecker.getCurrentVersion(context) }

    // 1. AUTO-CHECK SAAT PERTAMA KALI RUNNING
    LaunchedEffect(Unit) {
        val result = UpdateChecker.checkLatestRelease(context)
        if (result.hasUpdate) {
            updateInfo = result
            showDialog = true
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
                actions = {
                    // Menu Pilihan Tambahan
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
                                        showDialog = true
                                    } else {
                                        Toast.makeText(context, "Aplikasi sudah versi terbaru ($currentVersion)", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Konten Form dan Riwayat Transaksi Anda di sini
        }

        // 2. DIALOG KETIKA UPDATE DITEMUKAN
        if (showDialog && updateInfo != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
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
                            showDialog = false
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo?.downloadUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Unduh Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Nanti Saja")
                    }
                }
            )
        }
    }
}
