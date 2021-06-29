package com.app.bestbook.ui.read

import android.app.Dialog
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.Book

class ReadViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val subject = savedStateHandle.get<String>("subject")
    val grade = savedStateHandle.get<Int>("grade")
    val book = savedStateHandle.get<Book>("data")
    var pdfFile: String? = savedStateHandle.get<Uri>("uri")?.path
    var dialogSelectPage: Dialog? = null
}