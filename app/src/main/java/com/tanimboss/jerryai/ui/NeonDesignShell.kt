// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object JerryNeon {
    val pink = Color(0xFFFF3499)
    val rose = Color(0xFFFF91CA)
    val gold = Color(0xFFFFCE77)
    val goldDeep = Color(0xFFEC9D48)
    val background = Color(0xFF0E0413)
    val dim = Color(0xFFD3BBD0)
    val green = Color(0xFF7FE3A8)
    val panel = Brush.linearGradient(listOf(Color(0xFF3A1531), Color(0xFF2B1029), Color(0xFF1A0A1C)))
    val edge = Brush.linearGradient(listOf(goldDeep, pink, rose))
    val backgroundGradient = Brush.verticalGradient(listOf(Color(0xFF0B0312), background, Color(0xFF26091D), background))
}

@Composable
internal fun JerryNeonBottomNavigation(current: String, onOpen: (String) -> Unit) {
    val destinations = listOf(
        Triple("HOME", "⌂", "Home"),
        Triple("CHAT", "▤", "Chat"),
        Triple("MEMORY", "◎", "Memory"),
        Triple("ACTIONS", "▦", "Tools"),
        Triple("ABOUT", "♙", "Profile"),
    )
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF32152E), Color(0xFF160919))))
            .border(1.dp, JerryNeon.edge, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        destinations.forEach { (target, icon, name) ->
            val selected = current == target
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF95295F), Color(0xFF551533)))
                        else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onOpen(target) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(icon, color = if (selected) JerryNeon.gold else JerryNeon.dim, fontSize = 23.sp)
                Text(name, color = if (selected) Color.White else JerryNeon.dim, fontSize = 11.sp)
            }
        }
    }
}

/** Reuse the same neon design on every screen; never restore the old multi-row chip navigation. */
@Composable
internal fun JerryNeonPageShell(
    title: String, current: String, onOpen: (String) -> Unit,
    showBottom: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(JerryNeon.backgroundGradient)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹", color = JerryNeon.gold, fontSize = 38.sp,
                modifier = Modifier.clickable { onOpen("HOME") }.padding(end = 14.dp))
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            Text("?", color = JerryNeon.gold, fontSize = 23.sp,
                modifier = Modifier.clickable { onOpen("DIAGNOSTICS") }.padding(7.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            content = content,
        )
        if (showBottom) JerryNeonBottomNavigation(current = current, onOpen = onOpen)
    }
}

@Composable
internal fun JerryNeonInfoCard(title: String, body: String, onClick: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(JerryNeon.panel)
            .border(1.dp, JerryNeon.edge, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = JerryNeon.gold, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(body, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
