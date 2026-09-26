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
        containerColor = MaterialTheme.colorScheme.background,

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
                            Icons.Default.History,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("History")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
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
                modifier = Modifier.padding(padding)
            )

            1 -> HistoryScreen(
                vm = vm,
                modifier = Modifier.padding(padding)
            )

            2 -> SettingsScreen(
                modifier = Modifier.padding(padding)
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
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
            modifier = Modifier.fillMaxWidth(),
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
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            result.config.name.ifBlank {
                                "Converted Configuration"
                            },
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "${result.config.protocol.uppercase()} • " +
                                "${result.config.server}:${result.config.port}",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "OPENCLASH YAML",
                style =
                    MaterialTheme.typography.labelLarge,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color =
                    MaterialTheme.colorScheme.background
            ) {

                Text(
                    text =
                        result.openClashYaml,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .padding(12.dp),

                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

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
                        Icons.Default.ContentCopy,
                    text = "Copy",
                    onClick = onCopy
                )

                SmallAction(
                    icon =
                        Icons.Default.Share,
                    text = "Share",
                    onClick = onShare
                )

                SmallAction(
                    icon =
                        Icons.Default.FileDownload,
                    text = "Export",
                    onClick = onExport
                )

                SmallAction(
                    icon =
                        Icons.Default.QrCode2,
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
            Modifier.width(6.dp)
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
                "OpenClash Configuration QR",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Image(
                    bitmap = bitmap.asImageBitmap(),

                    contentDescription =
                        "OpenClash Configuration QR Code",

                    modifier =
                        Modifier
                            .size(260.dp)
                            .background(
                                ComposeColor.White,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Scan this QR code with a compatible client.",

                    style =
                        MaterialTheme.typography.bodySmall
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
                style =
                    MaterialTheme.typography.headlineSmall,
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
            Modifier.height(10.dp)
        )

        if (history.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        text = "No conversion history",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
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
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                history.forEach { item ->

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp)
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(12.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        item.name.ifBlank {
                                            "Unnamed configuration"
                                        },
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    text =
                                        "${item.protocol.uppercase()} • " +
                                            "${item.server}:${item.port}",
                                    style =
                                        MaterialTheme.typography
                                            .bodySmall,
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
                                    Icons.Default.DeleteOutline,
                                    contentDescription =
                                        "Delete"
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
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Settings",
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "Config Converter",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Native Android OpenClash configuration converter.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        "Supported: VMess, VLESS, Trojan, Shadowsocks, SOCKS, HTTP, WireGuard, Hysteria",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Output: OpenClash YAML",
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Version 1.0.0",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
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
            "OpenClash Configuration",
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
