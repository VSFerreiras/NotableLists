package ucne.edu.notablelists.domain.notes.repository

import kotlinx.coroutines.flow.Flow
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.model.Note

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(id: String): Note?
    suspend fun createNoteLocal(note: Note): Resource<Note>
    suspend fun upsert(note: Note): Resource<Unit>
    suspend fun delete(id: String): Resource<Unit>
    suspend fun deleteRemote(id: Int): Resource<Unit>
    suspend fun postPendingNotes(): Resource<Unit>
    suspend fun postNote(note: Note): Resource<Note>
    suspend fun putNote(note: Note): Resource<Note>
}