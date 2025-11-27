package ucne.edu.notablelists.data.mappers

import ucne.edu.notablelists.data.local.Notes.NoteEntity
import ucne.edu.notablelists.data.remote.dto.NoteRequestDto
import ucne.edu.notablelists.data.remote.dto.NoteResponseDto
import ucne.edu.notablelists.domain.notes.model.Note
import java.util.UUID

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    remoteId = remoteId,
    title = title,
    description = description,
    tag = tag,
    isFinished = isFinished,
    reminder = reminder,
    checklist = checklist,
    priority = priority,
    deleteAt = deleteAt,
    autoDelete = autoDelete,
    isPendingCreate = isPendingCreate,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    remoteId = remoteId,
    title = title,
    description = description,
    tag = tag,
    isFinished = isFinished,
    reminder = reminder,
    checklist = checklist,
    priority = priority,
    deleteAt = deleteAt,
    autoDelete = autoDelete,
    isPendingCreate = isPendingCreate,
)

fun NoteResponseDto.toEntity(): NoteEntity = NoteEntity(
    id = UUID.randomUUID().toString(),
    remoteId = noteId,
    title = title,
    description = description,
    tag = tag,
    isFinished = isFinished,
    reminder = reminder,
    checklist = checklist,
    priority = priority,
    deleteAt = deleteAt,
    autoDelete = autoDelete,
    isPendingCreate = false,
)

fun NoteEntity.toRequest(): NoteRequestDto = NoteRequestDto(
    title = title,
    description = description,
    tag = tag,
    isFinished = isFinished,
    reminder = reminder,
    checklist = checklist,
    priority = priority,
    deleteAt = deleteAt,
    autoDelete = autoDelete
)

fun Note.toRequest(): NoteRequestDto = NoteRequestDto(
    title = title,
    description = description,
    tag = tag,
    isFinished = isFinished,
    reminder = reminder,
    checklist = checklist,
    priority = priority,
    deleteAt = deleteAt,
    autoDelete = autoDelete
)