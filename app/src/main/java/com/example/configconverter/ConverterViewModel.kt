package com.example.configconverter

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConverterState(
    val detected: String? = null,
    val result: ConversionResult? = null,
    val error: String? = null
)

class ConverterViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    init {
        LocalClipboard.init(app)
    }

    fun detect(raw: String) {
        val s = raw.trim()

        val d = when {
    s.startsWith("vmess://", true) -> "VMess"
    s.startsWith("vless://", true) -> "VLESS"
    s.startsWith("trojan://", true) -> "Trojan"
    s.startsWith("ss://", true) -> "Shadowsocks"
    s.startsWith("socks://", true) ||
    s.startsWith("socks5://", true) -> "SOCKS"
    s.startsWith("http://", true) ||
    s.startsWith("https://", true) -> "HTTP"
    s.startsWith("wireguard://", true) -> "WireGuard"
    s.startsWith("hysteria2://", true) ||
    s.startsWith("hy2://", true) ||
    s.startsWith("hysteria://", true) -> "Hysteria"
    s.startsWith("{") && s.contains("\"outbounds\"") -> "Xray"
    else -> null
        }

        _state.value = _state.value.copy(
            detected = d,
            error = null
        )
    }

    fun convert(raw: String) {
        val parsed = ConfigParser.parse(raw)

        when (parsed) {
            is ParseResult.Error -> {
                _state.value = _state.value.copy(
                    error = parsed.message,
                    result = null
                )
            }

            is ParseResult.Ok -> {
                val error = ConfigValidator.validate(parsed.config)

                if (error != null) {
                    _state.value = _state.value.copy(
                        error = error,
                        result = null
                    )
                    return
                }

                val result = ConversionResult(
                    parsed.config,
                    V2RayGenerator.generate(parsed.config),
                    OpenClashGenerator.generate(parsed.config)
                )

                _history.value = (
                    listOf(
                        HistoryItem(
                            name = result.config.name,
                            protocol = result.config.protocol,
                            server = result.config.server,
                            port = result.config.port
                        )
                    ) + _history.value
                ).take(100)

                _state.value = ConverterState(
                    detected = result.config.protocol
                        .replaceFirstChar { it.uppercase() },
                    result = result,
                    error = null
                )
            }
        }
    }

    fun reset() {
        _state.value = ConverterState()
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun removeHistory(id: String) {
        _history.value = _history.value.filterNot {
            it.id == id
        }
    }

    fun share(value: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        getApplication<Application>().startActivity(
            Intent.createChooser(
                intent,
                "Share configuration"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
