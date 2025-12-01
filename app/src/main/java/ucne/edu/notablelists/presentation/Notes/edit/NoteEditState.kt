package ucne.edu.notablelists.presentation.Notes.edit

data class NoteEditState(
    val id: String? = null,
    val remoteId: Int? = null,
    val title: String = "",
    val description: String = "",
    val tag: String = "",
    val priority: Int = 0,
    val isFinished: Boolean = false,
    val reminder: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isNoteSaved: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isTagSheetOpen: Boolean = false,
    val availableTags: List<String> = listOf("Personal", "Trabajo", "Estudio", "Ideas", "Urgente")
)

data class ChecklistItem(
    val text: String,
    val isDone: Boolean
)