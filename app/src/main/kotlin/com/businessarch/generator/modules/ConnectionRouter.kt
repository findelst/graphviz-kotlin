package com.businessarch.generator.modules

import com.businessarch.generator.model.*
import kotlin.math.*

/**
 * Модуль для управления маршрутизацией соединений между блоками
 * Содержит абстрактную модель правил для различных сценариев соединений
 */
class ConnectionRouter {
    // Параметры маршрутизации
    private val exitLength = 20.0        // Длина выходящего отрезка
    private val entryLength = 20.0       // Длина входящего отрезка
    private val verticalSpacing = 40.0   // Расстояние между параллельными вертикальными участками
    private val outgoingSpacing = 20.0   // Расстояние между исходящими стрелками
    private val bypassPadding = 30.0     // Отступ для обхода препятствий

    // Пространственная карта для анализа препятствий
    private var spatialMap: List<System> = emptyList()
    private var regions: List<Region> = emptyList()

    private val rules = initializeRules()

    /**
     * Инициализация правил маршрутизации
     */
    private fun initializeRules(): List<RoutingRule> {
        return listOf(
            RoutingRule(
                name = "internal_system_connection",
                description = "Связь внутри одной системы между FP - петля с правой стороны",
                condition = { _, _, context ->
                    context.connectionType == "internal"
                },
                strategy = "internal_loop"
            ),
            RoutingRule(
                name = "adjacent_systems_direct",
                description = "Соседние системы без препятствий - прямое соединение",
                condition = { source, target, context ->
                    areSystemsAdjacent(source, target) &&
                    !hasObstaclesBetween(source, target, context) &&
                    context.connectionType != "inter-region"
                },
                strategy = "direct_connection"
            ),
            RoutingRule(
                name = "same_region_with_obstacles",
                description = "Системы в одном регионе с препятствиями между ними",
                condition = { source, target, context ->
                    context.connectionType == "intra-platform" &&
                    hasObstaclesBetween(source, target, context)
                },
                strategy = "intra_region_bypass"
            ),
            RoutingRule(
                name = "inter_region_connection",
                description = "Соединение между регионами - обход региона сверху или снизу",
                condition = { source, target, context ->
                    context.connectionType == "inter-region"
                },
                strategy = "inter_region_bypass"
            ),
            RoutingRule(
                name = "spatial_optimized_routing",
                description = "Оптимизированная маршрутизация на основе пространственного анализа",
                condition = { _, _, context ->
                    context.spatial != null
                },
                strategy = "spatial_routing"
            ),
            RoutingRule(
                name = "default_routing",
                description = "Базовая маршрутизация по умолчанию",
                condition = { _, _, _ -> true }, // Всегда применяется как fallback
                strategy = "default_connection"
            )
        )
    }

    /**
     * Инициализирует пространственную карту
     */
    fun initializeSpatialMap(systems: List<System>, regions: List<Region>) {
        this.spatialMap = systems
        this.regions = regions
    }

    /**
     * Создает маршрут между двумя системами
     */
    fun createRoute(source: System, target: System, context: RoutingContext): RouteResult {
        // Определяем тип соединения
        val connectionType = determineConnectionType(source, target)
        val fullContext = context.copy(connectionType = connectionType)
        
        // Находим подходящее правило
        val applicableRule = rules.find { rule ->
            rule.condition(source, target, fullContext)
        } ?: rules.last() // fallback к default_routing
        
        println("🎯 Маршрут ${source.name} -> ${target.name}: правило '${applicableRule.name}', стратегия '${applicableRule.strategy}'")
        
        // Применяем стратегию маршрутизации
        return when (applicableRule.strategy) {
            "internal_loop" -> createInternalLoopRoute(source, target, fullContext)
            "direct_connection" -> createDirectRoute(source, target, fullContext)
            "intra_region_bypass" -> createIntraRegionBypassRoute(source, target, fullContext)
            "inter_region_bypass" -> createInterRegionBypassRoute(source, target, fullContext)
            "spatial_routing" -> createSpatialRoute(source, target, fullContext)
            else -> createDefaultRoute(source, target, fullContext)
        }
    }

