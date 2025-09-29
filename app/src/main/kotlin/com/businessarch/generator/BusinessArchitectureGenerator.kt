package com.businessarch.generator

import com.businessarch.generator.model.*
import com.businessarch.generator.modules.*
import java.io.File

/**
 * Главный класс для генерации бизнес-архитектуры
 * Координирует работу всех модулей согласно принципу SRP
 */
class BusinessArchitectureGenerator {
    // Инициализируем модули
    private val dataParser = DataParser()
    private val layoutManager = LayoutManager()
    private val svgRenderer = SvgRenderer()
    private val connectionRouter = ConnectionRouter()
    private val connectionManager = ConnectionManager(connectionRouter)
    private val spatialAnalyzer = SpatialAnalyzer()

    /**
     * Генерирует схему бизнес-архитектуры
     */
    fun generateBusinessArchitecture(businessData: BusinessData): GenerationResult {
        return try {
            println("Генерируем бизнес-архитектуру...")
            
            // Парсим данные с помощью DataParser
            val parsedData = dataParser.parseBusinessData(businessData)
            val totalSystems = parsedData.systemMap.size
            println("Найдено АС: $totalSystems, связей: ${parsedData.connections.size}, регионов: ${parsedData.regions.size}, систем без региона: ${parsedData.systemsWithoutRegion.size}")
            
            // Применяем компоновку с помощью LayoutManager
            layoutManager.applyHierarchicalLayout(parsedData.regions)
            val platformsWithoutRegion = layoutManager.layoutSystemsWithoutRegion(parsedData.systemsWithoutRegion, parsedData.regions)
            println("Применена иерархическая компоновка бизнес-архитектуры")
            
            // Инициализируем пространственную карту в ConnectionRouter
            val allSystems = parsedData.systemMap.values.toList() + parsedData.systemsWithoutRegion
            connectionRouter.initializeSpatialMap(allSystems, parsedData.regions)
            
            // Выполняем пространственный анализ связей
            val spatialAnalysis = spatialAnalyzer.analyzeAllConnections(allSystems, parsedData.connections)
            spatialAnalyzer.printAnalysisReport(spatialAnalysis)
            
            // Генерируем SVG с учетом пространственного анализа
            val svg = generateHierarchicalSVG(
                regions = parsedData.regions,
                connections = spatialAnalysis.connections,
                systemMap = parsedData.systemMap,
                systemsWithoutRegion = parsedData.systemsWithoutRegion,
                spatialAnalysis = spatialAnalysis,
                platformsWithoutRegion = platformsWithoutRegion
            )
            println("SVG бизнес-архитектуры сгенерирован")
            
            GenerationResult(
                success = true,
                data = GenerationData(
                    svg = svg,
                    stats = GenerationStats(
                        systems = totalSystems,
                        connections = parsedData.connections.size,
                        regions = parsedData.regions.size,
                        platforms = layoutManager.getTotalPlatformCount(parsedData.regions)
                    )
                )
            )
            
        } catch (error: Exception) {
            println("Ошибка в generateBusinessArchitecture: ${error.message}")
            GenerationResult(
                success = false,
                error = "Ошибка генерации бизнес-архитектуры: ${error.message}"
            )
        }
    }

    /**
     * Генерирует иерархический SVG для бизнес-архитектуры
     */
    private fun generateHierarchicalSVG(
        regions: List<Region>,
        connections: List<Connection>,
        systemMap: Map<String, System>,
        systemsWithoutRegion: List<System> = emptyList(),
        spatialAnalysis: SpatialAnalysisResult? = null,
        platformsWithoutRegion: List<Platform> = emptyList()
    ): String {
        // Генерируем базовый SVG с помощью SvgRenderer
        val svg = svgRenderer.generateHierarchicalSVG(regions, connections, systemMap, systemsWithoutRegion, spatialAnalysis, platformsWithoutRegion)
        
        // Добавляем связи в SVG
        val svgWithoutClosingTag = svg.replace("</svg>", "")
        var connectionsInSvg = ""
        
        // Анализируем все соединения для группировки параллельных стрелочек
        val connectionGroups = connectionManager.analyzeConnections(connections, systemMap)
        
        // Затем рендерим связи (поверх всех элементов) с учетом пространственного анализа
        connections.forEachIndexed { _, conn ->
            val sourceSystem = systemMap[conn.source]
            val targetSystem = systemMap[conn.target]
            
            if (sourceSystem != null && targetSystem != null) {
                val incomingContext = connectionGroups.incoming[conn.target]
                val outgoingContext = connectionGroups.outgoing[conn.source]
                
                val incomingIndex = incomingContext?.connections?.indexOfFirst { it.source == conn.source } ?: 0
                val outgoingIndex = outgoingContext?.connections?.indexOfFirst { it.target == conn.target } ?: 0
                
                // Добавляем информацию о пространственном анализе
                val contextWithSpatial = ParallelContext(
                    totalIncoming = incomingContext?.totalIncoming ?: 1,
                    totalOutgoing = outgoingContext?.totalOutgoing ?: 1,
                    incomingIndex = incomingIndex,
                    outgoingIndex = outgoingIndex,
                    side = incomingContext?.side,
                    spatial = conn.spatial
                )
                
                connectionsInSvg += connectionManager.renderBusinessConnection(sourceSystem, targetSystem, conn, contextWithSpatial)
            }
        }

        return "$svgWithoutClosingTag$connectionsInSvg</svg>"
    }

    /**
     * Простой экспорт в SVG файл (основной метод экспорта)
     */
    fun exportToSVG(svg: String, filename: String = "business-architecture.svg"): String {
        return try {
            File(filename).writeText(svg)
            println("✅ SVG экспорт завершен: $filename")
            filename
        } catch (error: Exception) {
            println("❌ Ошибка экспорта SVG: ${error.message}")
            throw error
        }
    }
}
