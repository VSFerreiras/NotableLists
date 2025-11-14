package ucne.edu.notablelists.domain.users.repository

import ucne.edu.notablelists.domain.users.model.User

interface UserRepository {
    suspend fun getUser(userId: Int): User?
    suspend fun postUser(user: User): User
    suspend fun putUser(user: User): User
    suspend fun deleteUser(userId: Int)
    suspend fun upsertUser(user: User):Int
}