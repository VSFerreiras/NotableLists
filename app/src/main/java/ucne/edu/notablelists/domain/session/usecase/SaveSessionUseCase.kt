package ucne.edu.notablelists.domain.session.usecase

import ucne.edu.notablelists.domain.session.SessionRepository
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(userId: Int, username: String) {
        repository.saveUserSession(userId, username)
    }
}