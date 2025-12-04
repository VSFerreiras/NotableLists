package ucne.edu.notablelists.presentation.Notes.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.friends.usecase.GetFriendsUseCase
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import ucne.edu.notablelists.domain.notes.usecase.*
import ucne.edu.notablelists.domain.notification.CancelReminderUseCase
import ucne.edu.notablelists.domain.notification.ScheduleReminderUseCase
import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
import ucne.edu.notablelists.domain.sharednote.usecase.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val upsertNoteUseCase: UpsertNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val deleteRemoteNoteUseCase: DeleteRemoteNoteUseCase,
    private val putNoteUseCase: PutNoteUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val noteRepository: NoteRepository,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val cancelReminderUseCase: CancelReminderUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    private val shareNoteUseCase: ShareNoteUseCase,
    private val getSharedNoteDetailsUseCase: GetSharedNoteDetailsUseCase,
    private val getRemoteUserNoteUseCase: GetRemoteUserNoteUseCase,
    private val updateSharedNoteStatusUseCase: UpdateSharedNoteStatusUseCase,
    private val getAllSharedNotesUseCase: GetAllSharedNotesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditState())
    val state = _state.asStateFlow()

    private var autoSaveJob: Job? = null
    private var pollingJob: Job? = null
    private var isDirty = false

    init {
        viewModelScope.launch {
            val userId = getUserIdUseCase().first() ?: 0
            _state.update { it.copy(currentUserId = userId) }
            val noteId = savedStateHandle.get<String>("noteId")
            (!noteId.isNullOrBlank()).takeIf { it }?.let {
                loadNote(noteId!!, userId)
            }
        }
    }

    fun onEvent(event: NoteEditEvent) {
        when(event) {
            is NoteEditEvent.TitleChanged -> updateState { it.copy(title = event.title) }
            is NoteEditEvent.DescriptionChanged -> updateState { it.copy(description = event.description) }
            is NoteEditEvent.TagChanged -> updateState { it.copy(tag = event.tag) }
            is NoteEditEvent.PriorityChanged -> updateState { it.copy(priority = event.priority) }
            is NoteEditEvent.ReminderSet -> setReminder(event.date, event.hour, event.minute)
            is NoteEditEvent.ReminderCleared -> clearReminder()
            is NoteEditEvent.ChecklistItemAdded -> updateState { it.copy(checklist = it.checklist + ChecklistItem("", false)) }
            is NoteEditEvent.ChecklistItemUpdated -> updateChecklistItem(event.index, event.text)
            is NoteEditEvent.ChecklistItemToggled -> toggleChecklistItem(event.index)
            is NoteEditEvent.ChecklistItemRemoved -> removeChecklistItem(event.index)
            is NoteEditEvent.SaveClicked -> viewModelScope.launch { saveNoteSilent() }
            is NoteEditEvent.BackClicked -> saveAndExit()
            is NoteEditEvent.DeleteClicked -> _state.update { it.copy(isDeleteDialogVisible = true) }
            is NoteEditEvent.DeleteConfirmed -> deleteNote()
            is NoteEditEvent.ShareClicked -> checkShareRequirements()
            is NoteEditEvent.ShareWithFriend -> shareNote(event.friendId)
            is NoteEditEvent.TagMenuClicked -> _state.update { it.copy(isTagSheetVisible = true) }
            is NoteEditEvent.TagSelected -> updateState { it.copy(tag = event.tag, isTagSheetVisible = false) }
            is NoteEditEvent.TagCreated -> createTag(event.tag)
            is NoteEditEvent.TagDeleted -> deleteTag(event.tag)
            is NoteEditEvent.CollaboratorMenuClicked -> toggleCollaboratorMenu()
            is NoteEditEvent.RemoveCollaboratorRequested -> _state.update { it.copy(collaboratorPendingRemoval = event.collaborator, isCollaboratorMenuExpanded = false) }
            is NoteEditEvent.RemoveCollaboratorConfirmed -> removeCollaborator()
            is NoteEditEvent.DialogDismissed -> dismissDialogs()
            is NoteEditEvent.LoginClicked -> {
                dismissDialogs()
                _state.update { it.copy(navigationEvent = NoteEditSideEffect.NavigateToLogin) }
            }
            is NoteEditEvent.FriendsClicked -> {
                dismissDialogs()
                _state.update { it.copy(navigationEvent = NoteEditSideEffect.NavigateToFriends) }
            }
            is NoteEditEvent.NavigationHandled -> _state.update { it.copy(navigationEvent = null) }
        }
    }

    private fun updateState(update: (NoteEditState) -> NoteEditState) {
        isDirty = true
        _state.update(update)
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            isDirty.takeIf { it }?.let { saveNoteSilent() }
        }
    }

    private fun loadNote(id: String, userId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            var note = getNoteUseCase(id)
            (note == null).takeIf { it }?.let {
                val remoteId = id.toIntOrNull()
                (remoteId != null).takeIf { it }?.let {
                    val res = getSharedNoteDetailsUseCase(userId, remoteId!!)
                    (res is Resource.Success).takeIf { it }?.let { note = res.data }
                }
            }
            note?.let { n ->
                val ownerId = n.userId.takeIf { it != null && it != 0 } ?: userId
                val isOwner = ownerId == userId
                _state.update { currentState ->
                    val tagShouldBeAdded = n.tag.isNotBlank() && !currentState.availableTags.contains(n.tag)
                    val newAvailableTags = tagShouldBeAdded.takeIf { it }?.let { currentState.availableTags + n.tag } ?: currentState.availableTags

                    currentState.copy(
                        id = n.id, remoteId = n.remoteId, title = n.title, description = n.description,
                        tag = n.tag, priority = n.priority, isFinished = n.isFinished,
                        reminder = n.reminder?.takeIf { r -> r.isNotBlank() },
                        checklist = parseChecklist(n.checklist),
                        availableTags = newAvailableTags,
                        isOwner = isOwner, noteOwnerId = ownerId, isLoading = false
                    )
                }
                n.remoteId?.let { rid ->
                    startPolling(userId, rid, isOwner)
                    loadCollaborators()
                }
            } ?: _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun saveNoteSilent() {
        val s = _state.value
        (s.title.isBlank() && s.description.isBlank() && s.checklist.isEmpty()).takeIf { it }?.let { return }

        val noteId = s.id ?: UUID.randomUUID().toString()
        val ownerId = s.noteOwnerId.takeIf { it != null && it != 0 } ?: s.currentUserId ?: 0

        val isNewNote = s.remoteId == null

        val note = Note(
            id = noteId,
            remoteId = s.remoteId,
            userId = ownerId,
            title = s.title,
            description = s.description,
            tag = s.tag,
            priority = s.priority,
            isFinished = s.isFinished,
            reminder = s.reminder,
            checklist = serializeChecklist(s.checklist),
            isPendingCreate = isNewNote
        )

        upsertNoteUseCase(note, ownerId)
        (s.id == null).takeIf { it }?.let { _state.update { it.copy(id = noteId) } }

        (isNewNote).takeIf { it }?.let {
            val result = noteRepository.postNote(note, ownerId)
            (result is Resource.Success).takeIf { it }?.let {
                result.data?.let { createdNote ->
                    val updatedNote = note.copy(
                        remoteId = createdNote.remoteId,
                        isPendingCreate = false
                    )
                    upsertNoteUseCase(updatedNote, ownerId)
                    _state.update { it.copy(remoteId = createdNote.remoteId) }
                }
            }
        } ?: putNoteUseCase(note, ownerId)

        isDirty = false
    }

    private fun saveAndExit() {
        viewModelScope.launch {
            isDirty.takeIf { it }?.let { saveNoteSilent() }
            _state.update { it.copy(navigationEvent = NoteEditSideEffect.NavigateBack) }
        }
    }

    private fun deleteNote() {
        viewModelScope.launch {
            val s = _state.value
            s.id?.let { id ->
                cancelReminderUseCase(id)
                s.isOwner.takeIf { it }?.let {
                    s.remoteId?.let { deleteRemoteNoteUseCase(it) }
                    deleteNoteUseCase(id)
                } ?: run {
                    s.remoteId?.let { leaveSharedNote(it) }
                    noteRepository.deleteLocalOnly(id)
                }
            }
            _state.update { it.copy(navigationEvent = NoteEditSideEffect.NavigateBack) }
        }
    }

    private suspend fun leaveSharedNote(remoteId: Int) {
        val userId = _state.value.currentUserId ?: return
        val res = getAllSharedNotesUseCase(userId)
        (res is Resource.Success).takeIf { it }?.let {
            val share = res.data?.first?.find { it.noteId == remoteId }
            share?.let { updateSharedNoteStatusUseCase(userId, it.sharedNoteId) }
        }
    }

    private fun setReminder(date: Long, hour: Int, minute: Int) {
        val dt = Instant.ofEpochMilli(date).atZone(ZoneId.of("UTC")).toLocalDate().atTime(hour, minute)
        val fmt = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        updateState { it.copy(reminder = fmt) }
        val id = _state.value.id ?: UUID.randomUUID().toString()
        (_state.value.id == null).takeIf { it }?.let { _state.update { it.copy(id = id) } }
        viewModelScope.launch { scheduleReminderUseCase(id, _state.value.title.ifBlank { "Nota" }, dt) }
    }

    private fun clearReminder() {
        _state.value.id?.let { viewModelScope.launch { cancelReminderUseCase(it) } }
        updateState { it.copy(reminder = null) }
    }

    private fun updateChecklistItem(idx: Int, txt: String) = updateState {
        it.copy(checklist = it.checklist.mapIndexed { i, item -> (i == idx).takeIf { it }?.let { item.copy(text = txt) } ?: item })
    }

    private fun toggleChecklistItem(idx: Int) = updateState {
        it.copy(checklist = it.checklist.mapIndexed { i, item -> (i == idx).takeIf { it }?.let { item.copy(isDone = !item.isDone) } ?: item })
    }

    private fun removeChecklistItem(idx: Int) = updateState {
        it.copy(checklist = it.checklist.filterIndexed { i, _ -> i != idx })
    }

    private fun createTag(tag: String) {
        (tag.isNotBlank() && !_state.value.availableTags.contains(tag)).takeIf { it }?.let {
            updateState { it.copy(tag = tag, availableTags = it.availableTags + tag, isTagSheetVisible = false) }
        }
    }

    private fun deleteTag(tag: String) {
        _state.update { it.copy(availableTags = it.availableTags - tag, tag = (it.tag == tag).takeIf { it }?.let { "" } ?: it.tag) }
    }

    private fun checkShareRequirements() {
        val uid = _state.value.currentUserId
        ((uid == null) || (uid == 0)).takeIf { it }?.let {
            _state.update { it.copy(isLoginRequiredDialogVisible = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when(val res = getFriendsUseCase(uid!!)) {
                is Resource.Success -> {
                    val friends = res.data ?: emptyList()
                    friends.isEmpty().takeIf { it }?.let { _state.update { it.copy(isLoading = false, isNoFriendsDialogVisible = true) } }
                        ?: _state.update { it.copy(isLoading = false, isShareSheetVisible = true, friends = friends.map { f -> ucne.edu.notablelists.domain.friends.model.Friend(f.userId, f.username) }) }
                }
                else -> _state.update { it.copy(isLoading = false, errorMessage = res.message) }
            }
        }
    }

    private fun shareNote(friendId: Int) {
        viewModelScope.launch {
            _state.value.remoteId?.let { rid ->
                _state.update { it.copy(isShareSheetVisible = false, isLoading = true) }
                val res = shareNoteUseCase(_state.value.currentUserId ?: 0, rid, friendId)
                (res is Resource.Success).takeIf { it }?.let {
                    _state.update { it.copy(isLoading = false, successMessage = "Compartido exitosamente") }
                    loadCollaborators()
                } ?: _state.update { it.copy(isLoading = false, errorMessage = res.message) }
            } ?: _state.update { it.copy(errorMessage = "Sincroniza la nota antes de compartir") }
        }
    }

    private fun toggleCollaboratorMenu() {
        _state.update { it.copy(isCollaboratorMenuExpanded = !it.isCollaboratorMenuExpanded) }
        _state.value.isCollaboratorMenuExpanded.takeIf { it }?.let { loadCollaborators() }
    }

    private fun loadCollaborators() {
        viewModelScope.launch {
            val uid = _state.value.currentUserId ?: return@launch
            val rid = _state.value.remoteId ?: return@launch
            val res = getAllSharedNotesUseCase(uid)
            (res is Resource.Success).takeIf { it }?.let {
                val list = mutableListOf<Collaborator>()
                _state.value.isOwner.takeIf { it }?.let {
                    list.add(Collaborator(uid, "Yo (Dueño)", true))
                    res.data?.second?.filter { it.noteId == rid && it.status == "active" }?.forEach {
                        list.add(Collaborator(it.targetUserId, it.targetUsername ?: "Usuario", false, it.sharedNoteId))
                    }
                } ?: run {
                    res.data?.first?.find { it.noteId == rid }?.let {
                        list.add(Collaborator(it.ownerUserId, it.ownerUsername ?: "Dueño", true))
                        list.add(Collaborator(uid, "Yo", false, it.sharedNoteId))
                    }
                }
                _state.update { it.copy(collaborators = list) }
            }
        }
    }

    private fun removeCollaborator() {
        viewModelScope.launch {
            val c = _state.value.collaboratorPendingRemoval ?: return@launch
            c.sharedNoteId?.let { updateSharedNoteStatusUseCase(_state.value.currentUserId ?: 0, it) }
            _state.update { it.copy(collaboratorPendingRemoval = null) }
            loadCollaborators()
        }
    }

    private fun dismissDialogs() {
        _state.update { it.copy(isShareSheetVisible = false, isDeleteDialogVisible = false, isLoginRequiredDialogVisible = false, isNoFriendsDialogVisible = false, errorMessage = null, successMessage = null, collaboratorPendingRemoval = null) }
    }

    private fun startPolling(uid: Int, rid: Int, owner: Boolean) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                (!isDirty).takeIf { it }?.let {
                    val res = if (owner) getRemoteUserNoteUseCase(uid, rid) else getSharedNoteDetailsUseCase(uid, rid)
                    (res is Resource.Success).takeIf { it }?.let {
                        res.data?.let { r ->
                            _state.update { it.copy(title = r.title, description = r.description, tag = r.tag, priority = r.priority, isFinished = r.isFinished, checklist = parseChecklist(r.checklist)) }
                        }
                    }
                    loadCollaborators()
                }
            }
        }
    }

    private fun parseChecklist(s: String?) = s?.split("\n")?.mapNotNull { val p = it.split("|", limit=2); (p.size==2).takeIf { it }?.let { ChecklistItem(p[1], p[0]=="1") } } ?: emptyList()
    private fun serializeChecklist(l: List<ChecklistItem>) = l.isEmpty().takeIf { it }?.let { null } ?: l.joinToString("\n") { "${if (it.isDone) "1" else "0"}|${it.text}" }
}