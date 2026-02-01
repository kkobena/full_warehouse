package com.kobe.warehouse.sales.utils

import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Extension function to create a Flow from EditText text changes
 */
fun EditText.textChangesFlow(): Flow<String> = callbackFlow {
    val watcher = addTextChangedListener { text ->
        trySend(text.toString())
    }
    awaitClose { removeTextChangedListener(watcher) }
}

/**
 * Extension function to observe text changes with debounce
 *
 * @param lifecycleOwner Lifecycle owner for coroutine scope
 * @param debounceMs Debounce time in milliseconds (default 300ms)
 * @param minLength Minimum text length to trigger callback (default 0)
 * @param onTextChanged Callback invoked with debounced text
 */
@OptIn(FlowPreview::class)
fun EditText.onTextChangedDebounced(
    lifecycleOwner: LifecycleOwner,
    debounceMs: Long = 300L,
    minLength: Int = 0,
    onTextChanged: (String) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            textChangesFlow()
                .debounce(debounceMs)
                .distinctUntilChanged()
                .filter { it.length >= minLength || it.isEmpty() }
                .collect { text ->
                    onTextChanged(text.trim())
                }
        }
    }
}
