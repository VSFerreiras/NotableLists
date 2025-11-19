package ucne.edu.notablelists.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ucne.edu.notablelists.data.local.Users.UserDao
import ucne.edu.notablelists.data.local.Users.UserEntity

@Database(entities = [UserEntity::class],
    version = 1,
    exportSchema = false)

abstract class NotableListDB: RoomDatabase() {

    abstract fun userDao(): UserDao
}