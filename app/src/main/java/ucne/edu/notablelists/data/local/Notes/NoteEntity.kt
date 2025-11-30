package ucne.edu.notablelists.data.local.Notes

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity (
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val remoteId: Int? = null,
    val title: String,
    val userId: Int? = null,
    val description: String,
    val tag: String,
    val isFinished: Boolean = false,
    val reminder: String?,
    val checklist: String?,
    val priority: Int,
    val deleteAt: String?,
    val autoDelete: Boolean = false,
    val isPendingCreate: Boolean = false
)

