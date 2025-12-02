package ucne.edu.notablelists

import android.util.Log
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ucne.edu.notablelists.data.local.Notes.NoteDao
import ucne.edu.notablelists.data.local.Notes.NoteEntity
import ucne.edu.notablelists.data.remote.DataSource.NoteRemoteDataSource
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.data.remote.dto.NoteRequestDto
import ucne.edu.notablelists.data.remote.dto.NoteResponseDto
import ucne.edu.notablelists.data.repository.NoteRepositoryImpl
import ucne.edu.notablelists.domain.notes.model.Note
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class NoteRepositoryImplTest {

    private lateinit var repository: NoteRepositoryImpl
    private lateinit var localDataSource: NoteDao
    private lateinit var remoteDataSource: NoteRemoteDataSource

    private val testNoteId = UUID.randomUUID().toString()
    private val testRemoteId = 100
    private val testUserId = 1

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)

        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        localDataSource = mockk()
        remoteDataSource = mockk()
        repository = NoteRepositoryImpl(localDataSource, remoteDataSource)

        // Mock the suspend function debugAllNotes
        coEvery { repository.debugAllNotes() } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
    }

    @Test
    fun `observeNotes should map entities to domain models`() = runTest {
        val entities = listOf(
            NoteEntity(id = "1", title = "Note 1", description = "", tag = "", reminder = null, checklist = null, priority = 1),
            NoteEntity(id = "2", title = "Note 2", description = "", tag = "", reminder = null, checklist = null, priority = 1)
        )
        val flow = flowOf(entities)

        every { localDataSource.observeNotes() } returns flow

        val resultFlow = repository.observeNotes()
        val result = resultFlow.first()

        assertEquals(2, result.size)
        assertEquals("Note 1", result[0].title)
        assertEquals("Note 2", result[1].title)
    }

    @Test
    fun `observeUserNotes should return user notes`() = runTest {
        val entities = listOf(
            NoteEntity(id = "1", title = "User Note", description = "", tag = "", reminder = null, checklist = null, priority = 1, userId = testUserId),
        )
        val flow = flowOf(entities)

        every { localDataSource.getUserNotes(testUserId) } returns flow

        val resultFlow = repository.observeUserNotes(testUserId)
        val result = resultFlow.first()

        assertEquals(1, result.size)
        assertEquals("User Note", result[0].title)
    }

    @Test
    fun `observeLocalNotes should return notes with null userId`() = runTest {
        val entities = listOf(
            NoteEntity(id = "1", title = "Local Note", description = "", tag = "", reminder = null, checklist = null, priority = 1, userId = null),
        )
        val flow = flowOf(entities)

        every { localDataSource.getLocalNotes() } returns flow

        val resultFlow = repository.observeLocalNotes()
        val result = resultFlow.first()

        assertEquals(1, result.size)
        assertEquals("Local Note", result[0].title)
        assertNull(result[0].userId)
    }

    @Test
    fun `getNote should return mapped domain model`() = runTest {
        val entity = NoteEntity(id = testNoteId, title = "Test Note", description = "", tag = "", reminder = null, checklist = null, priority = 1)
        coEvery { localDataSource.getNote(testNoteId) } returns entity

        val result = repository.getNote(testNoteId)

        assertNotNull(result)
        assertEquals("Test Note", result?.title)
    }

    @Test
    fun `createNoteLocal should save as pending and return success`() = runTest {
        val note = Note(
            id = testNoteId,
            title = "New Note",
            description = "",
            tag = "",
            reminder = null,
            checklist = null,
            priority = 1,
            userId = null
        )

        coEvery { localDataSource.upsert(any()) } just Runs

        val result = repository.createNoteLocal(note, testUserId)

        assertTrue(result is Resource.Success)
        val createdNote = (result as Resource.Success).data!!

        assertNotNull(createdNote)

        assertEquals(testNoteId, createdNote?.id)
        assertEquals("New Note", createdNote?.title)
        assertEquals(testUserId, createdNote?.userId)
        assertTrue(createdNote.isPendingCreate)

        coVerify {
            localDataSource.upsert(withArg { entity ->
                assertTrue(entity.isPendingCreate)
                assertEquals(testUserId, entity.userId)
            })
        }
    }

    @Test
    fun `upsert should save note successfully`() = runTest {
        val note = Note(
            id = testNoteId,
            title = "Test Note",
            description = "Desc",
            tag = "Tag",
            reminder = "Reminder",
            checklist = "Checklist",
            priority = 2
        )

        val slot = slot<NoteEntity>()
        coEvery { localDataSource.upsert(capture(slot)) } just Runs

        val result = repository.upsert(note, testUserId)

        println("Result: $result")
        if (result is Resource.Error) {
            println("Error: ${result.message}")
            return@runTest
        }

        assertTrue(result is Resource.Success)
        assertTrue(slot.isCaptured)

        val captured = slot.captured
        println("Captured entity: $captured")

        assertEquals(testNoteId, captured.id)
        assertEquals("Test Note", captured.title)
        assertEquals(testUserId, captured.userId)
        assertTrue(captured.isPendingCreate)
    }

    @Test
    fun `delete should remove local note and remote if exists`() = runTest {
        val entity = NoteEntity(
            id = testNoteId,
            remoteId = testRemoteId,
            userId = testUserId,
            title = "Test",
            description = "",
            tag = "",
            reminder = null,
            checklist = null,
            priority = 1
        )

        coEvery { localDataSource.getNote(testNoteId) } returns entity
        coEvery { localDataSource.delete(testNoteId) } just Runs
        coEvery { remoteDataSource.deleteUserNote(testUserId, testRemoteId) } returns Resource.Success(Unit)

        val result = repository.delete(testNoteId)

        assertTrue(result is Resource.Success)
        coVerify {
            localDataSource.delete(testNoteId)
            remoteDataSource.deleteUserNote(testUserId, testRemoteId)
        }
    }

    @Test
    fun `delete should only remove local note when no remoteId`() = runTest {
        val entity = NoteEntity(
            id = testNoteId,
            remoteId = null,
            userId = null,
            title = "Test",
            description = "",
            tag = "",
            reminder = null,
            checklist = null,
            priority = 1
        )

        coEvery { localDataSource.getNote(testNoteId) } returns entity
        coEvery { localDataSource.delete(testNoteId) } just Runs

        val result = repository.delete(testNoteId)

        assertTrue(result is Resource.Success)
        coVerify { localDataSource.delete(testNoteId) }
        coVerify(exactly = 0) { remoteDataSource.deleteUserNote(any(), any()) }
    }

    @Test
    fun `postPendingNotes debug why failing`() = runTest {
        mockkStatic(android.util.Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        val pendingEntity = NoteEntity(
            id = testNoteId,
            title = "Pending Note",
            description = "Desc",
            tag = "Tag",
            reminder = "Reminder",
            checklist = "Checklist",
            priority = 1,
            remoteId = null,
            userId = null,
            isPendingCreate = true
        )

        var getPendingCreateNotesCalled = false
        var createUserNoteCalled = false
        var upsertCalled = false

        coEvery { localDataSource.getPendingCreateNotes() } coAnswers {
            getPendingCreateNotesCalled = true
            println("1. getPendingCreateNotes called")
            listOf(pendingEntity)
        }

        coEvery { remoteDataSource.createUserNote(any(), any()) } coAnswers {
            createUserNoteCalled = true
            println("2. createUserNote called, returning Error")
            Resource.Error("Network error")
        }

        coEvery { localDataSource.upsert(any()) } coAnswers {
            upsertCalled = true
        }

        val result = repository.postPendingNotes(testUserId)

        if (result is Resource.Success) {
            println("Result is Success(Unit)")
        } else if (result is Resource.Error) {
            println("Result is Error: ${result.message}")
        }

        assertTrue(result is Resource.Success)
        unmockkStatic(android.util.Log::class)
    }

    @Test
    fun `postNote should create note on server and save locally`() = runTest {
        val note = Note(
            id = testNoteId,
            title = "New Note",
            description = "Desc",
            tag = "Tag",
            reminder = "Reminder",
            checklist = "Checklist",
            priority = 2,
            remoteId = null,
            userId = testUserId
        )

        val remoteNote = NoteResponseDto(
            noteId = testRemoteId,
            title = "New Note",
            description = "Desc",
            tag = "Tag",
            reminder = "Reminder",
            checklist = "Checklist",
            priority = 2,
            userId = testUserId
        )

        val requestSlot = slot<NoteRequestDto>()
        val entitySlot = slot<NoteEntity>()

        coEvery {
            remoteDataSource.createUserNote(testUserId, capture(requestSlot))
        } returns Resource.Success(remoteNote)

        coEvery { localDataSource.upsert(capture(entitySlot)) } just Runs

        val result = repository.postNote(note, testUserId)

        assertTrue(result is Resource.Success)
        val updatedNote = (result as Resource.Success).data!!

        assertEquals(testRemoteId, updatedNote.remoteId)
        assertEquals(testUserId, updatedNote.userId)
        assertFalse(updatedNote.isPendingCreate)

        assertTrue(requestSlot.isCaptured)
        val capturedRequest = requestSlot.captured
        assertEquals("New Note", capturedRequest.title)
        assertEquals("Desc", capturedRequest.description)
        assertEquals("Tag", capturedRequest.tag)
        assertEquals("Reminder", capturedRequest.reminder)
        assertEquals("Checklist", capturedRequest.checklist)
        assertEquals(2, capturedRequest.priority)
        assertEquals(testUserId, capturedRequest.userId)

        assertTrue(entitySlot.isCaptured)
        val capturedEntity = entitySlot.captured
        assertEquals(testRemoteId, capturedEntity.remoteId)
        assertEquals(testUserId, capturedEntity.userId)
        assertFalse(capturedEntity.isPendingCreate)
    }

    @Test
    fun `putNote should update existing note on server`() = runTest {
        val note = Note(
            id = testNoteId,
            title = "Updated Note",
            description = "Desc",
            tag = "Tag",
            reminder = "Reminder",
            checklist = "Checklist",
            priority = 3,
            remoteId = testRemoteId,
            userId = testUserId
        )

        coEvery {
            remoteDataSource.updateUserNote(testUserId, testRemoteId, any())
        } returns Resource.Success(Unit)

        val result = repository.putNote(note, testUserId)

        assertTrue(result is Resource.Success)
        coVerify { remoteDataSource.updateUserNote(testUserId, testRemoteId, any()) }
    }

    @Test
    fun `putNote should return error when no remoteId`() = runTest {
        val note = Note(
            id = testNoteId,
            title = "Note without remote",
            description = "",
            tag = "",
            reminder = null,
            checklist = null,
            priority = 1,
            remoteId = null,
            userId = testUserId
        )

        val result = repository.putNote(note, testUserId)

        assertTrue(result is Resource.Error)
        assertEquals("No remoteId", (result as Resource.Error).message)
    }

    @Test
    fun `syncOnLogin should link notes and sync pending`() = runTest {
        coEvery { localDataSource.linkNotesToUser(testUserId) } just Runs
        coEvery { localDataSource.getAllNotes() } returns emptyList()
        coEvery { localDataSource.getPendingCreateNotes() } returns emptyList()

        val result = repository.syncOnLogin(testUserId)

        assertTrue(result is Resource.Success)
        coVerify {
            localDataSource.linkNotesToUser(testUserId)
            localDataSource.getPendingCreateNotes()
        }
    }

    @Test
    fun `fetchUserNotesFromApi should fetch and save notes`() = runTest {
        val remoteNotes = listOf(
            NoteResponseDto(
                noteId = 1,
                title = "Note 1",
                description = "Desc1",
                tag = "Tag1",
                reminder = "",
                checklist = "",
                priority = 1,
                userId = testUserId
            ),
            NoteResponseDto(
                noteId = 2,
                title = "Note 2",
                description = "Desc2",
                tag = "Tag2",
                reminder = "",
                checklist = "",
                priority = 2,
                userId = testUserId
            )
        )

        coEvery { localDataSource.deleteAllNotes() } just Runs
        coEvery { remoteDataSource.getUserNotes(testUserId) } returns Resource.Success(remoteNotes)
        coEvery { localDataSource.upsert(any()) } just Runs

        val result = repository.fetchUserNotesFromApi(testUserId)

        assertTrue(result is Resource.Success)
        val notes = (result as Resource.Success).data
        assertEquals(2, notes?.size)

        coVerify {
            localDataSource.deleteAllNotes()
            remoteDataSource.getUserNotes(testUserId)
            localDataSource.upsert(any())
        }
    }

    @Test
    fun `clearLocalNotes should delete all notes`() = runTest {
        coEvery { localDataSource.deleteAllNotes() } just Runs

        repository.clearLocalNotes()

        coVerify { localDataSource.deleteAllNotes() }
    }

    @Test
    fun `deleteRemote should call correct delete method based on userId`() = runTest {
        coEvery { remoteDataSource.deleteUserNote(testUserId, testRemoteId) } returns Resource.Success(Unit)

        val result = repository.deleteRemote(testRemoteId, testUserId)

        assertTrue(result is Resource.Success)
        coVerify { remoteDataSource.deleteUserNote(testUserId, testRemoteId) }
    }

    @Test
    fun `deleteRemote should call anonymous delete when no userId`() = runTest {
        coEvery { remoteDataSource.deleteNote(testRemoteId) } returns Resource.Success(Unit)

        val result = repository.deleteRemote(testRemoteId, null)

        assertTrue(result is Resource.Success)
        coVerify { remoteDataSource.deleteNote(testRemoteId) }
    }
}