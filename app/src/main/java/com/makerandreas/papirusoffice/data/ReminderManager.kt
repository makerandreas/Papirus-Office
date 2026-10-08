package com.makerandreas.papirusoffice.data

data class DocumentReminder(
    val paragraphIndex: Int,
    val offset: Int,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    /**
     * Monotonic insertion counter. `timestamp` ties when two reminders are set inside
     * the same millisecond, and the Writer Guide's eviction rule needs a total order.
     */
    val sequence: Long = 0L
)

class ReminderManager {
    private val reminders = mutableListOf<DocumentReminder>()
    private var nextSequence = 0L

    fun getReminders(): List<DocumentReminder> = reminders

    fun setReminder(paragraphIndex: Int, offset: Int, note: String): Boolean {
        reminders.removeAll { it.paragraphIndex == paragraphIndex && it.offset == offset }
        reminders.add(
            DocumentReminder(paragraphIndex, offset, note, sequence = nextSequence++)
        )
        // Writer Guide 26.2 Ch.1 "Setting reminders": "You can set up to 5 reminders in
        // a document; setting a sixth causes the first to be deleted." "First" is
        // insertion order, and the list below is sorted by paragraphIndex, so eviction
        // has to key on the insertion sequence rather than on list position.
        while (reminders.size > MAX_REMINDERS) {
            val oldest = reminders.minByOrNull { it.sequence } ?: break
            reminders.remove(oldest)
        }
        reminders.sortBy { it.paragraphIndex }
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

    companion object {
        /** Writer Guide 26.2 Ch.1: five reminders per document, the sixth evicts the first. */
        const val MAX_REMINDERS = 5
    }
}
