package ucne.edu.notablelists.data.remote.DataSource

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.UserApiService
import ucne.edu.notablelists.data.remote.dto.FriendDto
import ucne.edu.notablelists.data.remote.dto.PendingRequestDto
import ucne.edu.notablelists.data.remote.dto.UserRequestDto
import ucne.edu.notablelists.data.remote.dto.UserResponseDto
import javax.inject.Inject

class UserRemoteDataSource @Inject constructor(
    private val api: UserApiService
) {
    suspend fun getAllUsers(): Resource<List<UserResponseDto>> {
        return try {
            val response = api.getUsers()
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Empty response from server")
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun createUser(request: UserRequestDto): Resource<UserResponseDto> {
        return try {
            val response = api.createUser(request)
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Respuesta vacía del servidor")
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun updateUser(id: Int, request: UserRequestDto): Resource<Unit> {
        return try {
            val response = api.updateUser(id, request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun deleteUser(id: Int): Resource<Unit> {
        return try {
            val response = api.deleteUser(id)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }
    suspend fun sendFriendRequest(userId: Int, friendId: Int): Resource<Unit> {
        return try {
            val response = api.sendFriendRequest(userId, friendId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun getPendingRequests(userId: Int): Resource<List<PendingRequestDto>> {
        return try {
            val response = api.getPendingRequests(userId)
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Respuesta vacía del servidor")
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun acceptFriendRequest(userId: Int, friendshipId: Int): Resource<Unit> {
        return try {
            val response = api.acceptFriendRequest(userId, friendshipId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun getFriends(userId: Int): Resource<List<FriendDto>> {
        return try {
            val response = api.getFriends(userId)
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Respuesta vacía del servidor")
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun removeFriend(userId: Int, friendId: Int): Resource<Unit> {
        return try {
            val response = api.removeFriend(userId, friendId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }
    suspend fun declineFriendRequest(userId: Int, friendshipId: Int): Resource<Unit> {
        return try {
            val response = api.declineFriendRequest(userId, friendshipId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }
}