package ucne.edu.notablelists.data.local.Notes

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shared_notes")
data class SharedNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val remoteId: Int? = null,
    val noteId: Int,
    val targetUserId: Int,
    val ownerUserId: Int,
    val status: String = "active"
)