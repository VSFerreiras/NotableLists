package ucne.edu.notablelists.presentation.Notes.list

import ucne.edu.notablelists.domain.notes.model.Note

sealed interface NotesListEvent {
    data object Refresh : NotesListEvent
    data object AddNoteClicked : NotesListEvent
    data class DeleteNote(val id: String) : NotesListEvent
    data class ToggleFinished(val note: Note) : NotesListEvent
    data class NoteClicked(val id: String) : NotesListEvent
    data class NoteLongClicked(val id: String) : NotesListEvent
    data class SearchQueryChanged(val query: String) : NotesListEvent
    data class FilterChanged(val filter: NoteFilter) : NotesListEvent
    data object LogoutClicked : NotesListEvent
    data object LogoutConfirmed : NotesListEvent
    data object LogoutDismissed : NotesListEvent
    data object DeleteSelectedClicked : NotesListEvent
    data object DeleteSelectedConfirmed : NotesListEvent
    data object DeleteSelectedDismissed : NotesListEvent
    data object SelectionCleared : NotesListEvent
    data object NavigationHandled : NotesListEvent
}