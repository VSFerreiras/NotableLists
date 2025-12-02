package ucne.edu.notablelists.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ucne.edu.notablelists.data.local.Notes.NoteDao
import ucne.edu.notablelists.data.local.Users.UserDao
import ucne.edu.notablelists.data.local.Users.UserEntity
import ucne.edu.notablelists.data.local.Notes.NoteEntity
import ucne.edu.notablelists.data.local.Notes.SharedNoteEntity

@Database(entities = [UserEntity::class, NoteEntity::class, SharedNoteEntity::class],
    version = 5,
    exportSchema = false)

abstract class NotableListDB: RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
}