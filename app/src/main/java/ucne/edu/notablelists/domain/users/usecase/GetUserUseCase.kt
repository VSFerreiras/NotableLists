package ucne.edu.notablelists.domain.users.usecase

import ucne.edu.notablelists.domain.users.repository.UserRepository
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
){
    suspend operator fun invoke(id: String) = repository.getUser(id)
}