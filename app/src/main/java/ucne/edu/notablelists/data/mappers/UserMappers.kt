package ucne.edu.notablelists.data.mappers

import ucne.edu.notablelists.data.local.Users.UserEntity
import ucne.edu.notablelists.data.remote.dto.UserRequestDto
import ucne.edu.notablelists.data.remote.dto.UserResponseDto
import ucne.edu.notablelists.domain.users.model.User
import java.util.UUID

fun UserEntity.toDomain(): User = User(
    id = id,
    remoteId = remoteId,
    username = userName,
    password = password
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    remoteId = remoteId,
    userName= username,
    password = password,
    isPendingCreate = false
)

fun UserResponseDto.toEntity(): UserEntity = UserEntity(
    id = UUID.randomUUID().toString(),
    remoteId = userId,
    userName = username,
    password = password
)

fun UserEntity.toRequest(): UserRequestDto = UserRequestDto(
    username = userName,
    password = password
)

fun User.toRequest(): UserRequestDto = UserRequestDto(
    username = username,
    password = password
)
