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
import ucne.edu.notablelists.data.local.Users.UserDao
import ucne.edu.notablelists.data.remote.DataSource.UserRemoteDataSource
import ucne.edu.notablelists.data.remote.UserApiService
import ucne.edu.notablelists.data.repository.UserRepositoryImpl
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
    fun provideUserRepository(userDao: UserDao, remoteDataSource: UserRemoteDataSource): UserRepository {
        return UserRepositoryImpl(userDao, remoteDataSource)
    }
}