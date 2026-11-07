// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import com.tanimboss.jerryai.memory.JerryMemoryCodec
import com.tanimboss.jerryai.memory.JerryMemoryEntry
import org.junit.Assert.*
import org.junit.Test

class JerryMemoryTest {
    @Test fun addsAndRanksBengaliAndEnglishNotes() {
        val one = JerryMemoryCodec.add(emptyList(), "preferences", "চা খেতে ভালো লাগে", 100, "entry-0001")
        val two = JerryMemoryCodec.add(one, "tasks", "Buy tea", 200, "entry-0002")
        val three = JerryMemoryCodec.add(two, "personal", "tea", 300, "entry-0003")
        assertEquals(listOf("entry-0003", "entry-0002"), JerryMemoryCodec.search(three, "TEA").map { it.id })
        assertEquals(listOf("entry-0001"), JerryMemoryCodec.search(three, "চা").map { it.id })
        assertTrue(JerryMemoryCodec.search(three, "").isEmpty())
        assertTrue(JerryMemoryCodec.search(three, "unknown").isEmpty())
    }

    @Test fun backupRoundTripPreservesUnicodeAndTextExactly() {
        val original = listOf(
            JerryMemoryEntry("entry-0001", "personal", "বাংলা এবং Tagalog 💜\nSecond line", 1234),
            JerryMemoryEntry("entry-0002", "tasks", "Buy eggs", 4567),
        )
        assertEquals(original, JerryMemoryCodec.decode(JerryMemoryCodec.encode(original)))
        assertEquals(emptyList<JerryMemoryEntry>(), JerryMemoryCodec.decode(JerryMemoryCodec.HEADER))
    }

    @Test fun restoreNeverOverwritesExistingEntries() {
        val existing = listOf(JerryMemoryEntry("entry-0001", "personal", "Keep this old text", 1234))
        val incoming = listOf(
            JerryMemoryEntry("entry-0001", "personal", "Malicious replacement", 9000),
            JerryMemoryEntry("entry-0002", "personal", "Keep this old text", 9999),
            JerryMemoryEntry("entry-0003", "tasks", "New task", 2345),
        )
        assertEquals(existing + incoming.last(), JerryMemoryCodec.merge(existing, incoming))
        assertEquals(existing, JerryMemoryCodec.merge(existing, existing))
    }

    @Test fun invalidBackupsAndOversizeNotesAreRejected() {
        val invalid = listOf(
            "", "MAYA_MEMORY_V1\n", "JERRY_MEMORY_V0\n",
            JerryMemoryCodec.HEADER + "\ninvalid",
            JerryMemoryCodec.HEADER + "\nentry-0001\tno-date\tother\tSGVsbG8=",
            JerryMemoryCodec.HEADER + "\nentry-0001\t10\tother\t%%%bad",
            JerryMemoryCodec.HEADER + "\nentry-0001\t10\tbad-category\tSGVsbG8=",
        )
        invalid.forEach { raw ->
            try { JerryMemoryCodec.decode(raw); fail("Must reject invalid backup") }
            catch (_: IllegalArgumentException) { }
        }
        try {
            JerryMemoryCodec.add(emptyList(), "personal", "A".repeat(701), 10L, "entry-0001")
            fail("Must reject oversized note")
        } catch (_: IllegalArgumentException) { }
        try {
            JerryMemoryCodec.decode("x".repeat(JerryMemoryCodec.MAX_EXPORT_BYTES + 1))
            fail("Must reject oversized file")
        } catch (_: IllegalArgumentException) { }
    }
}
