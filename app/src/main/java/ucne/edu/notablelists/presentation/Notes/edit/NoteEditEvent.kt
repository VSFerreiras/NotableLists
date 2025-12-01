package ucne.edu.notablelists.presentation.Notes.edit

sealed interface NoteEditEvent {
    data class EnteredTitle(val value: String) : NoteEditEvent
    data class EnteredDescription(val value: String) : NoteEditEvent
    data class EnteredTag(val value: String) : NoteEditEvent
    data class ChangePriority(val value: Int) : NoteEditEvent
    data class SetReminder(val date: Long, val timeHour: Int, val timeMinute: Int) : NoteEditEvent
    data object AddChecklistItem : NoteEditEvent
    data class UpdateChecklistItem(val index: Int, val text: String) : NoteEditEvent
    data class ToggleChecklistItem(val index: Int) : NoteEditEvent
    data class RemoveChecklistItem(val index: Int) : NoteEditEvent
    data class ToggleFinished(val isFinished: Boolean) : NoteEditEvent
    data object DeleteNote : NoteEditEvent
    data object ShowDeleteDialog : NoteEditEvent
    data object DismissDeleteDialog : NoteEditEvent
    data object OnBackClick : NoteEditEvent
    data object ClearReminder : NoteEditEvent
}