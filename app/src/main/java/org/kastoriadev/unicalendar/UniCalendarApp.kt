package org.kastoriadev.unicalendar

import android.app.Application
import org.kastoriadev.unicalendar.data.local.AppDatabase
import org.kastoriadev.unicalendar.data.repository.EventRepositoryImpl
import org.kastoriadev.unicalendar.data.sync.ScheduleSyncManager
import org.kastoriadev.unicalendar.domain.repository.EventRepository

class UniCalendarApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var eventRepository: EventRepository
        private set

    lateinit var syncManager: ScheduleSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        eventRepository = EventRepositoryImpl(database.eventDao())
        syncManager = ScheduleSyncManager(eventRepository)
    }
}
