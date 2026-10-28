package app.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max

sealed interface CatDialog {
    data object Add : CatDialog
    data class Manage(val name: String) : CatDialog
    data class Rename(val name: String) : CatDialog
    data class Delete(val name: String) : CatDialog
}

@Composable
fun TodoScreen(vm: TodoViewModel) {
    val colors = MaterialTheme.colorScheme
    val inFocus = vm.selected == FOCUS
    val visible = remember(vm.tasks, vm.selected) { vm.visibleTasks() }
    val listState = rememberLazyListState()

    var editId by remember { mutableStateOf<Long?>(null) }
    var dateId by remember { mutableStateOf<Long?>(null) }
    var catDialog by remember { mutableStateOf<CatDialog?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    // Close any open swipe actions as soon as the list scrolls.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) vm.revealedId = null
    }
    // The undo bar disappears after a few seconds.
    LaunchedEffect(vm.undoToken) {
        if (vm.undoLabel != null) {
            delay(5000)
            vm.dismissUndo()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
    ) {
        // Title + menu
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.weight(1f),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, "Menu", tint = colors.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Clear completed") },
                        onClick = {
                            menuOpen = false
                            vm.clearCompleted()
                        }
                    )
                }
            }
        }

        // Categories
        CategoryBar(
            categories = vm.categories,
            selected = vm.selected,
            onSelect = { vm.select(it) },
            onLongPress = { catDialog = CatDialog.Manage(it) },
            onAdd = { catDialog = CatDialog.Add }
        )
        HorizontalDivider(color = colors.outlineVariant)

        // Tasks
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(visible, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        depth = if (inFocus) 0 else task.depth,
                        showCategory = inFocus,
                        showFocus = !inFocus,
                        revealed = vm.revealedId == task.id,
                        onRevealChange = { open ->
                            if (open) vm.revealedId = task.id
                            else if (vm.revealedId == task.id) vm.revealedId = null
                        },
                        onToggle = { vm.toggleDone(task.id) },
                        onEdit = { editId = task.id },
                        onStar = {
                            vm.toggleStar(task.id)
                            vm.revealedId = null
                        },
                        onDate = {
                            vm.revealedId = null
                            dateId = task.id
                        },
                        onFocus = {
                            vm.toggleFocus(task.id)
                            vm.revealedId = null
                        },
                        onDelete = { vm.delete(task.id) }
                    )
                }
            }
            if (visible.isEmpty()) {
                Text(
                    text = if (inFocus) "Nothing in focus.\nSwipe a task left and tap the target."
                    else "Nothing here yet.\nAdd your first task below.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Undo bar
        val undoLabel = vm.undoLabel
        if (undoLabel != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceVariant)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = undoLabel,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = colors.onSurface
                )
                TextButton(onClick = { vm.undo() }) { Text("Undo") }
            }
        }

        // Add bar
        HorizontalDivider(color = colors.outlineVariant)
        AddBar(
            showLevel = !inFocus,
            level = vm.effectiveLevel(),
            onCycle = { vm.cycleLevel() },
            onAdd = { vm.addTask(it) }
        )
    }

    // ---- dialogs ----
    val editing = editId?.let { id -> vm.tasks.firstOrNull { it.id == id } }
    if (editing != null) {
        EditDialog(vm, editing, onClose = { editId = null })
    }

    val dateTask = dateId?.let { id -> vm.tasks.firstOrNull { it.id == id } }
    if (dateTask != null) {
        DateDialog(
            initial = dateTask.due,
            onDismiss = { dateId = null },
            onPick = {
                vm.setDue(dateTask.id, it)
                dateId = null
            }
        )
    }

    when (val d = catDialog) {
        null -> Unit
        is CatDialog.Add -> NameDialog(
            title = "New category",
            initial = "",
            confirmLabel = "Add",
            onDismiss = { catDialog = null }
        ) {
            vm.addCategory(it)
            catDialog = null
        }
        is CatDialog.Manage -> AlertDialog(
            onDismissRequest = { catDialog = null },
            title = { Text(d.name) },
            confirmButton = {
                TextButton(onClick = { catDialog = CatDialog.Rename(d.name) }) { Text("Rename") }
            },
            dismissButton = {
                if (vm.categories.size > 1) {
                    TextButton(onClick = { catDialog = CatDialog.Delete(d.name) }) {
                        Text("Delete", color = colors.error)
                    }
                }
            }
        )
        is CatDialog.Rename -> NameDialog(
            title = "Rename category",
            initial = d.name,
            confirmLabel = "Save",
            onDismiss = { catDialog = null }
        ) {
            vm.renameCategory(d.name, it)
            catDialog = null
        }
        is CatDialog.Delete -> {
            val n = vm.categoryTaskCount(d.name)
            AlertDialog(
                onDismissRequest = { catDialog = null },
                title = { Text("Delete \"${d.name}\"?") },
                text = {
                    Text(
                        if (n == 0) "This category is empty."
                        else "Its $n task${if (n == 1) "" else "s"} will be deleted too."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.deleteCategory(d.name)
                        catDialog = null
                    }) { Text("Delete", color = colors.error) }
                },
                dismissButton = { TextButton(onClick = { catDialog = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun CategoryBar(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onAdd: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val state = rememberLazyListState()

    // Keep the selected tab in view (e.g. right after adding a category).
    LaunchedEffect(selected, categories.size) {
        val i = if (selected == FOCUS) 0 else categories.indexOf(selected) + 1
        if (i >= 0) state.animateScrollToItem(max(i - 1, 0))
    }

    LazyRow(state = state, contentPadding = PaddingValues(horizontal = 6.dp)) {
        item(key = "focus") {
            TabItem(
                label = "Focus",
                selected = selected == FOCUS,
                onClick = { onSelect(FOCUS) },
                leading = {
                    FocusIcon(
                        if (selected == FOCUS) colors.primary else colors.onSurfaceVariant,
                        14.dp
                    )
                }
            )
        }
        items(categories, key = { it }) { c ->
            TabItem(
                label = c,
                selected = selected == c,
                onClick = { onSelect(c) },
                onLongClick = { onLongPress(c) }
            )
        }
        item(key = "add") {
            Box(
                Modifier
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, "New category",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddBar(
    showLevel: Boolean,
    level: Int,
    onCycle: () -> Unit,
    onAdd: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("") }
    fun submit() {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLevel) {
            LevelPill(level, onCycle)
        }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, color = colors.onBackground),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text("Add a task", fontSize = 16.sp, color = colors.onSurfaceVariant)
                    }
                    inner()
                }
            }
        )
        IconButton(onClick = { submit() }) {
            Icon(Icons.Default.Add, "Add task", tint = colors.primary)
        }
    }
}
