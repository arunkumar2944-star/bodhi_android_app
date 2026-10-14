package com.prodigit.bodhiattendance.data.local.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

import androidx.sqlite.db.SupportSQLiteDatabase

import com.prodigit.bodhiattendance.data.local.dao.RegisteredDeviceDao
import com.prodigit.bodhiattendance.data.local.entity.RegisteredDeviceEntity
import com.prodigit.bodhiattendance.data.local.dao.AttendanceConfigDao
import com.prodigit.bodhiattendance.data.local.dao.AttendanceEngineStateDao
import com.prodigit.bodhiattendance.data.local.dao.AttendanceEventDao
import com.prodigit.bodhiattendance.data.local.dao.TimeAnchorDao

import com.prodigit.bodhiattendance.data.local.entity.AttendanceBeaconEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceEngineStateEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceEventEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceLocationEntity
import com.prodigit.bodhiattendance.data.local.entity.AttendanceWifiEntity
import com.prodigit.bodhiattendance.data.local.entity.TimeAnchorEntity


// ============================================================
// ATTENDANCE DATABASE
//
// Version history:
//
// Version 1
// - Initial attendance_events table
//
// Version 2
// - Added office Wi-Fi verification evidence:
//      wifiVerified
//      wifiBssid
//
// Version 3
// - Added local attendance configuration cache:
//      attendance_locations
//      attendance_beacons
//      attendance_wifi
//
// Version 4
// - Added trusted server time anchor persistence:
//      time_anchors
//
// - Added trusted-time fields to attendance_events:
//      timeAnchorId
//      eventElapsedRealtimeMs
//
// Version 5
// - Added persistent automatic attendance-engine runtime state:
//      attendance_engine_state
//
// This table stores ONLY the current engine state required to
// recover after process death / app restart.
//
// It does NOT contain real attendance history.
//
// IMPORTANT:
//
// attendance_events
//      Contains real locally generated attendance events:
//
//      CHECK_IN
//      BREAK_START
//      BREAK_END
//      CHECK_OUT
//
// attendance_locations
// attendance_beacons
// attendance_wifi
//      Contain office attendance configuration cached from
//      Express.
//
// time_anchors
//      Contains trusted server-issued time anchors used for
//      offline trusted-time reconstruction.
//
// attendance_engine_state
//      Contains current automatic-engine runtime state:
//
//      attendance state
//      internal phase
//      original absence monotonic time
//      break-confirmation monotonic time
//      checkout-classification monotonic time
//      boot session
//
// Configuration is managed from React CRM.
// Android treats configuration as read-only business data.
//
// Phone wall-clock time must NOT be used as authoritative
// attendance time.
// ============================================================

