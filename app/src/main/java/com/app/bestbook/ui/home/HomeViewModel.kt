package com.app.bestbook.ui.home

import com.app.bestbook.base.BaseViewModel
import com.app.bestbook.model.ClassData

class HomeViewModel : BaseViewModel() {
    val adapter1 = HomeRcvAdapter(listOf(1, 2, 3, 4, 5)) {
        getData(it)
    }
    val adapter2 = HomeRcvAdapter(listOf(6, 7, 8, 9)) {
        getData(it)
    }
    val adapter3 = HomeRcvAdapter(listOf(10, 11, 12)) {
        getData(it)
    }
    lateinit var classData: (ClassData) -> Unit

    private fun getData(classSelected: Int) {
        classData.invoke(ClassData())
    }
}