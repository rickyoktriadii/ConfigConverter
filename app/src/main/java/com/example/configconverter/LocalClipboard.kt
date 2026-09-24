package com.example.configconverter

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object LocalClipboard {

    private lateinit var clipboard: ClipboardManager
    private lateinit var context: Context

    fun init(app: Application) {
        context = app.applicationContext

        clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager
    }

    fun copy(
        label: String,
        text: String
    ) {
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                label,
                text
            )
        )
    }

    fun read(): String {
        val clip = clipboard.primaryClip

        if (clip == null || clip.itemCount == 0) {
            return ""
        }

        return clip.getItemAt(0)
            .coerceToText(context)
            .toString()
    }

    fun clear() {
        clipboard.clearPrimaryClip()
    }
}
