package com.app.bestbook.model

import java.io.Serializable

class Subject(var id: String, var name: String, val books: ArrayList<Book> = ArrayList()): Serializable