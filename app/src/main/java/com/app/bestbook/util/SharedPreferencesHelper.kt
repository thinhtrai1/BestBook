package com.app.bestbook.util

import com.app.bestbook.application.Application

class SharedPreferencesHelper {
    private val mSharedPreferences = Application.instance.getSharedPreferences("BEST_BOOK_2021", 0)

    operator fun get(key: String, defaultValue: String? = null): String? {
        return mSharedPreferences.getString(key, defaultValue)
    }

    operator fun set(key: String, data: String?): SharedPreferencesHelper {
        mSharedPreferences.edit().putString(key, data).apply()
        return this
    }

    fun getSharedPreferences() = mSharedPreferences!!
}