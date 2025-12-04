package ucne.edu.notablelists.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ucne.edu.notablelists.data.mappers.toDomain
import ucne.edu.notablelists.data.mappers.toUserDomain
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.friends.usecase.AcceptFriendRequestUseCase
import ucne.edu.notablelists.domain.friends.usecase.DeclineFriendRequestUseCase
import ucne.edu.notablelists.domain.friends.usecase.GetFriendsUseCase
import ucne.edu.notablelists.domain.friends.usecase.GetPendingRequestUseCase
import ucne.edu.notablelists.domain.friends.usecase.RemoveFriendUseCase
import ucne.edu.notablelists.domain.friends.usecase.SearchUserUseCase
import ucne.edu.notablelists.domain.friends.usecase.SendFriendRequestUseCase
import ucne.edu.notablelists.domain.session.usecase.GetUserIdUseCase
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    private val getPendingRequestUseCase: GetPendingRequestUseCase,
    private val searchUserUseCase: SearchUserUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase,
    private val declineFriendRequestUseCase: DeclineFriendRequestUseCase,
    private val getUserIdUseCase: GetUserIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FriendsState())
    val state: StateFlow<FriendsState> = _state.asStateFlow()

    private var currentUserId: Int? = null

    init {
        viewModelScope.launch {
            getUserIdUseCase().collectLatest { id ->
                currentUserId = id
                if (id != null) {
                    loadData()
                }
            }
        }
    }

    fun onEvent(event: FriendsEvent) {
        when (event) {
            is FriendsEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            FriendsEvent.OnSearch -> {
                searchUsers()
            }
            is FriendsEvent.OnSendFriendRequest -> {
                sendFriendRequest(event.userId)
            }
            is FriendsEvent.OnAcceptFriendRequest -> {
                acceptFriendRequest(event.friendshipId)
            }
            is FriendsEvent.OnTabSelected -> {
                _state.update { it.copy(selectedTabIndex = event.index) }
            }
            FriendsEvent.OnRefresh -> {
                loadData()
            }
            FriendsEvent.OnDismissError -> {
                _state.update { it.copy(errorMessage = null, successMessage = null) }
            }
            is FriendsEvent.OnShowDeleteFriendDialog -> {
                _state.update { it.copy(friendToDelete = event.friend) }
            }
            FriendsEvent.OnDismissDeleteFriendDialog -> {
                _state.update { it.copy(friendToDelete = null) }
            }
            FriendsEvent.OnDeleteFriend -> {
                removeFriend()
            }
            is FriendsEvent.OnDeclineFriendRequest -> {
                declineFriendRequest(event.friendshipId)
            }
        }
    }

    private fun loadData() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val friendsResult = getFriendsUseCase(userId)
            val pendingResult = getPendingRequestUseCase(userId)

            _state.update { currentState ->
                val friends = if (friendsResult is Resource.Success) {
                    friendsResult.data?.map { it.toDomain() } ?: emptyList()
                } else currentState.friends

                val pending = if (pendingResult is Resource.Success) {
                    pendingResult.data?.map { it.toDomain() } ?: emptyList()
                } else currentState.pendingRequests

                val error = friendsResult.message ?: pendingResult.message

                currentState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    friends = friends,
                    pendingRequests = pending,
                    errorMessage = if (friendsResult is Resource.Error || pendingResult is Resource.Error) error else null
                )
            }
        }
    }

    private fun searchUsers() {
        val query = _state.value.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = searchUserUseCase(query)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            searchResults = result.data?.map { dto -> dto.toUserDomain() } ?: emptyList()
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun sendFriendRequest(targetUserId: Int) {
        val userId = currentUserId ?: return
        val isAlreadyFriend = _state.value.friends.any { friend ->
            friend.id == targetUserId
        }

        if (isAlreadyFriend) {
            _state.update { it.copy(errorMessage = "Ya son amigos") }
            return
        }

        viewModelScope.launch {
            when (val result = sendFriendRequestUseCase(userId, targetUserId)) {
                is Resource.Success -> {
                    _state.update { it.copy(successMessage = "Solicitud enviada") }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(errorMessage = "Ya enviaste una solicitud a este usuario")
                    }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun acceptFriendRequest(friendshipId: Int) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            when (val result = acceptFriendRequestUseCase(userId, friendshipId)) {
                is Resource.Success -> {
                    _state.update { it.copy(successMessage = "Solicitud aceptada") }
                    loadData()
                }
                is Resource.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun removeFriend() {
        val userId = currentUserId ?: return
        val friendToDelete = _state.value.friendToDelete ?: return

        viewModelScope.launch {
            _state.update { it.copy(friendToDelete = null) }

            when (val result = removeFriendUseCase(userId, friendToDelete.id)) {
                is Resource.Success -> {
                    _state.update { it.copy(successMessage = "Amigo eliminado") }
                    loadData()
                }
                is Resource.Error -> {
                    _state.update { it.copy(errorMessage = result.message) }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }
    private fun declineFriendRequest(friendshipId: Int) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = declineFriendRequestUseCase(userId, friendshipId)) {
                is Resource.Success -> {
                    val pendingResult = getPendingRequestUseCase(userId)
                    _state.update { currentState ->
                        val pending = if (pendingResult is Resource.Success) {
                            pendingResult.data?.map { it.toDomain() } ?: emptyList()
                        } else currentState.pendingRequests

                        currentState.copy(
                            isLoading = false,
                            successMessage = "Solicitud rechazada",
                            pendingRequests = pending
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}