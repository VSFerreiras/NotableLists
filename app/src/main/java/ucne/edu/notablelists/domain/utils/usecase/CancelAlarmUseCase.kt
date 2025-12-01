package ucne.edu.notablelists.domain.utils.usecase

import ucne.edu.notablelists.data.local.AlarmScheduler
import javax.inject.Inject

class CancelAlarmUseCase @Inject constructor(
    private val alarmScheduler: AlarmScheduler
) {
    operator fun invoke(noteId: String) {
        alarmScheduler.cancel(noteId)
    }
}