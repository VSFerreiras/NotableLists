package ucne.edu.notablelists.data.remote.DataSource

import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.NoteApiService
import ucne.edu.notablelists.data.remote.dto.NoteRequestDto
import ucne.edu.notablelists.data.remote.dto.NoteResponseDto
import javax.inject.Inject

class NoteRemoteDataSource @Inject constructor(
    private val api: NoteApiService
) {
    suspend fun getNotes(): Resource<List<NoteResponseDto>> {
        return try {
            val response = api.getNotes()
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Respuesta vacía del servidor")
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun getNoteById(id: Int): Resource<NoteResponseDto> {
        return try {
            val response = api.getNoteById(id)
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Respuesta vacía del servidor")
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun createNote(request: NoteRequestDto): Resource<NoteResponseDto> {
        return try {
            val response = api.createNote(request)
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Respuesta vacía del servidor")
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun updateNote(id: Int, request: NoteRequestDto): Resource<Unit> {
        return try {
            val response = api.updateNote(id, request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }

    suspend fun deleteNote(id: Int): Resource<Unit> {
        return try {
            val response = api.deleteNote(id)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error de red")
        }
    }
}