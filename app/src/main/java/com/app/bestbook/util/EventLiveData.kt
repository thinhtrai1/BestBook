package com.app.bestbook.util

class EventLiveData <T>(type: T? = null) {
    private var listener: ((T) -> Unit)? = null

    fun observer(listener: (T) -> Unit) {
        this.listener = listener
    }

    fun invoke(data: T) {
        listener?.invoke(data)
    }
}