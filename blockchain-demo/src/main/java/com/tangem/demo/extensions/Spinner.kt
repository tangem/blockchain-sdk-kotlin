package com.tangem.demo.extensions

import android.view.View
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Spinner

/**
[REDACTED_AUTHOR]
 */
fun <T> Spinner.onItemSelected(callback: (T, Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        var pos: Int = 0

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            if (pos == position) return
            pos = position

            (parent.adapter as? BaseAdapter)?.let {
                callback(it.getItem(position) as T, position)
            }
        }
    }
}