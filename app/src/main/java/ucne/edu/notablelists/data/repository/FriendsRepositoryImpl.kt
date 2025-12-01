package ucne.edu.notablelists.data.repository

import ucne.edu.notablelists.domain.friends.repository.FriendsRepository
import ucne.edu.notablelists.data.remote.DataSource.UserRemoteDataSource
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.FriendDto
import ucne.edu.notablelists.data.remote.dto.PendingRequestDto
import ucne.edu.notablelists.data.remote.dto.UserResponseDto
import javax.inject.Inject

class FriendsRepositoryImpl @Inject constructor(
    private val remoteDataSource: UserRemoteDataSource
) : FriendsRepository {

    override suspend fun sendFriendRequest(userId: Int, friendId: Int): Resource<Unit> {
        return remoteDataSource.sendFriendRequest(userId, friendId)
    }

    override suspend fun getPendingRequests(userId: Int): Resource<List<PendingRequestDto>> {
        return remoteDataSource.getPendingRequests(userId)
    }

    override suspend fun acceptFriendRequest(userId: Int, friendshipId: Int): Resource<Unit> {
        return remoteDataSource.acceptFriendRequest(userId, friendshipId)
    }

    override suspend fun getFriends(userId: Int): Resource<List<FriendDto>> {
        return remoteDataSource.getFriends(userId)
    }

    override suspend fun getAllUsers(): Resource<List<UserResponseDto>> {
        return remoteDataSource.getAllUsers()
    }

    override suspend fun searchUsers(username: String): Resource<List<UserResponseDto>> {
        val allUsers = remoteDataSource.getAllUsers()
        return when (allUsers) {
            is Resource.Success -> {
                val users = allUsers.data ?: emptyList()
                val filteredUsers = users.filter { user ->
                    user.username.contains(username, ignoreCase = true)
                }
                Resource.Success(filteredUsers)
            }
            is Resource.Error -> allUsers
            is Resource.Loading -> allUsers
        }
    }

    override suspend fun removeFriend(userId: Int, friendId: Int): Resource<Unit> {
        return remoteDataSource.removeFriend(userId, friendId)
    }
}