package ucne.edu.notablelists.data.local.Users

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val remoteId: Int? = null,
    val userName: String,
    val password: String,
    val isPendingCreate: Boolean = false
)