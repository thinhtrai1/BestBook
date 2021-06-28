package com.app.bestbook.ui.addBook

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.Book
import com.app.bestbook.model.Subject
import com.app.bestbook.util.Constant
import com.app.bestbook.util.Utility
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class AddBookViewModel(savedStateHandle: SavedStateHandle) : BaseViewModel() {
    val data = savedStateHandle.get<Book>("data")
    val grade = savedStateHandle.get<Int>("grade")
    val subject = savedStateHandle.get<String>("subject")

    val subjectData = ArrayList<List<Subject>>().apply { Utility.generateData(this) }
    val databaseReference = Firebase.database(Constant.FIREBASE_DATABASE).reference.child("data")
    val storageReference = Firebase.storage.reference
    var fileUri: Uri? = null
    var imageUri: Uri? = null
    var bookUrl: String? = null
    var imageUrl: String? = null
}