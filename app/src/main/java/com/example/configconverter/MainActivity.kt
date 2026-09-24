package com.example.configconverter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App(vm: ConverterViewModel = viewModel()) {
    var page by remember { mutableIntStateOf(0) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    listOf(
                        Icons.Default.SwapHoriz to "Convert",
                        Icons.Default.History to "History",
                        Icons.Default.Settings to "Settings"
                    ).forEachIndexed { i, item ->
                        NavigationBarItem(
                            selected = page == i,
                            onClick = { page = i },
                            icon = { Icon(item.first, null) },
                            label = { Text(item.second) }
                        )
                    }
                }
            }
        ) { p ->
            when (page) {
                0 -> ConvertPage(vm, Modifier.padding(p))
                1 -> HistoryPage(vm, Modifier.padding(p))
                2 -> SettingsPage(Modifier.padding(p))
            }
        }
    }
}

@Composable
private fun ConvertPage(vm: ConverterViewModel, modifier: Modifier) {
    val state by vm.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var output by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    "CONFIG CONVERTER",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "VMess • VLESS • Trojan",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            vm.detect(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        placeholder = {
                            Text("Paste VMess, VLESS or Trojan...")
                        },
                        minLines = 7,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                input = LocalClipboard.getText()
                                vm.detect(input)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentPaste, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Paste")
                        }

                        OutlinedButton(
                            onClick = {
                                input = ""
                                vm.reset()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Clear")
                        }
                    }
                }
            }
        }

        item {
            AssistChip(
                onClick = {},
                label = {
                    Text("Detected: ${state.detected ?: "Unknown"}")
                },
                leadingIcon = {
                    Icon(Icons.Default.RadioButtonChecked, null)
                }
            )
        }

        item {
            Button(
                onClick = { vm.convert(input) },
                enabled = input.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Convert Configuration",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        state.error?.let { error ->
            item {
                Text(
                    "⚠ $error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        state.result?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "CONVERSION RESULT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Name: ${result.config.name.ifBlank { "Unnamed" }}")
                        Text("Protocol: ${result.config.protocol.uppercase()}")
                        Text("Server: ${result.config.server}:${result.config.port}")
                        Text("Transport: ${result.config.network.uppercase()}")
                        Text("TLS: ${if (result.config.tls) "Enabled" else "Disabled"}")
                        result.config.sni?.let {
                            if (it.isNotBlank()) Text("SNI: $it")
                        }
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = output) {
                    Tab(
                        selected = output == 0,
                        onClick = { output = 0 },
                        text = { Text("V2Ray / Xray") }
                    )
                    Tab(
                        selected = output == 1,
                        onClick = { output = 1 },
                        text = { Text("OpenClash") }
                    )
                }
            }

            item {
                val code =
                    if (output == 0) result.v2rayJson
                    else result.openClashYaml

                OutlinedTextField(
                    value = code,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            LocalClipboard.copy(
                                if (output == 0)
                                    result.v2rayJson
                                else
                                    result.openClashYaml
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }

                    OutlinedButton(
                        onClick = {
                            vm.share(
                                if (output == 0)
                                    result.v2rayJson
                                else
                                    result.openClashYaml
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPage(
    vm: ConverterViewModel,
    modifier: Modifier
) {
    val history by vm.history.collectAsState()

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "HISTORY",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${history.size} conversion",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (history.isNotEmpty()) {
                TextButton({ vm.clearHistory() }) {
                    Text("Clear")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history, key = { it.id }) { item ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name.ifBlank { "Unnamed" },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${item.protocol.uppercase()} • ${item.server}:${item.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton({
                            vm.removeHistory(item.id)
                        }) {
                            Icon(Icons.Default.DeleteOutline, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "SETTINGS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Appearance", fontWeight = FontWeight.Bold)
                Text(
                    "Premium dark interface",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Privacy", fontWeight = FontWeight.Bold)
                Text(
                    "Configuration parsing and conversion are performed locally.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Config Converter", fontWeight = FontWeight.Bold)
                Text(
                    "Version 1.0.0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

object LocalClipboard {
    private var context: Context? = null

    fun init(c: Context) {
        context = c
    }

    fun getText(): String {
        val cm = context?.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as? ClipboardManager

        return cm?.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context!!)
            ?.toString()
            ?: ""
    }

    fun copy(value: String) {
        val cm = context?.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as? ClipboardManager

        cm?.setPrimaryClip(
            ClipData.newPlainText(
                "Config Converter",
                value
            )
        )
    }
}
