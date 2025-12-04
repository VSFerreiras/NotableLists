package ucne.edu.notablelists.presentation.Notes.list

data class NotesListState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val notes: List<NoteUiItem> = emptyList(),
    val filterChips: List<FilterUiItem> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val showLogoutDialog: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val showDeleteSelectionDialog: Boolean = false,
    val pendingRequestCount: Int = 0,
    val navigationEvent: NotesListSideEffect? = null
) {
    val isSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

sealed interface NotesListSideEffect {
    data class NavigateToDetail(val noteId: String?) : NotesListSideEffect
    data object NavigateToLogin : NotesListSideEffect
}

enum class NoteFilter(val label: String) {
    AZ("A-Z"),
    ZA("Z-A"),
    DATE("Reciente"),
    HIGH_PRIORITY("Alta"),
    MEDIUM_PRIORITY("Media"),
    LOW_PRIORITY("Baja"),
    SHARED("Compartidas")
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
    val isSelected: Boolean = false,
    val isShared: Boolean = false
)

data class PriorityUiItem(
    val label: String,
    val style: NoteStyle
)

data class TagUiItem(
    val label: String,
    val style: NoteStyle
)