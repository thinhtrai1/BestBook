package com.app.bestbook.ui.subject

import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel

class SubjectViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val adapter = SubjectRcvAdapter()
}