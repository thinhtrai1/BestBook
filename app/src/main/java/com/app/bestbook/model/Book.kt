package com.app.bestbook.model

import java.util.*

class Book (var name: String, var url: String, var startPage: Int, var time: Long = Calendar.getInstance().timeInMillis)