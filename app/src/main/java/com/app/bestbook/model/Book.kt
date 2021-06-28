package com.app.bestbook.model

import java.io.Serializable

class Book : Serializable {
    var id: String? = null
    var name: String? = null
    var url: String? = null
    var image: String? = null
    var startPage = 0
    var time = 0L
}