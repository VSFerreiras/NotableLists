package ucne.edu.notablelists.domain.notes.usecase

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import javax.inject.Inject

class UpsertNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Resource<Unit> = repository.upsert(note)
}