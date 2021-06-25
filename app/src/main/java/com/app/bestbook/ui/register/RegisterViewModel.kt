package com.app.bestbook.ui.register

import android.net.Uri
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.util.SharedPreferencesHelper
import java.io.File

class RegisterViewModel : BaseViewModel() {
    val sharedPreferencesHelper = SharedPreferencesHelper()
    var tempFile: File? = null
    var imageUri: Uri? = null
}