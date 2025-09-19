package com.businessarch.generator.modules

import com.businessarch.generator.model.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Модуль для управления связями между системами
 * Отвечает за анализ соединений и генерацию SVG путей для связей
 */
class ConnectionManager(private val connectionRouter: ConnectionRouter) {

    /**
     * Рендерит связь между системами используя только правила пространственного анализа
     */
    fun renderBusinessConnection(
        sourceSystem: System, 
        targetSystem: System, 
        connectionData: Connection = Connection("", ""),
        parallelContext: ParallelContext = ParallelContext()
    ): String {
        // Подготавливаем контекст для ConnectionRouter на основе только пространственного анализа
        val routingContext = RoutingContext(
            parallelContext = parallelContext,
            connectionData = connectionData,
            spatial = parallelContext.spatial ?: connectionData.spatial
        )
        
        // Логируем информацию о пространственном анализе
        parallelContext.spatial?.let { spatial ->
            println("🎯 Связь ${connectionData.source} -> ${connectionData.target}: ${spatial.primaryDirection} (${spatial.angle.roundToInt()}°, диагональ: ${spatial.isDiagonal})")
        }
        
        // Создаем маршрут с помощью ConnectionRouter на основе пространственного анализа
        val route = connectionRouter.createRoute(sourceSystem, targetSystem, routingContext)
        
        // Определяем CSS класс и маркер на основе пространственных данных
        val cssClass = getSpatialConnectionCssClass(sourceSystem, targetSystem, parallelContext.spatial)
        val markerType = getSpatialConnectionMarkerType(sourceSystem, targetSystem, parallelContext.spatial)
        
        // Формируем SVG элемент пути
        var connectionSVG = """<path d="${route.path}" class="$cssClass" marker-end="url(#$markerType)" />"""
        
        // Добавляем описание связи, если есть (опционально)
        connectionData.description?.let { description ->
            connectionSVG += createConnectionLabel(route.labelPosition, description)
        }
        
        return connectionSVG
    }

    /**
     * Анализирует все соединения для группировки входящих и исходящих стрелочек
     */
    fun analyzeConnections(connections: List<Connection>, systemMap: Map<String, System>): ConnectionGroups {
        val incomingGroups = mutableMapOf<String, ConnectionGroup>()
        val outgoingGroups = mutableMapOf<String, ConnectionGroup>()
        
        // Группируем соединения по целевым системам (входящие)
        connections.forEach { conn ->
            val targetSystem = systemMap[conn.target]
            val sourceSystem = systemMap[conn.source]
            
            if (targetSystem == null || sourceSystem == null) return@forEach
            
            // Входящие соединения
            val incomingGroup = incomingGroups.getOrPut(conn.target) {
                ConnectionGroup(
                    connections = mutableListOf(),
                    totalIncoming = 0,
                    totalOutgoing = 0,
                    side = determineDominantSide(targetSystem, connections, systemMap)
                )
            }
            
            incomingGroup.connections.add(conn)
            incomingGroup.totalIncoming++
            
            // Исходящие соединения
            val outgoingGroup = outgoingGroups.getOrPut(conn.source) {
                ConnectionGroup(
                    connections = mutableListOf(),
                    totalIncoming = 0,
                    totalOutgoing = 0,
                    side = null
                )
            }
            
            outgoingGroup.connections.add(conn)
            outgoingGroup.totalOutgoing++
        }
        
        return ConnectionGroups(incoming = incomingGroups, outgoing = outgoingGroups)
    }

    /**
     * Определяет доминирующую сторону для входящих соединений
     */
    private fun determineDominantSide(targetSystem: System, connections: List<Connection>, systemMap: Map<String, System>): String {
        val sides = mutableMapOf("left" to 0, "right" to 0, "top" to 0, "bottom" to 0)
        
        connections
            .filter { it.target == targetSystem.name }
            .forEach { conn ->
                val sourceSystem = systemMap[conn.source] ?: return@forEach
                
                val relativePos = analyzeRelativePosition(sourceSystem, targetSystem)
                
                when (relativePos.primaryDirection) {
                    "left" -> sides["left"] = sides["left"]!! + 1
                    "right" -> sides["right"] = sides["right"]!! + 1
                    "up" -> sides["top"] = sides["top"]!! + 1
                    "down" -> sides["bottom"] = sides["bottom"]!! + 1
                }
            }
        
        // Возвращаем сторону с максимальным количеством соединений
        return sides.maxByOrNull { it.value }?.key ?: "left"
    }

