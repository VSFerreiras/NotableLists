package ucne.edu.notablelists.presentation.Notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.friends.usecase.GetPendingRequestUseCase
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.usecase.*
import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
import ucne.edu.notablelists.domain.sharednote.usecase.GetNotesSharedWithMeUseCase
import ucne.edu.notablelists.domain.sharednote.usecase.SyncSharedNotesUseCase
import javax.inject.Inject

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val upsertNoteUseCase: UpsertNoteUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val fetchUserNotesUseCase: FetchUserNotesUseCase,
    private val postPendingNotesUseCase: PostPendingNotesUseCase,
    private val getPendingRequestUseCase: GetPendingRequestUseCase,
    private val getNotesSharedWithMeUseCase: GetNotesSharedWithMeUseCase,
    private val syncSharedNotesUseCase: SyncSharedNotesUseCase
) : ViewModel() {

    private val _localNotes = MutableStateFlow<List<Note>>(emptyList())
    private val _sharedNotes = MutableStateFlow<List<Note>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(NoteFilter.DATE)

    private val _state = MutableStateFlow(NotesListState())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<NotesListSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private var pollingJob: Job? = null
    private var _currentUserId: Int? = null

    init {
        loadNotes()
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                _localNotes,
                _sharedNotes,
                _searchQuery,
                _selectedFilter,
                _state
            ) { local, shared, query, filter, currentState ->
                val combined = combineNotes(local, shared)
                val filtered = filterAndSortNotes(combined, query, filter)
                val uiNotes = filtered.map { mapToUiItem(it, currentState.selectedNoteIds, _currentUserId) }
                val uiFilters = NoteFilter.entries.map { mapToFilterUiItem(it, filter) }

                currentState.copy(
                    notes = uiNotes,
                    filterChips = uiFilters,
                    searchQuery = query
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun onEvent(event: NotesListEvent) {
        when(event) {
            is NotesListEvent.Refresh -> refresh()
            is NotesListEvent.AddNoteClicked -> handleAddNote()
            is NotesListEvent.DeleteNote -> deleteNote(event.id)
            is NotesListEvent.ToggleFinished -> toggleNoteFinished(event.note)
            is NotesListEvent.NoteClicked -> handleNoteClick(event.id)
            is NotesListEvent.NoteLongClicked -> toggleSelection(event.id)
            is NotesListEvent.SearchQueryChanged -> _searchQuery.value = event.query
            is NotesListEvent.FilterChanged -> _selectedFilter.value = event.filter
            is NotesListEvent.LogoutClicked -> _state.update { it.copy(showLogoutDialog = true) }
            is NotesListEvent.LogoutConfirmed -> _state.update { it.copy(showLogoutDialog = false) }
            is NotesListEvent.LogoutDismissed -> _state.update { it.copy(showLogoutDialog = false) }
            is NotesListEvent.DeleteSelectedClicked -> _state.update { it.copy(showDeleteSelectionDialog = true) }
            is NotesListEvent.DeleteSelectedConfirmed -> deleteSelectedNotes()
            is NotesListEvent.DeleteSelectedDismissed -> _state.update { it.copy(showDeleteSelectionDialog = false) }
            is NotesListEvent.SelectionCleared -> _state.update { it.copy(selectedNoteIds = emptySet()) }
        }
    }

    private fun handleAddNote() {
        if (_state.value.isSelectionMode) {
            _state.update { it.copy(showDeleteSelectionDialog = true) }
        } else {
            viewModelScope.launch { _sideEffect.send(NotesListSideEffect.NavigateToDetail(null)) }
        }
    }

    private fun handleNoteClick(id: String) {
        if (_state.value.isSelectionMode) {
            toggleSelection(id)
        } else {
            viewModelScope.launch { _sideEffect.send(NotesListSideEffect.NavigateToDetail(id)) }
        }
    }

    private fun toggleSelection(id: String) {
        _state.update {
            val current = it.selectedNoteIds
            val newIds = if (current.contains(id)) current - id else current + id
            it.copy(selectedNoteIds = newIds)
        }
    }

    private fun combineNotes(local: List<Note>, shared: List<Note>): List<Note> {
        val localRemoteIds = local.mapNotNull { it.remoteId }.toSet()
        val sharedUnique = shared.filter { it.remoteId != null && !localRemoteIds.contains(it.remoteId) }
        return local + sharedUnique
    }

    private fun mapToUiItem(note: Note, selectedIds: Set<String>, currentUserId: Int?): NoteUiItem {
        val style = when (note.priority) {
            2 -> NoteStyle.Error
            1 -> NoteStyle.Primary
            else -> NoteStyle.Secondary
        }

        val isShared = (note.remoteId != null && _sharedNotes.value.any { it.remoteId == note.remoteId }) ||
                (note.userId != null && currentUserId != null && note.userId != currentUserId)

        return NoteUiItem(
            id = note.id,
            title = note.title,
            description = note.description,
            style = style,
            reminder = note.reminder?.takeIf { it.isNotBlank() },
            isSelected = selectedIds.contains(note.id),
            isShared = isShared,
            priorityChips = if (note.priority > 0) listOf(PriorityUiItem(if(note.priority==1) "Media" else "Alta", style)) else emptyList(),
            tags = if (note.tag.isNotBlank()) listOf(TagUiItem(note.tag, style)) else emptyList()
        )
    }

    private fun mapToFilterUiItem(filter: NoteFilter, selected: NoteFilter) =
        FilterUiItem(filter, filter.label, filter == selected)

    private fun loadNotes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val userId = getUserIdUseCase().first()
            _currentUserId = userId

            if (userId != null) {
                launch { checkPendingRequests(userId) }
                launch { fetchSharedNotes(userId) }
                startPolling(userId)
                fetchUserNotesUseCase(userId)
            }

            getNotesUseCase().collect { notes ->
                _localNotes.value = notes
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startPolling(userId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                try {
                    checkPendingRequests(userId)
                    fetchSharedNotes(userId)
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun fetchSharedNotes(userId: Int) {
        val result = getNotesSharedWithMeUseCase(userId)
        if (result is Resource.Success) {
            _sharedNotes.value = result.data ?: emptyList()
        }
    }

    private suspend fun checkPendingRequests(userId: Int) {
        val result = getPendingRequestUseCase(userId)
        if (result is Resource.Success) {
            _state.update { it.copy(pendingRequestCount = result.data?.size ?: 0) }
        }
    }

    private fun filterAndSortNotes(notes: List<Note>, query: String, filter: NoteFilter): List<Note> {
        val filtered = if (query.isBlank()) notes else notes.filter {
            it.title.contains(query, true) || it.description.contains(query, true)
        }

        return when (filter) {
            NoteFilter.AZ -> filtered.sortedBy { it.title.lowercase() }
            NoteFilter.ZA -> filtered.sortedByDescending { it.title.lowercase() }
            NoteFilter.DATE -> filtered
            NoteFilter.HIGH_PRIORITY -> filtered.filter { it.priority == 2 }
            NoteFilter.MEDIUM_PRIORITY -> filtered.filter { it.priority == 1 }
            NoteFilter.LOW_PRIORITY -> filtered.filter { it.priority == 0 }
            NoteFilter.SHARED -> filtered.filter { note ->
                (note.remoteId != null && _sharedNotes.value.any { it.remoteId == note.remoteId }) ||
                        (note.userId != null && _currentUserId != null && note.userId != _currentUserId)
            }
        }
    }

    private fun deleteNote(id: String) {
        viewModelScope.launch { deleteNoteUseCase(id) }
    }

    private fun deleteSelectedNotes() {
        viewModelScope.launch {
            _state.update { it.copy(showDeleteSelectionDialog = false, isLoading = true) }
            val ids = _state.value.selectedNoteIds.toList()
            _state.update { it.copy(selectedNoteIds = emptySet()) }
            ids.forEach { deleteNoteUseCase(it) }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun toggleNoteFinished(note: Note) {
        viewModelScope.launch {
            val userId = getUserIdUseCase().first()
            upsertNoteUseCase(note.copy(isFinished = !note.isFinished), userId)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val userId = getUserIdUseCase().first()
            if (userId != null) {
                checkPendingRequests(userId)
                fetchUserNotesUseCase(userId)
                syncSharedNotesUseCase(userId)
                fetchSharedNotes(userId)
                val res = postPendingNotesUseCase(userId)
                if (res is Resource.Error) {
                    _state.update { it.copy(errorMessage = res.message) }
                }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }
}