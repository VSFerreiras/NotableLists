package ucne.edu.notablelists.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ucne.edu.notablelists.data.remote.dto.NoteRequestDto
import ucne.edu.notablelists.data.remote.dto.NoteResponseDto

interface NoteApiService {

    @GET("api/Notes")
    suspend fun getNotes(): Response<List<NoteResponseDto>>

    @GET("api/Notes/{id}")
    suspend fun getNoteById(@Path("id") id: Int): Response<NoteResponseDto>

    @POST("api/Notes")
    suspend fun createNote(@Body request: NoteRequestDto): Response<NoteResponseDto>

    @PUT("api/Notes/{id}")
    suspend fun updateNote(@Path("id") id: Int, @Body request: NoteRequestDto): Response<Unit>

    @DELETE("api/Notes/{id}")
    suspend fun deleteNote(@Path("id") id: Int): Response<Unit>
}