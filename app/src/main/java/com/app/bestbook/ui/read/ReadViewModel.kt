package com.app.bestbook.ui.read

import android.app.Dialog
import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.Book

class ReadViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val subject = savedStateHandle.get<String>("subject")
    val grade = savedStateHandle.get<String>("grade")
    val book = savedStateHandle.get<Book>("data")
    var pdfFile: String? = null
    var dialogSelectPage: Dialog? = null
}