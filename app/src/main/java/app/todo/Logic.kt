package app.todo

/**
 * A task. Subtasks are expressed with [depth] (outliner style): a task's children are the
 * tasks directly after it, in the same category, that have a greater depth.
 */
data class Task(
    val id: Long,
    val title: String,
    val done: Boolean = false,
    val starred: Boolean = false,
    val focus: Boolean = false,
    val due: Long? = null, // epoch day
    val category: String,
    val depth: Int = 0
)

/** Pure list operations, no Android dependencies. */
object TaskOps {
    const val MAX_DEPTH = 3

    /** The task itself plus all its descendants. */
    fun subtreeIds(tasks: List<Task>, id: Long): Set<Long> {
        val t = tasks.firstOrNull { it.id == id } ?: return emptySet()
        val list = tasks.filter { it.category == t.category }
        val i = list.indexOfFirst { it.id == id }
        val out = linkedSetOf(id)
        var j = i + 1
        while (j < list.size && list[j].depth > t.depth) {
            out.add(list[j].id)
            j++
        }
        return out
    }

    /** Deepest level a new task appended to [category] may have. */
    fun maxAddLevel(tasks: List<Task>, category: String): Int {
        val last = tasks.lastOrNull { it.category == category } ?: return 0
        return minOf(MAX_DEPTH, last.depth + 1)
    }

    fun add(
        tasks: List<Task>,
        id: Long,
        title: String,
        category: String,
        level: Int,
        focus: Boolean
    ): List<Task> {
        val depth = minOf(level, maxAddLevel(tasks, category))
        return tasks + Task(id = id, title = title, focus = focus, category = category, depth = depth)
    }

    /** Completing a task completes its subtasks too; un-completing does the same. */
    fun toggleDone(tasks: List<Task>, id: Long): List<Task> {
        val t = tasks.firstOrNull { it.id == id } ?: return tasks
        val ids = subtreeIds(tasks, id)
        val target = !t.done
        return tasks.map { if (it.id in ids) it.copy(done = target) else it }
    }

    fun delete(tasks: List<Task>, id: Long): List<Task> {
        val ids = subtreeIds(tasks, id)
        return tasks.filter { it.id !in ids }
    }

    fun canShift(tasks: List<Task>, id: Long, d: Int): Boolean {
        val t = tasks.firstOrNull { it.id == id } ?: return false
        if (t.depth + d < 0) return false
        if (d > 0) {
            val list = tasks.filter { it.category == t.category }
            val i = list.indexOfFirst { it.id == id }
            if (i <= 0) return false
            if (t.depth + d > list[i - 1].depth + 1) return false
            val ids = subtreeIds(tasks, id)
            val deepest = list.filter { it.id in ids }.maxOf { it.depth } + d
            if (deepest > MAX_DEPTH) return false
        }
        return true
    }

    /** Indent (d = 1) or outdent (d = -1) a task together with its subtasks. */
    fun shift(tasks: List<Task>, id: Long, d: Int): List<Task> {
        if (!canShift(tasks, id, d)) return tasks
        val ids = subtreeIds(tasks, id)
        return tasks.map { if (it.id in ids) it.copy(depth = it.depth + d) else it }
    }

    /** Moves a task and its subtasks to the end of another category. */
    fun moveToCategory(tasks: List<Task>, id: Long, category: String): List<Task> {
        val t = tasks.firstOrNull { it.id == id } ?: return tasks
        if (t.category == category) return tasks
        val ids = subtreeIds(tasks, id)
        val moved = tasks.filter { it.id in ids }
            .map { it.copy(category = category, depth = it.depth - t.depth) }
        return tasks.filter { it.id !in ids } + moved
    }

    /** Repairs depths after removals so no task is indented deeper than its predecessor allows. */
    fun normalize(tasks: List<Task>): List<Task> {
        val prevDepth = HashMap<String, Int>()
        return tasks.map { t ->
            val prev = prevDepth[t.category]
            val d = if (prev == null) 0 else minOf(t.depth, prev + 1)
            prevDepth[t.category] = d
            if (d != t.depth) t.copy(depth = d) else t
        }
    }
}
