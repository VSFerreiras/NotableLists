package ucne.edu.notablelists.data.repository

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

    override suspend fun getNote(id: String): Note? {
        return localDataSource.getNote(id)?.toDomain()
    }

    override suspend fun createNoteLocal(note: Note): Resource<Note> {
        val pending = note.copy(isPendingCreate = true)
        localDataSource.upsert(pending.toEntity())
        return Resource.Success(pending)
    }

    override suspend fun upsert(note: Note): Resource<Unit> {
        return try {
            val isNew = note.remoteId == null
            val entity = note.toEntity().copy(isPendingCreate = isNew)
            localDataSource.upsert(entity)
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
                    remoteDataSource.deleteNote(noteEntity.remoteId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al eliminar")
        }
    }

    override suspend fun deleteRemote(id: Int): Resource<Unit> {
        return try {
            val result = remoteDataSource.deleteNote(id)
            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.message ?: "No se pudo eliminar en el servidor")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error de red al eliminar")
        }
    }

    override suspend fun postPendingNotes(): Resource<Unit> {
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
                    deleteAt = note.deleteAt?: "",
                    autoDelete = note.autoDelete
                )

                val result = remoteDataSource.createNote(request)
                if (result is Resource.Success && result.data != null) {
                    val syncedEntity = note.copy(
                        remoteId = result.data.noteId,
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

    override suspend fun postNote(note: Note): Resource<Note> {
        return try {
            val request = NoteRequestDto(
                title = note.title,
                description = note.description,
                tag = note.tag,
                isFinished = note.isFinished,
                reminder = note.reminder?: "",
                checklist = note.checklist?: "",
                priority = note.priority,
                deleteAt = note.deleteAt?: "",
                autoDelete = note.autoDelete
            )
            val result = remoteDataSource.createNote(request)

            when (result) {
                is Resource.Success -> {
                    if (result.data != null) {
                        val remoteNote = result.data
                        val updatedNote = note.copy(remoteId = remoteNote.noteId)
                        Resource.Success(updatedNote)
                    } else {
                        Resource.Error("Respuesta vacía del servidor")
                    }
                }
                is Resource.Error -> {
                    Resource.Error(result.message ?: "Error desconocido de API")
                }
                else -> Resource.Error("Resultado inesperado")
            }
        } catch (e: Exception) {
            Resource.Error("Error de red: ${e.message}")
        }
    }

    override suspend fun putNote(note: Note): Resource<Note> {
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
                deleteAt = note.deleteAt?: "",
                autoDelete = note.autoDelete
            )
            val result = remoteDataSource.updateNote(remoteId, request)

            if (result is Resource.Success) {
                Resource.Success(note)
            } else {
                Resource.Error(result.message ?: "Failed to update note on server")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to update note: ${e.message}")
        }
    }
}