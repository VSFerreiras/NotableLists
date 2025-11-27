package ucne.edu.notablelists.data.remote.dto

data class AuthResponseDto(
    val success: Boolean,
    val message: String? = null,
    val user: UserResponseDto? = null
)