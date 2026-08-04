package com.makerandreas.papirusoffice.data.framework

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class DocumentLockMode {
    NONE,
    READ_ONLY,
    IN_USE,
    CLOUD_SYNC,
    AUTO_SAVE
}

object DocumentLockManager {
    private val _lockStates = MutableStateFlow<Map<UUID, DocumentLockMode>>(emptyMap())
    val lockStates = _lockStates.asStateFlow()

    fun lock(sessionId: UUID, mode: DocumentLockMode) {
        _lockStates.value = _lockStates.value + (sessionId to mode)
    }

    fun unlock(sessionId: UUID) {
        _lockStates.value = _lockStates.value - sessionId
    }

    fun getLockMode(sessionId: UUID): DocumentLockMode {
        return _lockStates.value[sessionId] ?: DocumentLockMode.NONE
    }

    fun isLocked(sessionId: UUID): Boolean {
        val mode = getLockMode(sessionId)
        return mode != DocumentLockMode.NONE && mode != DocumentLockMode.READ_ONLY
    }
}
