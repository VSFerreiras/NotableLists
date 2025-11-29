package ucne.edu.notablelists.domain.session.usecase

import kotlinx.coroutines.flow.Flow
import ucne.edu.notablelists.domain.session.SessionRepository
import javax.inject.Inject

class GetSessionUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    operator fun invoke(): Flow<String?> = repository.getUserSession()
}