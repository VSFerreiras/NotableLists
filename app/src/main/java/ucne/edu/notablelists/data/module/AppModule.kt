package ucne.edu.notablelists.data.module

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ucne.edu.notablelists.data.db.NotableListDB
import ucne.edu.notablelists.data.local.AlarmScheduler
import ucne.edu.notablelists.data.local.Notes.NoteDao
import ucne.edu.notablelists.data.local.SessionManager
import ucne.edu.notablelists.data.local.Users.UserDao
import ucne.edu.notablelists.data.remote.AuthApiService
import ucne.edu.notablelists.data.remote.DataSource.NoteRemoteDataSource
import ucne.edu.notablelists.data.remote.DataSource.UserRemoteDataSource
import ucne.edu.notablelists.data.remote.NoteApiService
import ucne.edu.notablelists.data.remote.UserApiService
import ucne.edu.notablelists.data.repository.AuthRepositoryImpl
import ucne.edu.notablelists.data.repository.FriendsRepositoryImpl
import ucne.edu.notablelists.data.repository.NoteRepositoryImpl
import ucne.edu.notablelists.data.repository.SessionRepositoryImpl
import ucne.edu.notablelists.data.repository.SharedNoteRepositoryImpl
import ucne.edu.notablelists.data.repository.UserRepositoryImpl
import ucne.edu.notablelists.domain.auth.AuthRepository
import ucne.edu.notablelists.domain.friends.repository.FriendsRepository
import ucne.edu.notablelists.domain.notes.repository.NoteRepository
import ucne.edu.notablelists.domain.notification.ReminderScheduler
import ucne.edu.notablelists.domain.session.SessionRepository
import ucne.edu.notablelists.domain.sharednote.repository.SharedNoteRepository
import ucne.edu.notablelists.domain.users.repository.UserRepository
import javax.inject.Singleton

@InstallIn(
    SingletonComponent::class)
@Module

object AppModule {
    @Provides
    @Singleton
    fun provideNotableListDB(@ApplicationContext appContext: Context): NotableListDB {
        return Room.databaseBuilder(
            appContext,
            NotableListDB::class.java,
            "NotableList_DB"
        ).fallbackToDestructiveMigration(false)
            .build()
    }

    private const val BASE_URL = "https://registrotecnicosapi.somee.com/"

    @Provides
    @Singleton
    fun provideUserDao(notableListDB: NotableListDB): UserDao {
        return notableListDB.userDao()
    }

    @Provides
    @Singleton
    fun provideNoteDao(notableListDB: NotableListDB): NoteDao {
        return notableListDB.noteDao()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApi(moshi: Moshi): UserApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNoteApi(moshi: Moshi): NoteApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NoteApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(moshi: Moshi): AuthApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao, remoteDataSource: UserRemoteDataSource): UserRepository {
        return UserRepositoryImpl(userDao, remoteDataSource)
    }

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao, remoteDataSource: NoteRemoteDataSource): NoteRepository {
        return NoteRepositoryImpl(noteDao, remoteDataSource)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(authApiService: AuthApiService): AuthRepository {
        return AuthRepositoryImpl(authApiService)
    }

    @Provides
    @Singleton
    fun provideFriendsRepository(remoteDataSource: UserRemoteDataSource): FriendsRepository {
        return FriendsRepositoryImpl(remoteDataSource)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(sessionManager: SessionManager): SessionRepository {
        return SessionRepositoryImpl(sessionManager)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler {
        return AlarmScheduler(context)
    }

    @Provides
    @Singleton
    fun provideSharedNotesRepository(noteDao: NoteDao, remoteDataSource: NoteRemoteDataSource): SharedNoteRepository {
        return SharedNoteRepositoryImpl(remoteDataSource,noteDao)
    }
}