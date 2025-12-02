package ucne.edu.notablelists.domain.sharednote.repository

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.ShareResponseDto
import ucne.edu.notablelists.data.remote.dto.SharedNoteByMeDto
import ucne.edu.notablelists.data.remote.dto.SharedNoteWithDetailsDto
import ucne.edu.notablelists.data.remote.dto.UpdateSharedStatusResponseDto
import ucne.edu.notablelists.domain.notes.model.Note

interface SharedNoteRepository {
    suspend fun shareNote(userId: Int, noteId: Int, friendId: Int): Resource<ShareResponseDto>
    suspend fun getNotesSharedWithMe(userId: Int): Resource<List<Note>>
    suspend fun getNotesSharedByMe(userId: Int): Resource<List<SharedNoteByMeDto>>
    suspend fun updateSharedNoteStatus(userId: Int, sharedNoteId: Int): Resource<UpdateSharedStatusResponseDto>
    suspend fun getSharedNoteDetails(userId: Int, noteId: Int): Resource<SharedNoteWithDetailsDto?>
    suspend fun canAccessNote(userId: Int, noteId: Int): Resource<Boolean>
    suspend fun getAllSharedNotes(userId: Int): Resource<Pair<List<SharedNoteWithDetailsDto>, List<SharedNoteByMeDto>>>
    suspend fun syncSharedNotes(userId: Int): Resource<Unit>
    suspend fun canShareNote(noteId: Int?): Resource<Boolean>
}