package ucne.edu.notablelists.presentation.Notes.list

import android.util.Log
import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.usecase.*
import javax.inject.Inject

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val upsertNoteUseCase: UpsertNoteUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val fetchUserNotesUseCase: FetchUserNotesUseCase,
    private val postPendingNotesUseCase: PostPendingNotesUseCase
) : ViewModel() {

    private val _rawNotes = MutableStateFlow<List<Note>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(NoteFilter.DATE)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _navigationEvent = MutableStateFlow<List<String?>>(emptyList())
    private val _showLogoutDialog = MutableStateFlow(false)

    private val _filterState = combine(_searchQuery, _selectedFilter) { query, filter ->
        Pair(query, filter)
    }

    private val _uiStatusState = combine(_isLoading, _errorMessage, _isRefreshing) { loading, error, refreshing ->
        Triple(loading, error, refreshing)
    }

    val state: StateFlow<NotesListState> = combine(
        _rawNotes,
        _filterState,
        _uiStatusState,
        _navigationEvent,
        _showLogoutDialog
    ) { notes, (query, selectedFilter), (isLoading, errorMessage, _), navEvent, showDialog ->

        val filteredNotes = filterAndSortNotes(notes, query, selectedFilter)

        val uiNotes = if (isLoading && notes.isEmpty()) {
            emptyList()
        } else {
            filteredNotes.map { note -> mapToUiItem(note) }
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
            showLogoutDialog = showDialog
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
            is NotesListEvent.OnAddNoteClick -> _navigationEvent.value = listOf(null)
            is NotesListEvent.OnNoteClick -> _navigationEvent.value = listOf(event.id)
            is NotesListEvent.OnSearchQueryChange -> _searchQuery.value = event.query
            is NotesListEvent.OnFilterChange -> _selectedFilter.value = event.filter
            is NotesListEvent.OnNavigationHandled -> _navigationEvent.value = emptyList()
            is NotesListEvent.OnShowLogoutDialog -> _showLogoutDialog.value = true
            is NotesListEvent.OnDismissLogoutDialog -> _showLogoutDialog.value = false
        }
    }

    private fun mapToUiItem(note: Note): NoteUiItem {
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

        return NoteUiItem(
            id = note.id,
            title = note.title,
            description = note.description,
            style = style,
            priorityChips = priorityList,
            tags = tagList
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

            if (userId != null) {
                when (val apiResult = fetchUserNotesUseCase(userId)) {
                    is Resource.Success -> {
                        Log.d("LOAD_NOTES", "API fetch successful, got ${apiResult.data?.size ?: 0} notes")
                    }
                    is Resource.Error -> {
                    }
                    else -> {}
                }
            } else {
            }

            getNotesUseCase().onEach { notes ->
                _rawNotes.value = notes
                _isLoading.value = false
            }.launchIn(viewModelScope)
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
                fetchUserNotesUseCase(userId)
                val result = postPendingNotesUseCase(userId)
                if (result is Resource.Error) {
                    _errorMessage.value = result.message
                }
            }
            _isRefreshing.value = false
        }
    }
}