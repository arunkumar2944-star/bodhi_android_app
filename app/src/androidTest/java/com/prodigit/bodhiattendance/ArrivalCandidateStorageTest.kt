package com.prodigit.bodhiattendance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prodigit.bodhiattendance.data.local.database.AttendanceDatabase
import com.prodigit.bodhiattendance.data.local.entity.ArrivalCandidateEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArrivalCandidateStorageTest {
    @Test fun firstArrivalSurvivesLaterGpsRetryAndRemainsUserScoped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AttendanceDatabase::class.java).build()
        try {
            val dao = db.arrivalCandidateDao()
            val first = ArrivalCandidateEntity("arrival",7,1,"device",1,"boot","anchor",1000,"aa:bb:cc:dd:ee:ff")
            dao.recordFirst(first)
            dao.recordFirst(first.copy(observedElapsedMs = 1_801_000))
            assertEquals(1000L, dao.pending(7,"device").single().observedElapsedMs)
            assertTrue(dao.pending(8,"device").isEmpty())
            dao.markUploaded(8,"device",listOf("arrival"))
            assertEquals(1, dao.pending(7,"device").size)
            dao.markUploaded(7,"device",listOf("arrival"))
            assertTrue(dao.pending(7,"device").isEmpty())
        } finally { db.close() }
    }
}
