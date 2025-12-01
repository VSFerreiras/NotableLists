package ucne.edu.notablelists.domain.friends.repository

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.FriendDto
import ucne.edu.notablelists.data.remote.dto.PendingRequestDto
import ucne.edu.notablelists.data.remote.dto.UserResponseDto

interface FriendsRepository {
    suspend fun sendFriendRequest(userId: Int, friendId: Int): Resource<Unit>
    suspend fun getPendingRequests(userId: Int): Resource<List<PendingRequestDto>>
    suspend fun acceptFriendRequest(userId: Int, friendshipId: Int): Resource<Unit>
    suspend fun getFriends(userId: Int): Resource<List<FriendDto>>
    suspend fun getAllUsers(): Resource<List<UserResponseDto>>
    suspend fun searchUsers(username: String): Resource<List<UserResponseDto>>
    suspend fun removeFriend(userId: Int, friendId: Int): Resource<Unit>
}