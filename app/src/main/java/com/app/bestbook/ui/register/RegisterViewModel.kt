package com.app.bestbook.ui.register

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.User
import com.app.bestbook.util.SharedPreferencesHelper
import java.io.File

class RegisterViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val sharedPreferencesHelper = SharedPreferencesHelper()
    var tempFile: File? = null
    var imageUri: Uri? = null
    val user = savedStateHandle.get<User>("user")
}