// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.automation

import android.text.InputType

/** Password variations stay protected even if a widget omits its password flag. */
fun protectedEditor(password: Boolean, inputType: Int): Boolean {
    val type = inputType and InputType.TYPE_MASK_CLASS
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    return password ||
        (type == InputType.TYPE_CLASS_TEXT && variation in setOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
        (type == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
}

fun searchField(hint: String?, description: String?, viewId: String?): Boolean {
    val label = Regex("^search(?:$|\\s.*)", RegexOption.IGNORE_CASE)
    return listOfNotNull(hint, description).any { label.matches(it.trim()) } ||
        viewId?.substringAfterLast('/') in setOf("search", "search_src_text", "search_field", "search_view", "search_edit_text")
}
