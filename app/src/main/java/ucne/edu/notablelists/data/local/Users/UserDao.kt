package ucne.edu.notablelists.data.local.Users

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM users WHERE isPendingCreate = 1")
    suspend fun getPendingCreateUsers(): List<UserEntity>
}