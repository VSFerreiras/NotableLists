package ucne.edu.notablelists.domain.utils.usecase

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class FormatDateTimeUseCase @Inject constructor() {
    operator fun invoke(localDateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return localDateTime.format(formatter)
    }
}