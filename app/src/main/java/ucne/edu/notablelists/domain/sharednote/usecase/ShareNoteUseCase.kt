package ucne.edu.notablelists.domain.sharednote.usecase

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.ShareResponseDto
import ucne.edu.notablelists.domain.sharednote.repository.SharedNoteRepository
import javax.inject.Inject

class ShareNoteUseCase @Inject constructor(
    private val repository: SharedNoteRepository
) {
    suspend operator fun invoke(userId: Int, noteId: Int, friendId: Int): Resource<ShareResponseDto> {
        return repository.shareNote(userId, noteId, friendId)
    }
}