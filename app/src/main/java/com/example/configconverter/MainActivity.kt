package com.example.configconverter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConfigConverterTheme {
                ConfigConverterApp()
            }
        }
    }
}


@Composable
private fun ConfigConverterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ComposeColor(0xFF8AB4F8),
            secondary = ComposeColor(0xFF9AA0A6),
            background = ComposeColor(0xFF0B0D10),
            surface = ComposeColor(0xFF15181D),
            surfaceVariant = ComposeColor(0xFF20242A)
        ),
        content = content
    )
}

@Composable
private fun ConfigConverterApp(
    vm: ConverterViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Transform,
                            contentDescription = "Convert"
                        )
                    },
                    label = { Text("Convert") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->

        when (selectedTab) {
            0 -> ConvertScreen(
                vm = vm,
                padding = padding
            )

            1 -> HistoryScreen(
                vm = vm,
                padding = padding
            )

            2 -> SettingsScreen(
                padding = padding
            )
        }
    }
}


@Composable
private fun ConvertScreen(
    vm: ConverterViewModel,
    padding: PaddingValues
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var input by remember { mutableStateOf("") }
    var outputType by remember { mutableStateOf("V2Ray / Xray") }
    var showQr by remember { mutableStateOf(false) }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri == null) return@rememberLauncherForActivityResult

            runCatching {
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }.onSuccess { text ->
                if (!text.isNullOrBlank()) {
                    input = text
                    vm.detect(text)
                }
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain")
        ) { uri: Uri? ->

            if (uri == null) return@rememberLauncherForActivityResult

            val content = when (outputType) {
                "OpenClash" -> state.result?.openClashYaml
                else -> state.result?.v2rayJson
            }

            if (content != null) {
                runCatching {
                    context.contentResolver
                        .openOutputStream(uri)
                        ?.bufferedWriter()
                        ?.use { it.write(content) }
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Config Converter",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Convert and manage your proxy configurations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.detected != null) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(state.detected!!)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as ClipboardManager

                            val clip =
                                clipboard.primaryClip

                            if (clip != null && clip.itemCount > 0) {
                                input =
                                    clip.getItemAt(0)
                                        .coerceToText(context)
                                        .toString()

                                vm.detect(input)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Paste"
                        )
                    }

                    IconButton(
                        onClick = {
                            input = ""
                            vm.reset()
                        }
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        vm.detect(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    placeholder = {
                        Text(
                            "Paste VMess, VLESS, Trojan, JSON or YAML configuration..."
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 6
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    FilledTonalButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "application/json",
                                    "application/x-yaml",
                                    "application/yaml"
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.FileOpen,
                            contentDescription = null
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text("Import")
                    }

                    FilledTonalButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as ClipboardManager

                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Configuration",
                                    input
                                )
                            )
                        },
                        enabled = input.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text("Copy")
                    }

                    FilledTonalButton(
                        onClick = {
                            vm.share(input)
                        },
                        enabled = input.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text("Share")
                    }
                }

                Button(
                    onClick = {
                        vm.convert(input)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = input.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        "Analyze & Convert",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        state.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(10.dp)
                    )

                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        state.result?.let { result ->

            ResultCard(
                result = result,
                outputType = outputType,
                onOutputTypeChange = {
                    outputType = it
                },
                onCopy = { value ->
                    val clipboard =
                        context.getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as ClipboardManager

                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "Converted configuration",
                            value
                        )
                    )
                },
                onShare = { value ->
                    vm.share(value)
                },
                onExport = {
                    exportLauncher.launch(
                        if (outputType == "OpenClash") {
                            "openclash.yaml"
                        } else {
                            "config.json"
                        }
                    )
                },
                onQr = {
                    showQr = true
                }
            )
        }
    }

    if (showQr && state.result != null) {
        QrDialog(
            value = if (outputType == "OpenClash") {
                state.result!!.openClashYaml
            } else {
                state.result!!.v2rayJson
            },
            onDismiss = {
                showQr = false
            }
        )
    }
}

@Composable
private fun ResultCard(
    result: ConversionResult,
    outputType: String,
    onOutputTypeChange: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onExport: () -> Unit,
    onQr: () -> Unit
) {
    val output = if (outputType == "OpenClash") {
        result.openClashYaml
    } else {
        result.v2rayJson
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Conversion Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${result.config.protocol.uppercase()} • ${result.config.server}:${result.config.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = outputType == "V2Ray / Xray",
                    onClick = {
                        onOutputTypeChange("V2Ray / Xray")
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    )
                ) {
                    Text("V2Ray / Xray")
                }

                SegmentedButton(
                    selected = outputType == "OpenClash",
                    onClick = {
                        onOutputTypeChange("OpenClash")
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    )
                ) {
                    Text("OpenClash")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = output,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 360.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .horizontalScroll(
                            rememberScrollState()
                        )
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                FilledTonalButton(
                    onClick = {
                        onCopy(output)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text("Copy")
                }

                FilledTonalButton(
                    onClick = {
                        onShare(output)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text("Share")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text("Export")
                }

                Button(
                    onClick = onQr,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text("QR Code")
                }
            }
        }
    }
}


@Composable
private fun QrDialog(
    value: String,
    onDismiss: () -> Unit
) {
    val bitmap = remember(value) {
        generateQrBitmap(value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configuration QR Code")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(280.dp)
                    )
                } else {
                    Text(
                        "Unable to generate QR code."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}


private fun generateQrBitmap(
    value: String
): Bitmap? {
    return runCatching {
        val size = 800

        val matrix: BitMatrix =
            MultiFormatWriter().encode(
                value,
                BarcodeFormat.QR_CODE,
                size,
                size
            )

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix[x, y]) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
                )
            }
        }

        bitmap
    }.getOrNull()
}

@Composable
private fun HistoryScreen(
    vm: ConverterViewModel,
    padding: PaddingValues
) {
    val history by vm.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${history.size} saved configurations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (history.isNotEmpty()) {
                IconButton(
                    onClick = {
                        vm.clearHistory()
                    }
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Clear history"
                    )
                }
            }
        }

        if (history.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )

                    Text(
                        text = "No history yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Converted configurations will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            history.forEach { item ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.name.ifBlank {
                                    "Unnamed configuration"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = item.protocol.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "${item.server}:${item.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                vm.removeHistory(item.id)
                            }
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete"
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SettingsScreen(
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Application information and preferences",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "Config Converter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "VMess, VLESS and Trojan configuration converter.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Version 1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Supported formats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text("• VMess")
                Text("• VLESS")
                Text("• Trojan")
                Text("• V2Ray / Xray JSON")
                Text("• OpenClash YAML")
                Text("• QR Code")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Configurations are processed locally by the application. " +
                        "No configuration is intentionally uploaded to a remote server.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun copyToClipboard(
    context: Context,
    value: String
) {
    val clipboard =
        context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            "Config Converter",
            value
        )
    )
}


private fun readClipboard(
    context: Context
): String {
    val clipboard =
        context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

    val clip = clipboard.primaryClip

    if (clip == null || clip.itemCount == 0) {
        return ""
    }

    return clip.getItemAt(0)
        .coerceToText(context)
        .toString()
}


private fun shareText(
    context: Context,
    value: String
) {
    val intent =
        android.content.Intent(
            android.content.Intent.ACTION_SEND
        ).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                value
            )
        }

    context.startActivity(
        android.content.Intent.createChooser(
            intent,
            "Share configuration"
        )
    )
}

@Composable
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(text)
    }
}

@Composable
private fun ConfigInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f)
        )
    }
}
