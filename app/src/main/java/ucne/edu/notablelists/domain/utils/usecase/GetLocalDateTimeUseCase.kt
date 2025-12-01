package ucne.edu.notablelists.domain.utils.usecase

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class GetLocalDateTimeUseCase @Inject constructor() {
    operator fun invoke(date: Long, hour: Int, minute: Int): LocalDateTime {
        val localDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
        return LocalDateTime.of(localDate, java.time.LocalTime.of(hour, minute))
    }
}