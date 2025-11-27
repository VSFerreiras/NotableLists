package ucne.edu.notablelists.domain.notes.usecase

import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import javax.inject.Inject

class PostNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Note = repository.postNote(note)
}