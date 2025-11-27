package ucne.edu.notablelists.data.remote.dto

data class PendingRequestDto(
    val friendshipId: Int,
    val requesterId: Int,
    val requesterUsername: String
)