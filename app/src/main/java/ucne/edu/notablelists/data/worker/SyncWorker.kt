package ucne.edu.notablelists.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ucne.edu.notablelists.data.remote.Resource
import ucne.edu.notablelists.domain.notes.repository.NoteRepository

@HiltWorker

class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteRepository: NoteRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (noteRepository.postPendingNotes()) {
            is Resource.Success -> Result.success()
            is Resource.Error -> Result.retry()
            else -> Result.failure()
        }
    }
}