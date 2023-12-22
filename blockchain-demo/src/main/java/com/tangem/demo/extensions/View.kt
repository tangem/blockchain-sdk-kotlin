package com.tangem.demo.extensions

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager

/**
 * Created by Anton Zhilenkov on 12/08/2022.
 */
fun ViewParent?.beginDelayedTransition(transition: Transition = AutoTransition()) {
    if (this == null) return

    (this as? ViewGroup)?.beginDelayedTransition(transition)
}

fun ViewGroup.beginDelayedTransition(transition: Transition = AutoTransition()) {
    TransitionManager.beginDelayedTransition(this, transition)
}

fun View.beginDelayedTransition(transition: Transition = AutoTransition()) {
    (this as? ViewGroup)?.beginDelayedTransition(transition)
}

fun View.show(show: Boolean, invokeBeforeStateChanged: (() -> Unit)? = null) {
    return if (show) {
        this.show(invokeBeforeStateChanged)
    } else {
        this.hide(invokeBeforeStateChanged)
    }
}

fun View.show(invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (this.visibility == View.VISIBLE) return

    invokeBeforeStateChanged?.invoke()
    this.visibility = View.VISIBLE
}

fun View.hide(invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (this.visibility == View.GONE) return

    invokeBeforeStateChanged?.invoke()
    this.visibility = View.GONE
}

fun View.invisible(invisible: Boolean = true, invokeBeforeStateChanged: (() -> Unit)? = null) {
    if (invisible) {
        if (this.visibility == View.INVISIBLE) return

        invokeBeforeStateChanged?.invoke()
        this.visibility = View.INVISIBLE
    } else {
        this.show(invokeBeforeStateChanged)
    }
}

fun TextView.setTextFromClipboard() {
    context.getFromClipboard()?.let { this.text = it }
}
