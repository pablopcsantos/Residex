package com.pablopcsantos.residex.residency.domain.model

data class Selection(
    val id: String,
    val uf: String,
    val name: String,
    val editalInfo: String = "",
    val editalLink: String = "",
    val inscriptions: String = "",
    val fee: String = "",
    val objectiveExam: String = "",
    val curriculumAnalysis: String = "",
    val practicalExam: String = "",
    val interview: String = "",
    val finalResult: String = "",
    val informationLink: String = "",
    val active: Boolean = true,
    val notes: String = ""
)

enum class SelectionStatus {
    OPEN,
    UPCOMING,
    STAGE_SOON,
    CLOSED,
    FOLLOWING
}

enum class SelectionSortMode {
    MANUAL,
    OPEN_FIRST,
    NEXT_EVENT,
    ALPHABETICAL
}