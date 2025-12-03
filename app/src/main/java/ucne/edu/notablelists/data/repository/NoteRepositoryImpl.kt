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
            val entity = note.toEntity().copy(isPendingCreate = isNew,userId = userId)
            localDataSource.upsert(entity)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error desconocido al guardar localmente")
        }
    }

    override suspend fun deleteLocalOnly(id: String) {
        try {
            localDataSource.delete(id)
        } catch (e: Exception) {
            e.printStackTrace()
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
                val result = remoteDataSource.createUserNote(userId, request)
                if (result is Resource.Success && result.data != null) {
                    val syncedEntity = note.copy(
                        remoteId = result.data.noteId,
                        userId = userId,
                        isPendingCreate = false
                    )
                    localDataSource.upsert(syncedEntity)

                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Error en sincronización: ${e.message}")
        }
    }

    override suspend fun postNote(note: Note, userId: Int?): Resource<Note> {
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

            val result = if (userId == null) {
                remoteDataSource.createNote(request)
            } else {
                remoteDataSource.createUserNote(userId, request)
            }

            when (result) {
                is Resource.Success -> {
                    if (result.data != null) {
                        val remoteNote = result.data
                        val updatedNote = note.copy(
                            remoteId = remoteNote.noteId,
                            userId = userId,
                            isPendingCreate = false
                        )
                        localDataSource.upsert(updatedNote.toEntity())
                        Resource.Success(updatedNote)
                    } else {
                        Resource.Error("Empty response")
                    }
                }
                is Resource.Error -> {
                    Resource.Error(result.message ?: "API error")
                }
                else -> Resource.Error("Unexpected result")
            }
        } catch (e: Exception) {
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
            localDataSource.linkNotesToUser(userId)
            postPendingNotes(userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }

    override suspend fun fetchUserNotesFromApi(userId: Int): Resource<List<Note>> {
        return try {
            clearLocalNotes()
            val result = remoteDataSource.getUserNotes(userId)

            when (result) {
                is Resource.Success -> {
                    val notes = result.data?.map { it.toDomain() } ?: emptyList()

                    notes.forEach { note ->
                        localDataSource.upsert(note.toEntity())
                    }
                    Resource.Success(notes)
                }
                is Resource.Error -> {
                    Resource.Error(result.message ?: "Failed to fetch notes from API")
                }
                else -> {
                    Resource.Error("Unexpected result from API")
                }
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    override suspend fun clearLocalNotes() {
        localDataSource.deleteAllNotes()
    }

    override suspend fun getRemoteNote(noteId: Int): Resource<Note> {
        return try {
            val result = remoteDataSource.getNoteById(noteId)
            when (result) {
                is Resource.Success -> {
                    val note = result.data?.toDomain()
                    if (note != null) {
                        Resource.Success(note)
                    } else {
                        Resource.Error("Nota vacía")
                    }
                }
                is Resource.Error -> Resource.Error(result.message)
                else -> Resource.Loading()
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error obteniendo nota remota")
        }
    }

    override suspend fun getRemoteUserNote(userId: Int, noteId: Int): Resource<Note> {
        return try {
            val result = remoteDataSource.getUserNoteById(userId, noteId)
            when (result) {
                is Resource.Success -> {
                    val note = result.data?.toDomain()
                    if (note != null) {
                        Resource.Success(note)
                    } else {
                        Resource.Error("Nota vacía")
                    }
                }
                is Resource.Error -> Resource.Error(result.message)
                else -> Resource.Loading()
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error obteniendo nota de usuario")
        }
    }
}