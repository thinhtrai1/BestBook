package com.app.bestbook.ui.book

import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel

class BookViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val adapter = BookRcvAdapter()
}