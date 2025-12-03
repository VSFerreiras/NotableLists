package ucne.edu.notablelists.presentation.Notes.edit

sealed interface NoteEditEvent {
    data class TitleChanged(val title: String) : NoteEditEvent
    data class DescriptionChanged(val description: String) : NoteEditEvent
    data class TagChanged(val tag: String) : NoteEditEvent
    data class PriorityChanged(val priority: Int) : NoteEditEvent
    data class ReminderSet(val date: Long, val hour: Int, val minute: Int) : NoteEditEvent
    data object ReminderCleared : NoteEditEvent
    data object ChecklistItemAdded : NoteEditEvent
    data class ChecklistItemUpdated(val index: Int, val text: String) : NoteEditEvent
    data class ChecklistItemToggled(val index: Int) : NoteEditEvent
    data class ChecklistItemRemoved(val index: Int) : NoteEditEvent
    data object SaveClicked : NoteEditEvent
    data object BackClicked : NoteEditEvent
    data object DeleteClicked : NoteEditEvent
    data object DeleteConfirmed : NoteEditEvent
    data object ShareClicked : NoteEditEvent
    data class ShareWithFriend(val friendId: Int) : NoteEditEvent
    data object TagMenuClicked : NoteEditEvent
    data class TagSelected(val tag: String) : NoteEditEvent
    data class TagCreated(val tag: String) : NoteEditEvent
    data class TagDeleted(val tag: String) : NoteEditEvent
    data object CollaboratorMenuClicked : NoteEditEvent
    data class RemoveCollaboratorRequested(val collaborator: Collaborator) : NoteEditEvent
    data object RemoveCollaboratorConfirmed : NoteEditEvent
    data object DialogDismissed : NoteEditEvent
    data object LoginClicked : NoteEditEvent
    data object FriendsClicked : NoteEditEvent
}