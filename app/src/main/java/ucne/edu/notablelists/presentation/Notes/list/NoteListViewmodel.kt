package ucne.edu.notablelists.presentation.Notes.list

import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.friends.usecase.GetPendingRequestUseCase
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.usecase.*
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
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _navigationEvent = MutableStateFlow<List<String?>>(emptyList())
    private val _showLogoutDialog = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    private val _showDeleteSelectionDialog = MutableStateFlow(false)
    private val _pendingRequestCount = MutableStateFlow(0)
    private var _currentUserId: Int? = null

    private var pollingJob: Job? = null

    private val _filterState = combine(_searchQuery, _selectedFilter) { query, filter ->
        Pair(query, filter)
    }

    private val _uiStatusState = combine(_isLoading, _errorMessage, _isRefreshing) { loading, error, refreshing ->
        Triple(loading, error, refreshing)
    }

    private val _combinedNotes = combine(_localNotes, _sharedNotes) { local, shared ->
        val localRemoteIds = local.mapNotNull { it.remoteId }.toSet()

        val uniqueShared = shared.filter { sharedNote ->
            sharedNote.remoteId == null || !localRemoteIds.contains(sharedNote.remoteId)
        }

        local + uniqueShared
    }

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<NotesListState> = combine(
        _combinedNotes,
        _filterState,
        _uiStatusState,
        _navigationEvent,
        _showLogoutDialog,
        _selectedNoteIds,
        _showDeleteSelectionDialog,
        _pendingRequestCount
    ) { args ->
        val notes = args[0] as List<Note>
        val filterState = args[1] as Pair<String, NoteFilter>
        val uiStatusState = args[2] as Triple<Boolean, String?, Boolean>
        val navEvent = args[3] as List<String?>
        val showLogout = args[4] as Boolean
        val selectedIds = args[5] as Set<String>
        val showDeleteDialog = args[6] as Boolean
        val pendingCount = args[7] as Int

        val (query, selectedFilter) = filterState
        val (isLoading, errorMessage, _) = uiStatusState

        val filteredNotes = filterAndSortNotes(notes, query, selectedFilter)

        val uiNotes = if (isLoading && notes.isEmpty()) {
            emptyList()
        } else {
            filteredNotes.map { note -> mapToUiItem(note, selectedIds.contains(note.id)) }
        }

        val uiFilters = NoteFilter.entries.map { filter ->
            mapToFilterUiItem(filter, selectedFilter)
        }

        val loadingList = if (isLoading && notes.isEmpty()) listOf(Unit) else emptyList()
        val errorList = if (errorMessage != null) listOf(errorMessage) else emptyList()

        NotesListState(
            notes = uiNotes,
            filterChips = uiFilters,
            loadingStatus = loadingList,
            errorMessage = errorList,
            navigateToDetail = navEvent,
            searchQuery = query,
            showLogoutDialog = showLogout,
            selectedNoteIds = selectedIds,
            showDeleteSelectionDialog = showDeleteDialog,
            pendingRequestCount = pendingCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NotesListState()
    )

    init {
        loadNotes()
    }

    fun onEvent(event: NotesListEvent) {
        when (event) {
            is NotesListEvent.Refresh -> refresh()
            is NotesListEvent.DeleteNote -> deleteNote(event.id)
            is NotesListEvent.ToggleNoteFinished -> toggleNoteFinished(event.note)
            is NotesListEvent.OnAddNoteClick -> {
                if (_selectedNoteIds.value.isNotEmpty()) {
                    _showDeleteSelectionDialog.value = true
                } else {
                    _navigationEvent.value = listOf(null)
                }
            }
            is NotesListEvent.OnNoteClick -> handleNoteClick(event.id)
            is NotesListEvent.OnNoteLongClick -> handleNoteLongClick(event.id)
            is NotesListEvent.OnSearchQueryChange -> _searchQuery.value = event.query
            is NotesListEvent.OnFilterChange -> _selectedFilter.value = event.filter
            is NotesListEvent.OnNavigationHandled -> _navigationEvent.value = emptyList()
            is NotesListEvent.OnShowLogoutDialog -> _showLogoutDialog.value = true
            is NotesListEvent.OnDismissLogoutDialog -> _showLogoutDialog.value = false
            NotesListEvent.OnDeleteSelectedNotes -> deleteSelectedNotes()
            NotesListEvent.OnShowDeleteSelectionDialog -> _showDeleteSelectionDialog.value = true
            NotesListEvent.OnDismissDeleteSelectionDialog -> _showDeleteSelectionDialog.value = false
            NotesListEvent.OnClearSelection -> _selectedNoteIds.value = emptySet()
        }
    }

    private fun handleNoteClick(id: String) {
        if (_selectedNoteIds.value.isNotEmpty()) {
            toggleSelection(id)
        } else {
            _navigationEvent.value = listOf(id)
        }
    }

    private fun handleNoteLongClick(id: String) {
        toggleSelection(id)
    }

    private fun toggleSelection(id: String) {
        val current = _selectedNoteIds.value
        _selectedNoteIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    private fun mapToUiItem(note: Note, isSelected: Boolean): NoteUiItem {
        val style = when (note.priority) {
            2 -> NoteStyle.Error
            1 -> NoteStyle.Primary
            else -> NoteStyle.Secondary
        }

        val priorityList = if (note.priority > 0) {
            val label = when (note.priority) {
                1 -> "Media"
                2 -> "Alta"
                else -> ""
            }
            listOf(PriorityUiItem(label, style))
        } else {
            emptyList()
        }

        val tagList = if (note.tag.isNotBlank()) {
            listOf(TagUiItem(note.tag, style))
        } else {
            emptyList()
        }

        val isShared = note.userId != null && _currentUserId != null && note.userId != _currentUserId

        return NoteUiItem(
            id = note.id,
            title = note.title,
            description = note.description,
            style = style,
            reminder = note.reminder,
            priorityChips = priorityList,
            tags = tagList,
            isSelected = isSelected,
            isShared = isShared
        )
    }

    private fun mapToFilterUiItem(filter: NoteFilter, selected: NoteFilter): FilterUiItem {
        return FilterUiItem(
            filter = filter,
            label = filter.label,
            isSelected = filter == selected
        )
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true

            val userId = getUserIdUseCase().first()
            _currentUserId = userId

            if (userId != null) {
                launch { checkPendingRequests(userId) }
                launch { fetchSharedNotes(userId) }
                startPolling(userId)

                when (val apiResult = fetchUserNotesUseCase(userId)) {
                    is Resource.Success -> {
                        apiResult.data?.let { apiNotes ->
                        }
                    }
                    is Resource.Error -> {}
                    else -> {}
                }
            }

            getNotesUseCase().onEach { notes ->
                _localNotes.value = notes
                _isLoading.value = false
            }.launchIn(viewModelScope)
        }
    }

    private fun startPolling(userId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    delay(5000)
                    fetchSharedNotes(userId)
                    fetchUserNotesUseCase(userId)
                } catch (e: Exception) {
                }
            }
        }
    }

    private suspend fun fetchSharedNotes(userId: Int) {
        when (val result = getNotesSharedWithMeUseCase(userId)) {
            is Resource.Success -> {
                _sharedNotes.value = result.data ?: emptyList()
            }
            is Resource.Error -> {
            }
            else -> {}
        }
    }

    private suspend fun checkPendingRequests(userId: Int) {
        when (val result = getPendingRequestUseCase(userId)) {
            is Resource.Success -> {
                _pendingRequestCount.value = result.data?.size ?: 0
            }
            else -> {}
        }
    }

    private fun filterAndSortNotes(notes: List<Note>, query: String, filter: NoteFilter): List<Note> {
        val filtered = if (query.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }

        return when (filter) {
            NoteFilter.AZ -> filtered.sortedBy { it.title.lowercase() }
            NoteFilter.ZA -> filtered.sortedByDescending { it.title.lowercase() }
            NoteFilter.DATE -> filtered
            NoteFilter.HIGH_PRIORITY -> filtered.filter { it.priority == 2 }
            NoteFilter.MEDIUM_PRIORITY -> filtered.filter { it.priority == 1 }
            NoteFilter.LOW_PRIORITY -> filtered.filter { it.priority == 0 }
        }
    }

    private fun deleteNote(id: String) {
        viewModelScope.launch {
            when (val result = deleteNoteUseCase(id)) {
                is Resource.Success -> _errorMessage.value = null
                is Resource.Error -> _errorMessage.value = result.message
                is Resource.Loading -> _isLoading.value = true
            }
        }
    }

    private fun deleteSelectedNotes() {
        viewModelScope.launch {
            _showDeleteSelectionDialog.value = false
            val idsToDelete = _selectedNoteIds.value.toList()
            _selectedNoteIds.value = emptySet()

            _isLoading.value = true
            idsToDelete.forEach { id ->
                deleteNoteUseCase(id)
            }
            _isLoading.value = false
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
            _isRefreshing.value = true
            val userId = getUserIdUseCase().first()
            if (userId != null) {
                checkPendingRequests(userId)
                fetchUserNotesUseCase(userId)
                syncSharedNotesUseCase(userId)
                fetchSharedNotes(userId)

                val result = postPendingNotesUseCase(userId)
                if (result is Resource.Error) {
                    _errorMessage.value = result.message
                }
            }
            _isRefreshing.value = false
        }
    }
}