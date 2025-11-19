package ucne.edu.notablelists.data.remote.DataSource

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.UserApiService
import ucne.edu.notablelists.data.remote.dto.UserRequestDto
import ucne.edu.notablelists.data.remote.dto.UserResponseDto
import javax.inject.Inject

class UserRemoteDataSource @Inject constructor(
    private val api: UserApiService
) {
    suspend fun getUsers(): Resource<List<UserResponseDto>> {
        return try {
            val response = api.getUsers()
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

    suspend fun getUserById(id: Int): Resource<UserResponseDto> {
        return try {
            val response = api.getUserById(id)
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
}