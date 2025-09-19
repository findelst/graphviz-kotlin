package com.businessarch.generator.model

import kotlinx.serialization.Serializable

/**
 * Модель данных для бизнес-архитектуры
 */

@Serializable
data class BusinessData(
    val AS: List<AutomatedSystem> = emptyList(),
    val Function: List<BusinessFunction> = emptyList(),
    val Link: List<SystemLink> = emptyList(),
    val Links: List<SystemLink> = emptyList() // Поддержка обеих форм
)

@Serializable
data class AutomatedSystem(
    val id: String,
    val name: String,
    val platform: String? = null,
    val region: String? = null,
    val role: List<String> = emptyList()
)

@Serializable
data class BusinessFunction(
    val id: String,
    val name: String,
    val type: String? = null,
    val AS: String // Название АС, к которой принадлежит функция
)

@Serializable
data class SystemLink(
    val source: LinkTarget,
    val target: LinkTarget,
    val description: String? = null
)

@Serializable
data class LinkTarget(
    val AS: String
)

/**
 * Внутренние модели для обработки
 */

data class System(
    val id: String,
    val name: String,
    val platform: String,
    val region: String?,
    val role: List<String>,
    val functions: MutableList<Function>,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 320.0,
    var height: Double = 80.0
)

data class Function(
    val id: String,
    val name: String,
    val type: String?
)

data class Platform(
    val name: String,
    val region: String,
    val systems: MutableList<System>,
    var x: Double = 0.0,
    var y: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0
)

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
