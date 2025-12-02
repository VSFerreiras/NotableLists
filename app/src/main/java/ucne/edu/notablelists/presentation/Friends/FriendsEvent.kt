package ucne.edu.notablelists.presentation.friends

import ucne.edu.notablelists.domain.friends.model.Friend

sealed interface FriendsEvent {
    data class OnSearchQueryChange(val query: String) : FriendsEvent
    data object OnSearch : FriendsEvent
    data class OnSendFriendRequest(val userId: Int) : FriendsEvent
    data class OnAcceptFriendRequest(val friendshipId: Int) : FriendsEvent
    data class OnTabSelected(val index: Int) : FriendsEvent
    data object OnRefresh : FriendsEvent
    data object OnDismissError : FriendsEvent
    data class OnShowDeleteFriendDialog(val friend: Friend) : FriendsEvent
    data object OnDismissDeleteFriendDialog : FriendsEvent
    data object OnDeleteFriend : FriendsEvent
    data class OnDeclineFriendRequest(val friendshipId: Int) : FriendsEvent
}