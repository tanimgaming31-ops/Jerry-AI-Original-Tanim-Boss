// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import android.text.InputType
import com.tanimboss.jerryai.automation.protectedEditor
import com.tanimboss.jerryai.automation.searchField
import org.junit.Assert.*
import org.junit.Test

class EditorPolicyTest {
    @Test fun passwordFlagAlwaysBlocksEntry() { assertTrue(protectedEditor(true, InputType.TYPE_CLASS_TEXT)) }
    @Test fun passwordVariationsBlockEntryWithoutFlag() {
        listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD).forEach {
            assertTrue(protectedEditor(false, InputType.TYPE_CLASS_TEXT or it))
        }
        assertTrue(protectedEditor(false, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
    }
    @Test fun ordinaryEditorsAreNotBlocked() {
        assertFalse(protectedEditor(false, InputType.TYPE_CLASS_TEXT))
        assertFalse(protectedEditor(false, InputType.TYPE_CLASS_NUMBER))
    }
    @Test fun searchFieldRequiresClearSearchSemantics() {
        assertTrue(searchField("Search music", null, null))
        assertTrue(searchField(null, "Search", null))
        assertTrue(searchField(null, null, "app:id/search_src_text"))
        assertFalse(searchField("Research notes", null, "app:id/research"))
        assertFalse(searchField("Message", "Send a message", "app:id/message"))
        assertFalse(searchField(null, null, null))
    }
}
