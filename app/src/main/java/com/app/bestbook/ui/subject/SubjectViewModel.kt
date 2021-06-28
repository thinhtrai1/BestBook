package com.app.bestbook.ui.subject

import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.Subject
import java.io.Serializable

class SubjectViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val isAdmin = savedStateHandle.get<Boolean>("isAdmin")
    val grade = savedStateHandle.get<Int>("grade")
    val adapter = SubjectRcvAdapter(savedStateHandle.get<Serializable>("data") as List<Subject>, isAdmin)
}