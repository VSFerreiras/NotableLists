package ucne.edu.notablelists.domain.users.usecase

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.users.model.User
import ucne.edu.notablelists.domain.users.repository.UserRepository
import javax.inject.Inject

class PostUserUseCase @Inject constructor(
    private val repository: UserRepository,
    private val validateUseCase: ValidateUserUseCase
) {
    suspend operator fun invoke(username: String, password: String): Resource<User> {
        val validation = validateUseCase(username, password)
        return when (validation) {
            is Resource.Error -> Resource.Error(validation.message)
            is Resource.Success -> {
                val user = User(username = username, password = password)
                try {
                    val result = repository.postUser(user)
                    Resource.Success(result)
                } catch (e: Exception) {
                    Resource.Error("Error al crear usuario: ${e.message}")
                }
            }
            is Resource.Loading -> {
                Resource.Error("Validación en progreso, intente nuevamente")
            }
        }
    }
}