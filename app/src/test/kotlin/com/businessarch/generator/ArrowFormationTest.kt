package com.businessarch.generator

import com.businessarch.generator.model.*
import com.businessarch.generator.modules.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Тестирование корректности формирования стрелок и соединений
 * Группировка тестов по функциональности для лучшей читаемости
 */
@DisplayName("Тестирование формирования стрелок")
class ArrowFormationTest {

    private lateinit var connectionRouter: ConnectionRouter
    private lateinit var connectionManager: ConnectionManager
    private lateinit var svgRenderer: SvgRenderer

    @BeforeEach
    fun setUp() {
        connectionRouter = ConnectionRouter()
        connectionManager = ConnectionManager(connectionRouter)
        svgRenderer = SvgRenderer()
    }
    
    @Test
    fun `проверка формирования прямой стрелочки между соседними системами`() {
        // Подготавливаем данные
        val sourceSystem = System(
            id = "sys-a",
            name = "Система A",
            platform = "Платформа 1",
            region = "Регион 1",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 100.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val targetSystem = System(
            id = "sys-b", 
            name = "Система B",
            platform = "Платформа 1",
            region = "Регион 1",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 350.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val connection = Connection(
            source = "Система A",
            target = "Система B",
            description = "Тестовая связь"
        )
        
        // Инициализируем пространственную карту
        connectionRouter.initializeSpatialMap(listOf(sourceSystem, targetSystem), emptyList())
        
        // Создаем контекст маршрутизации
        val context = RoutingContext()
        
        // Создаем маршрут
        val route = connectionRouter.createRoute(sourceSystem, targetSystem, context)
        
        // Проверяем результат
        assertNotNull(route.path)
        assertTrue(route.path.startsWith("M "))
        assertTrue(route.path.contains("L "))
        assertNotNull(route.labelPosition)
        
        println("✅ Путь стрелочки: ${route.path}")
        println("✅ Позиция подписи: (${route.labelPosition.x}, ${route.labelPosition.y})")
    }
    
    @Test
    fun `проверка формирования стрелочки с горизонтальным отрезком`() {
        val sourceSystem = System(
            id = "sys-a",
            name = "Система A",
            platform = "Платформа 1",
            region = "Регион 1",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 100.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val targetSystem = System(
            id = "sys-b",
            name = "Система B",
            platform = "Платформа 2",
            region = "Регион 2",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 400.0
            y = 250.0  // Смещена по вертикали
            width = 200.0
            height = 100.0
        }
        
        connectionRouter.initializeSpatialMap(listOf(sourceSystem, targetSystem), emptyList())
        
        val context = RoutingContext()
        val route = connectionRouter.createRoute(sourceSystem, targetSystem, context)
        
        // Проверяем, что путь содержит несколько точек (горизонтальный отрезок)
        assertNotNull(route.path)
        val pathParts = route.path.split(" L ")
        assertTrue(pathParts.size > 2, "Путь должен содержать горизонтальный отрезок")
        
        println("✅ Путь с горизонтальным отрезком: ${route.path}")
    }
    
    @Test
    fun `проверка маркеров для разных типов связей`() {
        val internalSystem = System(
            id = "internal-sys",
            name = "Внутренняя система",
            platform = "Платформа 1",
            region = "Регион 1",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 100.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val externalSystem = System(
            id = "bank-sys",
            name = "Банк",
            platform = "Внешняя АС",
            region = "Внешние системы",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 400.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        // Тестируем внутреннюю связь
        val internalConnection = Connection(
            source = "Система A",
            target = "Система B",
            description = "Внутренняя связь"
        )
        
        val internalSvg = connectionManager.renderBusinessConnection(
            internalSystem, internalSystem, internalConnection
        )
        
        assertTrue(internalSvg.contains("arrowhead-business"))
        assertFalse(internalSvg.contains("arrowhead-external"))
        
        // Тестируем внешнюю связь
        val externalConnection = Connection(
            source = "Внутренняя система",
            target = "Банк",
            description = "Внешняя связь"
        )
        
        val externalSvg = connectionManager.renderBusinessConnection(
            internalSystem, externalSystem, externalConnection
        )
        
        assertTrue(externalSvg.contains("arrowhead-external"))
        assertFalse(externalSvg.contains("arrowhead-business"))
        
        println("✅ Внутренняя связь: ${internalSvg.contains("arrowhead-business")}")
        println("✅ Внешняя связь: ${externalSvg.contains("arrowhead-external")}")
    }
    
    @Test
    fun `проверка CSS классов для разных типов связей`() {
        val system1 = System(
            id = "sys-1",
            name = "Система 1",
            platform = "Платформа 1",
            region = "Регион 1",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 100.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val externalSystem = System(
            id = "ext-sys",
            name = "Внешняя система",
            platform = "Внешняя АС",
            region = "Внешние системы",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 400.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val connection = Connection(
            source = "Система 1",
            target = "Внешняя система",
            description = "Связь с внешней системой"
        )
        
        val svg = connectionManager.renderBusinessConnection(system1, externalSystem, connection)
        
        // Проверяем правильность CSS класса для внешней связи
        assertTrue(svg.contains("business-connection-external"))
        
        println("✅ CSS класс для внешней связи: business-connection-external")
    }
    
    @Test
    fun `проверка корректности определения маркеров в SVG`() {
        val markers = svgRenderer.getArrowMarkers()
        
        // Проверяем наличие всех необходимых маркеров
        assertTrue(markers.contains("arrowhead-business"))
        assertTrue(markers.contains("arrowhead-external"))
        assertTrue(markers.contains("arrowhead-inter-region"))
        assertTrue(markers.contains("arrowhead-inter-platform"))
        assertTrue(markers.contains("arrowhead-intra-platform"))
        assertTrue(markers.contains("arrowhead-horizontal"))
        assertTrue(markers.contains("arrowhead-vertical"))
        
        // Проверяем корректность параметров маркеров
        assertTrue(markers.contains("markerWidth=\"12\""))
        assertTrue(markers.contains("markerHeight=\"10\""))
        assertTrue(markers.contains("refX=\"11\""))
        assertTrue(markers.contains("refY=\"5\""))
        assertTrue(markers.contains("orient=\"auto\""))
        
        println("✅ Все маркеры стрелок корректно определены")
    }
    
    @Test
    fun `проверка формирования подписей к стрелочкам`() {
        val sourceSystem = System(
            id = "source-sys",
            name = "Источник",
            platform = "Платформа 1",
            region = "Регион 1",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 100.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val targetSystem = System(
            id = "target-sys",
            name = "Цель",
            platform = "Платформа 1",
            region = "Регион 1",
            role = emptyList(),
            functions = mutableListOf()
        ).apply {
            x = 400.0
            y = 100.0
            width = 200.0
            height = 100.0
        }
        
        val connection = Connection(
            source = "Источник",
            target = "Цель",
            description = "Передача данных для обработки"
        )
        
        val svg = connectionManager.renderBusinessConnection(sourceSystem, targetSystem, connection)
        
        println("🔍 Сгенерированный SVG: $svg")
        
        // Проверяем наличие элементов подписи
        assertTrue(svg.contains("connection-label-bg"), "SVG должен содержать connection-label-bg")
        assertTrue(svg.contains("connection-label-text"), "SVG должен содержать connection-label-text")
        // Текст обрезается до 20 символов, поэтому проверяем сокращенную версию
        assertTrue(svg.contains("Передача данных для ..."), "SVG должен содержать обрезанное описание связи")
        
        println("✅ Подпись к стрелочке корректно сформирована")
    }
}
