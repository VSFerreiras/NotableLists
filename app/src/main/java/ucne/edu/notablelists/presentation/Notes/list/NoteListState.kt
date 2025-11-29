package ucne.edu.notablelists.presentation.Notes.list

import ucne.edu.notablelists.domain.notes.model.Note

data class NotesListState(
    val loadingStatus: List<Unit> = emptyList(),
    val notes: List<NoteUiItem> = emptyList(),
    val filterChips: List<FilterUiItem> = emptyList(),
    val errorMessage: List<String> = emptyList(),
    val navigateToDetail: List<String?> = emptyList(),
    val searchQuery: String = ""
)

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
    val priorityChips: List<PriorityUiItem> = emptyList(),
    val tags: List<TagUiItem> = emptyList()
)

data class PriorityUiItem(
    val label: String,
    val style: NoteStyle
)

data class TagUiItem(
    val label: String,
    val style: NoteStyle
)