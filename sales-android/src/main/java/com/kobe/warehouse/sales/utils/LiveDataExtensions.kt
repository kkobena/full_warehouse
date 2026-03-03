package com.kobe.warehouse.sales.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Observe a LiveData only once, then remove the observer automatically.
 * Prevents observer leaks when observing inside click handlers or dialogs.
 */
fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, onChanged: (T) -> Unit) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            onChanged(value)
            removeObserver(this)
        }
    })
}

