package com.centiox.verdant

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.*
import kotlin.math.*

val backgroundPresets =
    listOf(
        "Forest" to DEFAULT_BACKGROUND,
        "Ocean" to "#268CBA",
        "Amethyst" to "#9863CE",
        "Ember" to "#D17A43",
        "Rose" to "#BE5D84",
        "Teal" to "#329F91",
    )

fun backgroundColor(hex: String): Color = Color(0xFF000000 or hex.drop(1).toLong(16))

fun colorHex(color: Color): String =
    "#" +
        listOf(color.red, color.green, color.blue)
            .joinToString("") {
                (it * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0')
            }
            .uppercase()

/**
 * Keep every mesh layer dark enough for foreground labels, including white/yellow custom colors.
 */
private fun meshTint(color: Color, strength: Float): Color {
    var low = 0f
    var high = strength
    repeat(12) {
        val middle = (low + high) / 2
        if (lerp(Color.Black, color, middle).luminance() <= .045f) low = middle else high = middle
    }
    return lerp(Color.Black, color, low)
}

/**
 * Drifting palette mesh inspired by Music App's GlowMesh/LiquidArtwork. No image decoding, network,
 * full-screen blur, or Android-version-specific shaders. A smaller offscreen canvas is scaled up to
 * fill the window. InfiniteTransition respects Compose's motion scale and pauses with the Android
 * window frame clock.
 */
@Composable
fun LiquidBackground(hex: String, moving: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(backgroundColor(hex), tween(700), label = "Liquid palette")
    val phase: State<Float> =
        if (moving) {
            rememberInfiniteTransition(label = "Liquid drift")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * PI).toFloat(),
                    animationSpec =
                        infiniteRepeatable(
                            tween(80_000, easing = LinearEasing),
                            RepeatMode.Restart,
                        ),
                    label = "Liquid phase",
                )
        } else remember { mutableFloatStateOf(0f) }
    val shades =
        remember(color) {
            listOf(
                meshTint(color, .72f),
                meshTint(lerp(color, Color.White, .12f), .65f),
                meshTint(color, .35f),
            )
        }
    val base = remember(color) { meshTint(color, .12f) }
    BoxWithConstraints(modifier.clipToBounds()) {
        val density = LocalDensity.current
        val width = (constraints.maxWidth / 3f).roundToInt().coerceAtLeast(1)
        val height = (constraints.maxHeight / 3f).roundToInt().coerceAtLeast(1)
        Canvas(
            Modifier.size(with(density) { width.toDp() }, with(density) { height.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = constraints.maxWidth.toFloat() / width
                    scaleY = constraints.maxHeight.toFloat() / height
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            drawRect(base)
            val t = phase.value
            val anchors =
                listOf(
                    Offset(.18f, .2f),
                    Offset(.8f, .35f),
                    Offset(.4f, .72f),
                    Offset(.88f, .88f),
                    Offset(.18f, .94f),
                    Offset(.6f, .05f),
                )
            anchors.forEachIndexed { index, anchor ->
                // Integer harmonics keep both position and velocity continuous at the loop seam.
                val speed = (index % 3 + 1) * if (index % 2 == 0) 1 else -1
                val seed = index * 1.7f
                val center =
                    Offset(
                        (anchor.x + .18f * cos(t * speed + seed)) * size.width,
                        (anchor.y + .14f * sin(t * speed + seed * .7f)) * size.height,
                    )
                val radius = size.maxDimension * (.36f + .05f * sin(t + seed))
                val tint = shades[index % shades.size]
                drawCircle(
                    Brush.radialGradient(
                        0f to tint.copy(alpha = .94f),
                        .48f to tint.copy(alpha = .58f),
                        1f to tint.copy(alpha = 0f),
                        center = center,
                        radius = radius,
                    ),
                    radius,
                    center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePanel(data: BudgetData, busy: Boolean, onChange: (String, Boolean) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Make it yours.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Liquid background", fontSize = 12.sp, color = Muted)
        Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp))) {
            LiquidBackground(
                data.backgroundColor,
                data.animateBackground,
                Modifier.matchParentSize(),
            )
            Text("Balance header preview", Modifier.align(Alignment.Center), fontSize = 12.sp)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            backgroundPresets.forEach { (name, hex) ->
                FilterChip(
                    selected = hex.equals(data.backgroundColor, true),
                    onClick = { onChange(hex, data.animateBackground) },
                    enabled = !busy,
                    label = { Text(name, fontSize = 11.sp) },
                    leadingIcon = {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(backgroundColor(hex)))
                    },
                    modifier =
                        Modifier.weight(1f).semantics { contentDescription = "$name background" },
                )
            }
        }
        OutlinedButton({ custom = true }, Modifier.fillMaxWidth(), enabled = !busy) {
            Icon(Icons.Outlined.Palette, null, Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text("Custom color", fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(data.backgroundColor.uppercase(), fontSize = 10.sp, color = Muted)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Animate background", fontSize = 13.sp)
                Text("Turn off for less motion.", fontSize = 11.sp, color = Muted)
            }
            Switch(
                data.animateBackground,
                { onChange(data.backgroundColor, it) },
                enabled = !busy,
                modifier =
                    Modifier.testTag("background-motion").semantics {
                        contentDescription = "Animate background"
                    },
            )
        }
        if (data.backgroundColor != DEFAULT_BACKGROUND || !data.animateBackground)
            TextButton({ onChange(DEFAULT_BACKGROUND, true) }, enabled = !busy) {
                Text("Reset appearance", fontSize = 12.sp)
            }
    }
    if (custom)
        BackgroundColorPicker(
            data.backgroundColor,
            data.animateBackground,
            { custom = false },
            { hex ->
                onChange(hex, data.animateBackground)
                custom = false
            },
        )
}

@Composable
private fun BackgroundColorPicker(
    initial: String,
    moving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var hex by rememberSaveable { mutableStateOf(initial.uppercase()) }
    val valid = Regex("#[0-9A-Fa-f]{6}").matches(hex)
    val preview = if (valid) backgroundColor(hex) else backgroundColor(initial)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your color", fontSize = 20.sp) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(16.dp))) {
                    LiquidBackground(
                        if (valid) hex else initial,
                        moving,
                        Modifier.matchParentSize(),
                    )
                    Text(
                        "Liquid preview",
                        Modifier.align(Alignment.Center),
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
                OutlinedTextField(
                    hex,
                    { hex = it.take(7) },
                    singleLine = true,
                    label = { Text("Hex color") },
                    placeholder = { Text("#67A642") },
                    isError = !valid,
                    supportingText = {
                        Text(
                            if (valid) "RGB color" else "Use # and six hex digits.",
                            fontSize = 10.sp,
                        )
                    },
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth().testTag("background-hex"),
                )
                listOf("Red", "Green", "Blue").forEachIndexed { index, label ->
                    val channels = listOf(preview.red, preview.green, preview.blue)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, fontSize = 11.sp)
                        Text(
                            (channels[index] * 255).roundToInt().toString(),
                            fontSize = 11.sp,
                            color = Muted,
                        )
                    }
                    Slider(
                        channels[index],
                        { value ->
                            val c = channels.toMutableList().also { it[index] = value }
                            hex = colorHex(Color(c[0], c[1], c[2]))
                        },
                        modifier =
                            Modifier.fillMaxWidth().semantics {
                                contentDescription = "$label color channel"
                            },
                    )
                }
            }
        },
        confirmButton = {
            TextButton({ onSave(hex.uppercase()) }, enabled = valid) { Text("Apply color") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
