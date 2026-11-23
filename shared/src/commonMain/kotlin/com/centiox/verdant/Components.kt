package com.centiox.verdant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.centiox.verdant.resources.*
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

val Lime = Color(0xFF9BD16B)
val Muted = Color(0xFF9A9B98)
val Card = Color(0xFF191A18)
val Line = Color(0xFF2B2D29)
val Orange = Color(0xFFDC994A)

@Composable
fun VerdantTheme(content: @Composable () -> Unit) {
    val family =
        FontFamily(
            Font(Res.font.poppins_regular, FontWeight.Normal),
            Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        )
    val base = Typography()
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Lime,
                onPrimary = Color(0xFF14230E),
                primaryContainer = Color(0xFF263D1C),
                onPrimaryContainer = Color(0xFFE1EFD4),
                onSecondary = Color(0xFF17250F),
                secondaryContainer = Color(0xFF2B4021),
                onSecondaryContainer = Color(0xFFD8E8CD),
                tertiary = Color(0xFFD8AE67),
                tertiaryContainer = Color(0xFF443117),
                onTertiaryContainer = Color(0xFFF8DEB3),
                surfaceVariant = Color(0xFF23291F),
                onSurfaceVariant = Color(0xFFB6BCAF),
                surfaceTint = Lime,
                surfaceContainer = Card,
                surfaceContainerLow = Color(0xFF141611),
                surfaceContainerLowest = Color(0xFF080B07),
                surfaceContainerHigh = Color(0xFF22251F),
                surfaceContainerHighest = Color(0xFF2A2F26),
                surfaceBright = Color(0xFF2C3227),
                surfaceDim = Color(0xFF10130E),
                background = Color.Black,
                surface = Card,
                onSurface = Color(0xFFF5F5F0),
                onBackground = Color.White,
                secondary = Lime,
                outline = Color(0xFF454C3E),
                error = Color(0xFFF3AD98),
            ),
        typography =
            Typography(
                displayLarge = base.displayLarge.copy(fontFamily = family),
                displayMedium = base.displayMedium.copy(fontFamily = family),
                displaySmall = base.displaySmall.copy(fontFamily = family),
                headlineLarge = base.headlineLarge.copy(fontFamily = family),
                headlineMedium = base.headlineMedium.copy(fontFamily = family),
                headlineSmall = base.headlineSmall.copy(fontFamily = family),
                titleLarge = base.titleLarge.copy(fontFamily = family),
                titleMedium = base.titleMedium.copy(fontFamily = family),
                titleSmall = base.titleSmall.copy(fontFamily = family),
                bodyLarge = base.bodyLarge.copy(fontFamily = family),
                bodyMedium = base.bodyMedium.copy(fontFamily = family),
                bodySmall = base.bodySmall.copy(fontFamily = family),
                labelLarge = base.labelLarge.copy(fontFamily = family),
                labelMedium = base.labelMedium.copy(fontFamily = family),
                labelSmall = base.labelSmall.copy(fontFamily = family),
            ),
        content = {
            CompositionLocalProvider(LocalContentColor provides Color(0xFFF5F5F0)) {
                CompositionLocalProvider(
                    LocalTextStyle provides
                        MaterialTheme.typography.bodyMedium.copy(lineHeight = TextUnit.Unspecified)
                ) {
                    content()
                }
            }
        },
    )
}

@Composable
fun RoundAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 44.dp,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = .07f)),
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(23.dp))
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glow: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(30.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (glow) Color.Transparent else Color(0xFF214E16),
                contentColor = Color.White,
            ),
        border = BorderStroke(1.dp, Color(0xFF37762A)),
        modifier =
            modifier.fillMaxWidth().heightIn(min = 51.dp).drawBehind {
                if (glow)
                    drawRoundRect(
                        Brush.radialGradient(
                            listOf(Color(0xFF1C510B), Color(0xFF061402), Color.Black),
                            center = Offset(size.width * .5f, size.height * 1.3f),
                            radius = size.width * .55f,
                        ),
                        cornerRadius = CornerRadius(size.height / 2),
                    )
            },
    ) {
        Text(text, fontSize = 14.sp)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Outlined.ArrowForward, null, Modifier.size(18.dp))
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Card, Color(0xFF111310)))),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (action != null)
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(action, fontSize = 11.sp, color = Color.White)
            }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.AccountBalanceWallet,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = Lime, modifier = Modifier.size(30.dp))
        Text(title, fontSize = 16.sp)
        Text(description, fontSize = 12.sp, color = Muted)
        if (action != null) TextButton(onClick = onAction) { Text(action, color = Lime) }
    }
}

