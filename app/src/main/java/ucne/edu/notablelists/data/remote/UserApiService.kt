package ucne.edu.notablelists.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ucne.edu.notablelists.data.remote.dto.FriendDto
import ucne.edu.notablelists.data.remote.dto.PendingRequestDto
import ucne.edu.notablelists.data.remote.dto.UserRequestDto
import ucne.edu.notablelists.data.remote.dto.UserResponseDto

interface UserApiService {
    @GET("api/Users")
    suspend fun getUsers(): Response<List<UserResponseDto>>

    @GET("api/Users/{id}")
    suspend fun getUserById(@Path("id") id: Int): Response<UserResponseDto>

    @POST("api/Users")
    suspend fun createUser(@Body request: UserRequestDto): Response<UserResponseDto>

    @PUT("api/Users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body request: UserRequestDto): Response<Unit>

    @DELETE("api/Users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<Unit>

    @POST("api/Users/{userId}/friend-requests/{friendId}")
    suspend fun sendFriendRequest(
        @Path("userId") userId: Int,
        @Path("friendId") friendId: Int
    ): Response<Unit>

    @GET("api/Users/{userId}/pending-requests")
    suspend fun getPendingRequests(@Path("userId") userId: Int): Response<List<PendingRequestDto>>

    @POST("api/Users/{userId}/friend-requests/{friendshipId}/accept")
    suspend fun acceptFriendRequest(
        @Path("userId") userId: Int,
        @Path("friendshipId") friendshipId: Int
    ): Response<Unit>

    @GET("api/Users/{userId}/friends")
    suspend fun getFriends(@Path("userId") userId: Int): Response<List<FriendDto>>
}