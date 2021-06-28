package com.app.bestbook.model

import java.io.Serializable

class User: Serializable {
    var id: String? = null
    var name: String? = null
    var phone: String? = null
    var grade: String? = null
    var image: String? = null
    var isAdmin = false
}