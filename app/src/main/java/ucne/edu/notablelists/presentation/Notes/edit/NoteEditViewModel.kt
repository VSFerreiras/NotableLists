package ucne.edu.notablelists.presentation.Notes.edit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.friends.usecase.GetFriendsUseCase
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import ucne.edu.notablelists.domain.notes.usecase.*
import ucne.edu.notablelists.domain.notification.CancelReminderUseCase
import ucne.edu.notablelists.domain.notification.ScheduleReminderUseCase
import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
import ucne.edu.notablelists.domain.sharednote.usecase.GetSharedNoteDetailsUseCase
import ucne.edu.notablelists.domain.sharednote.usecase.ShareNoteUseCase
import java.time.Instant
import java.time.LocalDateTime
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditState())
    val state: StateFlow<NoteEditState> = _state.asStateFlow()

    private val _uiEvent = Channel<NoteEditUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var pollingJob: Job? = null
    private var autoSaveJob: Job? = null

    private var isDirty = false

    init {
        savedStateHandle.get<String>("noteId")?.let { noteId ->
            if (noteId.isNotBlank()) {
                loadNote(noteId)
            }
        }
        checkUserSession()
    }

    private fun checkUserSession() {
        viewModelScope.launch {
            val userId = getUserIdUseCase().first()
            _state.update { it.copy(currentUserId = userId) }
        }
    }

    fun onEvent(event: NoteEditEvent) {
        when (event) {
            is NoteEditEvent.EnteredTitle -> {
                markAsDirty()
                _state.update { it.copy(title = event.value) }
                triggerAutoSave()
            }
            is NoteEditEvent.EnteredDescription -> {
                markAsDirty()
                _state.update { it.copy(description = event.value) }
                triggerAutoSave()
            }
            is NoteEditEvent.EnteredTag -> {
                markAsDirty()
                _state.update { it.copy(tag = event.value) }
                triggerAutoSave()
            }
            is NoteEditEvent.ChangePriority -> {
                markAsDirty()
                _state.update { it.copy(priority = event.value) }
                triggerAutoSave()
            }
            is NoteEditEvent.SetReminder -> {
                val localDateTime = getLocalDateTime(event.date, event.timeHour, event.timeMinute)
                val formattedDate = formatDateTime(localDateTime)
                val noteId = _state.value.id ?: UUID.randomUUID().toString()

                markAsDirty()
                _state.update { it.copy(id = noteId, reminder = formattedDate) }
                scheduleReminderUseCase(noteId, _state.value.title.ifBlank { "Sin Título" }, localDateTime)
                triggerAutoSave()
            }
            is NoteEditEvent.AddChecklistItem -> {
                val newItem = ChecklistItem("", false)
                markAsDirty()
                _state.update { it.copy(checklist = it.checklist + newItem) }
                triggerAutoSave()
            }
            is NoteEditEvent.UpdateChecklistItem -> {
                markAsDirty()
                _state.update {
                    val updatedList = it.checklist.mapIndexed { index, item ->
                        if (index == event.index) item.copy(text = event.text) else item
                    }
                    it.copy(checklist = updatedList)
                }
                triggerAutoSave()
            }
            is NoteEditEvent.ToggleChecklistItem -> {
                markAsDirty()
                _state.update {
                    val updatedList = it.checklist.mapIndexed { index, item ->
                        if (index == event.index) item.copy(isDone = !item.isDone) else item
                    }
                    it.copy(checklist = updatedList)
                }
                triggerAutoSave()
            }
            is NoteEditEvent.RemoveChecklistItem -> {
                markAsDirty()
                _state.update {
                    val updatedList = it.checklist.toMutableList().apply { removeAt(event.index) }
                    it.copy(checklist = updatedList)
                }
                triggerAutoSave()
            }
            is NoteEditEvent.ToggleFinished -> {
                markAsDirty()
                _state.update { it.copy(isFinished = event.isFinished) }
                triggerAutoSave()
            }
            is NoteEditEvent.ShowDeleteDialog -> {
                _state.update { it.copy(showDeleteDialog = true) }
            }
            is NoteEditEvent.DismissDeleteDialog -> {
                _state.update { it.copy(showDeleteDialog = false) }
            }
            is NoteEditEvent.DeleteNote -> {
                handleDeleteAction()
            }
            is NoteEditEvent.OnBackClick -> {
                saveNoteAndExit()
            }
            is NoteEditEvent.ClearReminder -> {
                _state.value.id?.let { cancelReminderUseCase(it) }
                markAsDirty()
                _state.update { it.copy(reminder = null) }
                triggerAutoSave()
            }
            is NoteEditEvent.ShowTagSheet -> {
                _state.update { it.copy(isTagSheetOpen = true) }
            }
            is NoteEditEvent.HideTagSheet -> {
                _state.update { it.copy(isTagSheetOpen = false) }
            }
            is NoteEditEvent.SelectTag -> {
                markAsDirty()
                _state.update { it.copy(tag = event.tag, isTagSheetOpen = false) }
                triggerAutoSave()
            }
            is NoteEditEvent.CreateNewTag -> {
                if (event.tag.isNotBlank()) {
                    val newTags = if (!_state.value.availableTags.contains(event.tag)) {
                        _state.value.availableTags + event.tag
                    } else {
                        _state.value.availableTags
                    }
                    markAsDirty()
                    _state.update {
                        it.copy(
                            tag = event.tag,
                            availableTags = newTags,
                            isTagSheetOpen = false
                        )
                    }
                }
            }
            is NoteEditEvent.DeleteAvailableTag -> {
                _state.update {
                    val newTags = it.availableTags - event.tag
                    val currentTag = if (it.tag == event.tag) "" else it.tag
                    it.copy(availableTags = newTags, tag = currentTag)
                }
            }
            is NoteEditEvent.OnShareClick -> {
                checkShareRequirements()
            }
            is NoteEditEvent.DismissShareDialogs -> {
                _state.update {
                    it.copy(
                        showLoginRequiredDialog = false,
                        showNoFriendsDialog = false,
                        showShareSheet = false,
                        errorMessage = null,
                        successMessage = null
                    )
                }
            }
            is NoteEditEvent.NavigateToLogin -> {
                _state.update { it.copy(showLoginRequiredDialog = false) }
                sendUiEvent(NoteEditUiEvent.NavigateToLogin)
            }
            is NoteEditEvent.NavigateToFriends -> {
                _state.update { it.copy(showNoFriendsDialog = false) }
                sendUiEvent(NoteEditUiEvent.NavigateToFriendList)
            }
            is NoteEditEvent.ShareWithFriend -> {
                shareNote(event.friendId)
            }
        }
    }

    private fun markAsDirty() {
        isDirty = true
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            if (isActive) {
                saveNoteSilent()
                isDirty = false
            }
        }
    }

    private suspend fun saveNoteSilent() {
        val currentState = _state.value
        if (currentState.id == null) return

        val effectiveOwnerId = currentState.noteOwnerId ?: currentState.currentUserId ?: return

        val checklistString = serializeChecklist(currentState.checklist)
        val note = Note(
            id = currentState.id,
            remoteId = currentState.remoteId,
            userId = effectiveOwnerId,
            title = currentState.title,
            description = currentState.description,
            tag = currentState.tag,
            priority = currentState.priority,
            isFinished = currentState.isFinished,
            reminder = currentState.reminder,
            checklist = checklistString
        )

        upsertNoteUseCase(note, effectiveOwnerId)

        if (note.remoteId != null) {
            putNoteUseCase(note, effectiveOwnerId)
        }
    }

    private fun checkShareRequirements() {
        viewModelScope.launch {
            val userId = _state.value.currentUserId
            if (userId == null || userId == 0) {
                _state.update { it.copy(showLoginRequiredDialog = true) }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }
            when (val result = getFriendsUseCase(userId)) {
                is Resource.Success -> {
                    val friends = result.data ?: emptyList()
                    if (friends.isEmpty()) {
                        _state.update { it.copy(isLoading = false, showNoFriendsDialog = true) }
                    } else {
                        val domainFriends = friends.map {
                            ucne.edu.notablelists.domain.friends.model.Friend(it.userId, it.username)
                        }
                        _state.update { it.copy(isLoading = false, showShareSheet = true, friends = domainFriends) }
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun shareNote(friendId: Int) {
        viewModelScope.launch {
            val userId = _state.value.currentUserId ?: return@launch
            val noteRemoteId = _state.value.remoteId

            if (noteRemoteId == null) {
                _state.update { it.copy(errorMessage = "Debes sincronizar la nota antes de compartirla.") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, showShareSheet = false) }

            when (val result = shareNoteUseCase(userId, noteRemoteId, friendId)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, successMessage = "Nota compartida exitosamente") }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val userId = getUserIdUseCase().first()

            var note = getNoteUseCase(id)

            if (note == null && userId != null) {
                val remoteId = id.toIntOrNull()
                if (remoteId != null) {
                    when(val result = getSharedNoteDetailsUseCase(userId, remoteId)) {
                        is Resource.Success -> {
                            note = result.data
                            startPolling(userId, remoteId)
                        }
                        is Resource.Error -> {
                            _state.update { it.copy(errorMessage = "Error cargando nota compartida: ${result.message}") }
                        }
                        else -> {}
                    }
                }
            } else if (note != null && note.remoteId != null && userId != null) {
                startPolling(userId, note.remoteId)
            }

            note?.let { n ->
                _state.update { state ->
                    val updatedTags = if (n.tag.isNotBlank() && !state.availableTags.contains(n.tag)) {
                        state.availableTags + n.tag
                    } else {
                        state.availableTags
                    }
                    val isOwner = n.userId == userId || n.userId == null

                    state.copy(
                        id = n.id,
                        remoteId = n.remoteId,
                        title = n.title,
                        description = n.description,
                        tag = n.tag,
                        priority = n.priority,
                        isFinished = n.isFinished,
                        reminder = n.reminder,
                        checklist = parseChecklist(n.checklist),
                        availableTags = updatedTags,
                        isLoading = false,
                        isOwner = isOwner,
                        noteOwnerId = n.userId
                    )
                }
            } ?: _state.update { it.copy(isLoading = false) }
        }
    }

    private fun startPolling(userId: Int, remoteId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    delay(5000)
                    if (!isDirty) {
                        when(val result = getSharedNoteDetailsUseCase(userId, remoteId)) {
                            is Resource.Success -> {
                                result.data?.let { n ->
                                    _state.update { state ->
                                        if (!isDirty) {
                                            state.copy(
                                                title = n.title,
                                                description = n.description,
                                                tag = n.tag,
                                                priority = n.priority,
                                                isFinished = n.isFinished,
                                                checklist = parseChecklist(n.checklist)
                                            )
                                        } else {
                                            state
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun handleDeleteAction() {
        if (_state.value.isOwner) {
            deleteNoteOwner()
        } else {
            leaveSharedNote()
        }
    }

    private fun leaveSharedNote() {
        viewModelScope.launch {
            val id = _state.value.id
            if (id != null) {
                deleteNoteUseCase(id)
                _state.update { it.copy(showDeleteDialog = false) }
                sendUiEvent(NoteEditUiEvent.NavigateBack)
            }
        }
    }

    private fun deleteNoteOwner() {
        viewModelScope.launch {
            val id = _state.value.id
            val remoteId = _state.value.remoteId

            if (id != null) {
                cancelReminderUseCase(id)
                if (remoteId != null) {
                    deleteRemoteNoteUseCase(remoteId)
                }
                when (val result = deleteNoteUseCase(id)) {
                    is Resource.Success -> {
                        _state.update { it.copy(showDeleteDialog = false) }
                        sendUiEvent(NoteEditUiEvent.NavigateBack)
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(errorMessage = result.message, showDeleteDialog = false) }
                    }
                    else -> {}
                }
            } else {
                _state.update { it.copy(showDeleteDialog = false) }
                sendUiEvent(NoteEditUiEvent.NavigateBack)
            }
        }
    }

    private fun saveNoteAndExit() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.title.isBlank() && currentState.description.isBlank() && currentState.checklist.isEmpty()) {
                sendUiEvent(NoteEditUiEvent.NavigateBack)
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            val effectiveOwnerId = currentState.noteOwnerId ?: currentState.currentUserId
            val checklistString = serializeChecklist(currentState.checklist)

            val note = Note(
                id = currentState.id ?: UUID.randomUUID().toString(),
                remoteId = currentState.remoteId,
                userId = effectiveOwnerId,
                title = currentState.title,
                description = currentState.description,
                tag = currentState.tag,
                priority = currentState.priority,
                isFinished = currentState.isFinished,
                reminder = currentState.reminder,
                checklist = checklistString,
            )

            when (val result = upsertNoteUseCase(note, effectiveOwnerId)) {
                is Resource.Success -> {
                    if (note.remoteId != null && effectiveOwnerId != null) {
                        putNoteUseCase(note, effectiveOwnerId)
                    } else {
                        if (effectiveOwnerId != null && currentState.isOwner) {
                            noteRepository.syncOnLogin(effectiveOwnerId)
                        }
                    }

                    _state.update { it.copy(isLoading = false) }
                    sendUiEvent(NoteEditUiEvent.NavigateBack)
                }
                is Resource.Error -> {
                    _state.update { it.copy(errorMessage = result.message, isLoading = false) }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun getLocalDateTime(date: Long, hour: Int, minute: Int): LocalDateTime {
        val localDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
        return LocalDateTime.of(localDate, java.time.LocalTime.of(hour, minute))
    }

    private fun formatDateTime(localDateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return localDateTime.format(formatter)
    }

    private fun parseChecklist(checklistString: String?): List<ChecklistItem> {
        if (checklistString.isNullOrBlank()) return emptyList()
        return checklistString.split("\n").mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) {
                ChecklistItem(parts[1], parts[0] == "1")
            } else null
        }
    }

    private fun serializeChecklist(items: List<ChecklistItem>): String? {
        if (items.isEmpty()) return null
        return items.joinToString("\n") {
            "${if (it.isDone) "1" else "0"}|${it.text}"
        }
    }

    private fun sendUiEvent(event: NoteEditUiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}