package com.patrick.core.alert

import android.content.Context
import android.widget.Toast

object UserAlert {
    @JvmStatic
    fun show(context: Context, message: CharSequence) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
