package com.app.bestbook.util

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.app.bestbook.application.Application

fun showToast(message: String?) {
    Toast.makeText(Application.instance, message, Toast.LENGTH_SHORT).show()
}

fun showToast(stringRes: Int) {
    Toast.makeText(Application.instance, stringRes, Toast.LENGTH_SHORT).show()
}

fun getString(stringRes: Int) = Application.instance.getString(stringRes)

fun getString(stringRes: Int, vararg formatArgs: Any?) = Application.instance.getString(stringRes, *formatArgs)

fun Context.isPermissionGranted(vararg permissions: String): Boolean {
    return permissions.indexOfFirst { ContextCompat.checkSelfPermission(this, it) != 0 } == -1
}

fun String.email() = plus("@vnbooks.com")
fun String.emailToUsername() = split("@vnbooks.com")[0]

fun metrics() = Resources.getSystem().displayMetrics!!