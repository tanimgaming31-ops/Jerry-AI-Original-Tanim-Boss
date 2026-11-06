// Copyright (c) 2026 Tanim Boss. See LICENSE.
// Original, native Compose design inspired by the owner's pink/gold Jerry reference.
package com.tanimboss.jerryai.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanimboss.jerryai.voice.VoicePhase
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

private val Rose = Color(0xFFFF399F)
private val RoseLight = Color(0xFFFF91CC)
private val Gold = Color(0xFFFFCD70)
private val GoldDeep = Color(0xFFE89D40)
private val Ink = Color(0xFF100615)
private val Panel = Color(0xFF2B1127)
private val Muted = Color(0xFFD1B8CB)
private val Mint = Color(0xFF87E6AD)

@Composable
internal fun JerryNeonHomeScreen(
    voicePhase: VoicePhase,
    keyConfigured: Boolean,
    onOpen: (String) -> Unit,
    onMic: () -> Unit,
) {
    val live = homeLiveStatus(voicePhase)
    val activeLabel = when (voicePhase) {
        VoicePhase.LISTENING -> "Listening..."
        VoicePhase.THINKING -> "Thinking..."
        VoicePhase.SPEAKING -> "Speaking..."
        VoicePhase.CONNECTING -> "Connecting..."
        VoicePhase.RECONNECTING -> "Reconnecting..."
        VoicePhase.MUTED -> "Microphone muted"
        VoicePhase.ERROR -> "Voice connection error"
        VoicePhase.IDLE -> "Tap the mic to start"
    }
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF09030F), Ink, Color(0xFF260A22), Ink)))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(52.dp).border(2.dp, Brush.sweepGradient(listOf(Gold, Rose, Gold)), CircleShape)
                        .background(Color(0xFF260B2A), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("J", color = Gold, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Jerry AI Original", color = RoseLight, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Your Personal AI Assistant", color = Muted, fontSize = 11.sp)
                }
                Text("ⓘ", modifier = Modifier.clip(CircleShape).clickable { onOpen("DIAGNOSTICS") }.padding(8.dp),
                    color = Gold, fontSize = 24.sp)
                Text("⚙", modifier = Modifier.clip(CircleShape).clickable { onOpen("SETTINGS") }.padding(8.dp),
                    color = Gold, fontSize = 26.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonBadge("♛", "Tanim Boss", "Owner & Publisher", Modifier.weight(1f)) { onOpen("ABOUT") }
                NeonBadge("✦", "Gemini Live", live.label, Modifier.weight(1f)) { onOpen("VOICE") }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                JerryAnimatedOrb(Modifier.size(204.dp), voicePhase)
                AnimatedWave(Modifier.fillMaxWidth(0.73f).height(35.dp), voicePhase)
                Text(activeLabel, color = if (live.active) RoseLight else Gold, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (!keyConfigured) "Add your own Gemini key on the Voice screen"
                    else if (live.active) "Foreground voice session active"
                    else "Tap Voice to start a foreground session",
                    color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PhasePill("Listen", voicePhase == VoicePhase.LISTENING)
                    PhasePill("Think", voicePhase == VoicePhase.THINKING)
                    PhasePill("Speak", voicePhase == VoicePhase.SPEAKING)
                    PhasePill("Idle", voicePhase == VoicePhase.IDLE)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NeonShortcut("🚀", "Open Apps", "Confirm", Modifier.weight(1f)) { onOpen("ACTIONS") }
                NeonShortcut("☎", "Call Contact", "Dialer", Modifier.weight(1f)) { onOpen("ACTIONS") }
                NeonShortcut("◌", "WhatsApp", "Planned", Modifier.weight(1f)) { onOpen("FEATURES") }
                NeonShortcut("▶", "YouTube", "Planned", Modifier.weight(1f)) { onOpen("FEATURES") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NeonShortcut("▣", "Camera & Scan", "Camera", Modifier.weight(1f)) { onOpen("CAMERA") }
                NeonShortcut("▤", "Read Documents", "Planned", Modifier.weight(1f)) { onOpen("FEATURES") }
                NeonShortcut("A文", "Translate & Interpret", "Voice only", Modifier.weight(1f)) { onOpen("VOICE") }
                NeonShortcut("▱", "More Tools", "View list", Modifier.weight(1f)) { onOpen("FEATURES") }
            }
            Box(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 3.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(90.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFCA337E), Color(0xFF521536), Color(0xFF210C24))))
                        .border(3.dp, Brush.sweepGradient(listOf(Rose, Gold, RoseLight, GoldDeep, Rose)), CircleShape)
                        .clickable(onClick = onMic),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎙", color = Gold, fontSize = 44.sp)
                }
            }
            Spacer(Modifier.height(5.dp))
        }
        JerryNeonBottomNavigation("HOME", onOpen)
    }
}


