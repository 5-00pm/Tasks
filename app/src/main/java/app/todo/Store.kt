package app.todo

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class AppData(val categories: List<String>, val tasks: List<Task>)

/** Saves everything as one small JSON file in the app's private storage. Fully offline. */
class Store(private val file: File) {

    fun load(): AppData {
        if (!file.exists()) return defaultData()
        return try {
            parse(file.readText())
        } catch (e: Exception) {
            // Keep the unreadable file around instead of overwriting it.
            runCatching { file.renameTo(File(file.parentFile, file.name + ".bad")) }
            defaultData()
        }
    }

    @Synchronized
    fun save(data: AppData) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(toJson(data).toString())
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun parse(text: String): AppData {
        val root = JSONObject(text)
        val catArr = root.getJSONArray("categories")
        val cats = List(catArr.length()) { catArr.getString(it) }
        val taskArr = root.getJSONArray("tasks")
        val tasks = List(taskArr.length()) { i ->
            val o = taskArr.getJSONObject(i)
            Task(
                id = o.getLong("id"),
                title = o.getString("title"),
                done = o.optBoolean("done", false),
                starred = o.optBoolean("starred", false),
                focus = o.optBoolean("focus", false),
                due = if (o.has("due") && !o.isNull("due")) o.getLong("due") else null,
                category = o.getString("cat"),
                depth = o.optInt("depth", 0)
            )
        }
        val allCats = (cats + tasks.map { it.category }).distinct().ifEmpty { listOf("Inbox") }
        return AppData(allCats, tasks)
    }

    private fun toJson(d: AppData): JSONObject {
        val tasks = JSONArray()
        for (t in d.tasks) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("done", t.done)
            o.put("starred", t.starred)
            o.put("focus", t.focus)
            val due = t.due
            if (due != null) o.put("due", due)
            o.put("cat", t.category)
            o.put("depth", t.depth)
            tasks.put(o)
        }
        return JSONObject().apply {
            put("categories", JSONArray(d.categories))
            put("tasks", tasks)
        }
    }

    private fun defaultData() = AppData(
        categories = listOf("Inbox", "Work", "Personal"),
        tasks = listOf(
            Task(id = 1, title = "Swipe this task to the left", category = "Inbox", depth = 0),
            Task(id = 2, title = "Star it, give it a date, focus on it, or delete it", category = "Inbox", depth = 1),
            Task(id = 3, title = "Tap a task to edit, indent or move it", category = "Inbox", depth = 0),
            Task(id = 4, title = "Tap the circle to complete a task", category = "Inbox", depth = 0)
        )
    )
}
