package ucne.edu.notablelists.presentation.Notes.edit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.local.AlarmScheduler
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import ucne.edu.notablelists.domain.notes.usecase.*
import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
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
    private val alarmScheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditState())
    val state: StateFlow<NoteEditState> = _state.asStateFlow()

    private val _uiEvent = Channel<NoteEditUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        savedStateHandle.get<String>("noteId")?.let { noteId ->
            if (noteId.isNotBlank()) {
                loadNote(noteId)
            }
        }
    }

    fun onEvent(event: NoteEditEvent) {
        when (event) {
            is NoteEditEvent.EnteredTitle -> {
                _state.update { it.copy(title = event.value) }
            }
            is NoteEditEvent.EnteredDescription -> {
                _state.update { it.copy(description = event.value) }
            }
            is NoteEditEvent.EnteredTag -> {
                _state.update { it.copy(tag = event.value) }
            }
            is NoteEditEvent.ChangePriority -> {
                _state.update { it.copy(priority = event.value) }
            }
            is NoteEditEvent.SetReminder -> {
                val localDateTime = getLocalDateTime(event.date, event.timeHour, event.timeMinute)
                val formattedDate = formatDateTime(localDateTime)
                val noteId = _state.value.id ?: UUID.randomUUID().toString()

                _state.update { it.copy(id = noteId, reminder = formattedDate) }
                alarmScheduler.schedule(noteId, _state.value.title.ifBlank { "Sin Título" }, localDateTime)
            }
            is NoteEditEvent.AddChecklistItem -> {
                val newItem = ChecklistItem("", false)
                _state.update { it.copy(checklist = it.checklist + newItem) }
            }
            is NoteEditEvent.UpdateChecklistItem -> {
                _state.update {
                    val updatedList = it.checklist.mapIndexed { index, item ->
                        if (index == event.index) item.copy(text = event.text) else item
                    }
                    it.copy(checklist = updatedList)
                }
            }
            is NoteEditEvent.ToggleChecklistItem -> {
                _state.update {
                    val updatedList = it.checklist.mapIndexed { index, item ->
                        if (index == event.index) item.copy(isDone = !item.isDone) else item
                    }
                    it.copy(checklist = updatedList)
                }
            }
            is NoteEditEvent.RemoveChecklistItem -> {
                _state.update {
                    val updatedList = it.checklist.toMutableList().apply { removeAt(event.index) }
                    it.copy(checklist = updatedList)
                }
            }
            is NoteEditEvent.ToggleFinished -> {
                _state.update { it.copy(isFinished = event.isFinished) }
            }
            is NoteEditEvent.ShowDeleteDialog -> {
                _state.update { it.copy(showDeleteDialog = true) }
            }
            is NoteEditEvent.DismissDeleteDialog -> {
                _state.update { it.copy(showDeleteDialog = false) }
            }
            is NoteEditEvent.DeleteNote -> {
                deleteNote()
            }
            is NoteEditEvent.OnBackClick -> {
                saveNoteAndExit()
            }
            is NoteEditEvent.ClearReminder -> {
                _state.value.id?.let { alarmScheduler.cancel(it) }
                _state.update { it.copy(reminder = null) }
            }
            is NoteEditEvent.ShowTagSheet -> {
                _state.update { it.copy(isTagSheetOpen = true) }
            }
            is NoteEditEvent.HideTagSheet -> {
                _state.update { it.copy(isTagSheetOpen = false) }
            }
            is NoteEditEvent.SelectTag -> {
                _state.update { it.copy(tag = event.tag, isTagSheetOpen = false) }
            }
            is NoteEditEvent.CreateNewTag -> {
                if (event.tag.isNotBlank()) {
                    val newTags = if (!_state.value.availableTags.contains(event.tag)) {
                        _state.value.availableTags + event.tag
                    } else {
                        _state.value.availableTags
                    }
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
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val note = getNoteUseCase(id)
            note?.let { n ->
                _state.update { state ->
                    val updatedTags = if (n.tag.isNotBlank() && !state.availableTags.contains(n.tag)) {
                        state.availableTags + n.tag
                    } else {
                        state.availableTags
                    }
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
                        isLoading = false
                    )
                }
            } ?: _state.update { it.copy(isLoading = false) }
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

            val userId = getUserIdUseCase().first()
            val checklistString = serializeChecklist(currentState.checklist)

            val note = Note(
                id = currentState.id ?: UUID.randomUUID().toString(),
                remoteId = currentState.remoteId,
                title = currentState.title,
                description = currentState.description,
                tag = currentState.tag,
                priority = currentState.priority,
                isFinished = currentState.isFinished,
                reminder = currentState.reminder,
                checklist = checklistString,
            )

            when (val result = upsertNoteUseCase(note, userId)) {
                is Resource.Success -> {
                    if (note.remoteId != null) {
                        when (val apiResult = putNoteUseCase(note, userId)) {
                            is Resource.Success -> {
                                apiResult.data?.let { updatedNote ->
                                    upsertNoteUseCase(updatedNote, userId)
                                }
                            }
                            is Resource.Error -> {
                                Log.e("UPDATE", "Failed to update on server: ${apiResult.message}")
                            }
                            else -> {}
                        }
                    } else {
                        if (userId != null) {
                            noteRepository.syncOnLogin(userId)
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

    private fun deleteNote() {
        viewModelScope.launch {
            val id = _state.value.id
            val remoteId = _state.value.remoteId

            if (id != null) {
                alarmScheduler.cancel(id)
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