/** A small original robot/headset portrait drawn natively, never a cropped third-party image. */
@Composable
internal fun JerryRobotAvatar(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(brush = Brush.radialGradient(listOf(Rose.copy(alpha = 0.36f), Color.Transparent)),
            radius = w * 0.56f, center = Offset(w * .5f, h * .46f))
        // Rounded metallic shoulders and neck.
        drawRoundRect(brush = Brush.linearGradient(listOf(GoldDeep, Rose, Color(0xFF481637))),
            topLeft = Offset(w * .13f, h * .68f), size = Size(w * .74f, h * .27f),
            cornerRadius = CornerRadius(w * .14f))
        drawRoundRect(color = Color(0xFF171020), topLeft = Offset(w * .36f, h * .65f),
            size = Size(w * .28f, h * .14f), cornerRadius = CornerRadius(w * .08f))
        // Helmet, faceplate and gold headphone speakers.
        drawRoundRect(brush = Brush.linearGradient(listOf(RoseLight, Color(0xFF7C3276), Gold)),
            topLeft = Offset(w * .23f, h * .11f), size = Size(w * .54f, h * .59f),
            cornerRadius = CornerRadius(w * .21f))
        drawRoundRect(brush = Brush.linearGradient(listOf(Color(0xFF2E152A), Color(0xFF08070D))),
            topLeft = Offset(w * .27f, h * .22f), size = Size(w * .46f, h * .35f),
            cornerRadius = CornerRadius(w * .15f))
        for (x in listOf(.20f, .80f)) {
            drawCircle(color = Gold, radius = w * .125f, center = Offset(w * x, h * .43f),
                style = Stroke(width = 3.dp.toPx()))
            drawCircle(color = Color(0xFF7A284D), radius = w * .081f, center = Offset(w * x, h * .43f))
            drawCircle(color = GoldDeep, radius = w * .045f, center = Offset(w * x, h * .43f))
        }
        // Two lit visor eyes.
        for (x in listOf(.42f, .59f)) {
            drawRoundRect(color = RoseLight, topLeft = Offset(w * x - w * .055f, h * .355f),
                size = Size(w * .11f, h * .055f), cornerRadius = CornerRadius(w * .027f))
        }
        // Center chest emitter and antenna.
        drawCircle(color = Gold, radius = w * .052f, center = Offset(w * .5f, h * .82f))
        drawCircle(color = Color(0xFF1B0A18), radius = w * .029f, center = Offset(w * .5f, h * .82f))
        drawCircle(color = RoseLight, radius = w * .032f, center = Offset(w * .67f, h * .09f))
    }
}

@Composable
private fun NeonBadge(icon: String, title: String, description: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF39172B), Color(0xFF291126))))
            .border(1.dp, Brush.horizontalGradient(listOf(GoldDeep, Rose)), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(vertical = 9.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 22.sp, color = Gold)
        Spacer(Modifier.width(7.dp))
        Column {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 9.sp, maxLines = 2)
        }
    }
}

@Composable
private fun NeonPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Panel, Color(0xFF351028), Color(0xFF190C1C))))
            .border(1.dp, Brush.linearGradient(listOf(Rose, GoldDeep, Rose.copy(alpha = 0.55f))), RoundedCornerShape(20.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun NeonShortcut(symbol: String, title: String, caption: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.heightIn(min = 85.dp).clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF4A1838), Color(0xFF210D21))))
            .border(1.dp, Color(0xB6FA80B8), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 3.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(symbol, color = Gold, fontSize = 25.sp, maxLines = 1)
        Text(title, color = Color.White, fontSize = 10.sp, lineHeight = 11.sp,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 2)
        Text(caption, color = if (caption == "Available") Mint else Gold, fontSize = 9.sp,
            textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun NeonTool(symbol: String, title: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(45.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF713253), Color(0xFF36142D))))
                .border(1.dp, Color(0xBBFBA0C5), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(symbol, color = Gold, fontSize = 21.sp, textAlign = TextAlign.Center) }
        Text(title, color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun NeonServiceRow(icon: String, title: String, value: String, statusColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0x48290B26)).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = Gold, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Text(value, color = statusColor, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun BottomNeonItem(symbol: String, title: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF932B61), Color(0xFF4C1937)))
                else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
            .clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(symbol, color = if (selected) Gold else Muted, fontSize = 24.sp)
        Text(title, color = if (selected) Color.White else Muted, fontSize = 10.sp)
    }
}

