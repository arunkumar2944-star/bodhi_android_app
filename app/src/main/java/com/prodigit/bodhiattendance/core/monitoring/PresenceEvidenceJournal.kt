package com.prodigit.bodhiattendance.core.monitoring

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
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

    data class Pending(val id: String, val officeId: Long?, val elapsedMs: Long, val latitude: Double,
                       val longitude: Double, val accuracyMeters: Double, val wifiMatched: Boolean,
                       val classification: String)

    fun pending(limit: Int = 30): List<Pending> {
        val result = mutableListOf<Pending>()
        helper.readableDatabase.rawQuery("SELECT id,office_id,observed_elapsed_ms,latitude,longitude,accuracy_m,wifi_matched,classification FROM presence_evidence WHERE uploaded=0 ORDER BY rowid LIMIT ?", arrayOf(limit.toString())).use { cursor ->
            while (cursor.moveToNext()) result.add(Pending(cursor.getString(0), if (cursor.isNull(1)) null else cursor.getLong(1),
                cursor.getLong(2), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), cursor.getInt(6)==1,cursor.getString(7)))
        }
        return result
    }

    /**
     * Durable audit candidate, not an attendance event. Kept over process death
     * to support review/reconciliation without assuming Wi-Fi loss = departure.
     * elapsedRealtime values are valid only within their originating boot.
     */
    fun markDepartureCandidate(
        userId: Int, companyId: Int, deviceId: String, bootId: String,
        officeId: Long, elapsedMs: Long, status: String
    ): Boolean {
        val values = ContentValues().apply {
            put("id", UUID.randomUUID().toString())
            put("user_id", userId)
            put("company_id", companyId)
            put("device_id", deviceId)
            put("boot_id", bootId)
            put("office_id", officeId)
            put("first_observed_elapsed_ms", elapsedMs)
            put("status", status)
            put("updated_wall_ms", System.currentTimeMillis())
        }
        return helper.writableDatabase.insertWithOnConflict(
            "departure_candidates", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun resolveDepartureCandidate(userId: Int, deviceId: String, bootId: String, status: String): Int {
        val values = ContentValues().apply {
            put("status", status)
            put("updated_wall_ms", System.currentTimeMillis())
        }
        return helper.writableDatabase.update(
            "departure_candidates", values,
            "user_id=? AND device_id=? AND boot_id=? AND status='UNCONFIRMED'",
            arrayOf(userId.toString(), deviceId, bootId)
        )
    }

    /** Aggregate counts only. No coordinates, tokens, raw BSSIDs or unrelated users. */
    fun candidateCounts(userId: Int, companyId: Int, deviceId: String): Pair<Int, Int> {
        helper.readableDatabase.rawQuery(
            """SELECT
                COUNT(*),
                COALESCE(SUM(CASE WHEN status='UNCONFIRMED' THEN 1 ELSE 0 END),0)
               FROM departure_candidates WHERE user_id=? AND company_id=? AND device_id=?""",
            arrayOf(userId.toString(), companyId.toString(), deviceId)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) to cursor.getInt(1) else 0 to 0
        }
    }

    fun markUploaded(ids: List<String>) {
        val db=helper.writableDatabase
        db.beginTransaction()
        try {
            val statement=db.compileStatement("UPDATE presence_evidence SET uploaded=1 WHERE id=?")
            ids.forEach { statement.clearBindings(); statement.bindString(1,it); statement.executeUpdateDelete() }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

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
        db.execSQL("""DELETE FROM presence_evidence WHERE uploaded=1 AND id IN
            (SELECT id FROM presence_evidence WHERE uploaded=1 ORDER BY rowid DESC LIMIT -1 OFFSET 2000)""")
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, "presence_evidence.db", null, 3) {
        private fun createDepartureCandidates(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS departure_candidates (
              id TEXT PRIMARY KEY, user_id INTEGER NOT NULL, company_id INTEGER NOT NULL,
              device_id TEXT NOT NULL, boot_id TEXT NOT NULL, office_id INTEGER NOT NULL,
              first_observed_elapsed_ms INTEGER NOT NULL, status TEXT NOT NULL,
              updated_wall_ms INTEGER NOT NULL)""")
            db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS idx_departure_candidates_active
               ON departure_candidates(user_id,device_id,boot_id,office_id)
               WHERE status='UNCONFIRMED'""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS idx_departure_candidates_device
               ON departure_candidates(user_id,device_id,boot_id,status)""")
        }

        override fun onCreate(db: SQLiteDatabase) {
            createDepartureCandidates(db)
            db.execSQL("""CREATE TABLE presence_evidence (
              id TEXT PRIMARY KEY, office_id INTEGER, observed_elapsed_ms INTEGER NOT NULL,
              recorded_wall_ms INTEGER NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL,
              accuracy_m REAL NOT NULL, wifi_matched INTEGER NOT NULL,
              classification TEXT NOT NULL, uploaded INTEGER NOT NULL DEFAULT 0)""")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) db.execSQL("ALTER TABLE presence_evidence ADD COLUMN uploaded INTEGER NOT NULL DEFAULT 0")
            if (oldVersion < 3) createDepartureCandidates(db)
        }
    }
}
