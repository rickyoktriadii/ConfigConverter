package com.example.configconverter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ConfigConverterApp() }
    }
}

@Composable
fun ConfigConverterApp(vm: ConverterViewModel = viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Sync, null) }, label = { Text("CONVERT") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("HISTORY") })
                    NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("SETTINGS") })
                }
            }
        ) { p ->
            when (tab) {
                0 -> ConvertScreen(vm, Modifier.padding(p))
                1 -> HistoryScreen(vm, Modifier.padding(p))
                else -> SettingsScreen(vm, Modifier.padding(p))
            }
        }
    }
}

@Composable
private fun ConvertScreen(vm: ConverterViewModel, modifier: Modifier) {
    val state by vm.state.collectAsState()
    var text by remember { mutableStateOf("") }
    var outputTab by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboard

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("CONFIG CONVERTER", style = MaterialTheme.typography.headlineSmall)
            Text("VMess • VLESS • Trojan → V2Ray / OpenClash", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; vm.detect(it) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                placeholder = { Text("Paste your configuration here...") },
                minLines = 7
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ text = clipboard.getText(); vm.detect(text) }) { Text("PASTE") }
                OutlinedButton({ text = ""; vm.reset() }) { Text("CLEAR") }
            }
        }
        item {
            val detected = state.detected ?: "Unknown / Invalid Configuration"
            AssistChip(onClick = {}, label = { Text("Detected: $detected") })
        }
        item {
            Button(
                onClick = { vm.convert(text) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank()
            ) { Text("CONVERT") }
        }
        state.error?.let { msg ->
            item { Text("✕ $msg", color = MaterialTheme.colorScheme.error) }
        }
        state.result?.let { result ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("CONVERSION RESULT", style = MaterialTheme.typography.titleMedium)
                        Text("Name: ${result.config.name.ifBlank { "Unnamed" }}")
                        Text("Protocol: ${result.config.protocol.uppercase()}")
                        Text("Server: ${result.config.server}:${result.config.port}")
                        Text("Transport: ${result.config.network}")
                        Text("TLS: ${if (result.config.tls) "Enabled" else "Disabled"}")
                        result.config.sni?.takeIf { it.isNotBlank() }?.let { Text("SNI: $it") }
                    }
                }
            }
            item {
                TabRow(outputTab) {
                    Tab(outputTab == 0, { outputTab = 0 }, text = { Text("V2RAY / XRAY") })
                    Tab(outputTab == 1, { outputTab = 1 }, text = { Text("OPENCLASH") })
                }
            }
            item {
                val code = if (outputTab == 0) result.v2rayJson else result.openClashYaml
                OutlinedTextField(
                    value = code,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ clipboard.copy(if (outputTab == 0) result.v2rayJson else result.openClashYaml) }) {
                        Text("COPY")
                    }
                    OutlinedButton({ vm.share(if (outputTab == 0) result.v2rayJson else result.openClashYaml) }) {
                        Text("SHARE")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(vm: ConverterViewModel, modifier: Modifier) {
    val items by vm.history.collectAsState()
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("HISTORY", style = MaterialTheme.typography.headlineSmall)
            TextButton({ vm.clearHistory() }) { Text("CLEAR ALL") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { h ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(h.name.ifBlank { "Unnamed" })
                            Text("${h.protocol.uppercase()} • ${h.server}:${h.port}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ vm.removeHistory(h.id) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: ConverterViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SETTINGS", style = MaterialTheme.typography.headlineSmall)
        Text("Theme: Dark (default)")
        Text("Default output: V2Ray / Xray")
        Text("History stores metadata only by default; credentials are not persisted.")
        Text("Config Converter v1.0.0")
        Text("All parsing and conversion is performed locally.")
    }
}

private object LocalClipboard {
    private var context: Context? = null
    fun init(c: Context) { context = c }
    fun getText(): String {
        val cm = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return cm?.primaryClip?.getItemAt(0)?.coerceToText(context!!)?.toString() ?: ""
    }
    fun copy(value: String) {
        val cm = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Config Converter", value))
    }
}
