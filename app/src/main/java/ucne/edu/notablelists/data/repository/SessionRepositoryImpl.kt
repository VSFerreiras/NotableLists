package ucne.edu.notablelists.data.repository

import ucne.edu.notablelists.data.local.SessionManager
import ucne.edu.notablelists.domain.session.SessionRepository
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager
) : SessionRepository {

    override fun getUserSession() = sessionManager.getUser()
    override fun getUserId() = sessionManager.getUserId()

    override suspend fun saveUserSession(userId: Int, username: String) {
        sessionManager.saveUser(userId,username)
    }

    override suspend fun clearUserSession() {
        sessionManager.clearUser()
    }
}