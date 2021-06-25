package com.app.bestbook.model

import java.io.Serializable

class Subject : Serializable {
    var id: String? = null
    var name: String? = null
    var image: String? = null
    val books = HashMap<String, Book>()
}