    /**
     * Определяет тип соединения между системами
     */
    private fun determineConnectionType(source: System, target: System): String {
        // Проверяем, связь внутри одной системы (между FP одной АС)
        if (source.name == target.name) {
            return "internal"
        }

        // Проверяем, являются ли системы внешними
        if (source.platform == "Внешняя АС" || target.platform == "Внешняя АС") {
            return "external"
        }
        
        // Проверяем регионы
        val connectionType = when {
            source.region != target.region -> "inter-region"
            source.platform != target.platform -> "inter-platform"
            else -> "intra-platform"
        }
        
        return connectionType
    }

    /**
     * Создает прямой маршрут между системами
     */
    private fun createDirectRoute(source: System, target: System, context: RoutingContext): RouteResult {
        val sourcePoint = getOptimalExitPoint(source, target, context)
        val targetPoint = getOptimalEntryPoint(target, source, context)
        
        // Проверяем, нужен ли горизонтальный отрезок
        val needsHorizontalSegment = needsHorizontalSegment(sourcePoint, targetPoint, source, target)
        
        val (path, labelPos) = if (needsHorizontalSegment) {
            println("🔧 Добавляем горизонтальный отрезок для: ${source.name} -> ${target.name}")
            createRouteWithHorizontalSegment(sourcePoint, targetPoint, source, target)
        } else {
            // Прямое соединение
            val path = "M ${sourcePoint.x},${sourcePoint.y} L ${targetPoint.x},${targetPoint.y}"
            val labelPos = Point(
                x = (sourcePoint.x + targetPoint.x) / 2,
                y = (sourcePoint.y + targetPoint.y) / 2
            )
            Pair(path, labelPos)
        }
        
        return RouteResult(path = path, labelPosition = labelPos)
    }

    /**
     * Создает маршрут с обходом препятствий внутри региона
     */
    private fun createIntraRegionBypassRoute(source: System, target: System, context: RoutingContext): RouteResult {
        val sourcePoint = getOptimalExitPoint(source, target, context)
        val targetPoint = getOptimalEntryPoint(target, source, context)
        
        // Если точки можно соединить прямо (нет препятствий), делаем прямое соединение
        if (!hasObstaclesBetween(source, target, context)) {
            return createDirectRoute(source, target, context)
        }
        
        // Для обхода препятствий также используем горизонтальные отрезки
        val needsHorizontalSegment = needsHorizontalSegment(sourcePoint, targetPoint, source, target)
        
        val (path, labelPos) = if (needsHorizontalSegment) {
            createRouteWithHorizontalSegment(sourcePoint, targetPoint, source, target)
        } else {
            // Определяем направление обхода для случаев без горизонтальных отрезков
            val bypassDirection = determineBestBypassDirection(source, target, context)
            
            val waypoints = when (bypassDirection) {
                "right" -> createRightBypassWaypoints(sourcePoint, targetPoint, source, target)
                "left" -> createLeftBypassWaypoints(sourcePoint, targetPoint, source, target)
                "top" -> createTopBypassWaypoints(sourcePoint, targetPoint, source, target)
                else -> createBottomBypassWaypoints(sourcePoint, targetPoint, source, target)
            }
            
            val allPoints = listOf(sourcePoint) + waypoints + targetPoint
            val path = buildPathFromWaypoints(allPoints)
            val labelPosition = calculateMidpointOfPath(allPoints)
            
            Pair(path, labelPosition)
        }
        
        return RouteResult(path = path, labelPosition = labelPos)
    }

