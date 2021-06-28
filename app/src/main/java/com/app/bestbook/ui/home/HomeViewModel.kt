package com.app.bestbook.ui.home

import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.User
import com.app.bestbook.util.Constant
import com.app.bestbook.util.SharedPreferencesHelper
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class HomeViewModel : BaseViewModel() {
    val firebaseDatabase = Firebase.database(Constant.FIREBASE_DATABASE).reference
    val sharedPreferencesHelper = SharedPreferencesHelper()
    var user: User? = null

    val adapter1 = HomeRcvAdapter(listOf(1, 2, 3, 4, 5)) {
        getData(it)
    }
    val adapter2 = HomeRcvAdapter(listOf(6, 7, 8, 9)) {
        getData(it)
    }
    val adapter3 = HomeRcvAdapter(listOf(10, 11, 12)) {
        getData(it)
    }
    lateinit var classData: (Int) -> Unit

    private fun getData(classSelected: Int) {
        classData.invoke(classSelected)
    }
}