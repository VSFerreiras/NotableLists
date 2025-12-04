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
            if (!noteId.isNullOrBlank()) {
                loadNote(noteId, userId)
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
            if (isDirty) saveNoteSilent()
        }
    }

    private fun loadNote(id: String, userId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            var note = getNoteUseCase(id)
            if (note == null) {
                val remoteId = id.toIntOrNull()
                if (remoteId != null) {
                    val res = getSharedNoteDetailsUseCase(userId, remoteId)
                    if (res is Resource.Success) note = res.data
                }
            }
            note?.let { n ->
                val ownerId = if (n.userId != null && n.userId != 0) n.userId else userId
                val isOwner = ownerId == userId
                _state.update {
                    it.copy(
                        id = n.id, remoteId = n.remoteId, title = n.title, description = n.description,
                        tag = n.tag, priority = n.priority, isFinished = n.isFinished,
                        reminder = n.reminder?.takeIf { r -> r.isNotBlank() },
                        checklist = parseChecklist(n.checklist),
                        availableTags = if (n.tag.isNotBlank() && !it.availableTags.contains(n.tag)) it.availableTags + n.tag else it.availableTags,
                        isOwner = isOwner, noteOwnerId = ownerId, isLoading = false
                    )
                }
                if (n.remoteId != null) {
                    startPolling(userId, n.remoteId, isOwner)
                    loadCollaborators()
                }
            } ?: _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun saveNoteSilent() {
        val s = _state.value
        if (s.title.isBlank() && s.description.isBlank() && s.checklist.isEmpty()) return

        val noteId = s.id ?: UUID.randomUUID().toString()
        val ownerId = if (s.noteOwnerId != null && s.noteOwnerId != 0) s.noteOwnerId else s.currentUserId ?: 0

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
        if (s.id == null) _state.update { it.copy(id = noteId) }

        if (isNewNote) {
            val result = noteRepository.postNote(note, ownerId)
            if (result is Resource.Success) {
                result.data?.let { createdNote ->
                    val updatedNote = note.copy(
                        remoteId = createdNote.remoteId,
                        isPendingCreate = false
                    )
                    upsertNoteUseCase(updatedNote, ownerId)
                    _state.update { it.copy(remoteId = createdNote.remoteId) }
                }
            }
        } else {
            putNoteUseCase(note, ownerId)
        }

        isDirty = false
    }

    private fun saveAndExit() {
        viewModelScope.launch {
            if (isDirty) saveNoteSilent()
            _state.update { it.copy(navigationEvent = NoteEditSideEffect.NavigateBack) }
        }
    }

    private fun deleteNote() {
        viewModelScope.launch {
            val s = _state.value
            s.id?.let { id ->
                cancelReminderUseCase(id)
                if (s.isOwner) {
                    s.remoteId?.let { deleteRemoteNoteUseCase(it) }
                    deleteNoteUseCase(id)
                } else {
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
        if (res is Resource.Success) {
            val share = res.data?.first?.find { it.noteId == remoteId }
            share?.let { updateSharedNoteStatusUseCase(userId, it.sharedNoteId) }
        }
    }

    private fun setReminder(date: Long, hour: Int, minute: Int) {
        val dt = Instant.ofEpochMilli(date).atZone(ZoneId.of("UTC")).toLocalDate().atTime(hour, minute)
        val fmt = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        updateState { it.copy(reminder = fmt) }
        val id = _state.value.id ?: UUID.randomUUID().toString()
        if (_state.value.id == null) _state.update { it.copy(id = id) }
        viewModelScope.launch { scheduleReminderUseCase(id, _state.value.title.ifBlank { "Nota" }, dt) }
    }

    private fun clearReminder() {
        _state.value.id?.let { viewModelScope.launch { cancelReminderUseCase(it) } }
        updateState { it.copy(reminder = null) }
    }

    private fun updateChecklistItem(idx: Int, txt: String) = updateState {
        it.copy(checklist = it.checklist.mapIndexed { i, item -> if (i == idx) item.copy(text = txt) else item })
    }

    private fun toggleChecklistItem(idx: Int) = updateState {
        it.copy(checklist = it.checklist.mapIndexed { i, item -> if (i == idx) item.copy(isDone = !item.isDone) else item })
    }

    private fun removeChecklistItem(idx: Int) = updateState {
        it.copy(checklist = it.checklist.filterIndexed { i, _ -> i != idx })
    }

    private fun createTag(tag: String) {
        if (tag.isNotBlank() && !_state.value.availableTags.contains(tag)) {
            updateState { it.copy(tag = tag, availableTags = it.availableTags + tag, isTagSheetVisible = false) }
        }
    }

    private fun deleteTag(tag: String) {
        _state.update { it.copy(availableTags = it.availableTags - tag, tag = if (it.tag == tag) "" else it.tag) }
    }

    private fun checkShareRequirements() {
        val uid = _state.value.currentUserId
        if (uid == null || uid == 0) {
            _state.update { it.copy(isLoginRequiredDialogVisible = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when(val res = getFriendsUseCase(uid)) {
                is Resource.Success -> {
                    val friends = res.data ?: emptyList()
                    if (friends.isEmpty()) _state.update { it.copy(isLoading = false, isNoFriendsDialogVisible = true) }
                    else _state.update { it.copy(isLoading = false, isShareSheetVisible = true, friends = friends.map { f -> ucne.edu.notablelists.domain.friends.model.Friend(f.userId, f.username) }) }
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
                if (res is Resource.Success) {
                    _state.update { it.copy(isLoading = false, successMessage = "Compartido exitosamente") }
                    loadCollaborators()
                } else _state.update { it.copy(isLoading = false, errorMessage = res.message) }
            } ?: _state.update { it.copy(errorMessage = "Sincroniza la nota antes de compartir") }
        }
    }

    private fun toggleCollaboratorMenu() {
        _state.update { it.copy(isCollaboratorMenuExpanded = !it.isCollaboratorMenuExpanded) }
        if (_state.value.isCollaboratorMenuExpanded) loadCollaborators()
    }

    private fun loadCollaborators() {
        viewModelScope.launch {
            val uid = _state.value.currentUserId ?: return@launch
            val rid = _state.value.remoteId ?: return@launch
            val res = getAllSharedNotesUseCase(uid)
            if (res is Resource.Success) {
                val list = mutableListOf<Collaborator>()
                if (_state.value.isOwner) {
                    list.add(Collaborator(uid, "Yo (Dueño)", true))
                    res.data?.second?.filter { it.noteId == rid && it.status == "active" }?.forEach {
                        list.add(Collaborator(it.targetUserId, it.targetUsername ?: "Usuario", false, it.sharedNoteId))
                    }
                } else {
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
                if (!isDirty) {
                    val res = if (owner) getRemoteUserNoteUseCase(uid, rid) else getSharedNoteDetailsUseCase(uid, rid)
                    if (res is Resource.Success) {
                        res.data?.let { r ->
                            _state.update { it.copy(title = r.title, description = r.description, tag = r.tag, priority = r.priority, isFinished = r.isFinished, checklist = parseChecklist(r.checklist)) }
                        }
                    }
                    loadCollaborators()
                }
            }
        }
    }

    private fun parseChecklist(s: String?) = s?.split("\n")?.mapNotNull { val p = it.split("|", limit=2); if(p.size==2) ChecklistItem(p[1], p[0]=="1") else null } ?: emptyList()
    private fun serializeChecklist(l: List<ChecklistItem>) = if (l.isEmpty()) null else l.joinToString("\n") { "${if (it.isDone) "1" else "0"}|${it.text}" }
}