    /**
     * Создает маршрут между регионами
     */
    private fun createInterRegionBypassRoute(source: System, target: System, context: RoutingContext): RouteResult {
        val sourcePoint = getOptimalExitPoint(source, target, context)
        val targetPoint = getOptimalEntryPoint(target, source, context)
        
        // Проверяем, нужен ли горизонтальный отрезок
        val needsHorizontalSegment = needsHorizontalSegment(sourcePoint, targetPoint, source, target)
        
        val (path, labelPos) = if (needsHorizontalSegment) {
            println("🔧 Добавляем горизонтальный отрезок для inter-region route: ${source.name} -> ${target.name}")
            createRouteWithHorizontalSegment(sourcePoint, targetPoint, source, target)
        } else {
            // Для межрегиональных соединений используем более сложную логику
            val sourceRegion = findSystemRegion(source)
            val targetRegion = findSystemRegion(target)
            
            val waypoints = if (sourceRegion != null && targetRegion != null) {
                createInterRegionWaypoints(sourcePoint, targetPoint, sourceRegion, targetRegion)
            } else {
                // Fallback к простому обходу
                createSimpleBypassWaypoints(sourcePoint, targetPoint, source, target)
            }
            
            val allPoints = listOf(sourcePoint) + waypoints + targetPoint
            val path = buildPathFromWaypoints(allPoints)
            val labelPosition = calculateMidpointOfPath(allPoints)
            
            Pair(path, labelPosition)
        }
        
        return RouteResult(path = path, labelPosition = labelPos)
    }

    /**
     * Создает маршрут на основе пространственного анализа
     */
    private fun createSpatialRoute(source: System, target: System, context: RoutingContext): RouteResult {
        val spatial = context.spatial ?: return createDefaultRoute(source, target, context)
        
        val sourcePoint = getOptimalExitPoint(source, target, context)
        val targetPoint = getOptimalEntryPoint(target, source, context)
        
        // Проверяем, нужен ли горизонтальный отрезок
        val needsHorizontalSegment = needsHorizontalSegment(sourcePoint, targetPoint, source, target)
        
        val (path, labelPos) = if (needsHorizontalSegment) {
            println("🔧 Добавляем горизонтальный отрезок для spatial route: ${source.name} -> ${target.name}")
            createRouteWithHorizontalSegment(sourcePoint, targetPoint, source, target)
        } else {
            // Определяем стратегию на основе пространственных характеристик
            val waypoints = when {
                spatial.isClose -> {
                    // Для близких систем - прямое соединение или простой обход
                    if (spatial.isDiagonal) {
                        createDiagonalWaypoints(sourcePoint, targetPoint, spatial)
                    } else {
                        emptyList()
                    }
                }
                spatial.isDiagonal -> {
                    // Для диагональных связей - ступенчатый маршрут
                    createSteppedWaypoints(sourcePoint, targetPoint, spatial)
                }
                else -> {
                    // Для остальных - стандартный обход
                    createDirectionalWaypoints(sourcePoint, targetPoint, spatial)
                }
            }
            
            val allPoints = listOf(sourcePoint) + waypoints + targetPoint
            val path = buildPathFromWaypoints(allPoints)
            val labelPosition = calculateMidpointOfPath(allPoints)
            
            Pair(path, labelPosition)
        }
        
        return RouteResult(path = path, labelPosition = labelPos)
    }

    /**
     * Создает маршрут по умолчанию
     */
    private fun createDefaultRoute(source: System, target: System, context: RoutingContext): RouteResult {
        return createDirectRoute(source, target, context)
    }

