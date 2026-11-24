package com.centiox.verdant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*

val LocalContentColor = staticCompositionLocalOf { Color(0xFFF5F5F0) }
val LocalTextStyle = staticCompositionLocalOf { TextStyle(fontSize = 14.sp) }
val ErrorColor = Color(0xFFF3AD98)

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    val base = LocalTextStyle.current
    BasicText(
        text,
        modifier,
        style =
            base.copy(
                color = color,
                fontSize = if (fontSize.isUnspecified) base.fontSize else fontSize,
                fontWeight = fontWeight ?: base.fontWeight,
                textAlign = textAlign ?: base.textAlign,
                lineHeight = lineHeight,
                letterSpacing =
                    if (letterSpacing.isUnspecified) base.letterSpacing else letterSpacing,
            ),
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
    )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Image(
        rememberVectorPainter(imageVector),
        contentDescription,
        modifier.size(24.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    border: BorderStroke? = null,
    containerColor: Color = Lime.copy(alpha = .2f),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHaptic.current
    val reduced = LocalReducedMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            if (pressed) .98f else 1f,
            if (reduced) snap() else spring(dampingRatio = .82f, stiffness = 700f),
            label = "Button press",
        )
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = .4f))
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic()
                    onClick()
                },
            )
            .heightIn(min = 48.dp)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) Color.White else Muted) {
            content()
        }
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    border: BorderStroke = BorderStroke(1.dp, Color.White.copy(alpha = .18f)),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier, enabled, shape, border, Color.Transparent, contentPadding, content)

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) =
    Button(
        onClick,
        modifier,
        enabled,
        RoundedCornerShape(12.dp),
        null,
        Color.Transparent,
        contentPadding,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) Lime else Muted) {
            content()
        }
    }

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    shape: Shape = RoundedCornerShape(14.dp),
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value,
        onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = if (enabled) LocalContentColor.current else Muted),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(Lime),
        visualTransformation = visualTransformation,
        modifier =
            modifier
                .onFocusChanged { focused = it.isFocused }
                .clip(shape)
                .background(Color.Black.copy(alpha = .13f))
                .border(
                    if (focused) 1.5.dp else 1.dp,
                    if (isError) ErrorColor
                    else if (focused) Lime.copy(alpha = .75f) else Color.White.copy(alpha = .15f),
                    shape,
                ),
        decorationBox = { inner ->
            Column(
                Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (label != null)
                    CompositionLocalProvider(
                        LocalContentColor provides if (isError) ErrorColor else Muted,
                        LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 11.sp),
                    ) {
                        label()
                    }
                Row(
                    Modifier.heightIn(min = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (leadingIcon != null) leadingIcon()
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null)
                            CompositionLocalProvider(LocalContentColor provides Muted) {
                                placeholder()
                            }
                        inner()
                    }
                    if (trailingIcon != null) trailingIcon()
                }
                if (supportingText != null)
                    CompositionLocalProvider(
                        LocalContentColor provides if (isError) ErrorColor else Muted
                    ) {
                        supportingText()
                    }
            }
        },
    )
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val reduced = LocalReducedMotion.current
    val fill by
        animateColorAsState(
            if (selected) Lime.copy(alpha = .11f) else Color.Transparent,
            tween(if (reduced) 0 else 180),
            label = "Selection fill",
        )
    val outline by
        animateColorAsState(
            if (selected) Lime.copy(alpha = .6f) else Color.White.copy(alpha = .14f),
            tween(if (reduced) 0 else 180),
            label = "Selection border",
        )
    Row(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(fill)
            .border(1.dp, outline, RoundedCornerShape(13.dp))
            .selectable(selected, enabled = enabled, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) leadingIcon()
        CompositionLocalProvider(LocalContentColor provides if (selected) Color.White else Muted) {
            label()
        }
    }
}

@Composable
fun InputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    trailingIcon: @Composable () -> Unit,
) =
    FilterChip(
        selected,
        onClick,
        {
            label()
            trailingIcon()
        },
    )

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(48.dp)
            .toggleable(
                checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(19.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) Lime else Color.Transparent)
                .border(1.dp, if (checked) Lime else Muted, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), Color(0xFF15210F))
        }
    }
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val reduced = LocalReducedMotion.current
    val progress by
        animateFloatAsState(
            if (checked) 1f else 0f,
            tween(if (reduced) 0 else 170),
            label = "Switch position",
        )
    Box(
        modifier
            .size(52.dp, 48.dp)
            .toggleable(
                checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(44.dp, 24.dp)
                .clip(CircleShape)
                .background(
                    if (checked) Lime.copy(alpha = .23f) else Color.White.copy(alpha = .08f)
                )
                .border(
                    1.dp,
                    if (checked) Lime.copy(alpha = .6f) else Color.White.copy(alpha = .18f),
                    CircleShape,
                )
        ) {
            Box(
                Modifier.offset(x = (3 + 20 * progress).dp, y = 3.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (checked) Lime else Muted)
            )
        }
    }
}

