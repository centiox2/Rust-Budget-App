package com.centiox.verdant

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.centiox.verdant.resources.*
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

val Lime = Color(0xFF9BD16B)
val Muted = Color(0xFFAEB6A7)
val Card = Color(0xFF191A18)
val HomeBackground = Color(0xFF0D110D)
val Line = Color(0xFF2B2D29)
val Orange = Color(0xFFDC994A)

@Composable
fun VerdantTheme(content: @Composable () -> Unit) {
    val family =
        FontFamily(
            Font(Res.font.poppins_regular, FontWeight.Normal),
            Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        )
    CompositionLocalProvider(
        LocalContentColor provides Color(0xFFF5F5F0),
        LocalTextStyle provides
            androidx.compose.ui.text.TextStyle(fontFamily = family, fontSize = 14.sp),
        androidx.compose.foundation.text.selection.LocalTextSelectionColors provides
            androidx.compose.foundation.text.selection.TextSelectionColors(
                Lime,
                Lime.copy(alpha = .28f),
            ),
    ) {
        content()
    }
}

@Composable
fun AmountText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    color: Color = LocalContentColor.current,
    weight: FontWeight = FontWeight.SemiBold,
) {
    BasicText(
        text,
        modifier,
        style =
            LocalTextStyle.current.copy(fontSize = fontSize, color = color, fontWeight = weight),
        maxLines = 1,
        autoSize =
            TextAutoSize.StepBased(
                minFontSize = minOf(11f, fontSize.value).sp,
                maxFontSize = fontSize,
            ),
    )
}

@Composable
fun RoundAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 48.dp,
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
        containerColor = if (glow) Color.Transparent else Color(0xFF214E16),
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
        modifier.clip(RoundedCornerShape(24.dp)).background(Card.copy(alpha = .94f)),
        content = content,
    )
}

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
    horizontalPadding: Dp = 16.dp,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 8.dp),
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
        IconButton(onClick = { onChange(shiftMonth(month, -1)) }, enabled = month > MIN_MONTH) {
            Icon(Icons.Outlined.ChevronLeft, "Previous month", Modifier.size(20.dp))
        }
        Text(monthLabel(month), Modifier.weight(1f), fontSize = 12.sp, maxLines = 2)
        IconButton(onClick = { onChange(shiftMonth(month, 1)) }, enabled = month < MAX_MONTH) {
            Icon(Icons.Outlined.ChevronRight, "Next month", Modifier.size(20.dp))
        }
        if (month != currentMonth())
            TextButton(
                { onChange(currentMonth()) },
                Modifier.semantics { contentDescription = "Go to current month" },
            ) {
                Text("Current", fontSize = 11.sp)
            }
    }
}

@Composable
fun EntryRows(
    entries: List<Entry>,
    currency: String,
    onEdit: (Entry) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 14.dp,
) {
    Column(modifier) {
        entries.forEachIndexed { index, entry ->
            val amount = "${if(entry.type=="income")"+" else "−"}${money(entry.amount,currency)}"
            val stacked = LocalDensity.current.fontScale >= 1.3f || amount.length > 17
            if (index > 0) HorizontalDivider(color = Line, thickness = .5.dp)
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onEdit(entry) }
                    .padding(horizontal = horizontalPadding, vertical = 17.dp),
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
                    if (stacked) {
                        Spacer(Modifier.height(6.dp))
                        AmountText(amount, Modifier.fillMaxWidth(), fontSize = 12.sp)
                    }
                }
                if (!stacked)
                    AmountText(amount, modifier = Modifier.widthIn(max = 140.dp), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun Progress(value: Float, modifier: Modifier = Modifier, color: Color = Lime) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by
        animateFloatAsState(
            if (entered) value.coerceIn(0f, 1f) else 0f,
            tween(550),
            label = "Budget progress",
        )
    Box(modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0xFF30352B))) {
        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(color))
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
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
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
                        (a.y + (b.y - before.y) / 6).coerceIn(size.height * .18f, baseline),
                        b.x - (after.x - a.x) / 6,
                        (b.y - (after.y - a.y) / 6).coerceIn(size.height * .18f, baseline),
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
        clipRect(right = size.width * reveal.value) {
            drawPath(
                fill,
                Brush.verticalGradient(listOf(color.copy(alpha = .2f), Color.Transparent)),
            )
            drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
fun RibbonArt(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.ribbon), null, modifier, contentScale = ContentScale.Crop)
}