    /**
     * Создает петлю для связи внутри одной системы
     */
    private fun createInternalLoopRoute(source: System, target: System, context: RoutingContext): RouteResult {
        // Для внутренних связей используем правую сторону системы
        val systemCenterY = source.y + source.height / 2
        val rightEdge = source.x + source.width

        // Определяем высоту выхода и входа на основе контекста параллельных соединений
        val outgoingOffset = context.parallelContext.outgoingIndex * outgoingSpacing
        val incomingOffset = context.parallelContext.incomingIndex * outgoingSpacing

        val sourcePoint = Point(x = rightEdge, y = systemCenterY + outgoingOffset)
        val targetPoint = Point(x = rightEdge, y = systemCenterY + incomingOffset)

        // Создаем петлю справа от системы
        val loopWidth = 20.0 // Ширина петли
        val loopX = rightEdge + loopWidth

        // Определяем точки петли
        val waypoints = listOf(
            sourcePoint,
            Point(x = loopX, y = sourcePoint.y), // Горизонтальный выход
            Point(x = loopX, y = targetPoint.y), // Вертикальный переход
            Point(x = rightEdge, y = targetPoint.y) // Горизонтальный вход
        )

        val path = buildPathFromWaypoints(waypoints)

        // Позиция метки в середине петли
        val labelPosition = Point(
            x = loopX + 10,
            y = (sourcePoint.y + targetPoint.y) / 2
        )

        println("🔄 Создана внутрисистемная петля для: ${source.name}")

        return RouteResult(path = "", labelPosition = labelPosition)
    }

    /**
     * Получает оптимальную точку выхода из системы
     */
    private fun getOptimalExitPoint(source: System, target: System, context: RoutingContext): Point {
        val sourceCenterX = source.x + source.width / 2
        val sourceCenterY = source.y + source.height / 2
        val targetCenterX = target.x + target.width / 2
        val targetCenterY = target.y + target.height / 2
        
        val deltaX = targetCenterX - sourceCenterX
        val deltaY = targetCenterY - sourceCenterY
        
        // Специальная логика для систем в одной платформе
        val isSamePlatform = context.connectionType == "intra-platform"
        val isVerticallyAligned = abs(deltaX) < source.width / 2 // Системы примерно друг над другом
        
        // Основное правило: приоритет горизонтальным выходам (справа/слева)
        // Вертикальный выход только если блоки строго друг над другом И нет препятствий
        val isDirectlyAboveOrBelow = abs(deltaX) < source.width / 4 // Очень близко по горизонтали
        val hasObstacles = hasObstaclesBetween(source, target, context)
        
        return when {
            // Для систем в одной платформе, расположенных вертикально - использовать правую сторону
            isSamePlatform && isVerticallyAligned -> {
                Point(x = source.x + source.width, y = sourceCenterY)
            }
            isDirectlyAboveOrBelow && !hasObstacles -> {
                // Редкий случай: блоки строго друг над другом и нет препятствий - вертикальный выход
                if (deltaY > 0) {
                    // Цель снизу - выход снизу
                    Point(x = sourceCenterX, y = source.y + source.height)
                } else {
                    // Цель сверху - выход сверху
                    Point(x = sourceCenterX, y = source.y)
                }
            }
            else -> {
                // Основной случай: горизонтальный выход (справа или слева)
                if (deltaX >= 0) {
                    // Цель справа или на том же уровне - выход справа
                    Point(x = source.x + source.width, y = sourceCenterY)
                } else {
                    // Цель слева - выход слева
                    Point(x = source.x, y = sourceCenterY)
                }
            }
        }
    }

    /**
     * Получает оптимальную точку входа в систему
     */
    private fun getOptimalEntryPoint(target: System, source: System, context: RoutingContext): Point {
        val sourceCenterX = source.x + source.width / 2
        val sourceCenterY = source.y + source.height / 2
        val targetCenterX = target.x + target.width / 2
        val targetCenterY = target.y + target.height / 2
        
        val deltaX = targetCenterX - sourceCenterX
        val deltaY = targetCenterY - sourceCenterY
        
        // Специальная логика для систем в одной платформе
        val isSamePlatform = context.connectionType == "intra-platform"
        val isVerticallyAligned = abs(deltaX) < target.width / 2 // Системы примерно друг над другом
        
        // Основное правило: приоритет горизонтальным входам (справа/слева)
        // Вертикальный вход только если блоки строго друг над другом И нет препятствий
        val isDirectlyAboveOrBelow = abs(deltaX) < target.width / 4 // Очень близко по горизонтали
        val hasObstacles = hasObstaclesBetween(source, target, context)
        
        return when {
            // Для систем в одной платформе, расположенных вертикально - использовать правую сторону
            isSamePlatform && isVerticallyAligned -> {
                Point(x = target.x + target.width, y = targetCenterY)
            }
            isDirectlyAboveOrBelow && !hasObstacles -> {
                // Редкий случай: блоки строго друг над другом и нет препятствий - вертикальный вход
                if (deltaY > 0) {
                    // Источник сверху - вход сверху
                    Point(x = targetCenterX, y = target.y)
                } else {
                    // Источник снизу - вход снизу
                    Point(x = targetCenterX, y = target.y + target.height)
                }
            }
            else -> {
                // Основной случай: горизонтальный вход (справа или слева)
                if (deltaX >= 0) {
                    // Источник слева - вход слева
                    Point(x = target.x, y = targetCenterY)
                } else {
                    // Источник справа - вход справа
                    Point(x = target.x + target.width, y = targetCenterY)
                }
            }
        }
    }

