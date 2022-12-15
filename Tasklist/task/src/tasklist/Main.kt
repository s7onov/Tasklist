package tasklist

import kotlinx.datetime.*
import kotlin.system.exitProcess
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

const val ADD = "add"
const val PRINT = "print"
const val EDIT = "edit"
const val DELETE = "delete"
const val END = "end"

const val PRIORITY = "priority"
const val DATE = "date"
const val TIME = "time"
const val TASK = "task"

const val PRIORITY_CRITICAL = "C"
const val PRIORITY_HIGH = "H"
const val PRIORITY_NORMAL = "N"
const val PRIORITY_LOW = "L"

val PRIORITIES = listOf(PRIORITY_CRITICAL, PRIORITY_HIGH, PRIORITY_NORMAL, PRIORITY_LOW)

const val DUE_IN_TIME = "I"
const val DUE_TODAY = "T"
const val DUE_OVERDUE = "O"

data class Task(var priority: String, var date: String, var time: String, var subtasks: MutableList<String>) {
    fun getDueTag() : String {
        val taskDate = date.toLocalDate()
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
        val numberOfDays = currentDate.daysUntil(taskDate)
        return when {
            numberOfDays == 0 -> DUE_TODAY
            numberOfDays > 0 -> DUE_IN_TIME
            else -> DUE_OVERDUE
        }
    }
}

class TaskBuilder {

    fun build() : Task {
        val priority = inputPriority()
        val date = inputDate()
        val time = inputTime()
        val subtasks = inputSubtasks()
        return Task(priority, date, time, subtasks)
    }

    fun inputPriority() = getInputWhile("Input the task priority (${PRIORITIES.joinToString()}):", "", ::isPriorityCorrect)
    fun inputDate() = getInputWhile("Input the date (yyyy-mm-dd):", "The input date is invalid", ::isDateCorrect)
    fun inputTime() = getInputWhile("Input the time (hh:mm):", "The input time is invalid", ::isTimeCorrect)
    fun inputSubtasks(): MutableList<String> {
        val subtasks = mutableListOf<String>()
        println("Input a new task (enter a blank line to end):")
        while (true){
            val input = readln().trim()
            if (input.isEmpty()) break
            else subtasks.add(input)
        }
        return subtasks
    }

    fun isTimeCorrect(s: String): Any {
        if (!s.matches("\\d+:\\d+".toRegex())) return false
        try {
            val (h, m) = s.split(":").map { Integer.parseInt(it) }
            if (h !in 0..23) return false
            if (m !in 0..59) return false
            val str = "%02d:%02d".format(h, m)
            return str
        } catch (ex: Exception) { return false }
        //return true
    }

    fun isDateCorrect(s: String): Any {
        if (!s.matches("\\d{4}-\\d+-\\d+".toRegex())) return false
        try {
            val (y, m, d) = s.split("-").map { Integer.parseInt(it) }
            val str = "%04d-%02d-%02d".format(y, m, d)
            str.toLocalDate()
            return str
        } catch (ex: Exception) { return false }
    }

    fun isPriorityCorrect(s: String) : Any {
        val char = s.uppercase()
        if (char !in PRIORITIES) return false
        return char
    }
}

const val TEXT_SIZE = 44
const val BORDER_LINE = "+----+------------+-------+---+---+--------------------------------------------+"
const val CAPTION_LINE = "| N  |    Date    | Time  | P | D |                   Task                     |"
const val FIRST_LINE = "| %-2d | %s | %s | %s | %s |%-${TEXT_SIZE}s|"
const val TEXT_LINE = "|    |            |       |   |   |%-${TEXT_SIZE}s|"

const val RED = "\u001B[101m \u001B[0m"
const val YELLOW = "\u001B[103m \u001B[0m"
const val GREEN = "\u001B[102m \u001B[0m"
const val BLUE = "\u001B[104m \u001B[0m"

class TaskList(val list: MutableList<Task?> = mutableListOf()) {

