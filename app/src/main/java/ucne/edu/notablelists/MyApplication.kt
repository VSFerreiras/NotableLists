package ucne.edu.notablelists

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ucne.edu.notablelists.data.worker.MyWorkerFactory
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: MyWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}