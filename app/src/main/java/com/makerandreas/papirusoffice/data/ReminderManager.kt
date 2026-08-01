package com.makerandreas.papirusoffice.data

data class DocumentReminder(
    val paragraphIndex: Int,
    val offset: Int,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ReminderManager {
    private val reminders = mutableListOf<DocumentReminder>()

    fun getReminders(): List<DocumentReminder> = reminders

    fun setReminder(paragraphIndex: Int, offset: Int, note: String): Boolean {
        reminders.removeAll { it.paragraphIndex == paragraphIndex && it.offset == offset }
        reminders.add(DocumentReminder(paragraphIndex, offset, note))
        reminders.sortBy { it.paragraphIndex }
        if (reminders.size > 5) {
            reminders.removeAt(0)
        }
        return true
    }

    fun removeReminder(paragraphIndex: Int, offset: Int) {
        reminders.removeAll { it.paragraphIndex == paragraphIndex && it.offset == offset }
    }

    fun clear() {
        reminders.clear()
    }

    fun nextReminder(currentParagraphIndex: Int, currentOffset: Int): DocumentReminder? {
        if (reminders.isEmpty()) return null
        val nextList = reminders.filter { 
            it.paragraphIndex > currentParagraphIndex || 
            (it.paragraphIndex == currentParagraphIndex && it.offset > currentOffset)
        }
        return if (nextList.isNotEmpty()) {
            nextList.first()
        } else {
            reminders.first()
        }
    }

    fun previousReminder(currentParagraphIndex: Int, currentOffset: Int): DocumentReminder? {
        if (reminders.isEmpty()) return null
        val prevList = reminders.filter {
            it.paragraphIndex < currentParagraphIndex ||
            (it.paragraphIndex == currentParagraphIndex && it.offset < currentOffset)
        }
        return if (prevList.isNotEmpty()) {
            prevList.last()
        } else {
            reminders.last()
        }
    }
}
