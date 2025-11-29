package ucne.edu.notablelists.presentation.Notes.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.TriggerSyncUseCase
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.usecase.DeleteNoteUseCase
import ucne.edu.notablelists.domain.notes.usecase.GetNoteUseCase
import ucne.edu.notablelists.domain.notes.usecase.PostNoteUseCase
import ucne.edu.notablelists.domain.notes.usecase.PutNoteUseCase
import ucne.edu.notablelists.domain.notes.usecase.UpsertNoteUseCase
import ucne.edu.notablelists.presentation.add_edit_note.NoteEditState
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val upsertNoteUseCase: UpsertNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val postNoteUseCase: PostNoteUseCase,
    private val putNoteUseCase: PutNoteUseCase,
    private val triggerSyncUseCase: TriggerSyncUseCase,
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
            is NoteEditEvent.ChangeReminder -> {
                _state.update { it.copy(reminder = event.value) }
            }
            is NoteEditEvent.ChangeChecklist -> {
                _state.update { it.copy(checklist = event.value) }
            }
            is NoteEditEvent.ToggleAutoDelete -> {
                _state.update { it.copy(autoDelete = event.isEnabled) }
            }
            is NoteEditEvent.ToggleFinished -> {
                _state.update { it.copy(isFinished = event.isFinished) }
            }
            is NoteEditEvent.SaveNote -> {
                saveNote()
            }
            is NoteEditEvent.DeleteNote -> {
                deleteNote()
            }
            is NoteEditEvent.OnBackClick -> {
                sendUiEvent(NoteEditUiEvent.NavigateBack)
            }
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val note = getNoteUseCase(id)
            note?.let { n ->
                _state.update {
                    it.copy(
                        id = n.id,
                        remoteId = n.remoteId,
                        title = n.title,
                        description = n.description,
                        tag = n.tag,
                        priority = n.priority,
                        isFinished = n.isFinished,
                        reminder = n.reminder,
                        checklist = n.checklist,
                        autoDelete = n.autoDelete,
                        deleteAt = n.deleteAt,
                        isLoading = false
                    )
                }
            } ?: _state.update { it.copy(isLoading = false) }
        }
    }

    private fun saveNote() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.title.isBlank()) {
                _state.update { it.copy(errorMessage = "El título no puede estar vacío") }
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            val note = Note(
                id = currentState.id ?: UUID.randomUUID().toString(),
                remoteId = currentState.remoteId,
                title = currentState.title,
                description = currentState.description,
                tag = currentState.tag,
                priority = currentState.priority,
                isFinished = currentState.isFinished,
                reminder = currentState.reminder,
                checklist = currentState.checklist,
                autoDelete = currentState.autoDelete,
                deleteAt = currentState.deleteAt
            )

            when (val result = upsertNoteUseCase(note)) {
                is Resource.Success -> {
                    val apiResult = if (note.remoteId == null) {
                        postNoteUseCase(note)
                    } else {
                        putNoteUseCase(note)
                    }

                    when (apiResult) {
                        is Resource.Success -> {
                            triggerSyncUseCase()
                            _state.update { it.copy(isLoading = false) }
                            sendUiEvent(NoteEditUiEvent.NavigateBack)
                        }
                        is Resource.Error -> {
                            _state.update { it.copy(errorMessage = apiResult.message, isLoading = false) }
                        }
                        is Resource.Loading -> {
                            _state.update { it.copy(isLoading = true) }
                        }
                    }
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
            if (id != null) {
                when (val result = deleteNoteUseCase(id)) {
                    is Resource.Success -> {
                        triggerSyncUseCase()
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
            } else {
                sendUiEvent(NoteEditUiEvent.NavigateBack)
            }
        }
    }

    fun errorMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun sendUiEvent(event: NoteEditUiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}