@Composable
fun MonthPicker(month: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(shiftMonth(month, -1)) }) {
            Icon(Icons.Outlined.ChevronLeft, "Previous month", Modifier.size(20.dp))
        }
        Text(monthLabel(month), fontSize = 12.sp)
        IconButton(onClick = { onChange(shiftMonth(month, 1)) }) {
            Icon(Icons.Outlined.ChevronRight, "Next month", Modifier.size(20.dp))
        }
        Spacer(Modifier.weight(1f))
        if (LocalDensity.current.fontScale < 1.2f)
            Text("Monthly overview", fontSize = 9.sp, color = Muted)
    }
}

@Composable
fun EntryRows(
    entries: List<Entry>,
    currency: String,
    onEdit: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = Line, thickness = .5.dp)
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onEdit(entry) }
                    .padding(horizontal = 14.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2B2C29)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (entry.type == "income") Icons.Outlined.SouthWest
                        else Icons.Outlined.NorthEast,
                        null,
                        tint = if (entry.type == "income") Orange else Lime,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${entry.category} · ${entry.date.substring(5)}",
                        fontSize = 10.sp,
                        color = Muted,
                    )
                }
                Text(
                    "${if(entry.type=="income")"+" else "−"}${money(entry.amount,currency)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun Progress(value: Float, modifier: Modifier = Modifier, color: Color = Lime) {
    Box(modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0xFF30352B))) {
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

fun categoryIcon(name: String): ImageVector =
    when (name) {
        "Groceries" -> Icons.Outlined.ShoppingBasket
        "Dining" -> Icons.Outlined.Restaurant
        "Transport" -> Icons.Outlined.DirectionsBus
        "Shopping" -> Icons.Outlined.ShoppingBag
        "Utilities" -> Icons.Outlined.Bolt
        "Health" -> Icons.Outlined.FavoriteBorder
        "Entertainment" -> Icons.Outlined.Movie
        else -> Icons.Outlined.MoreHoriz
    }

fun categoryColor(name: String): Color =
    when (name) {
        "Groceries" -> Color(0xFFE0B879)
        "Dining" -> Color(0xFFD99B74)
        "Transport" -> Color(0xFF91BECC)
        "Shopping" -> Color(0xFFBDA0D9)
        "Utilities" -> Color(0xFFC9DB71)
        else -> Lime
    }

fun dailyValues(entries: List<Entry>, month: String): List<Long> {
    val date = kotlinx.datetime.LocalDate.parse("$month-01")
    val next = date.plus(1, kotlinx.datetime.DateTimeUnit.MONTH)
    val days = (next.toEpochDays() - date.toEpochDays()).toInt()
    val values = LongArray(days)
    entries.forEach { entry ->
        if (entry.date.startsWith(month)) {
            val index = entry.date.takeLast(2).toInt() - 1
            if (index in values.indices) values[index] += entry.amount
        }
    }
    return values.toList()
}

@Composable
fun SpendingLine(
    values: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF369B40),
    grid: Boolean = true,
) {
    Canvas(modifier) {
        val max = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val baseline = size.height * .9f
        if (grid)
            (0..4).forEach { i ->
                val y = size.height * .1f + i * size.height * .2f
                drawLine(
                    Color.White.copy(alpha = .1f),
                    Offset(0f, y),
                    Offset(size.width, y),
                    1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f)),
                )
            }
        if (values.size < 2) return@Canvas
        val points =
            values.mapIndexed { i, v ->
                Offset(
                    i * size.width / (values.size - 1),
                    baseline - v.toFloat() / max * size.height * .72f,
                )
            }
        val path =
            Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val a = points[i - 1]
                    val b = points[i]
                    val before = points[(i - 2).coerceAtLeast(0)]
                    val after = points[(i + 1).coerceAtMost(points.lastIndex)]
                    cubicTo(
                        a.x + (b.x - before.x) / 6,
                        a.y + (b.y - before.y) / 6,
                        b.x - (after.x - a.x) / 6,
                        b.y - (after.y - a.y) / 6,
                        b.x,
                        b.y,
                    )
                }
            }
        val fill =
            Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = .2f), Color.Transparent)))
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun RibbonArt(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.ribbon), null, modifier, contentScale = ContentScale.Crop)
}
