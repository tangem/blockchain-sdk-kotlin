package com.tangem.demo.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.TextView

/**
[REDACTED_AUTHOR]
 */
fun Context.copyToClipboard(value: Any, label: String = "") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

    val clip: ClipData = ClipData.newPlainText(label, value.toString())
    clipboard.setPrimaryClip(clip)
}

fun TextView.copyTextToClipboard(label: String = "") {
    this.context.copyToClipboard(this.text.toString(), label)
}

fun Context.getFromClipboard(default: CharSequence? = null): CharSequence? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return default
    val clipData = clipboard.primaryClip ?: return default
    if (clipData.itemCount == 0) return default

    return clipData.getItemAt(0).text
}