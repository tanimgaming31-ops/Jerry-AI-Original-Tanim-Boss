// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.memory

import android.annotation.SuppressLint
import android.content.Context
import java.io.IOException

/** Separate namespace; never reads Maya data or Jerry theme preferences. */
class JerryMemoryRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("jerry_local_memory_v1", Context.MODE_PRIVATE)
    private val key = "backup_payload"

    @Synchronized fun read(): List<JerryMemoryEntry> =
        JerryMemoryCodec.decode(prefs.getString(key, JerryMemoryCodec.HEADER) ?: JerryMemoryCodec.HEADER)

    // Preserve Android commit()'s Boolean result: edit KTX would silently discard failures.
    @SuppressLint("UseKtx")
    @Synchronized fun save(items: List<JerryMemoryEntry>) {
        val encoded = JerryMemoryCodec.encode(items)
        if (!prefs.edit().putString(key, encoded).commit()) {
            throw IOException("Android could not persist local memory. No success was reported.")
        }
    }
}