@Composable
fun Slider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val accent = Lime
    val change by rememberUpdatedState(onValueChange)
    Canvas(
        modifier
            .height(40.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                setProgress {
                    change(it.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(x: Float) {
                        change(
                            ((x - 8.dp.toPx()) / (size.width - 16.dp.toPx()).coerceAtLeast(1f))
                                .coerceIn(0f, 1f)
                        )
                    }
                    update(down.position.x)
                    down.consume()
                    horizontalDrag(down.id) {
                        update(it.position.x)
                        it.consume()
                    }
                }
            }
    ) {
        val inset = 8.dp.toPx()
        val y = size.height / 2
        val x = inset + (size.width - 2 * inset) * value
        drawLine(
            Color.White.copy(alpha = .15f),
            Offset(inset, y),
            Offset(size.width - inset, y),
            3.dp.toPx(),
            StrokeCap.Round,
        )
        drawLine(accent, Offset(inset, y), Offset(x, y), 3.dp.toPx(), StrokeCap.Round)
        drawCircle(accent, 7.dp.toPx(), Offset(x, y))
        drawCircle(Card, 3.dp.toPx(), Offset(x, y))
    }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = Line, thickness: Dp = 1.dp) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Lime,
    strokeWidth: Dp = 2.dp,
) {
    val spin by
        rememberInfiniteTransition(label = "Loading")
            .animateFloat(
                0f,
                360f,
                infiniteRepeatable(tween(1000, easing = LinearEasing)),
                label = "Spin",
            )
    Canvas(modifier.size(30.dp)) {
        drawArc(color, spin, 250f, false, style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal expect fun OverlayWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit)

class SheetController(private val reduced: Boolean = false) {
    val fraction = Animatable(1f)

    suspend fun hide() = fraction.animateTo(1f, tween(if (reduced) 0 else 180))

    suspend fun show() = fraction.animateTo(0f, tween(if (reduced) 0 else 240))
}

@Composable
fun rememberSheetController(): SheetController {
    val reduced = LocalReducedMotion.current
    return remember(reduced) { SheetController(reduced) }
}

@Composable
fun SheetOverlay(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    sheetState: SheetController,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    fun dismiss() {
        if (dismissEnabled) scope.launch { finishSheet({ sheetState.hide() }, onDismissRequest) }
    }
    LaunchedEffect(sheetState) { sheetState.show() }
    OverlayWindow(::dismiss) {
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = .5f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {
            Box(
                Modifier.matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::dismiss,
                    )
            )
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * .94f)
                    .graphicsLayer { translationY = size.height * sheetState.fraction.value }
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(containerColor)
                    .border(
                        1.dp,
                        Color.White.copy(alpha = .10f),
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    )
                    .pointerInput(Unit) { detectTapGestures {} }
            ) {
                Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(36.dp, 3.dp)
                            .clip(CircleShape)
                            .background(Muted.copy(alpha = .5f))
                    )
                }
                content()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
) {
    OverlayWindow(onDismissRequest) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = .55f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    )
            )
            Column(
                Modifier.padding(22.dp)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Card)
                    .border(1.dp, Color.White.copy(alpha = .13f), RoundedCornerShape(24.dp))
                    .pointerInput(Unit) { detectTapGestures {} }
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                title()
                Box(Modifier.weight(1f, fill = false)) { text() }
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded)
        Popup(
            alignment = Alignment.TopStart,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            Column(
                Modifier.width(260.dp)
                    .heightIn(max = 340.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Card)
                    .border(1.dp, Line, RoundedCornerShape(17.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp),
                content = content,
            )
        }
}

@Composable
fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
    ) {
        text()
    }
}

class SnackbarHostState {
    var message by mutableStateOf<String?>(null)
        private set

    var lastMessage by mutableStateOf("")
        private set

    var actionLabel by mutableStateOf<String?>(null)
        private set

    private var action: (() -> Unit)? = null
    private var dismissal: CompletableDeferred<Unit>? = null

    fun performAction() {
        action?.invoke()
        dismissal?.complete(Unit)
    }

    private val mutex = Mutex()

    suspend fun showSnackbar(value: String, label: String? = null, onAction: (() -> Unit)? = null) =
        mutex.withLock {
            try {
                lastMessage = value
                actionLabel = label
                action = onAction
                val signal = CompletableDeferred<Unit>()
                dismissal = signal
                message = value
                withTimeoutOrNull(if (label == null) 2800L else 8000L) { signal.await() }
            } finally {
                message = null
                action = null
                actionLabel = null
                dismissal = null
            }
        }
}

