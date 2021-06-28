package com.app.bestbook.ui.updateSubject

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.Subject
import com.app.bestbook.util.Constant
import com.app.bestbook.util.Utility
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class UpdateSubjectViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val grade = savedStateHandle.get<Int>("grade")
    val subject = savedStateHandle.get<Subject>("subject")

    val subjectData = ArrayList<List<Subject>>().apply { Utility.generateData(this) }
    var imageUri: Uri? = null
    val databaseReference = Firebase.database(Constant.FIREBASE_DATABASE).reference.child("data")
    val storageReference = Firebase.storage.reference
}