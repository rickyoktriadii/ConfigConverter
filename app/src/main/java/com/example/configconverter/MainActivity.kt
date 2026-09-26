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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

import androidx.compose.runtime.*
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
    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,

        bottomBar = {
            NavigationBar(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ) {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    icon = {
                        Icon(
                            Icons.Default.Transform,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Convert")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    icon = {
                        Icon(
                            Icons.Default.NetworkCheck,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Status")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                    },
                    icon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("History")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                    },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Settings")
                    }
                )
            }
        }
    ) { padding ->

        when (selectedTab) {

            0 -> ConvertScreen(
                vm = vm,
                modifier =
                    Modifier.padding(padding)
            )

            1 -> StatusScreen(
                modifier =
                    Modifier.padding(padding)
            )

            2 -> HistoryScreen(
                vm = vm,
                modifier =
                    Modifier.padding(padding)
            )

            3 -> SettingsScreen(
                modifier =
                    Modifier.padding(padding)
            )
        }
    }
}


@Composable
private fun ConvertScreen(
    vm: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var input by remember {
        mutableStateOf("")
    }

    var qrBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var showQr by remember {
        mutableStateOf(false)
    }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            val content = runCatching {
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    ?: ""
            }.getOrDefault("")

            if (content.isNotBlank()) {
                input = content
                vm.detect(content)
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/yaml")
        ) { uri: Uri? ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            val result =
                state.result
                    ?: return@rememberLauncherForActivityResult

            runCatching {
                context.contentResolver
                    .openOutputStream(uri)
                    ?.use {
                        it.write(
                            result.openClashYaml
                                .toByteArray(
                                    Charsets.UTF_8
                                )
                        )
                    }
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Config Converter",
                    style =
                        MaterialTheme.typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Convert your proxy configuration to OpenClash",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Icon(
                imageVector =
                    Icons.Default.SwapHoriz,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.size(30.dp)
            )
        }

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(14.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Configuration",
                        fontWeight =
                            FontWeight.SemiBold,
                        modifier =
                            Modifier.weight(1f)
                    )

                    state.detected?.let { detected ->

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(detected)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = input,

                    onValueChange = {
                        input = it
                        vm.detect(it)
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = 150.dp
                            ),

                    placeholder = {
                        Text(
                            "Paste vmess://, vless://, trojan:// or other supported config"
                        )
                    },

                    shape =
                        RoundedCornerShape(14.dp),

                    maxLines = 8
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    SmallAction(
                        icon =
                            Icons.Default.ContentPaste,
                        text = "Paste"
                    ) {

                        val clipboard =
                            context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager

                        val value =
                            clipboard.primaryClip
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                                .orEmpty()

                        input = value
                        vm.detect(value)
                    }

                    SmallAction(
                        icon =
                            Icons.Default.FileOpen,
                        text = "Import"
                    ) {

                        importLauncher.launch(
                            arrayOf(
                                "text/*",
                                "application/json",
                                "application/yaml",
                                "application/x-yaml",
                                "application/octet-stream"
                            )
                        )
                    }

                    SmallAction(
                        icon =
                            Icons.Default.Clear,
                        text = "Clear"
                    ) {

                        input = ""
                        vm.reset()
                        qrBitmap = null
                        showQr = false
                    }
                }
            }
        }

                Button(
            onClick = {
                vm.convert(input)
                qrBitmap = null
                showQr = false
            },

            enabled = input.isNotBlank(),

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),

            shape =
                RoundedCornerShape(16.dp)
        ) {

            Icon(
                Icons.Default.AutoFixHigh,
                contentDescription = null
            )

            Spacer(
                Modifier.width(8.dp)
            )

            Text(
                text = "Analyze & Convert",
                fontWeight = FontWeight.Bold
            )
        }

        state.error?.let { error ->

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme
                                .errorContainer
                    ),

                shape =
                    RoundedCornerShape(14.dp)
            ) {

                Row(
                    modifier =
                        Modifier.padding(12.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text = error,
                        color =
                            MaterialTheme.colorScheme
                                .onErrorContainer
                    )
                }
            }
        }

        state.result?.let { result ->

            ResultCard(
                result = result,

                onCopy = {
                    copyToClipboard(
                        context,
                        result.openClashYaml
                    )
                },

                onShare = {
                    vm.share(
                        result.openClashYaml
                    )
                },

                onExport = {
                    exportLauncher.launch(
                        "openclash.yaml"
                    )
                },

                onQr = {

                    qrBitmap =
                        generateQr(
                            result.openClashYaml
                        )

                    showQr =
                        qrBitmap != null
                }
            )
        }
    }

    if (
        showQr &&
        qrBitmap != null
    ) {

        QRDialog(
            bitmap = qrBitmap!!,

            onDismiss = {
                showQr = false
            }
        )
    }
}


