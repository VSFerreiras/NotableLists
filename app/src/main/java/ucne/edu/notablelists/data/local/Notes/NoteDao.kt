package ucne.edu.notablelists.data.local.Notes

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ucne.edu.notablelists.data.local.Notes.NoteEntity

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: String): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM notes WHERE isPendingCreate = 1")
    suspend fun getPendingCreateNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes")
    fun observeNotes(): Flow<List<NoteEntity>>
}