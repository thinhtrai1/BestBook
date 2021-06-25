package com.app.bestbook.ui.read

import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.Book

class ReadViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val subject = savedStateHandle.get<String>("subject")
    val book = savedStateHandle.get<Book>("data")
    var pdfFile: String? = null
}