@Composable
private fun StatusScreen(
    modifier: Modifier = Modifier
) {
    var configuration by remember {
        mutableStateOf("")
    }

    var checked by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Status Checker",

            style =
                MaterialTheme.typography
                    .headlineSmall,

            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "Check your configuration status",

            style =
                MaterialTheme.typography.bodySmall,

            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            StatusSummaryCard(
                modifier =
                    Modifier.weight(1f),

                title = "Active",

                value = if (checked) {
                    "0"
                } else {
                    "—"
                },

                icon =
                    Icons.Default.CheckCircle
            )

            StatusSummaryCard(
                modifier =
                    Modifier.weight(1f),

                title = "Offline",

                value = if (checked) {
                    "0"
                } else {
                    "—"
                },

                icon =
                    Icons.Default.ErrorOutline
            )

            StatusSummaryCard(
                modifier =
                    Modifier.weight(1f),

                title = "Unknown",

                value = if (checked) {
                    "1"
                } else {
                    "—"
                },

                icon =
                    Icons.Default.HelpOutline
            )
        }

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(14.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "Configuration",

                    fontWeight =
                        FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = configuration,

                    onValueChange = {
                        configuration = it
                        checked = false
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = 150.dp
                            ),

                    placeholder = {
                        Text(
                            "Paste configuration here..."
                        )
                    },

                    shape =
                        RoundedCornerShape(14.dp),

                    maxLines = 8
                )

                Button(
                    onClick = {
                        checked = true
                    },

                    enabled =
                        configuration.isNotBlank(),

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Icon(
                        Icons.Default.NetworkCheck,
                        contentDescription = null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text = "CHECK STATUS",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        if (!checked) {

            StatusEmptyCard()

        } else {

            StatusResultCard(
                configuration =
                    configuration
            )
        }
    }
}


@Composable
private fun StatusSummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(
            modifier =
                Modifier.padding(10.dp),

            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            Icon(
                imageVector = icon,

                contentDescription = null,

                modifier =
                    Modifier.size(20.dp),

                tint =
                    MaterialTheme.colorScheme.primary
            )

            Text(
                text = value,

                style =
                    MaterialTheme.typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = title,

                style =
                    MaterialTheme.typography
                        .labelSmall,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


@Composable
private fun StatusEmptyCard() {
    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Icon(
                Icons.Default.NetworkCheck,

                contentDescription = null,

                modifier =
                    Modifier.size(42.dp),

                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    "No status check yet",

                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Paste a configuration above and check its status.",

                style =
                    MaterialTheme.typography
                        .bodySmall,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusResultCard(
    configuration: String
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Configuration Status",

                        fontWeight =
                            FontWeight.Bold,

                        style =
                            MaterialTheme.typography
                                .titleMedium
                    )

                    Text(
                        text =
                            "Preliminary status result",

                        style =
                            MaterialTheme.typography
                                .bodySmall,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                Surface(
                    shape =
                        RoundedCornerShape(50.dp),

                    color =
                        ComposeColor(0xFF183B2A)
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.HelpOutline,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(16.dp),

                            tint =
                                ComposeColor(0xFF7BE495)
                        )

                        Spacer(
                            Modifier.width(5.dp)
                        )

                        Text(
                            text = "UNKNOWN",

                            style =
                                MaterialTheme.typography
                                    .labelMedium,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                ComposeColor(0xFF7BE495)
                        )
                    }
                }
            }

            HorizontalDivider()

            StatusInfoRow(
                label = "Configuration",
                value =
                    if (
                        configuration.length > 35
                    ) {
                        configuration.take(35) + "..."
                    } else {
                        configuration
                    }
            )

            StatusInfoRow(
                label = "Protocol",
                value =
                    detectStatusProtocol(
                        configuration
                    )
            )

            StatusInfoRow(
                label = "Server",
                value =
                    "Not checked yet"
            )

            StatusInfoRow(
                label = "Port",
                value =
                    "Not checked yet"
            )

            StatusInfoRow(
                label = "TLS",
                value =
                    "Not checked yet"
            )

            StatusInfoRow(
                label = "Latency",
                value =
                    "—"
            )

            HorizontalDivider()

            Text(
                text =
                    "The configuration has not been tested against the server yet.",

                style =
                    MaterialTheme.typography.bodySmall,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            OutlinedButton(
                onClick = {},

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(14.dp)
            ) {

                Icon(
                    Icons.Default.Refresh,

                    contentDescription =
                        null
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    "CHECK AGAIN"
                )
            }
        }
    }
}


