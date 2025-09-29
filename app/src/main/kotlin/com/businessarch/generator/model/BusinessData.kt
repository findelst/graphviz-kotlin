package com.businessarch.generator.model

import kotlinx.serialization.Serializable

/**
 * Модель данных для бизнес-архитектуры
 * Используется для десериализации JSON входных данных
 */

/**
 * Основная структура входных данных
 * @property AS Список автоматизированных систем
 * @property Function Список бизнес-функций
 * @property Link Список связей между системами (основной формат)
 * @property Links Список связей между системами (альтернативный формат для совместимости)
 */
@Serializable
data class BusinessData(
    val AS: List<AutomatedSystem> = emptyList(),
    val FP: List<AutomatedSystem> = emptyList(),
    val Function: List<BusinessFunction> = emptyList(),
    val Link: List<SystemLink> = emptyList(),
    val Links: List<SystemLink> = emptyList() // Поддержка обеих форм
)

/**
 * Автоматизированная система в бизнес-архитектуре
 * @property id Уникальный идентификатор системы
 * @property name Наименование системы для отображения
 * @property platform Платформа, к которой относится система (может быть null)
 * @property region Регион размещения системы (может быть null для внешних систем)
 * @property role Список ролей системы в архитектуре (например, "sales_channel")
 */
@Serializable
data class AutomatedSystem(
    val id: String = "",
    val name: String,
    val type: String = "",
    val platform: String? = null,
    val region: String? = null,
    val AS: String? = null,
    val role: List<String> = emptyList()
)

/**
 * Бизнес-функция, реализуемая автоматизированной системой
 * @property id Уникальный идентификатор функции
 * @property name Наименование функции
 * @property type Тип функции (обычно "business")
 * @property AS Наименование системы, которой принадлежит функция
 */
@Serializable
data class BusinessFunction(
    val id: String = "",
    val name: String,
    val type: String? = null,
    val AS: String = "", // Название АС, к которой принадлежит функция
    val FP: String = "" // Название ФП, к которой принадлежит функция
)

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
    val region: String? = null,
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