    /**
     * Анализирует относительное положение двух АС
     */
    private fun analyzeRelativePosition(sourceSystem: System, targetSystem: System): RelativePosition {
        val sourceCenterX = sourceSystem.x + sourceSystem.width / 2
        val sourceCenterY = sourceSystem.y + sourceSystem.height / 2
        val targetCenterX = targetSystem.x + targetSystem.width / 2
        val targetCenterY = targetSystem.y + targetSystem.height / 2

        val deltaX = targetCenterX - sourceCenterX
        val deltaY = targetCenterY - sourceCenterY
        
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

        return RelativePosition(
            primaryDirection = primaryDirection,
            secondaryDirection = secondaryDirection,
            deltaX = deltaX,
            deltaY = deltaY,
            horizontalDistance = horizontalDistance,
            verticalDistance = verticalDistance
        )
    }

    /**
     * Определяет CSS класс для связи на основе только пространственных характеристик
     */
    private fun getSpatialConnectionCssClass(sourceSystem: System, targetSystem: System, spatial: SpatialInfo?): String {
        // Проверяем, является ли связь внешней (к внешним АС)
        if (targetSystem.platform == "Внешняя АС" || sourceSystem.platform == "Внешняя АС") {
            return "business-connection-external"
        }
        
        // Если есть данные пространственного анализа, используем их для стилизации
        spatial?.let { spatialInfo ->
            if (spatialInfo.isDiagonal) {
                return "business-connection-diagonal"
            }
            if (spatialInfo.isClose) {
                return "business-connection-close"
            }
            
            // Стилизация на основе основного направления
            return when (spatialInfo.primaryDirection) {
                "right", "left" -> "business-connection" // Горизонтальные связи - стандартный стиль
                "up", "down" -> "business-connection" // Вертикальные связи - стандартный стиль с обходом справа
                else -> "business-connection"
            }
        }
        
        // Fallback к стандартному стилю
        return "business-connection"
    }

    /**
     * Определяет тип маркера для связи на основе только пространственных данных
     */
    private fun getSpatialConnectionMarkerType(sourceSystem: System, targetSystem: System, spatial: SpatialInfo?): String {
        // Проверяем, является ли связь внешней (к внешним АС)
        if (targetSystem.platform == "Внешняя АС" || sourceSystem.platform == "Внешняя АС") {
            return "arrowhead-external"
        }
        
        // Для всех остальных связей используем стандартный маркер
        // (пространственный анализ не влияет на тип маркера)
        return "arrowhead-business"
    }

    /**
     * Создает подпись для связи
     */
    private fun createConnectionLabel(position: Point, description: String): String {
        val maxLength = 20
        val truncatedDesc = if (description.length > maxLength) {
            description.substring(0, maxLength) + "..."
        } else {
            description
        }
        
        return """
            <rect x="${position.x - 40}" y="${position.y - 10}" width="80" height="20" 
                  fill="rgba(255,255,255,0.9)" stroke="#666" stroke-width="1" rx="3" class="connection-label-bg"/>
            <text x="${position.x}" y="${position.y + 4}" text-anchor="middle" 
                  class="connection-label-text">${escapeXml(truncatedDesc)}</text>
        """.trimIndent()
    }

    /**
     * Экранирует XML символы
     */
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

/**
 * Вспомогательные классы
 */
data class ConnectionGroups(
    val incoming: Map<String, ConnectionGroup>,
    val outgoing: Map<String, ConnectionGroup>
)

data class ConnectionGroup(
    val connections: MutableList<Connection>,
    var totalIncoming: Int,
    var totalOutgoing: Int,
    val side: String?
)

data class RelativePosition(
    val primaryDirection: String,
    val secondaryDirection: String,
    val deltaX: Double,
    val deltaY: Double,
    val horizontalDistance: Double,
    val verticalDistance: Double
)
