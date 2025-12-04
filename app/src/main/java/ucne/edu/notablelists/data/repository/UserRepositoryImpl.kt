package ucne.edu.notablelists.data.repository

import ucne.edu.notablelists.data.local.Users.UserDao
import ucne.edu.notablelists.data.mappers.toDomain
import ucne.edu.notablelists.data.mappers.toEntity
import ucne.edu.notablelists.data.remote.DataSource.UserRemoteDataSource
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.UserRequestDto
import ucne.edu.notablelists.domain.users.model.User
import ucne.edu.notablelists.domain.users.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserDao,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {

    override suspend fun getUser(id: String): User? {
        return localDataSource.getUser(id)?.toDomain()
    }

    override suspend fun upsertUser(user: User): Resource<Unit> {
        val remoteId = user.remoteId ?: return Resource.Error("No remoteId")
        val request = UserRequestDto(user.username, user.password)
        return when (val result = remoteDataSource.updateUser(remoteId, request)) {
            is Resource.Success -> {
                localDataSource.upsert(user.toEntity())
                Resource.Success(Unit)
            }
            is Resource.Error -> result
            else -> Resource.Loading()
        }
    }

    override suspend fun deleteUser(id: String): Resource<Unit> {
        val user = localDataSource.getUser(id) ?: return Resource.Error("No encontrado")
        val remoteId = user.remoteId ?: return Resource.Error("No remoteId")
        return when (val result = remoteDataSource.deleteUser(remoteId)) {
            is Resource.Success -> {
                localDataSource.delete(id)
                Resource.Success(Unit)
            }
            is Resource.Error -> result
            else -> Resource.Loading()
        }
    }

    override suspend fun postPendingUsers(): Resource<Unit> {
        val pending = localDataSource.getPendingCreateUsers()
        for (user in pending) {
            val request = UserRequestDto(user.userName, user.password)
            when (val result = remoteDataSource.createUser(request)) {
                is Resource.Success -> {
                    val synced = user.copy(remoteId = result.data?.userId, isPendingCreate = false)
                    localDataSource.upsert(synced)
                }
                is Resource.Error -> return Resource.Error("Falló sincronización")
                is Resource.Loading -> continue
            }
        }
        return Resource.Success(Unit)
    }

    override suspend fun postUser(user: User): User {
        val request = UserRequestDto(user.username, user.password)
        val result = remoteDataSource.createUser(request)

        return if (result is Resource.Success) {
            val remoteUser = result.data!!
            user.copy(
                remoteId = remoteUser.userId,
            )
        } else {
            throw Exception("Failed to create user on server")
        }
    }

    override suspend fun putUser(user: User): User {
        val remoteId = user.remoteId ?: throw Exception("No remoteId")
        val request = UserRequestDto(user.username, user.password)
        val result = remoteDataSource.updateUser(remoteId, request)

        return if (result is Resource.Success) {
            user.copy()
        } else {
            throw Exception("Failed to update user on server")
        }
    }
}