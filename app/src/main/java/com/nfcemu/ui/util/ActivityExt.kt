package com.nfcemu.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Compose's `LocalContext.current` is often a wrapped Context - walk up to find the real Activity. */
internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
