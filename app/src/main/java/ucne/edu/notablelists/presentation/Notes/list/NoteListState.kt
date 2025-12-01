package ucne.edu.notablelists.presentation.Notes.list

data class NotesListState(
    val loadingStatus: List<Unit> = emptyList(),
    val notes: List<NoteUiItem> = emptyList(),
    val filterChips: List<FilterUiItem> = emptyList(),
    val errorMessage: List<String> = emptyList(),
    val navigateToDetail: List<String?> = emptyList(),
    val searchQuery: String = "",
    val showLogoutDialog: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val showDeleteSelectionDialog: Boolean = false,
    val pendingRequestCount: Int = 0
) {
    val isSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

enum class NoteStyle {
    Primary,
    Secondary,
    Error
}

data class FilterUiItem(
    val filter: NoteFilter,
    val label: String,
    val isSelected: Boolean
)

data class NoteUiItem(
    val id: String,
    val title: String,
    val description: String,
    val style: NoteStyle,
    val reminder: String? = null,
    val priorityChips: List<PriorityUiItem> = emptyList(),
    val tags: List<TagUiItem> = emptyList(),
    val isSelected: Boolean = false
)

data class PriorityUiItem(
    val label: String,
    val style: NoteStyle
)

data class TagUiItem(
    val label: String,
    val style: NoteStyle
)