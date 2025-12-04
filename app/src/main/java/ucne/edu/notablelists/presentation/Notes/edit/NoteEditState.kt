package ucne.edu.notablelists.presentation.Notes.edit

import ucne.edu.notablelists.domain.friends.model.Friend

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
    val successMessage: String? = null,
    val availableTags: List<String> = listOf("Personal", "Trabajo", "Estudio", "Ideas", "Urgente"),
    val friends: List<Friend> = emptyList(),
    val collaborators: List<Collaborator> = emptyList(),
    val isOwner: Boolean = true,
    val noteOwnerId: Int? = null,
    val currentUserId: Int? = null,
    val isTagSheetVisible: Boolean = false,
    val isShareSheetVisible: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val isLoginRequiredDialogVisible: Boolean = false,
    val isNoFriendsDialogVisible: Boolean = false,
    val isCollaboratorMenuExpanded: Boolean = false,
    val collaboratorPendingRemoval: Collaborator? = null,
    val navigationEvent: NoteEditSideEffect? = null
)

sealed interface NoteEditSideEffect {
    data object NavigateBack : NoteEditSideEffect
    data object NavigateToLogin : NoteEditSideEffect
    data object NavigateToFriends : NoteEditSideEffect
}

data class ChecklistItem(
    val text: String,
    val isDone: Boolean
)

data class Collaborator(
    val userId: Int,
    val username: String,
    val isOwner: Boolean,
    val sharedNoteId: Int? = null
)