@Composable
private fun StatusInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,

            modifier =
                Modifier.weight(1f),

            style =
                MaterialTheme.typography
                    .bodySmall,

            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Text(
            text = value,

            style =
                MaterialTheme.typography
                    .bodySmall,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}


private fun detectStatusProtocol(
    configuration: String
): String {

    val value =
        configuration
            .trim()
            .lowercase()

    return when {

        value.startsWith("vmess://") ->
            "VMess"

        value.startsWith("vless://") ->
            "VLESS"

        value.startsWith("trojan://") ->
            "Trojan"

        value.startsWith("ss://") ->
            "Shadowsocks"

        value.startsWith("socks://") ||
        value.startsWith("socks5://") ->
            "SOCKS"

        value.startsWith("http://") ||
        value.startsWith("https://") ->
            "HTTP"

        value.startsWith("wireguard://") ->
            "WireGuard"

        value.startsWith("hysteria://") ||
        value.startsWith("hysteria2://") ||
        value.startsWith("hy2://") ->
            "Hysteria"

        else ->
            "Unknown"
    }
}

@Composable
private fun ResultCard(
    result: ConversionResult,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onQr: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = result.config.name.ifBlank {
                            "Converted Configuration"
                        },
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "${result.config.protocol.uppercase()} • " +
                                "${result.config.server}:${result.config.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Text(
                    text = result.openClashYaml,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                SmallAction(
                    icon = Icons.Default.ContentCopy,
                    text = "Copy",
                    onClick = onCopy
                )

                SmallAction(
                    icon = Icons.Default.Share,
                    text = "Share",
                    onClick = onShare
                )

                SmallAction(
                    icon = Icons.Default.FileDownload,
                    text = "Export",
                    onClick = onExport
                )

                SmallAction(
                    icon = Icons.Default.QrCode2,
                    text = "QR",
                    onClick = onQr
                )
            }
        }
    }
}

@Composable
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(text)
    }
}

@Composable
private fun QRDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Configuration QR",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Configuration QR Code",
                    modifier = Modifier
                        .size(260.dp)
                        .background(
                            ComposeColor.White,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Scan this QR code with a compatible client.",
                    style = MaterialTheme.typography.bodySmall
                )
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

@Composable
private fun HistoryScreen(
    vm: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val history by vm.history.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = {
                        vm.clearHistory()
                    }
                ) {
                    Text("Clear")
                }
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (history.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "No conversion history",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                history.forEach { item ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = item.name.ifBlank {
                                        "Unnamed configuration"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text =
                                        "${item.protocol.uppercase()} • " +
                                            "${item.server}:${item.port}",
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    vm.removeHistory(item.id)
                                }
                            ) {
                                Icon(
                                    imageVector =
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
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "Config Converter",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Native Android configuration converter.",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text =
                        "Supported: VMess, VLESS, Trojan, " +
                            "Shadowsocks, SOCKS, HTTP, WireGuard, Hysteria",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
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
            "Configuration",
            value
        )
    )
}

private fun generateQr(
    value: String
): Bitmap? {

    if (value.isBlank()) {
        return null
    }

    return runCatching {

        val matrix: BitMatrix =
            MultiFormatWriter().encode(
                value,
                BarcodeFormat.QR_CODE,
                800,
                800
            )

        val bitmap =
            Bitmap.createBitmap(
                matrix.width,
                matrix.height,
                Bitmap.Config.ARGB_8888
            )

        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {

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
