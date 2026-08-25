package com.pablopcsantos.residex.residency.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "residency_selections")
data class SelectionEntity(
    @PrimaryKey val id: String,
    val uf: String,
    val name: String,
    val editalInfo: String,
    val editalLink: String,
    val inscriptions: String,
    val fee: String,
    val objectiveExam: String,
    val curriculumAnalysis: String,
    val practicalExam: String,
    val interview: String,
    val finalResult: String,
    val informationLink: String,
    val active: Boolean,
    val notes: String,
    val cachedAt: Long
)