package ucne.edu.notablelists.domain.sharednote.usecase

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.UpdateSharedStatusResponseDto
import ucne.edu.notablelists.domain.sharednote.repository.SharedNoteRepository
import javax.inject.Inject

class UpdateSharedNoteStatusUseCase @Inject constructor(
    private val repository: SharedNoteRepository
) {
    suspend operator fun invoke(userId: Int, sharedNoteId: Int): Resource<UpdateSharedStatusResponseDto> {
        return repository.updateSharedNoteStatus(userId, sharedNoteId)
    }
}