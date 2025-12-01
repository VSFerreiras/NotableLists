package ucne.edu.notablelists.domain.notes.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val remoteId: Int? = null,
    val userId: Int? = null,
    val title: String,
    val description: String,
    val tag: String,
    val isFinished: Boolean = false,
    val reminder: String?,
    val checklist: String?,
    val priority: Int,
    val isPendingCreate: Boolean = false
)