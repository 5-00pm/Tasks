package app.todo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

val ActionWidth = 56.dp
val IndentStep = 22.dp

// ---- small helpers ---------------------------------------------------------------------------

/** Text for a due date and whether it is overdue. */
fun dueLabel(epochDay: Long): Pair<String, Boolean> {
    val diff = epochDay - LocalDate.now().toEpochDay()
    val text = when (diff) {
        0L -> "Today"
        1L -> "Tomorrow"
        -1L -> "Yesterday"
        else -> LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
    return text to (diff < 0)
}

/** A small "target" glyph used for Focus. */
@Composable
fun FocusIcon(color: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.11f
        drawCircle(color = color, radius = s / 2f - stroke / 2f, style = Stroke(width = stroke))
        drawCircle(color = color, radius = s * 0.17f)
    }
}

// ---- swipe to reveal -------------------------------------------------------------------------

/**
 * Foreground [content] that slides horizontally to reveal [actions] sitting behind it.
 * [revealed] is controlled from outside so only one row is open at a time.
 */
@Composable
fun SwipeReveal(
    revealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    actionsWidth: Dp,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val maxPx = with(LocalDensity.current) { actionsWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val drag = remember { FloatArray(1) }
    val currentOnChange by rememberUpdatedState(onRevealChange)

    LaunchedEffect(revealed, maxPx) {
        offset.animateTo(if (revealed) -maxPx else 0f, tween(160))
    }

    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End,
            content = actions
        )
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        drag[0] = (drag[0] + delta).coerceIn(-maxPx, 0f)
                        scope.launch { offset.snapTo(drag[0]) }
                    },
                    onDragStarted = { drag[0] = offset.value },
                    onDragStopped = { velocity ->
                        val open = if (abs(velocity) > 700f) velocity < 0f else drag[0] < -maxPx / 2f
                        currentOnChange(open)
                        offset.animateTo(if (open) -maxPx else 0f, tween(160))
                    }
                )
        ) {
            content()
        }
    }
}

@Composable
fun ActionButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .width(ActionWidth)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ---- task row --------------------------------------------------------------------------------

@Composable
fun TaskCheck(done: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (done) colors.primary else Color.Transparent)
                .border(1.5.dp, if (done) colors.primary else colors.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    depth: Int,
    showCategory: Boolean,
    showFocus: Boolean,
    revealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onStar: () -> Unit,
    onDate: () -> Unit,
    onFocus: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val muted = colors.onSurfaceVariant
    val guide = colors.outlineVariant

    SwipeReveal(
        revealed = revealed,
        onRevealChange = onRevealChange,
        actionsWidth = ActionWidth * 4,
        actions = {
            ActionButton(onStar) {
                Icon(
                    Icons.Default.Star, "Star",
                    tint = if (task.starred) StarColor else muted
                )
            }
            ActionButton(onDate) {
                Icon(
                    Icons.Default.DateRange, "Set date",
                    tint = if (task.due != null) colors.primary else muted
                )
            }
            ActionButton(onFocus) {
                FocusIcon(if (task.focus) colors.primary else muted, 22.dp)
            }
            ActionButton(onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = colors.error)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .drawBehind {
                    for (k in 0 until depth) {
                        val x = (28.dp + IndentStep * k).toPx()
                        drawLine(guide, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                    }
                }
                .clickable { if (revealed) onRevealChange(false) else onEdit() }
                .padding(start = 8.dp + IndentStep * depth, end = 16.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TaskCheck(task.done, onToggle)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    color = if (task.done) muted else colors.onBackground,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                val due = task.due?.let { dueLabel(it) }
                val focusBadge = showFocus && task.focus
                if (due != null || focusBadge || showCategory) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (focusBadge) {
                            FocusIcon(colors.primary, 12.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (due != null) {
                            Text(
                                text = due.first,
                                fontSize = 12.sp,
                                color = if (due.second && !task.done) colors.error else muted
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        if (showCategory) {
                            Text(task.category, fontSize = 12.sp, color = muted)
                        }
                    }
                }
            }
            if (task.starred) {
                Icon(
                    Icons.Default.Star, null,
                    tint = StarColor,
                    modifier = Modifier.padding(start = 8.dp).size(16.dp)
                )
            }
        }
    }
}

// ---- category tabs ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    Row(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .drawBehind {
                if (selected) {
                    val h = 2.dp.toPx()
                    val inset = 14.dp.toPx()
                    drawRect(
                        color = accent,
                        topLeft = Offset(inset, size.height - h),
                        size = Size(size.width - inset * 2f, h)
                    )
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onBackground else colors.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ---- dialogs ---------------------------------------------------------------------------------

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.focusRequester(focus)
            )
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditDialog(vm: TodoViewModel, task: Task, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var title by remember(task.id) { mutableStateOf(task.title) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Edit task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Level", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row {
                    OutlinedButton(
                        onClick = { vm.outdent(task.id) },
                        enabled = vm.canShift(task.id, -1)
                    ) { Text("Outdent") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { vm.indent(task.id) },
                        enabled = vm.canShift(task.id, 1)
                    ) { Text("Indent") }
                }
                Spacer(Modifier.height(16.dp))
                Text("Category", fontSize = 12.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    vm.categories.forEach { c ->
                        FilterChip(
                            selected = c == task.category,
                            onClick = { vm.moveToCategory(task.id, c) },
                            label = { Text(c) }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.rename(task.id, title); onClose() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(initial: Long?, onDismiss: () -> Unit, onPick: (Long?) -> Unit) {
    val dayMillis = 86_400_000L
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now().toEpochDay()) * dayMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) onPick(Math.floorDiv(ms, dayMillis)) else onDismiss()
            }) { Text("Set") }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { onPick(null) }) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    ) {
        DatePicker(state = state)
    }
}

/** Little pill that sets how deep the next added task is. */
@Composable
fun LevelPill(level: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = when (level) {
        0 -> "Task"
        1 -> "Sub"
        else -> "Sub $level"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (level > 0) colors.primaryContainer else colors.surfaceVariant)
            .clickable(onClick = onClick)
            .widthIn(min = 52.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (level > 0) colors.onPrimaryContainer else colors.onSurfaceVariant
        )
    }
}
