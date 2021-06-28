package com.app.bestbook.ui.book

import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.Book

class BookViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val adapter = BookRcvAdapter(savedStateHandle.get<List<Book>>("data")!!, savedStateHandle.get<Boolean>("isAdmin"))
    val subject = savedStateHandle.get<String>("subject")
    val grade = savedStateHandle.get<Int>("grade")
}