@Database(
    entities = [

        AttendanceEventEntity::class,

        AttendanceLocationEntity::class,

        AttendanceBeaconEntity::class,

        AttendanceWifiEntity::class,

        TimeAnchorEntity::class,

        AttendanceEngineStateEntity::class,
        RegisteredDeviceEntity::class

    ],

    version = 8,

    exportSchema = false
)
abstract class AttendanceDatabase :
    RoomDatabase() {


    // ========================================================
    // ATTENDANCE EVENT DAO
    // ========================================================

    abstract fun registeredDeviceDao(): RegisteredDeviceDao

    abstract fun attendanceEventDao():
            AttendanceEventDao


    // ========================================================
    // ATTENDANCE CONFIGURATION DAO
    // ========================================================

    abstract fun attendanceConfigDao():
            AttendanceConfigDao


    // ========================================================
    // TIME ANCHOR DAO
    //
    // Provides access to locally persisted server-issued
    // trusted time anchors.
    // ========================================================

    abstract fun timeAnchorDao():
            TimeAnchorDao


    // ========================================================
    // ATTENDANCE ENGINE STATE DAO
    //
    // Provides access to the ONE current runtime engine state
    // for each user/device pair.
    //
    // This is separate from attendance history.
    // ========================================================

    abstract fun attendanceEngineStateDao():
            AttendanceEngineStateDao


    companion object {


        // ====================================================
        // DATABASE INSTANCE
        // ====================================================

        @Volatile
        private var INSTANCE:
                AttendanceDatabase? = null


        // ====================================================
        // DATABASE NAME
        // ====================================================

        private const val DATABASE_NAME =
            "prodigit_attendance.db"


        // ====================================================
        // MIGRATION 1 -> 2
        //
        // Adds Wi-Fi evidence to existing attendance events.
        //
        // Existing rows receive:
        //
        // wifiVerified = false
        // wifiBssid    = null
        //
        // Existing attendance events are preserved.
        // ====================================================

        private val MIGRATION_1_2 =
            object : Migration(
                1,
                2
            ) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    // =========================================
                    // WIFI VERIFIED
                    // =========================================

                    db.execSQL(
                        """
                        ALTER TABLE attendance_events
                        ADD COLUMN wifiVerified INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )


                    // =========================================
                    // WIFI BSSID
                    // =========================================

                    db.execSQL(
                        """
                        ALTER TABLE attendance_events
                        ADD COLUMN wifiBssid TEXT
                        """.trimIndent()
                    )
                }
            }


        // ====================================================
        // MIGRATION 2 -> 3
        //
        // Adds the attendance configuration cache.
        //
        // Existing attendance_events are NOT modified.
        //
        // Creates:
        //
        // attendance_locations
        // attendance_beacons
        // attendance_wifi
        //
        // ====================================================

        private val MIGRATION_2_3 =
            object : Migration(
                2,
                3
            ) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {


                    // =========================================
                    // ATTENDANCE LOCATIONS
                    // =========================================

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS attendance_locations (
                            id INTEGER NOT NULL,
                            locationName TEXT NOT NULL,
                            locationCode TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            geofenceRadiusMeters REAL NOT NULL,
                            maxAccuracyMeters REAL,
                            cachedAt INTEGER NOT NULL,
                            PRIMARY KEY(id)
                        )
                        """.trimIndent()
                    )


                    // =========================================
                    // ATTENDANCE BEACONS
                    //
                    // Foreign key:
                    //
                    // attendanceLocationId
                    //      ->
                    // attendance_locations.id
                    //
                    // =========================================

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS attendance_beacons (
                            id INTEGER NOT NULL,
                            attendanceLocationId INTEGER NOT NULL,
                            name TEXT NOT NULL,
                            identifier TEXT NOT NULL,
                            expectedMinRssi INTEGER,
                            PRIMARY KEY(id),
                            FOREIGN KEY(attendanceLocationId)
                                REFERENCES attendance_locations(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )


                    // =========================================
                    // BEACON INDEXES
                    // =========================================

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_attendance_beacons_attendanceLocationId
                        ON attendance_beacons(attendanceLocationId)
                        """.trimIndent()
                    )


                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_attendance_beacons_identifier
                        ON attendance_beacons(identifier)
                        """.trimIndent()
                    )


                    // =========================================
                    // ATTENDANCE WI-FI
                    // =========================================

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS attendance_wifi (
                            id INTEGER NOT NULL,
                            attendanceLocationId INTEGER NOT NULL,
                            name TEXT NOT NULL,
                            ssid TEXT,
                            bssid TEXT NOT NULL,
                            PRIMARY KEY(id),
                            FOREIGN KEY(attendanceLocationId)
                                REFERENCES attendance_locations(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )


                    // =========================================
                    // WI-FI INDEXES
                    // =========================================

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_attendance_wifi_attendanceLocationId
                        ON attendance_wifi(attendanceLocationId)
                        """.trimIndent()
                    )


                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_attendance_wifi_bssid
                        ON attendance_wifi(bssid)
                        """.trimIndent()
                    )
                }
            }


        // ====================================================
        // MIGRATION 3 -> 4
        //
        // Adds trusted server time-anchor persistence.
        //
        // Creates:
        //
        // time_anchors
        //
        // Also adds to attendance_events:
        //
        // timeAnchorId
        // eventElapsedRealtimeMs
        //
        // Both attendance event columns are nullable.
        //
        // This is required because existing version 3
        // attendance events were created before trusted-time
        // anchor persistence existed.
        //
        // Existing attendance and configuration data is
        // preserved.
        // ====================================================

        private val MIGRATION_3_4 =
            object : Migration(
                3,
                4
            ) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {


                    // =========================================
                    // TIME ANCHORS
                    //
                    // Stores the trusted server-issued anchor.
                    //
                    // anchorUuid
                    //      Server-generated anchor identifier.
                    //
                    // deviceId
                    //      Registered Android installation.
                    //
                    // serverTime
                    //      Trusted server timestamp.
                    //
                    // elapsedRealtimeMs
                    //      Android monotonic time associated
                    //      with the server anchor.
                    //
                    // bootId
                    //      Identifies the boot session.
                    //
                    // storedAtEpochMs
                    //      Local storage metadata only.
                    //
                    // =========================================

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS time_anchors (
                            anchorUuid TEXT NOT NULL,
                            deviceId TEXT NOT NULL,
                            serverTime TEXT NOT NULL,
                            elapsedRealtimeMs INTEGER NOT NULL,
                            bootId TEXT NOT NULL,
                            storedAtEpochMs INTEGER NOT NULL,
                            PRIMARY KEY(anchorUuid)
                        )
                        """.trimIndent()
                    )


                    // =========================================
                    // ATTENDANCE EVENT TIME ANCHOR
                    //
                    // Logical reference:
                    //
                    // attendance_events.timeAnchorId
                    //             ->
                    // time_anchors.anchorUuid
                    //
                    // We intentionally keep this nullable so
                    // existing attendance events remain valid.
                    //
                    // No SQLite foreign key is added at this
                    // stage. The repository / attendance engine
                    // maintains the relationship.
                    // =========================================

                    db.execSQL(
                        """
                        ALTER TABLE attendance_events
                        ADD COLUMN timeAnchorId TEXT
                        """.trimIndent()
                    )


                    // =========================================
                    // EVENT MONOTONIC TIME
                    //
                    // Captures:
                    //
                    // SystemClock.elapsedRealtime()
                    //
                    // when the attendance event occurred.
                    //
                    // Trusted event time can later be derived:
                    //
                    // anchor.serverTime
                    //      +
                    // (
                    //   event.eventElapsedRealtimeMs
                    //      -
                    //   anchor.elapsedRealtimeMs
                    // )
                    //
                    // Nullable for existing attendance events.
                    // =========================================

                    db.execSQL(
                        """
                        ALTER TABLE attendance_events
                        ADD COLUMN eventElapsedRealtimeMs INTEGER
                        """.trimIndent()
                    )
                }
            }


        // ====================================================
        // MIGRATION 4 -> 5
        //
        // Adds persistent automatic attendance-engine runtime
        // state.
        //
        // Creates:
        //
        // attendance_engine_state
        //
        // IMPORTANT:
        //
        // This table is NOT attendance history.
        //
        // Only ONE current engine-state row exists for each:
        //
        // userId + deviceId
        //
        // It allows recovery of:
        //
        // - current attendance state
        // - current internal phase
        // - 3-minute absence grace timing
        // - active break timing
        // - original office-exit monotonic time
        // - checkout-classification timing
        // - boot session
        //
        // Existing tables are NOT modified.
        //
        // Existing attendance events, configuration cache and
        // trusted time anchors are preserved.
        // ====================================================

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS registered_devices (
                        userId INTEGER NOT NULL PRIMARY KEY,
                        registrationId INTEGER NOT NULL,
                        deviceId TEXT NOT NULL,
                        companyId INTEGER,
                        isActive INTEGER NOT NULL,
                        isOnboarded INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_locations ADD COLUMN gracePeriodMinutes INTEGER")
                db.execSQL("ALTER TABLE attendance_locations ADD COLUMN maximumAbsenceMinutes INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_locations ADD COLUMN wifiRequired INTEGER")
                db.execSQL("ALTER TABLE attendance_locations ADD COLUMN bleRequired INTEGER")
            }
        }

        private val MIGRATION_4_5 =
            object : Migration(
                4,
                5
            ) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {


                    // =========================================
                    // ATTENDANCE ENGINE STATE
                    //
                    // Composite primary key:
                    //
                    // userId + deviceId
                    //
                    // This guarantees one current state for each
                    // employee/device pair.
                    // =========================================

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS attendance_engine_state (
                            userId INTEGER NOT NULL,
                            companyId INTEGER NOT NULL,
                            deviceId TEXT NOT NULL,
                            attendanceState TEXT NOT NULL,
                            phaseType TEXT NOT NULL,
                            absenceStartedElapsedRealtimeMs INTEGER,
                            breakConfirmedElapsedRealtimeMs INTEGER,
                            checkoutClassifiedAtElapsedRealtimeMs INTEGER,
                            bootId TEXT NOT NULL,
                            updatedAtEpochMs INTEGER NOT NULL,
                            PRIMARY KEY(userId, deviceId)
                        )
                        """.trimIndent()
                    )


                    // =========================================
                    // DEVICE ID INDEX
                    //
                    // Matches:
                    //
                    // @Index(value = ["deviceId"])
                    //
                    // in AttendanceEngineStateEntity.
                    // =========================================

                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_attendance_engine_state_deviceId
                        ON attendance_engine_state(deviceId)
                        """.trimIndent()
                    )
                }
            }


        // ====================================================
        // SINGLETON DATABASE
        // ====================================================

        fun getInstance(
            context: Context
        ): AttendanceDatabase {


            return INSTANCE
                ?: synchronized(this) {


                    val instance =

                        Room.databaseBuilder(

                            context.applicationContext,

                            AttendanceDatabase::class.java,

                            DATABASE_NAME
                        )

                            // =================================
                            // DATABASE MIGRATIONS
                            //
                            // IMPORTANT:
                            //
                            // Keep EVERY migration.
                            //
                            // Version 1 installation:
                            //
                            // 1 -> 2 -> 3 -> 4 -> 5
                            //
                            // Version 2 installation:
                            //
                            // 2 -> 3 -> 4 -> 5
                            //
                            // Version 3 installation:
                            //
                            // 3 -> 4 -> 5
                            //
                            // Version 4 installation:
                            //
                            // 4 -> 5
                            //
                            // Fresh installations receive the
                            // complete version 5 schema.
                            // =================================

                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7,
                                MIGRATION_7_8
                            )

                            .build()


                    INSTANCE =
                        instance


                    instance
                }
        }
    }
}