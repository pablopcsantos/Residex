package com.pablopcsantos.residex.residency.data

import com.pablopcsantos.residex.residency.data.local.SelectionEntity
import com.pablopcsantos.residex.residency.data.remote.SelectionDto
import com.pablopcsantos.residex.residency.domain.model.Selection

fun SelectionDto.toEntity(cachedAt: Long): SelectionEntity = SelectionEntity(
    id = id.trim(),
    uf = uf,
    name = name,
    editalInfo = editalInfo,
    editalLink = editalLink,
    inscriptions = inscriptions,
    fee = fee,
    objectiveExam = objectiveExam,
    curriculumAnalysis = curriculumAnalysis,
    practicalExam = practicalExam,
    interview = interview,
    finalResult = finalResult,
    informationLink = informationLink,
    active = active.equals("TRUE", ignoreCase = true),
    notes = notes,
    cachedAt = cachedAt
)

fun SelectionEntity.toDomain(): Selection = Selection(
    id = id,
    uf = uf,
    name = name,
    editalInfo = editalInfo,
    editalLink = editalLink,
    inscriptions = inscriptions,
    fee = fee,
    objectiveExam = objectiveExam,
    curriculumAnalysis = curriculumAnalysis,
    practicalExam = practicalExam,
    interview = interview,
    finalResult = finalResult,
    informationLink = informationLink,
    active = active,
    notes = notes
)