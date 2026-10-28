package app.todo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.util.concurrent.Executors

/** Pseudo-category for the "Focus" tab. */
const val FOCUS = "\u0000focus"

class TodoViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(File(app.filesDir, "todo.json"))
    private val writer = Executors.newSingleThreadExecutor()
    private var nextId = 1L
    private var undoSnapshot: List<Task>? = null

    var categories by mutableStateOf<List<String>>(emptyList())
        private set
    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set

    /** Selected tab: a category name or [FOCUS]. */
    var selected by mutableStateOf(FOCUS)
        private set

    /** Id of the task whose swipe actions are currently open. */
    var revealedId by mutableStateOf<Long?>(null)

    /** Subtask level used for the next added task (0 = top level). */
    private var addLevel by mutableIntStateOf(0)

    var undoLabel by mutableStateOf<String?>(null)
        private set
    var undoToken by mutableIntStateOf(0)
        private set

    init {
        val data = store.load()
        categories = data.categories
        tasks = data.tasks
        nextId = (data.tasks.maxOfOrNull { it.id } ?: 0L) + 1
        selected = data.categories.first()
        persist()
    }

    // ---- derived -------------------------------------------------------------------------

    fun visibleTasks(): List<Task> =
        if (selected == FOCUS) tasks.filter { it.focus }.sortedBy { it.done }
        else tasks.filter { it.category == selected }

    fun maxAddLevel(): Int =
        if (selected == FOCUS) 0 else TaskOps.maxAddLevel(tasks, selected)

    fun effectiveLevel(): Int = minOf(addLevel, maxAddLevel())

    fun cycleLevel() {
        val max = maxAddLevel()
        val cur = effectiveLevel()
        addLevel = if (cur >= max) 0 else cur + 1
    }

    fun canShift(id: Long, d: Int) = TaskOps.canShift(tasks, id, d)

    fun categoryTaskCount(name: String) = tasks.count { it.category == name }

    // ---- selection -----------------------------------------------------------------------

    fun select(name: String) {
        selected = name
        revealedId = null
        addLevel = 0
    }

    // ---- task actions --------------------------------------------------------------------

    fun addTask(raw: String) {
        val title = raw.trim()
        if (title.isEmpty()) return
        val inFocus = selected == FOCUS
        val cat = if (inFocus) categories.first() else selected
        val level = if (inFocus) 0 else effectiveLevel()
        commit(TaskOps.add(tasks, nextId++, title, cat, level, focus = inFocus))
    }

    fun toggleDone(id: Long) = commit(TaskOps.toggleDone(tasks, id))

    fun toggleStar(id: Long) = update(id) { it.copy(starred = !it.starred) }

    fun toggleFocus(id: Long) = update(id) { it.copy(focus = !it.focus) }

    fun setDue(id: Long, day: Long?) = update(id) { it.copy(due = day) }

    fun rename(id: Long, raw: String) {
        val title = raw.trim()
        if (title.isEmpty()) return
        update(id) { it.copy(title = title) }
    }

    fun indent(id: Long) = commit(TaskOps.shift(tasks, id, 1))

    fun outdent(id: Long) = commit(TaskOps.shift(tasks, id, -1))

    fun moveToCategory(id: Long, category: String) =
        commit(TaskOps.moveToCategory(tasks, id, category))

    fun delete(id: Long) {
        val n = TaskOps.subtreeIds(tasks, id).size
        revealedId = null
        commit(
            TaskOps.delete(tasks, id),
            undoText = if (n == 1) "Task deleted" else "$n tasks deleted"
        )
    }

    fun clearCompleted() {
        val ids = visibleTasks().filter { it.done }.map { it.id }.toSet()
        if (ids.isEmpty()) return
        commit(
            TaskOps.normalize(tasks.filter { it.id !in ids }),
            undoText = "Cleared ${ids.size} completed"
        )
    }

    fun undo() {
        val snap = undoSnapshot ?: return
        undoSnapshot = null
        undoLabel = null
        tasks = snap
        persist()
    }

    fun dismissUndo() {
        undoSnapshot = null
        undoLabel = null
    }

    // ---- categories ----------------------------------------------------------------------

    fun addCategory(raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) return
        val existing = categories.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existing != null) {
            select(existing)
            return
        }
        commit(newCats = categories + name)
        select(name)
    }

    fun renameCategory(old: String, raw: String) {
        val name = raw.trim()
        if (name.isEmpty() || name == old) return
        if (categories.any { it != old && it.equals(name, ignoreCase = true) }) return
        commit(
            tasks.map { if (it.category == old) it.copy(category = name) else it },
            newCats = categories.map { if (it == old) name else it }
        )
        if (selected == old) selected = name
    }

    fun deleteCategory(name: String) {
        if (categories.size <= 1) return
        val newCats = categories - name
        commit(tasks.filter { it.category != name }, newCats = newCats)
        if (selected == name) select(newCats.first())
    }

    // ---- internals -----------------------------------------------------------------------

    private fun update(id: Long, change: (Task) -> Task) =
        commit(tasks.map { if (it.id == id) change(it) else it })

    private fun commit(
        newTasks: List<Task> = tasks,
        newCats: List<String> = categories,
        undoText: String? = null
    ) {
        if (undoText != null) {
            undoSnapshot = tasks
            undoLabel = undoText
            undoToken++
        } else {
            undoSnapshot = null
            undoLabel = null
        }
        tasks = newTasks
        categories = newCats
        persist()
    }

    private fun persist() {
        val snapshot = AppData(categories, tasks)
        writer.execute { runCatching { store.save(snapshot) } }
    }

    override fun onCleared() {
        writer.shutdown()
    }
}
