package ucne.edu.notablelists.domain.session.usecase

import ucne.edu.notablelists.domain.session.SessionRepository
import javax.inject.Inject

class ClearSessionUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke() = repository.clearUserSession()
}