@Composable
private fun PhasePill(name: String, current: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(18.dp))
            .background(if (current) Color(0xFF7B244F) else Color(0xFF281326))
            .border(1.dp, if (current) Gold else Color(0xAA8C5175), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) { Text(name, color = if (current) Gold else Muted, fontSize = 10.sp) }
}

@Composable
private fun JerryAnimatedOrb(modifier: Modifier, phase: VoicePhase) {
    val motion = rememberInfiniteTransition(label = "Jerry neon breathing")
    val rotation by motion.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 14000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb rotation",
    )
    val breath by motion.animateFloat(
        initialValue = 0.88f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1700), RepeatMode.Reverse),
        label = "orb pulse",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer(rotationZ = rotation)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.345f
            drawCircle(brush = Brush.radialGradient(listOf(Rose.copy(alpha = 0.26f), Color.Transparent)),
                radius = size.minDimension * 0.48f, center = Offset(cx, cy))
            for (ring in 0..4) {
                drawCircle(
                    brush = Brush.sweepGradient(listOf(Rose, Gold, RoseLight, Color(0xFF952CFF), Rose)),
                    radius = radius + ring * 3.8.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = if (ring % 2 == 0) 2.9.dp.toPx() else 1.2.dp.toPx(), cap = StrokeCap.Round),
                    alpha = (0.87f - ring * 0.12f).coerceAtLeast(0.2f),
                )
            }
            for (i in 0..49) {
                val angle = (i.toFloat() / 50f) * (2 * PI)
                val distance = radius + 14.dp.toPx() + (i % 4) * 2.dp.toPx()
                drawCircle(
                    color = if (i % 3 == 0) Gold else Rose,
                    radius = if (i % 4 == 0) 1.2.dp.toPx() else 0.7.dp.toPx(),
                    center = Offset((cx + cos(angle).toFloat() * distance), (cy + sin(angle).toFloat() * distance)),
                    alpha = 0.65f,
                )
            }
        }
        Box(
            Modifier.size(149.dp).graphicsLayer(scaleX = breath, scaleY = breath)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF32102C), Color(0xFF140619))))
                .border(1.dp, Color(0xB8FF8BBD), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Jerry", color = Gold, fontSize = 37.sp, fontWeight = FontWeight.Bold)
                if (phase in setOf(VoicePhase.LISTENING, VoicePhase.THINKING, VoicePhase.SPEAKING)) {
                    Text("● LIVE", color = Mint, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun AnimatedWave(modifier: Modifier, phase: VoicePhase) {
    val motion = rememberInfiniteTransition(label = "Jerry sound wave")
    val cycle by motion.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1300, easing = LinearEasing), RepeatMode.Restart),
        label = "wave phase",
    )
    Canvas(modifier) {
        val bars = 39
        val gap = size.width / (bars + 1)
        val active = phase in setOf(VoicePhase.LISTENING, VoicePhase.SPEAKING, VoicePhase.THINKING)
        for (i in 1..bars) {
            val x = i * gap
            val height = if (active) {
                val envelope = 0.18f + 0.82f * (sin(PI * i / bars)).toFloat()
                (0.3f + 0.7f * kotlin.math.abs(sin(i * 0.67 + cycle.toDouble())).toFloat()) * envelope * size.height
            } else {
                (0.08f + 0.1f * kotlin.math.abs(sin(i * 0.46 + cycle.toDouble())).toFloat()) * size.height
            }
            drawLine(color = if (i % 5 == 0) Gold else Rose, start = Offset(x, (size.height - height) / 2),
                end = Offset(x, (size.height + height) / 2), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}
