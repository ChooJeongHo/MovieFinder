package com.choo.moviefinder.core.util

import androidx.lifecycle.SavedStateHandle

internal inline fun <reified T : Enum<T>> SavedStateHandle.getEnum(key: String, default: T): T =
    get<String>(key)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
