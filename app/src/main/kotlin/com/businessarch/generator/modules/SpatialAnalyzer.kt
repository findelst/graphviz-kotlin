package com.businessarch.generator.modules

import com.businessarch.generator.model.*
import kotlin.math.*

/**
 * Модуль для анализа пространственных отношений между блоками
 * Определяет направления связей и оптимальные точки подключения
 */
class SpatialAnalyzer {
    // Пороги для определения направлений
    private val diagonalThreshold = 0.5 // Соотношение для определения диагонального направления
    private val proximityThreshold = 50.0  // Минимальное расстояние для считания блоков "близкими"

    /**
     * Анализирует все связи в системе и определяет пространственные отношения
     */
    fun analyzeAllConnections(systems: List<System>, connections: List<Connection>): SpatialAnalysisResult {
        println("🔍 Анализ пространственных отношений...")
        
        // Создаем карту систем для быстрого поиска
        val systemMap = systems.associateBy { it.name }
            .mapValues { (_, system) ->
                SystemWithSpatialInfo(
                    system = system,
                    center = calculateCenter(system),
                    bounds = calculateBounds(system)
                )
            }

        // Анализируем каждую связь
        val analyzedConnections = connections.map { connection ->
            val sourceSystem = systemMap[connection.source]
            val targetSystem = systemMap[connection.target]
            
            if (sourceSystem == null || targetSystem == null) {
                println("⚠️ Система не найдена: ${connection.source} -> ${connection.target}")
                return@map connection.copy(spatial = null)
            }

            val spatial = analyzeSpatialRelation(sourceSystem, targetSystem)
            
            println("📊 ${connection.source} -> ${connection.target}: ${spatial.primaryDirection} (${spatial.angle.roundToInt()}°)")
            
            connection.copy(spatial = spatial)
        }

        // Группируем связи по источникам для анализа множественных соединений
        val connectionsBySource = groupConnectionsBySource(analyzedConnections)
        
        // Создаем статистику
        val stats = createAnalysisStats(analyzedConnections)
        
        return SpatialAnalysisResult(
            connections = analyzedConnections,
            connectionsBySource = connectionsBySource,
            stats = stats
        )
    }

    /**
     * Анализирует пространственное отношение между двумя системами
     */
    private fun analyzeSpatialRelation(source: SystemWithSpatialInfo, target: SystemWithSpatialInfo): SpatialInfo {
        val deltaX = target.center.x - source.center.x
        val deltaY = target.center.y - source.center.y
        
        // Рассчитываем расстояние и угол
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        val angle = atan2(deltaY, deltaX) * 180 / PI
        
        // Определяем основное направление
        val horizontalDistance = abs(deltaX)
        val verticalDistance = abs(deltaY)
        
        val primaryDirection: String
        val secondaryDirection: String
        
        if (horizontalDistance > verticalDistance) {
            // Горизонтальное направление преобладает
            primaryDirection = if (deltaX > 0) "right" else "left"
            secondaryDirection = if (deltaY > 0) "down" else "up"
        } else {
            // Вертикальное направление преобладает
            primaryDirection = if (deltaY > 0) "down" else "up"
            secondaryDirection = if (deltaX > 0) "right" else "left"
        }
        
        // Определяем, является ли связь диагональной
        val ratio = min(horizontalDistance, verticalDistance) / max(horizontalDistance, verticalDistance)
        val isDiagonal = ratio > diagonalThreshold
        
        // Определяем, являются ли системы близкими
        val isClose = distance < proximityThreshold
        
        return SpatialInfo(
            primaryDirection = primaryDirection,
            secondaryDirection = secondaryDirection,
            angle = angle,
            distance = distance,
            isDiagonal = isDiagonal,
            isClose = isClose,
            deltaX = deltaX,
            deltaY = deltaY
        )
    }

    /**
     * Рассчитывает центр системы
     */
    private fun calculateCenter(system: System): Point {
        return Point(
            x = system.x + system.width / 2,
            y = system.y + system.height / 2
        )
    }

    /**
     * Рассчитывает границы системы
     */
    private fun calculateBounds(system: System): Bounds {
        return Bounds(
            left = system.x,
            top = system.y,
            right = system.x + system.width,
            bottom = system.y + system.height
        )
    }

    /**
     * Группирует связи по источникам
     */
    private fun groupConnectionsBySource(connections: List<Connection>): Map<String, List<Connection>> {
        return connections.groupBy { it.source }
    }

    /**
     * Создает статистику анализа
     */
    private fun createAnalysisStats(connections: List<Connection>): SpatialStats {
        val totalConnections = connections.size
        val diagonalConnections = connections.count { it.spatial?.isDiagonal == true }
        val closeConnections = connections.count { it.spatial?.isClose == true }
        
        val directionStats = connections.mapNotNull { it.spatial?.primaryDirection }
            .groupingBy { it }
            .eachCount()
        
        return SpatialStats(
            totalConnections = totalConnections,
            diagonalConnections = diagonalConnections,
            closeConnections = closeConnections,
            directionDistribution = directionStats
        )
    }

    /**
     * Выводит отчет о пространственном анализе
     */
    fun printAnalysisReport(result: SpatialAnalysisResult) {
        println("\n📋 === ОТЧЕТ ПРОСТРАНСТВЕННОГО АНАЛИЗА ===")
        println("🔗 Общее количество связей: ${result.stats.totalConnections}")
        println("↗️ Диагональных связей: ${result.stats.diagonalConnections}")
        println("📍 Близких связей: ${result.stats.closeConnections}")
        
        println("\n📊 Распределение по направлениям:")
        result.stats.directionDistribution.forEach { (direction, count) ->
            val percentage = (count * 100.0 / result.stats.totalConnections).roundToInt()
            println("  $direction: $count ($percentage%)")
        }
        
        println("\n🎯 Множественные исходящие соединения:")
        result.connectionsBySource
            .filter { it.value.size > 1 }
            .forEach { (source, connections) ->
                println("  $source → ${connections.size} связей")
                connections.forEach { conn ->
                    val spatial = conn.spatial
                    if (spatial != null) {
                        println("    → ${conn.target}: ${spatial.primaryDirection} (${spatial.distance.roundToInt()}px)")
                    }
                }
            }
        
        println("=== КОНЕЦ ОТЧЕТА ===\n")
    }
}

/**
 * Вспомогательные классы для пространственного анализа
 */
data class SystemWithSpatialInfo(
    val system: System,
    val center: Point,
    val bounds: Bounds
)

data class Point(
    val x: Double,
    val y: Double
)

data class Bounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
)

data class SpatialAnalysisResult(
    val connections: List<Connection>,
    val connectionsBySource: Map<String, List<Connection>>,
    val stats: SpatialStats
)

data class SpatialStats(
    val totalConnections: Int,
    val diagonalConnections: Int,
    val closeConnections: Int,
    val directionDistribution: Map<String, Int>
)