@Composable
fun SnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    AnimatedVisibility(
        state.message != null,
        modifier,
        enter =
            fadeIn(tween(if (reduced) 0 else 180)) +
                slideInVertically(tween(if (reduced) 0 else 220)) { it / 3 },
        exit = fadeOut(tween(if (reduced) 0 else 140)),
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF17241C))
                .border(1.dp, Color(0xFF344D35), RoundedCornerShape(16.dp))
                .padding(15.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), Lime)
            Text(state.lastMessage, Modifier.weight(1f), fontSize = 12.sp)
            if (state.actionLabel != null)
                TextButton(state::performAction) { Text(state.actionLabel!!, fontSize = 12.sp) }
        }
    }
}

@Composable
fun BottomNavigation(selected: Page, onSelect: (Page) -> Unit) {
    val reduced = LocalReducedMotion.current
    Row(
        Modifier.fillMaxWidth()
            .background(Color(0xF5080D09))
            .padding(horizontal = 6.dp, vertical = 9.dp)
    ) {
        Page.entries.forEach { page ->
            val icon =
                when (page) {
                    Page.Home -> Icons.Outlined.Home
                    Page.Transactions -> Icons.Outlined.NorthEast
                    Page.Budgets -> Icons.Outlined.AccountBalanceWallet
                    Page.Analytics -> Icons.Outlined.BarChart
                    Page.Settings -> Icons.Outlined.Settings
                }
            val label =
                when (page) {
                    Page.Transactions -> "Activity"
                    Page.Analytics -> "Insights"
                    else -> page.label
                }
            val tint by
                animateColorAsState(
                    if (page == selected) Lime else Muted,
                    tween(if (reduced) 0 else 180),
                    label = "Tab tint",
                )
            val fill by
                animateColorAsState(
                    if (page == selected) Color.White.copy(alpha = .035f) else Color.Transparent,
                    tween(if (reduced) 0 else 200),
                    label = "Tab fill",
                )
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(fill)
                    .selectable(page == selected, role = Role.Tab, onClick = { onSelect(page) })
                    .semantics { contentDescription = page.label }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, null, Modifier.size(21.dp), tint)
                BasicText(
                    label,
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    style = LocalTextStyle.current.copy(color = tint, textAlign = TextAlign.Center),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
fun BudgetDatePicker(initialDate: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selected by remember {
        mutableStateOf(
            runCatching { LocalDate.parse(initialDate).takeIf { it.year in 1..9999 }?.toString() }
                .getOrNull() ?: today().toString()
        )
    }
    var month by remember { mutableStateOf(selected.take(7)) }
    AlertDialog(
        onDismiss,
        title = { Text("Choose date", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ month = shiftMonth(month, -1) }, enabled = month > MIN_MONTH) {
                        Icon(Icons.Outlined.ChevronLeft, "Previous calendar month")
                    }
                    Text(
                        monthLabel(month),
                        Modifier.weight(1f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    IconButton({ month = shiftMonth(month, 1) }, enabled = month < MAX_MONTH) {
                        Icon(Icons.Outlined.ChevronRight, "Next calendar month")
                    }
                }
                Row {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                        Text(
                            it,
                            Modifier.weight(1f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = Muted,
                        )
                    }
                }
                val first = LocalDate.parse("$month-01")
                val days =
                    (first.plus(1, DateTimeUnit.MONTH).toEpochDays() - first.toEpochDays()).toInt()
                val offset = first.dayOfWeek.ordinal
                (0 until (offset + days + 6) / 7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        (0..6).forEach { weekday ->
                            val day = week * 7 + weekday - offset + 1
                            if (day !in 1..days) Spacer(Modifier.weight(1f).height(40.dp))
                            else {
                                val date = "$month-${day.toString().padStart(2, '0')}"
                                Box(
                                    Modifier.weight(1f)
                                        .height(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected == date) Lime.copy(alpha = .22f)
                                            else Color.Transparent
                                        )
                                        .selectable(
                                            selected == date,
                                            role = Role.Button,
                                            onClick = { selected = date },
                                        )
                                        .semantics {
                                            contentDescription = "$day ${monthLabel(month)}"
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        day.toString(),
                                        fontSize = 12.sp,
                                        color = if (selected == date) Lime else Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ month = shiftMonth(month, -12) }) {
                        Text("− year", fontSize = 11.sp)
                    }
                    TextButton({
                        selected = today().toString()
                        month = currentMonth()
                    }) {
                        Text("Today", fontSize = 11.sp)
                    }
                    TextButton({ month = shiftMonth(month, 12) }) {
                        Text("+ year", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(selected) }) { Text("Done") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
