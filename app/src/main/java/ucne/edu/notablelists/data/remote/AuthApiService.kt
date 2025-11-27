package ucne.edu.notablelists.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import ucne.edu.notablelists.data.remote.dto.AuthResponseDto
import ucne.edu.notablelists.data.remote.dto.UserRequestDto

interface AuthApiService {

    @POST("api/Auth/login")
    suspend fun login(@Body request: UserRequestDto): AuthResponseDto

    @POST("api/Auth/register")
    suspend fun register(@Body request: UserRequestDto): AuthResponseDto
}