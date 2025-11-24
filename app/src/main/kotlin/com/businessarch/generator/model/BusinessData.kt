package com.businessarch.generator.model

import kotlinx.serialization.Serializable

/**
 * Модель данных для бизнес-архитектуры
 * Используется для десериализации JSON входных данных
 * Теперь используется ArchResult из Model.kt
 */

/**
 * Автоматизированная система в бизнес-архитектуре
 * @property id Уникальный идентификатор системы
 * @property name Наименование системы для отображения
 * @property platform Платформа, к которой относится система (может быть null)
 * @property role Список ролей системы в архитектуре (например, "sales_channel")
 */
@Serializable
data class AutomatedSystem(
    val id: String = "",
    val name: String,
    val type: String = "",
    val platform: String? = null,
    val AS: String? = null,
    val role: List<String> = emptyList()
)

// BusinessFunction теперь не используется, заменен на FunctionObject из Model.kt

/**
 * Связь между автоматизированными системами
 * @property source Система-источник связи
 * @property target Система-получатель
 * @property description Описание характера связи
 */
@Serializable
data class SystemLink(
    val source: LinkTarget,
    val target: LinkTarget,
    val description: String? = null
)

/**
 * Целевая система в связи
 * @property AS Наименование автоматизированной системы
 */
@Serializable
data class LinkTarget(
    val AS: String = "",
    val FP: String = ""
)

/**
 * Внутренние модели для обработки
 */

data class System(
    val id: String,
    val name: String,
    val platform: String,
    val role: List<String>,
    val functions: MutableList<Function>,
    val functionalPlatforms: MutableList<FunctionalPlatform> = mutableListOf(),
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 320.0,
    var height: Double = 80.0
)

data class FunctionalPlatform(
    val id: String,
    val name: String,
    val functions: MutableList<Function>,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 280.0,
    var height: Double = 60.0
)

data class Function(
    val id: String,
    val name: String,
    val type: String?
)

data class Platform(
    val name: String,
    val systems: MutableList<System>,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0
)

// Region больше не используется, но оставляем stub для обратной совместимости
data class Region(
    val name: String,
    val platforms: MutableList<Platform>,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0
)

data class Connection(
    val source: String,
    val target: String,
    val type: String = "business",
    val description: String? = null,
    var spatial: SpatialInfo? = null
)

data class SpatialInfo(
    val primaryDirection: String,
    val secondaryDirection: String,
    val angle: Double,
    val distance: Double,
    val isDiagonal: Boolean,
    val isClose: Boolean,
    val deltaX: Double,
    val deltaY: Double
)

data class GenerationResult(
    val success: Boolean,
    val data: GenerationData? = null,
    val error: String? = null
)

data class GenerationData(
    val svg: String,
    val stats: GenerationStats
)

data class GenerationStats(
    val systems: Int,
    val connections: Int,
    val regions: Int,
    val platforms: Int
)
