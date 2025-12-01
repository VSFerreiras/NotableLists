package ucne.edu.notablelists.domain.sharednote.usecase

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.SharedNoteWithDetailsDto
import ucne.edu.notablelists.domain.sharednote.repository.SharedNoteRepository
import javax.inject.Inject

class GetNotesSharedWithMeUseCase @Inject constructor(
    private val repository: SharedNoteRepository
) {
    suspend operator fun invoke(userId: Int): Resource<List<SharedNoteWithDetailsDto>> {
        return repository.getNotesSharedWithMe(userId)
    }
}