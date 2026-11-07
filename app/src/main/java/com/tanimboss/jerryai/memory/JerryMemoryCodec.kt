// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.memory

import java.util.Base64
import java.util.UUID

/** Local, opt-in memory data. No AI calls, internet access, or old app data migration. */
data class JerryMemoryEntry(
    val id: String,
    val category: String,
    val text: String,
    val createdAt: Long,
)

object JerryMemoryCodec {
    const val HEADER = "JERRY_MEMORY_V1"
    const val MAX_ENTRIES = 200
    const val MAX_NOTE_CHARS = 700
    const val MAX_EXPORT_BYTES = 256_000
    val categories = listOf("personal", "preferences", "tasks", "other")

    fun add(
        current: List<JerryMemoryEntry>,
        category: String,
        text: String,
        createdAt: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString(),
    ): List<JerryMemoryEntry> {
        require(current.size < MAX_ENTRIES) { "Memory is full. Export a backup before removing notes." }
        val normalized = JerryMemoryEntry(id, category, text.trim(), createdAt)
        validate(normalized)
        require(current.none { it.id == normalized.id }) { "Duplicate memory ID." }
        return current + normalized
    }

    fun search(items: List<JerryMemoryEntry>, query: String): List<JerryMemoryEntry> {
        val needle = query.trim().lowercase().take(120)
        if (needle.length < 2) return emptyList()
        val terms = needle.split(Regex("[\\s,.;:!?।]+")).filter { it.length > 1 }.distinct().take(12)
        return items.withIndex()
            .mapNotNull { (index, note) ->
                val body = note.text.lowercase()
                val category = note.category.lowercase()
                var score = when {
                    body == needle -> 60
                    body.contains(needle) -> 30
                    category.contains(needle) -> 15
                    else -> 0
                }
                terms.forEach {
                    if (body.contains(it)) score += 5
                    if (category.contains(it)) score += 2
                }
                if (score > 0) Triple(note, score, index) else null
            }
            .sortedWith(compareByDescending<Triple<JerryMemoryEntry, Int, Int>> { it.second }.thenBy { it.third })
            .take(30).map { it.first }
    }

    /** Portable UTF-8 text backup: tab records and Base64-encoded note text. */
    fun encode(items: List<JerryMemoryEntry>): String {
        require(items.size <= MAX_ENTRIES) { "Too many memory entries." }
        require(items.map { it.id }.toSet().size == items.size) { "Duplicate memory IDs." }
        val rows = items.map { entry ->
            validate(entry)
            listOf(
                entry.id,
                entry.createdAt.toString(),
                entry.category,
                Base64.getEncoder().encodeToString(entry.text.toByteArray(Charsets.UTF_8)),
            ).joinToString("\t")
        }
        val text = HEADER + "\n" + rows.joinToString("\n")
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_EXPORT_BYTES) { "Memory backup is too large." }
        return text
    }

    fun decode(content: String): List<JerryMemoryEntry> {
        require(content.toByteArray(Charsets.UTF_8).size <= MAX_EXPORT_BYTES) { "Memory file exceeds the size limit." }
        val rows = content.trimEnd('\r', '\n').split('\n').map { it.removeSuffix("\r") }
        require(rows.firstOrNull() == HEADER) { "Not a Jerry Memory v1 backup." }
        require(rows.size - 1 <= MAX_ENTRIES) { "Memory file has too many entries." }
        val parsed = rows.drop(1).map { line ->
            val values = line.split('\t')
            require(values.size == 4) { "Invalid memory backup record." }
            val bytes = try { Base64.getDecoder().decode(values[3]) }
                catch (_: IllegalArgumentException) { throw IllegalArgumentException("Invalid memory backup text.") }
            val created = values[1].toLongOrNull() ?: throw IllegalArgumentException("Invalid memory date.")
            JerryMemoryEntry(values[0], values[2], String(bytes, Charsets.UTF_8), created).also { validate(it) }
        }
        require(parsed.map { it.id }.toSet().size == parsed.size) { "Duplicate backup memory IDs." }
        return parsed
    }

    /** Merge-only restore. Existing notes always win; importing never overwrites them. */
    fun merge(existing: List<JerryMemoryEntry>, imported: List<JerryMemoryEntry>): List<JerryMemoryEntry> {
        encode(existing)
        encode(imported)
        val result = existing.toMutableList()
        val ids = existing.mapTo(mutableSetOf()) { it.id }
        val content = existing.mapTo(mutableSetOf()) { it.category + "\u0000" + it.text.lowercase() }
        imported.forEach { entry ->
            val key = entry.category + "\u0000" + entry.text.lowercase()
            if (entry.id !in ids && key !in content) {
                require(result.size < MAX_ENTRIES) { "Not enough memory space to import without losing notes." }
                result.add(entry)
                ids.add(entry.id)
                content.add(key)
            }
        }
        return result
    }

    private fun validate(entry: JerryMemoryEntry) {
        require(entry.id.matches(Regex("[A-Za-z0-9-]{8,64}"))) { "Invalid memory ID." }
        require(entry.category in categories) { "Invalid memory category." }
        require(entry.text.isNotBlank() && entry.text.length <= MAX_NOTE_CHARS) { "Memory note must be 1–700 characters." }
        require(entry.createdAt > 0) { "Invalid creation time." }
    }
}
