package ucne.edu.notablelists.data.remote.dto

data class SharedNoteWithDetailsDto(
    val sharedNoteId: Int,
    val noteId: Int,
    val noteTitle: String,
    val noteDescription: String,
    val ownerUserId: Int,
    val ownerUsername: String,
    val tag: String,
    val isFinished: Boolean,
    val reminder: String?,
    val checklist: String?,
    val priority: Int
)