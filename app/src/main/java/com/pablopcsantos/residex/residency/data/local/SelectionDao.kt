package com.pablopcsantos.residex.residency.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectionDao {
    @Query("SELECT * FROM residency_selections WHERE active = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<SelectionEntity>>

    @Query("SELECT * FROM residency_selections ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<SelectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SelectionEntity>)

    @Transaction
    suspend fun replaceAll(items: List<SelectionEntity>) {
        clear()
        insertAll(items)
    }

    @Query("DELETE FROM residency_selections")
    suspend fun clear()
}