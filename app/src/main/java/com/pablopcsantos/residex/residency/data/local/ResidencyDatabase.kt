package com.pablopcsantos.residex.residency.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SelectionEntity::class], version = 2, exportSchema = false)
abstract class ResidencyDatabase : RoomDatabase() {
    abstract fun selectionDao(): SelectionDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE residency_selections_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        uf TEXT NOT NULL,
                        name TEXT NOT NULL,
                        editalInfo TEXT NOT NULL,
                        editalLink TEXT NOT NULL,
                        inscriptions TEXT NOT NULL,
                        fee TEXT NOT NULL,
                        objectiveExam TEXT NOT NULL,
                        curriculumAnalysis TEXT NOT NULL,
                        practicalExam TEXT NOT NULL,
                        interview TEXT NOT NULL,
                        finalResult TEXT NOT NULL,
                        informationLink TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO residency_selections_new
                    SELECT id, uf, name, editalInfo, editalLink, inscriptions, fee,
                           objectiveExam, theoreticalExam, practicalExam, interview,
                           finalResult, informationLink, active, notes, cachedAt
                    FROM residency_selections
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE residency_selections")
                db.execSQL("ALTER TABLE residency_selections_new RENAME TO residency_selections")
            }
        }
    }
}