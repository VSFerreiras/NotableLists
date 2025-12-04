package ucne.edu.notablelists.data.repository

import android.util.Log
import ucne.edu.notablelists.data.local.Notes.NoteDao
import ucne.edu.notablelists.data.local.Notes.SharedNoteEntity
import ucne.edu.notablelists.data.mappers.toDomainNote
import ucne.edu.notablelists.data.remote.DataSource.NoteRemoteDataSource
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.*
import ucne.edu.notablelists.domain.notes.model.Note
import ucne.edu.notablelists.domain.sharednote.repository.SharedNoteRepository
import javax.inject.Inject

class SharedNoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: NoteRemoteDataSource,
    private val localDataSource: NoteDao
) : SharedNoteRepository {

    companion object {
        private const val UnknownErrorMessage = "Unknown error"
    }

    override suspend fun shareNote(userId: Int, noteId: Int, friendId: Int): Resource<ShareResponseDto> {
        return try {
            Log.d("SHARE_NOTE", "Sharing note $noteId with friend $friendId")

            val result = remoteDataSource.shareNoteWithFriend(userId, noteId, friendId)

            if (result is Resource.Success) {
                val shareResponse = result.data
                if (shareResponse != null) {
                    val sharedNoteEntity = SharedNoteEntity(
                        remoteId = shareResponse.sharedNoteId,
                        noteId = noteId,
                        ownerUserId = userId,
                        targetUserId = friendId,
                        status = "active"
                    )
                    localDataSource.upsertSharedNote(sharedNoteEntity)

                    Log.d("SHARE_NOTE", "Shared successfully: ${shareResponse.sharedNoteId}")
                    Resource.Success(shareResponse)
                } else {
                    Resource.Error("Empty response from server")
                }
            } else {
                Resource.Error(result.message ?: "Failed to share note")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }

    override suspend fun getNotesSharedWithMe(userId: Int): Resource<List<Note>> {
        return try {
            val result = remoteDataSource.getNotesSharedWithMe(userId)

            when (result) {
                is Resource.Success -> {
                    val dtos = result.data ?: emptyList()
                    val notes = dtos.map { it.toDomainNote() }
                    Resource.Success(notes)
                }
                is Resource.Error -> Resource.Error(result.message)
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getNotesSharedByMe(userId: Int): Resource<List<SharedNoteByMeDto>> {
        return try {
            remoteDataSource.getNotesSharedByMe(userId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }

    override suspend fun updateSharedNoteStatus(userId: Int, sharedNoteId: Int): Resource<UpdateSharedStatusResponseDto> {
        return try {
            Log.d("UPDATE_SHARED", "Updating shared note $sharedNoteId for user $userId")

            val result = remoteDataSource.updateSharedNoteStatus(userId, sharedNoteId)

            if (result is Resource.Success) {
                val updateResponse = result.data
                if (updateResponse != null) {
                    val entity = localDataSource.getSharedNoteByRemoteId(sharedNoteId)
                    if (entity != null) {
                        localDataSource.upsertSharedNote(entity.copy(status = updateResponse.newStatus))
                    }

                    Log.d("UPDATE_SHARED", "Updated to: ${updateResponse.newStatus}")
                    Resource.Success(updateResponse)
                } else {
                    Resource.Error("Empty response from server")
                }
            } else {
                Resource.Error(result.message ?: "Failed to update shared note")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }

    override suspend fun getSharedNoteDetails(userId: Int, noteId: Int): Resource<Note> {
        return try {
            val result = remoteDataSource.getSharedNoteDetails(userId, noteId)

            if (result is Resource.Success) {
                val dto = result.data
                if (dto != null) {
                    Resource.Success(dto.toDomainNote())
                } else {
                    Resource.Error("Nota no encontrada o vacía")
                }
            } else {
                Resource.Error(result.message ?: "Error obteniendo detalles de la nota compartida")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }

    override suspend fun canAccessNote(userId: Int, noteId: Int): Resource<Boolean> {
        return try {
            val note = localDataSource.getNoteByRemoteId(noteId)
            if (note != null && note.userId == userId) {
                Log.d("CAN_ACCESS", "User owns the note")
                Resource.Success(true)
            } else {
                val isShared = localDataSource.isNoteSharedWithUser(noteId, userId)
                Log.d("CAN_ACCESS", "Note shared with user: $isShared")
                Resource.Success(isShared)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }

    override suspend fun getAllSharedNotes(userId: Int): Resource<Pair<List<SharedNoteWithDetailsDto>, List<SharedNoteByMeDto>>> {
        return try {
            val sharedWithMe = remoteDataSource.getNotesSharedWithMe(userId)
            val sharedByMe = remoteDataSource.getNotesSharedByMe(userId)

            if (sharedWithMe is Resource.Success && sharedByMe is Resource.Success) {
                val withMe = sharedWithMe.data ?: emptyList()
                val byMe = sharedByMe.data ?: emptyList()
                Resource.Success(Pair(withMe, byMe))
            } else {
                val error = sharedWithMe.message ?: sharedByMe.message
                Resource.Error(error ?: "Failed to load shared notes")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }

    override suspend fun syncSharedNotes(userId: Int): Resource<Unit> {
        return try {
            Log.d("SYNC_SHARED", "Starting shared notes sync for user $userId")

            val sharedWithMe = remoteDataSource.getNotesSharedWithMe(userId)
            val sharedByMe = remoteDataSource.getNotesSharedByMe(userId)

            if (sharedWithMe is Resource.Error || sharedByMe is Resource.Error) {
                val error = sharedWithMe.message ?: sharedByMe.message
                return Resource.Error(error ?: "Sync failed")
            }

            localDataSource.clearAllSharedNotes()

            val withMeData = (sharedWithMe as Resource.Success).data ?: emptyList()
            withMeData.forEach { sharedNote ->
                val entity = SharedNoteEntity(
                    remoteId = sharedNote.sharedNoteId,
                    noteId = sharedNote.noteId,
                    ownerUserId = sharedNote.ownerUserId,
                    targetUserId = userId,
                    status = "active"
                )
                localDataSource.upsertSharedNote(entity)
            }

            val byMeData = (sharedByMe as Resource.Success).data ?: emptyList()
            byMeData.forEach { sharedNote ->
                val entity = SharedNoteEntity(
                    remoteId = sharedNote.sharedNoteId,
                    noteId = sharedNote.noteId,
                    ownerUserId = userId,
                    targetUserId = sharedNote.targetUserId,
                    status = sharedNote.status
                )
                localDataSource.upsertSharedNote(entity)
            }

            Log.d("SYNC_SHARED", "Sync completed successfully")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }

    override suspend fun canShareNote(noteId: Int?): Resource<Boolean> {
        return try {
            val canShare = noteId != null && noteId > 0
            Resource.Success(canShare)
        } catch (e: Exception) {
            Resource.Error(e.message ?: UnknownErrorMessage)
        }
    }
}