    // Вспомогательные методы (упрощенные версии из JavaScript кода)
    
    private fun areSystemsAdjacent(source: System, target: System): Boolean {
        val distance = sqrt((source.x - target.x).pow(2) + (source.y - target.y).pow(2))
        return distance < 100 // Простая проверка расстояния
    }

    private fun hasObstaclesBetween(source: System, target: System, context: RoutingContext): Boolean {
        // Упрощенная проверка препятствий
        return spatialMap.any { system ->
            system != source && system != target &&
            isSystemBetween(source, target, system)
        }
    }

    private fun isSystemBetween(source: System, target: System, obstacle: System): Boolean {
        // Проверяем, пересекает ли прямая линия между source и target систему obstacle
        val sourceCenter = Point(source.x + source.width / 2, source.y + source.height / 2)
        val targetCenter = Point(target.x + target.width / 2, target.y + target.height / 2)
        val obstacleCenter = Point(obstacle.x + obstacle.width / 2, obstacle.y + obstacle.height / 2)
        
        // Простая проверка - находится ли центр препятствия близко к линии соединения
        val lineLength = sqrt((targetCenter.x - sourceCenter.x).pow(2) + (targetCenter.y - sourceCenter.y).pow(2))
        if (lineLength == 0.0) return false
        
        val t = max(0.0, min(1.0, 
            ((obstacleCenter.x - sourceCenter.x) * (targetCenter.x - sourceCenter.x) + 
             (obstacleCenter.y - sourceCenter.y) * (targetCenter.y - sourceCenter.y)) / lineLength.pow(2)
        ))
        
        val projection = Point(
            x = sourceCenter.x + t * (targetCenter.x - sourceCenter.x),
            y = sourceCenter.y + t * (targetCenter.y - sourceCenter.y)
        )
        
        val distance = sqrt((obstacleCenter.x - projection.x).pow(2) + (obstacleCenter.y - projection.y).pow(2))
        return distance < max(obstacle.width, obstacle.height) / 2
    }

    private fun determineBestBypassDirection(source: System, target: System, context: RoutingContext): String {
        // Простая логика определения направления обхода
        val spatial = context.spatial
        return when (spatial?.primaryDirection) {
            "right", "left" -> "top" // Для горизонтальных связей обходим сверху
            "up", "down" -> "right" // Для вертикальных связей обходим справа
            else -> "right"
        }
    }

    private fun findSystemRegion(system: System): Region? {
        return regions.find { region ->
            region.platforms.any { platform ->
                platform.systems.contains(system)
            }
        }
    }

    // Методы создания waypoints (упрощенные версии)
    
    private fun createRightBypassWaypoints(start: Point, end: Point, source: System, target: System): List<Point> {
        val midX = max(source.x + source.width, target.x + target.width) + bypassPadding
        return listOf(
            Point(x = midX, y = start.y),
            Point(x = midX, y = end.y)
        )
    }

    private fun createLeftBypassWaypoints(start: Point, end: Point, source: System, target: System): List<Point> {
        val midX = min(source.x, target.x) - bypassPadding
        return listOf(
            Point(x = midX, y = start.y),
            Point(x = midX, y = end.y)
        )
    }

