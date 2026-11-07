// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tanimboss.jerryai.memory.JerryMemoryCodec
import com.tanimboss.jerryai.memory.JerryMemoryEntry
import com.tanimboss.jerryai.memory.JerryMemoryRepository
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Explicit user-managed memories. No AI auto-learning or silent cloud sync.
 * This screen intentionally uses a different local storage namespace from settings.
 */
@Composable
fun JerryMemoryScreen() {
    val context = LocalContext.current
    val repository = remember(context) { JerryMemoryRepository(context) }
    val initial = remember(repository) { runCatching { repository.read() } }
    val storageLocked = initial.isFailure

    var notes by remember { mutableStateOf(initial.getOrElse { emptyList() }) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("personal") }
    var expanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(if (storageLocked) "Stored memory is invalid. Nothing was erased or overwritten." else "") }
    var imported by remember { mutableStateOf<List<JerryMemoryEntry>?>(null) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                val content = JerryMemoryCodec.encode(notes)
                val output = context.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("Unable to open selected backup location.")
                output.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { status = "Memory backup saved to your selected document."; error = "" }
                .onFailure { error = "Backup failed: " + (it.message ?: "storage error") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !storageLocked) {
            runCatching {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to read selected document.")
                val buffer = ByteArray(8192)
                val bytes = ByteArrayOutputStream()
                input.use { source ->
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        if (bytes.size() + count > JerryMemoryCodec.MAX_EXPORT_BYTES) {
                            throw IllegalArgumentException("Memory backup exceeds 256 KB.")
                        }
                        bytes.write(buffer, 0, count)
                    }
                }
                JerryMemoryCodec.decode(bytes.toString("UTF-8"))
            }.onSuccess { imported = it; status = "Review the merge confirmation."; error = "" }
                .onFailure { error = "Import rejected, existing notes untouched: " + (it.message ?: "invalid file") }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Personal Memory", style = MaterialTheme.typography.headlineSmall)
        Text("Local memory v1. Save, find, export and import your own short notes. No automatic learning, Gemini upload or background sync.")
        Text("Privacy: notes are stored unencrypted on this phone. Backups are plain text with Base64 note encoding, NOT encryption. Never store passwords, PINs, API keys or OTPs.")
        Text("Saved entries: " + notes.size + " / " + JerryMemoryCodec.MAX_ENTRIES)
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it.take(JerryMemoryCodec.MAX_NOTE_CHARS) },
            label = { Text("Remember this (max 700 characters)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            enabled = !storageLocked,
        )
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = !storageLocked) {
                Text("Category: " + selectedCategory)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                JerryMemoryCodec.categories.forEach { category ->
                    DropdownMenuItem(text = { Text(category) }, onClick = {
                        selectedCategory = category
                        expanded = false
                    })
                }
            }
        }
        Button(
            enabled = !storageLocked && noteText.isNotBlank() && notes.size < JerryMemoryCodec.MAX_ENTRIES,
            onClick = {
                runCatching {
                    val next = JerryMemoryCodec.add(notes, selectedCategory, noteText)
                    repository.save(next)
                    next
                }.onSuccess {
                    notes = it
                    noteText = ""
                    status = "Memory saved on this device."
                    error = ""
                }.onFailure { error = "Save failed: " + (it.message ?: "unknown error") }
            },
        ) { Text("Save memory") }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(120) },
            label = { Text("Search your saved memory") },
            modifier = Modifier.fillMaxWidth(),
        )
        val visible = if (query.isBlank()) notes.asReversed().take(40) else JerryMemoryCodec.search(notes, query)
        Text(if (query.isBlank()) "Latest memories" else "Matching memories: " + visible.size)
        if (visible.isEmpty()) Text("No matching memories.")
        visible.forEach { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.category, style = MaterialTheme.typography.labelMedium)
                    Text(entry.text)
                    TextButton(onClick = { pendingDelete = entry.id }, enabled = !storageLocked) {
                        Text("Delete this note")
                    }
                }
            }
        }

        OutlinedButton(onClick = { exportLauncher.launch("Jerry-memory-backup-v1.txt") }, enabled = !storageLocked) {
            Text("Export memory backup")
        }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/plain", "application/octet-stream")) }, enabled = !storageLocked) {
            Text("Import and merge backup")
        }
        Text("Import checks the backup first. Existing notes are never replaced; duplicates are skipped.")
    }

    imported?.let { incoming ->
        AlertDialog(
            onDismissRequest = { imported = null },
            title = { Text("Merge backup?") },
            text = { Text("Import up to " + incoming.size + " entries. Existing memories will not be replaced. The operation stays on this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        val next = JerryMemoryCodec.merge(notes, incoming)
                        repository.save(next)
                        next
                    }.onSuccess {
                        status = "Backup merged. New entries: " + (it.size - notes.size)
                        notes = it
                        error = ""
                    }.onFailure { error = "Merge cancelled: " + (it.message ?: "storage error") }
                    imported = null
                }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { imported = null }) { Text("Cancel") } },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete memory?") },
            text = { Text("This deletes one local Jerry memory note. Export a backup first if needed.") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        val next = notes.filterNot { it.id == id }
                        repository.save(next)
                        next
                    }.onSuccess {
                        notes = it
                        status = "One memory note deleted."
                        error = ""
                    }.onFailure { error = "Delete failed: " + (it.message ?: "storage error") }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
