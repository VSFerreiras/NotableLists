package ucne.edu.notablelists.domain.users.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val remoteId: Int? = null,
    val username: String,
    val password: String
)