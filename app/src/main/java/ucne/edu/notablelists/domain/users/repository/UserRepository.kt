package ucne.edu.notablelists.domain.users.repository

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.users.model.User

interface UserRepository {
    suspend fun getUser(id: String): User?
    suspend fun createUserLocal(user: User): Resource<User>
    suspend fun postUser(user: User): User
    suspend fun putUser(user: User): User
    suspend fun deleteUser(id: String): Resource<Unit>
    suspend fun upsertUser(user: User):Resource<Unit>
    suspend fun postPendingUsers(): Resource<Unit>
}