    private fun createTopBypassWaypoints(start: Point, end: Point, source: System, target: System): List<Point> {
        val midY = min(source.y, target.y) - bypassPadding
        return listOf(
            Point(x = start.x, y = midY),
            Point(x = end.x, y = midY)
        )
    }

    private fun createBottomBypassWaypoints(start: Point, end: Point, source: System, target: System): List<Point> {
        val midY = max(source.y + source.height, target.y + target.height) + bypassPadding
        return listOf(
            Point(x = start.x, y = midY),
            Point(x = end.x, y = midY)
        )
    }

    private fun createInterRegionWaypoints(start: Point, end: Point, sourceRegion: Region, targetRegion: Region): List<Point> {
        // Упрощенная логика для межрегиональных соединений
        val midY = if (sourceRegion.y < targetRegion.y) {
            sourceRegion.y - bypassPadding
        } else {
            sourceRegion.y + sourceRegion.height + bypassPadding
        }
        
        return listOf(
            Point(x = start.x, y = midY),
            Point(x = end.x, y = midY)
        )
    }

    private fun createSimpleBypassWaypoints(start: Point, end: Point, source: System, target: System): List<Point> {
        return createRightBypassWaypoints(start, end, source, target)
    }

    private fun createDiagonalWaypoints(start: Point, end: Point, spatial: SpatialInfo): List<Point> {
        // Для диагональных близких связей - одна промежуточная точка
        return listOf(
            Point(x = start.x + spatial.deltaX / 2, y = start.y + spatial.deltaY / 2)
        )
    }

    private fun createSteppedWaypoints(start: Point, end: Point, spatial: SpatialInfo): List<Point> {
        // Ступенчатый маршрут для диагональных связей
        return when (spatial.primaryDirection) {
            "right", "left" -> listOf(Point(x = end.x, y = start.y))
            else -> listOf(Point(x = start.x, y = end.y))
        }
    }

    private fun createDirectionalWaypoints(start: Point, end: Point, spatial: SpatialInfo): List<Point> {
        // Направленный маршрут на основе основного направления
        return when (spatial.primaryDirection) {
            "right" -> listOf(Point(x = end.x, y = start.y))
            "left" -> listOf(Point(x = end.x, y = start.y))
            "down" -> listOf(Point(x = start.x, y = end.y))
            "up" -> listOf(Point(x = start.x, y = end.y))
            else -> emptyList()
        }
    }

    /**
     * Проверяет, нужен ли горизонтальный отрезок после выхода из блока
     */
    private fun needsHorizontalSegment(sourcePoint: Point, targetPoint: Point, source: System, target: System): Boolean {
        // Если выход горизонтальный (справа или слева)
        val isHorizontalExit = sourcePoint.x == source.x || sourcePoint.x == source.x + source.width
        
        // Всегда добавляем горизонтальный отрезок для горизонтальных выходов,
        // кроме случаев когда цель находится строго на той же горизонтали И очень близко
        val isSameHorizontalLevel = abs(sourcePoint.y - targetPoint.y) <= 5 // Очень маленькая погрешность
        val isVeryClose = abs(sourcePoint.x - targetPoint.x) < 50 // Очень близко по горизонтали
        
        return isHorizontalExit && !(isSameHorizontalLevel && isVeryClose)
    }
    
