package ucne.edu.notablelists.domain.notes.repository

import kotlinx.coroutines.flow.Flow
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.model.Note

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(id: String): Note?
    suspend fun createNoteLocal(note: Note, userId: Int? = null): Resource<Note>
    suspend fun upsert(note: Note, userId: Int? = null): Resource<Unit>
    suspend fun delete(id: String): Resource<Unit>
    suspend fun deleteRemote(id: Int, userId: Int? = null): Resource<Unit>
    suspend fun postPendingNotes(userId: Int): Resource<Unit>
    suspend fun postNote(note: Note, userId: Int? = null): Resource<Note>
    suspend fun putNote(note: Note, userId: Int? = null): Resource<Note>
    suspend fun syncOnLogin(userId: Int): Resource<Unit>
    fun observeUserNotes(userId: Int): Flow<List<Note>>
    fun observeLocalNotes(): Flow<List<Note>>
    suspend fun fetchUserNotesFromApi(userId: Int): Resource<List<Note>>
    suspend fun clearLocalNotes()
    suspend fun getRemoteNote(noteId: Int): Resource<Note>
}