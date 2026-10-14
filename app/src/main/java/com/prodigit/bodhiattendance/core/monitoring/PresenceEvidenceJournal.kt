package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.os.SystemClock
import java.util.UUID

/**
 * Append-only, bounded sensor evidence journal. Evidence is NOT an attendance event
 * and must never be used by Android to backdate CHECK_IN automatically.
 *
 * Uses elapsedRealtime + boot-scoped observation timestamps for subsequent
 * trusted-time reconciliation. Local wall time is display metadata only.
 */
class PresenceEvidenceJournal(context: Context) {
    private val helper = Helper(context.applicationContext)

    fun record(location: Location, officeId: Long?, bssidMatched: Boolean, classification: String) {
        @Suppress("DEPRECATION")
        if (location.isFromMockProvider || !location.hasAccuracy() ||
            !location.accuracy.isFinite() || location.accuracy < 0 ||
            location.elapsedRealtimeNanos <= 0 ||
            SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos !in 0..60_000_000_000L) return

        val db = helper.writableDatabase
        db.execSQL(
            """INSERT INTO presence_evidence
               (id, office_id, observed_elapsed_ms, recorded_wall_ms, latitude, longitude,
                accuracy_m, wifi_matched, classification)
               VALUES (?,?,?,?,?,?,?,?,?)""",
            arrayOf<Any?>(
                UUID.randomUUID().toString(), officeId,
                location.elapsedRealtimeNanos / 1_000_000L, System.currentTimeMillis(),
                location.latitude, location.longitude, location.accuracy.toDouble(),
                if (bssidMatched) 1 else 0, classification
            )
        )
        // Bounded local retention; oldest observations are discarded after 2000 rows.
        db.execSQL("""DELETE FROM presence_evidence WHERE id IN
            (SELECT id FROM presence_evidence ORDER BY rowid DESC LIMIT -1 OFFSET 2000)""")
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, "presence_evidence.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE presence_evidence (
              id TEXT PRIMARY KEY, office_id INTEGER, observed_elapsed_ms INTEGER NOT NULL,
              recorded_wall_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL,
              accuracy_m REAL NOT NULL, wifi_matched INTEGER NOT NULL,
              classification TEXT NOT NULL)""")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            throw IllegalStateException("Presence evidence migration required")
        }
    }
}