    private fun getPriorityColor(priority: String): String {
        return when (priority) {
            PRIORITY_CRITICAL -> RED
            PRIORITY_HIGH -> YELLOW
            PRIORITY_NORMAL -> GREEN
            PRIORITY_LOW -> BLUE
            else -> " "
        }
    }

    private fun getDueColor(dueTag: String): String {
        return when (dueTag) {
            DUE_IN_TIME -> GREEN
            DUE_TODAY -> YELLOW
            DUE_OVERDUE -> RED
            else -> " "
        }
    }

    fun showTasks(): Boolean {
        if (list.size == 0) {
            println("No tasks have been input")
            return false
        }
        println(BORDER_LINE)
        println(CAPTION_LINE)
        println(BORDER_LINE)
        for (i in list.indices) {
            val (priority, date, time, subtasks) = list[i]!!
            val pColor = getPriorityColor(priority)
            val dueColor = getDueColor(list[i]!!.getDueTag())
            var p = 0
            for (text in subtasks) {
                for (part in text.chunked(TEXT_SIZE)) {
                    if (p != 0) println(TEXT_LINE.format(part))
                    else println(FIRST_LINE.format(i + 1, date, time, pColor, dueColor, part))
                    p++
                }
            }
            println(BORDER_LINE)
        }
        return true
    }

    fun add() {
        val task = TaskBuilder().build()
        if (task.subtasks.isEmpty()) println("The task is blank")
        else list.add(task)
    }

    fun edit() {
        if (!showTasks()) return
        val taskIndex = inputTaskNumber()
        val task = list[taskIndex.toInt() - 1]!!
        val taskField = getInputWhile("Input a field to edit ($PRIORITY, $DATE, $TIME, $TASK):", "Invalid field", ::isFieldCorrect)
        when (taskField) {
            PRIORITY -> task.priority = TaskBuilder().inputPriority()
            DATE -> task.date = TaskBuilder().inputDate()
            TIME -> task.time = TaskBuilder().inputTime()
            TASK -> task.subtasks = TaskBuilder().inputSubtasks()
        }
        println("The task is changed")
    }

    fun delete() {
        if (!showTasks()) return
        val taskIndex = inputTaskNumber()
        list.removeAt(taskIndex.toInt() - 1)
        println("The task is deleted")
    }

    private fun inputTaskNumber() = getInputWhile("Input the task number (1-${list.size}):", "Invalid task number", ::isIndexCorrect)

    fun isIndexCorrect(s: String): Any {
        return try {
            val index = Integer.parseInt(s)
            if (index !in 1..list.size) false
            else index
        } catch (ex: Exception) {
            false
        }
    }

    fun isFieldCorrect(s: String): Any {
        val input = s.lowercase()
        return if (input !in listOf(PRIORITY, DATE, TIME, TASK)) false
        else input
    }
}

fun getInputWhile(request: String, errorMessage: String, function: (String) -> Any): String {
    while (true) {
        println(request)
        val input = readln()
        val result = function(input)
        if (result != false) return result.toString()
        else if (errorMessage.isNotEmpty()) println(errorMessage)
    }
}

fun main() {

    val jsonFile = File("tasklist.json")
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    //val jsonAdapter: JsonAdapter<TaskList> = moshi.adapter(TaskList::class.java)
    val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
    val jsonAdapter = moshi.adapter<MutableList<Task?>>(type)
    var taskList = TaskList()
    if (jsonFile.exists()) taskList = TaskList(jsonAdapter.fromJson(jsonFile.readText())!!)

    while (true) {
        println("Input an action ($ADD, $PRINT, $EDIT, $DELETE, $END):")
        when (readln().trim().lowercase()) {
            ADD -> taskList.add()
            PRINT -> taskList.showTasks()
            EDIT -> taskList.edit()
            DELETE -> taskList.delete()
            END -> {
                jsonFile.writeText(jsonAdapter.toJson(taskList.list))
                println("Tasklist exiting!");
                exitProcess(0)
            }
            else -> println("The input action is invalid")
        }
    }
}
