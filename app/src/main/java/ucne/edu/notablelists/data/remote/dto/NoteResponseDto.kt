package ucne.edu.notablelists.data.remote.dto

data class NoteResponseDto(
    val noteId: Int,
    val title: String,
    val description: String,
    val tag: String,
    val isFinished: Boolean = false,
    val reminder: String,
    val checklist: String,
    val priority: Int,
    val deleteAt: String,
    val autoDelete: Boolean = false
)