package ucne.edu.notablelists.domain.auth

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.users.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): Resource<User>
    suspend fun register(username: String, password: String): Resource<User>
    suspend fun logout(): Resource<Unit>
}