    /**
     * Создает маршрут с обязательным горизонтальным отрезком
     */
    private fun createRouteWithHorizontalSegment(sourcePoint: Point, targetPoint: Point, source: System, target: System): Pair<String, Point> {
        val horizontalLength = 30.0 // Длина горизонтального отрезка
        
        // Определяем направление горизонтального отрезка
        val isExitingRight = sourcePoint.x == source.x + source.width
        val horizontalEndX = if (isExitingRight) {
            sourcePoint.x + horizontalLength
        } else {
            sourcePoint.x - horizontalLength
        }
        
        // Создаем промежуточные точки
        val horizontalEndPoint = Point(x = horizontalEndX, y = sourcePoint.y)
        
        // Если цель уже на горизонтальном уровне, добавляем промежуточную вертикальную точку
        val waypoints = if (abs(sourcePoint.y - targetPoint.y) <= 5) {
            // Цель на том же уровне - только горизонтальный отрезок, затем к цели
            listOf(sourcePoint, horizontalEndPoint, targetPoint)
        } else {
            // Цель на другом уровне - горизонтальный отрезок, затем вертикальный, затем к цели
            val beforeTargetPoint = Point(x = horizontalEndX, y = targetPoint.y)
            listOf(sourcePoint, horizontalEndPoint, beforeTargetPoint, targetPoint)
        }
        
        // Строим путь
        val path = buildPathFromWaypoints(waypoints)
        
        // Улучшенный расчет позиции подписи - точно по середине всего пути
        val labelPosition = calculateMidpointOfPath(waypoints)
        
        return Pair(path, labelPosition)
    }

    private fun buildPathFromWaypoints(waypoints: List<Point>): String {
        if (waypoints.isEmpty()) return ""
        if (waypoints.size == 1) return "M ${waypoints[0].x},${waypoints[0].y}"
        
        val path = StringBuilder("M ${waypoints[0].x},${waypoints[0].y}")
        
        // Убираем дублирующиеся точки
        val uniqueWaypoints = waypoints.zipWithNext().filter { (current, next) ->
            current.x != next.x || current.y != next.y
        }.map { it.second }
        
        uniqueWaypoints.forEach { point ->
            path.append(" L ${point.x},${point.y}")
        }
        
        return path.toString()
    }
    
    /**
     * Рассчитывает точную середину пути по длине сегментов
     */
    private fun calculateMidpointOfPath(waypoints: List<Point>): Point {
        if (waypoints.size < 2) {
            return waypoints.firstOrNull() ?: Point(0.0, 0.0)
        }
        
        // Рассчитываем общую длину пути
        var totalLength = 0.0
        val segments = mutableListOf<Pair<Double, Pair<Point, Point>>>()
        
        for (i in 0 until waypoints.size - 1) {
            val start = waypoints[i]
            val end = waypoints[i + 1]
            val length = sqrt((end.x - start.x).pow(2) + (end.y - start.y).pow(2))
            segments.add(Pair(length, Pair(start, end)))
            totalLength += length
        }
        
        // Находим середину по длине
        val halfLength = totalLength / 2
        var currentLength = 0.0
        
        for ((segmentLength, segment) in segments) {
            if (currentLength + segmentLength >= halfLength) {
                // Середина находится в этом сегменте
                val remainingLength = halfLength - currentLength
                val ratio = if (segmentLength > 0) remainingLength / segmentLength else 0.0
                
                val start = segment.first
                val end = segment.second
                
                return Point(
                    x = start.x + (end.x - start.x) * ratio,
                    y = start.y + (end.y - start.y) * ratio
                )
            }
            currentLength += segmentLength
        }
        
        // Fallback - середина между первой и последней точкой
        val first = waypoints.first()
        val last = waypoints.last()
        return Point(
            x = (first.x + last.x) / 2,
            y = (first.y + last.y) / 2
        )
    }
}

/**
 * Вспомогательные классы
 */
data class RoutingRule(
    val name: String,
    val description: String,
    val condition: (System, System, RoutingContext) -> Boolean,
    val strategy: String
)

data class RoutingContext(
    val parallelContext: ParallelContext = ParallelContext(),
    val connectionData: Connection? = null,
    val spatial: SpatialInfo? = null,
    val connectionType: String = "unknown"
)

data class ParallelContext(
    val totalIncoming: Int = 1,
    val totalOutgoing: Int = 1,
    val incomingIndex: Int = 0,
    val outgoingIndex: Int = 0,
    val side: String? = null,
    val spatial: SpatialInfo? = null
)

data class RouteResult(
    val path: String,
    val labelPosition: Point
)
