package ucne.edu.notablelists.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ucne.edu.notablelists.data.local.Notes.NoteDao
import ucne.edu.notablelists.data.mappers.toDomain
import ucne.edu.notablelists.data.mappers.toEntity
import ucne.edu.notablelists.data.remote.DataSource.NoteRemoteDataSource
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.NoteRequestDto
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val localDataSource: NoteDao,
    private val remoteDataSource: NoteRemoteDataSource
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> {
        return localDataSource.observeNotes().map { notes ->
            notes.map { it.toDomain() }
        }
    }
    override fun observeUserNotes(userId: Int): Flow<List<Note>> =
        localDataSource.getUserNotes(userId).map { list -> list.map { it.toDomain() } }

    override fun observeLocalNotes(): Flow<List<Note>> =
        localDataSource.getLocalNotes().map { list -> list.map { it.toDomain() } }

    override suspend fun getNote(id: String): Note? {
        return localDataSource.getNote(id)?.toDomain()
    }

    override suspend fun createNoteLocal(note: Note, userId: Int?): Resource<Note> {
        val pending = note.copy(isPendingCreate = true, userId = userId)
        localDataSource.upsert(pending.toEntity())
        return Resource.Success(pending)
    }

    override suspend fun upsert(note: Note,userId: Int?): Resource<Unit> {
        return try {
            val isNew = note.remoteId == null
            Log.d("UPSERT", "Note: ${note.title}, isNew: $isNew, remoteId: ${note.remoteId}, userId: $userId")
            val entity = note.toEntity().copy(isPendingCreate = isNew,userId = userId)
            localDataSource.upsert(entity)
            Log.d("UPSERT", "Saved to DB with isPendingCreate: $isNew")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error desconocido al guardar localmente")
        }
    }

    override suspend fun delete(id: String): Resource<Unit> {
        return try {
            val noteEntity = localDataSource.getNote(id) ?: return Resource.Error("Nota no encontrada")

            localDataSource.delete(id)

            if (noteEntity.remoteId != null) {
                try {
                    if (noteEntity.userId != null) {
                        remoteDataSource.deleteUserNote(noteEntity.userId, noteEntity.remoteId)
                    } else {
                        remoteDataSource.deleteNote(noteEntity.remoteId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar")
        }
    }

    override suspend fun deleteRemote(id: Int, userId: Int?): Resource<Unit> {
        return try {
            val result = if (userId != null) {
                remoteDataSource.deleteUserNote(userId, id)
            } else {
                remoteDataSource.deleteNote(id)
            }

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.message ?: "No se pudo eliminar en el servidor")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de red al eliminar")
        }
    }

    override suspend fun postPendingNotes(userId: Int): Resource<Unit> {
        return try {
            val pending = localDataSource.getPendingCreateNotes()
            Log.d("SYNC", "Found ${pending.size} pending notes for user $userId")

            for (note in pending) {
                val request = NoteRequestDto(
                    title = note.title,
                    description = note.description,
                    tag = note.tag,
                    isFinished = note.isFinished,
                    reminder = note.reminder?: "",
                    checklist = note.checklist?: "",
                    priority = note.priority,
                )
                Log.d("SYNC", "Sending note: ${note.title} to user $userId")
                val result = remoteDataSource.createUserNote(userId, request)
                if (result is Resource.Success && result.data != null) {
                    Log.d("SYNC", "Success! Response - noteId: ${result.data.noteId}, userId: ${result.data.userId}")
                    val syncedEntity = note.copy(
                        remoteId = result.data.noteId,
                        userId = userId,
                        isPendingCreate = false
                    )
                    localDataSource.upsert(syncedEntity)
                    Log.d("SYNC", "Note synced successfully")

                }else if (result is Resource.Error) {
                    Log.e("SYNC", "Error syncing note: ${result.message}")
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("SYNC", "Sync failed: ${e.message}")
            Resource.Error("Error en sincronización: ${e.message}")
        }
    }

    override suspend fun postNote(note: Note, userId: Int?): Resource<Note> {
        Log.d("REPO_POSTNOTE", "START: ${note.title}, userId=$userId")
        return try {
            val request = NoteRequestDto(
                title = note.title,
                description = note.description,
                tag = note.tag,
                isFinished = note.isFinished,
                reminder = note.reminder?: "",
                checklist = note.checklist?: "",
                priority = note.priority,

                userId = userId
            )

            Log.d("REPO_POSTNOTE", "Calling API...")
            val result = if (userId == null) {
                remoteDataSource.createNote(request)
            } else {
                remoteDataSource.createUserNote(userId, request)
            }

            Log.d("REPO_POSTNOTE", "API result: ${result is Resource.Success}")

            when (result) {
                is Resource.Success -> {
                    if (result.data != null) {
                        val remoteNote = result.data
                        val updatedNote = note.copy(
                            remoteId = remoteNote.noteId,
                            userId = userId,
                            isPendingCreate = false
                        )
                        Log.d("REPO_POSTNOTE", "Saving to local DB: ${updatedNote.title}")
                        localDataSource.upsert(updatedNote.toEntity())
                        Log.d("REPO_POSTNOTE", "Saved to local DB successfully")
                        Resource.Success(updatedNote)
                    } else {
                        Log.e("REPO_POSTNOTE", "Empty API response")
                        Resource.Error("Empty response")
                    }
                }
                is Resource.Error -> {
                    Log.e("REPO_POSTNOTE", "API error: ${result.message}")
                    Resource.Error(result.message ?: "API error")
                }
                else -> Resource.Error("Unexpected result")
            }
        } catch (e: Exception) {
            Log.e("REPO_POSTNOTE", "Exception: ${e.message}")
            Resource.Error("Network error: ${e.message}")
        }
    }

    override suspend fun putNote(note: Note, userId: Int?): Resource<Note> {
        return try {
            val remoteId = note.remoteId ?: return Resource.Error("No remoteId")
            val request = NoteRequestDto(
                title = note.title,
                description = note.description,
                tag = note.tag,
                isFinished = note.isFinished,
                reminder = note.reminder?: "",
                checklist = note.checklist?: "",
                priority = note.priority,
                userId = userId
            )

            val result = if (userId != null) {
                remoteDataSource.updateUserNote(userId, remoteId, request)
            } else {
                remoteDataSource.updateNote(remoteId, request)
            }

            if (result is Resource.Success) {
                Resource.Success(note)
            } else {
                Resource.Error(result.message ?: "Failed to update note on server")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to update note: ${e.message}")
        }
    }
    override suspend fun syncOnLogin(userId: Int): Resource<Unit> {
        return try {
            Log.d("DEBUG_NOTES", "=== BEFORE SYNC ===")
            debugAllNotes()
            localDataSource.linkNotesToUser(userId)
            Log.d("DEBUG_NOTES", "=== AFTER LINKING ===")
            debugAllNotes()

            postPendingNotes(userId)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }
    suspend fun debugAllNotes() {
        val allNotes = localDataSource.getAllNotes()
        allNotes.forEach { note ->
            Log.d("DEBUG_NOTES", "Note: id=${note.id}, title=${note.title}, remoteId=${note.remoteId}, userId=${note.userId}, isPendingCreate=${note.isPendingCreate}")
        }
    }

    override suspend fun fetchUserNotesFromApi(userId: Int): Resource<List<Note>> {
        return try {
            Log.d("API_FETCH", "Fetching notes from API for user: $userId")
            val result = remoteDataSource.getUserNotes(userId)

            when (result) {
                is Resource.Success -> {
                    Log.d("API_FETCH", "Success! Found ${result.data?.size ?: 0} notes from API")
                    val notes = result.data?.map { it.toDomain() } ?: emptyList()

                    // Save to local database
                    notes.forEach { note ->
                        localDataSource.upsert(note.toEntity())
                    }
                    Log.d("API_FETCH", "Saved ${notes.size} notes to local database")
                    Resource.Success(notes)
                }
                is Resource.Error -> {
                    Log.e("API_FETCH", "API error: ${result.message}")
                    Resource.Error(result.message ?: "Failed to fetch notes from API")
                }
                else -> {
                    Log.e("API_FETCH", "Unexpected result")
                    Resource.Error("Unexpected result from API")
                }
            }
        } catch (e: Exception) {
            Log.e("API_FETCH", "Exception: ${e.message}")
            Resource.Error("Network error: ${e.message}")
        }
    }
}
