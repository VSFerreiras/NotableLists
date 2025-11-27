package ucne.edu.notablelists.domain.notes.usecase

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: String): Resource<Unit> = repository.delete(id)
}