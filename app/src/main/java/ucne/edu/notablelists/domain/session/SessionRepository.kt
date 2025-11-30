package ucne.edu.notablelists.domain.session

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getUserSession(): Flow<String?>
    fun getUserId(): Flow<Int?>
    suspend fun saveUserSession(userId: Int,username: String)
    suspend fun clearUserSession()
}