package ucne.edu.notablelists.presentation.Notes.list

import ucne.edu.notablelists.domain.notes.model.Note

sealed interface NotesListEvent {
    data object Refresh : NotesListEvent
    data class DeleteNote(val id: String) : NotesListEvent
    data class ToggleNoteFinished(val note: Note) : NotesListEvent
    data class OnNoteClick(val id: String) : NotesListEvent
    data object OnAddNoteClick : NotesListEvent
    data class OnSearchQueryChange(val query: String) : NotesListEvent
    data class OnFilterChange(val filter: NoteFilter) : NotesListEvent
    data object OnNavigationHandled : NotesListEvent
    data object OnShowLogoutDialog : NotesListEvent
    data object OnDismissLogoutDialog : NotesListEvent
}

enum class NoteFilter(val label: String) {
    AZ("A-Z"),
    ZA("Z-A"),
    DATE("Fecha"),
    HIGH_PRIORITY("Alta"),
    MEDIUM_PRIORITY("Media"),
    LOW_PRIORITY("Baja")
}