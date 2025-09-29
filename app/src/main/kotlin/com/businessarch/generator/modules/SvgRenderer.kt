package com.businessarch.generator.modules

import com.businessarch.generator.model.*
import kotlin.math.floor
import kotlin.math.max

/**
 * Модуль для генерации SVG элементов бизнес-архитектуры
 * Отвечает за создание визуальных элементов диаграммы
 */
class SvgRenderer {
    private val asFixedWidth = 320.0
    private val maxFunctionTextWidth = 280.0
    private val functionHeight = 20.0

    /**
     * Генерирует иерархический SVG для бизнес-архитектуры
     */
    fun generateHierarchicalSVG(
        regions: List<Region>,
        connections: List<Connection>,
        systemMap: Map<String, System>,
        systemsWithoutRegion: List<System> = emptyList(),
        spatialAnalysis: Any? = null,
        platformsWithoutRegion: List<Platform> = emptyList()
    ): String {
        // Рассчитываем размеры canvas с учетом систем без региона
        var maxX = if (regions.isNotEmpty()) regions.maxOf { it.x + it.width } + 50 else 50.0
        var maxY = if (regions.isNotEmpty()) regions.maxOf { it.y + it.height } + 50 else 50.0

        // Учитываем системы без региона
        if (systemsWithoutRegion.isNotEmpty()) {
            val validSystems = systemsWithoutRegion.filter { it.x != 0.0 && it.y != 0.0 }
            if (validSystems.isNotEmpty()) {
                val maxSystemX = validSystems.maxOf { it.x + it.width }
                val maxSystemY = validSystems.maxOf { it.y + it.height }
                maxX = max(maxX, maxSystemX + 50)
                maxY = max(maxY, maxSystemY + 50)
            }
        }

        // Учитываем платформы без региона
        if (platformsWithoutRegion.isNotEmpty()) {
            val maxPlatformX = platformsWithoutRegion.maxOf { it.x + it.width }
            val maxPlatformY = platformsWithoutRegion.maxOf { it.y + it.height }
            maxX = max(maxX, maxPlatformX + 50)
            maxY = max(maxY, maxPlatformY + 50)
        }

        var svg = """<svg width="${maxX.toInt()}" height="${maxY.toInt()}" xmlns="http://www.w3.org/2000/svg">"""

        // Добавляем стили
        svg += getHierarchicalStyles()

        // Добавляем маркеры для стрелок
        svg += getArrowMarkers()

        // Сначала рендерим регионы (на заднем плане)
        regions.forEach { region ->
            svg += renderRegion(region)
        }

        // Рендерим платформы без региона
        platformsWithoutRegion.forEach { platform ->
            svg += renderPlatform(platform)
        }

        // Рендерим системы без региона напрямую (только если нет платформ)
        if (platformsWithoutRegion.isEmpty()) {
            systemsWithoutRegion.forEach { system ->
                svg += renderHierarchicalSystem(system)
            }
        }

        svg += "</svg>"
        
        return svg
    }

    /**
     * Генерирует маркеры для стрелок
     */
    fun getArrowMarkers(): String {
        return """
            <defs>
              <marker id="arrowhead-business" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto">
                <polygon points="0 0, 12 5, 0 10" fill="#999999" stroke="#999999" stroke-width="1" />
              </marker>
              <marker id="arrowhead-inter-region" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto">
                <polygon points="0 0, 12 5, 0 10" fill="#999999" stroke="#999999" stroke-width="1" />
              </marker>
              <marker id="arrowhead-inter-platform" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto">
                <polygon points="0 0, 12 5, 0 10" fill="#999999" stroke="#999999" stroke-width="1" />
              </marker>
              <marker id="arrowhead-intra-platform" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto">
                <polygon points="0 0, 12 5, 0 10" fill="#999999" stroke="#999999" stroke-width="1" />
              </marker>
              <marker id="arrowhead-external" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto">
                <polygon points="0 0, 12 5, 0 10" fill="#999999" stroke="#999999" stroke-width="1" />
              </marker>
              <marker id="arrowhead-horizontal" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto">
                <polygon points="0 0, 12 5, 0 10" fill="#333" stroke="#333" stroke-width="1" />
              </marker>
              <marker id="arrowhead-vertical" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto">
                <polygon points="0 0, 12 5, 0 10" fill="#333" stroke="#333" stroke-width="1" />
              </marker>
            </defs>
        """.trimIndent()
    }

    /**
     * Рендерит регион
     */
    private fun renderRegion(region: Region): String {
        var svg = """<g class="region">"""
        
        // Фон региона
        svg += """<rect x="${region.x}" y="${region.y}" width="${region.width}" height="${region.height}" class="region-bg" />"""
        
        // Заголовок региона
        svg += """<text x="${region.x + 15}" y="${region.y + 25}" class="region-title">${escapeXml(region.name)}</text>"""
        
        // Рендерим платформы
        region.platforms.forEach { platform ->
            svg += renderPlatform(platform)
        }
        
        svg += """</g>"""
        return svg
    }

    /**
     * Рендерит платформу
     */
    private fun renderPlatform(platform: Platform): String {
        var svg = """<g class="platform">"""
        
        // Фон платформы
        svg += """<rect x="${platform.x}" y="${platform.y}" width="${platform.width}" height="${platform.height}" class="platform-bg" />"""
        
        // Заголовок платформы
        svg += """<text x="${platform.x + 15}" y="${platform.y + 20}" class="platform-title">${escapeXml(platform.name)}</text>"""
        
        // Рендерим системы
        platform.systems.forEach { system ->
            svg += renderHierarchicalSystem(system)
        }
        
        svg += """</g>"""
        return svg
    }

    /**
     * Рендерит АС в иерархическом стиле
     */
    private fun renderHierarchicalSystem(system: System): String {
        val roleClass = getSystemClass(system)
        
        var svg = """<g class="system">"""
        
        // Основной прямоугольник системы
        svg += """<rect x="${system.x}" y="${system.y}" width="${system.width}" height="${system.height}" class="$roleClass" />"""
        
        // Заголовок системы с переносом если нужно
        val titleLines = wrapText(system.name, asFixedWidth - 20)
        titleLines.forEachIndexed { index, line ->
            svg += """<text x="${system.x + 10}" y="${system.y + 18 + index * 16}" class="business-system-title">${escapeXml(line)}</text>"""
        }
        
        // Блок функций
        if (system.functions.isNotEmpty()) {
            val titleHeight = titleLines.size * 16 + 10
            val functionsY = system.y + titleHeight + 10
            
            // Рассчитываем высоту блока функций
            var totalFunctionHeight = 35.0 // заголовок "Функции:" + больше отступа
            var currentY = functionsY + 30 // увеличенный отступ после заголовка
            
            system.functions.forEach { func ->
                val lines = wrapText(func.name, maxFunctionTextWidth)
                totalFunctionHeight += lines.size * functionHeight + 15 // увеличен отступ между функциями с 8px до 15px
            }
            
            // Фон блока функций
            svg += """<rect x="${system.x + 10}" y="$functionsY" width="${system.width - 20}" height="$totalFunctionHeight" class="functions-block" />"""
            
            // Заголовок блока функций
            svg += """<text x="${system.x + 15}" y="${functionsY + 20}" class="functions-title">Функции:</text>"""
            
            // Функции с переносом текста
            system.functions.forEach { func ->
                val lines = wrapText(func.name, maxFunctionTextWidth)
                val funcHeight = lines.size * functionHeight + 10 // увеличена внутренняя высота функции
                
                // Фон функции
                svg += """<rect x="${system.x + 15}" y="${currentY - 5}" width="${system.width - 30}" height="$funcHeight" class="function-item" />"""
                
                // Текст функции с переносом
                lines.forEachIndexed { lineIndex, line ->
                    svg += """<text x="${system.x + 20}" y="${currentY + 15 + lineIndex * functionHeight}" class="function-text">${escapeXml(line)}</text>"""
                }
                
                currentY += funcHeight + 15 // увеличен отступ между функциями с 8px до 15px
            }
        }
        
        svg += """</g>"""
        return svg
    }

    /**
     * Определяет CSS класс системы на основе роли
     */
    private fun getSystemClass(system: System): String {
        if (system.platform == "Внешняя АС") {
            return "business-system-external"
        }
        
        if (system.role.contains("product_fabric")) {
            return "business-system-product"
        }
        
        if (system.role.contains("sales_channel")) {
            return "business-system-sales"
        }
        
        return "business-system"
    }

    /**
     * Разбивает текст на строки для переноса
     */
    private fun wrapText(text: String, maxWidth: Double): List<String> {
        if (text.isEmpty()) return listOf("")
        
        val charWidth = 6
        val maxCharsPerLine = floor(maxWidth / charWidth).toInt()
        
        if (text.length <= maxCharsPerLine) {
            return listOf(text)
        }
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        words.forEach { word ->
            if ((currentLine + word).length > maxCharsPerLine && currentLine.isNotEmpty()) {
                lines.add(currentLine.trim())
                currentLine = "$word "
            } else {
                currentLine += "$word "
            }
        }
        
        if (currentLine.trim().isNotEmpty()) {
            lines.add(currentLine.trim())
        }
        
        return if (lines.isNotEmpty()) lines else listOf("")
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

    /**
     * Возвращает CSS стили для иерархической бизнес-архитектуры
     */
    private fun getHierarchicalStyles(): String {
        return """
            <style>
              .region-bg {
                fill: #ffffff;
                stroke: #333333;
                stroke-width: 3;
                stroke-dasharray: 8,4;
                rx: 12;
              }
              .region-title {
                font-family: Arial, sans-serif;
                font-size: 18px;
                font-weight: bold;
                fill: #333333;
              }
              .platform-bg {
                fill: #fff3e0;
                stroke: #ffcc02;
                stroke-width: 2;
                rx: 8;
              }
              .platform-title {
                font-family: Arial, sans-serif;
                font-size: 14px;
                font-weight: bold;
                fill: #e65100;
              }
              .business-system {
                fill: #f1f8e9;
                stroke: #8bc34a;
                stroke-width: 2;
                rx: 8;
                filter: drop-shadow(2px 2px 4px rgba(0,0,0,0.1));
              }
              .business-system-external {
                fill: #e8f5e8;
                stroke: #66bb6a;
                stroke-width: 2;
                rx: 8;
                filter: drop-shadow(2px 2px 4px rgba(0,0,0,0.1));
              }
              .business-system-product {
                fill: #e3f2fd;
                stroke: #42a5f5;
                stroke-width: 2;
                rx: 8;
                filter: drop-shadow(2px 2px 4px rgba(0,0,0,0.1));
              }
              .business-system-sales {
                fill: #fce4ec;
                stroke: #ec407a;
                stroke-width: 2;
                rx: 8;
                filter: drop-shadow(2px 2px 4px rgba(0,0,0,0.1));
              }
              .business-system-title {
                font-family: 'Segoe UI', Arial, sans-serif;
                font-size: 13px;
                font-weight: 600;
                fill: #37474f;
              }
              .functions-block {
                fill: #f3e5f5;
                stroke: #ba68c8;
                stroke-width: 1;
                rx: 6;
              }
              .functions-title {
                font-family: 'Segoe UI', Arial, sans-serif;
                font-size: 11px;
                font-weight: 600;
                fill: #6a1b9a;
              }
              .function-item {
                fill: #f8bbd9;
                stroke: #f48fb1;
                stroke-width: 1;
                rx: 4;
              }
              .function-text {
                font-family: 'Segoe UI', Arial, sans-serif;
                font-size: 10px;
                fill: #4a148c;
                font-weight: 500;
              }
              .business-connection {
                stroke: #999999;
                stroke-width: 2;
                fill: none;
                marker-end: url(#arrowhead-business);
                stroke-linecap: round;
                stroke-linejoin: round;
                stroke-dasharray: 5,5;
              }
              .business-connection-diagonal {
                stroke: #ff6b35;
                stroke-width: 2.5;
                fill: none;
                marker-end: url(#arrowhead-business);
                stroke-linecap: round;
                stroke-linejoin: round;
                stroke-dasharray: 8,3;
              }
              .business-connection-close {
                stroke: #4ecdc4;
                stroke-width: 3;
                fill: none;
                marker-end: url(#arrowhead-business);
                stroke-linecap: round;
                stroke-linejoin: round;
                stroke-dasharray: 3,2;
              }
              .business-connection-external {
                stroke: #999999;
                stroke-width: 2;
                fill: none;
                marker-end: url(#arrowhead-external);
                stroke-linecap: round;
                stroke-linejoin: round;
                stroke-dasharray: 5,5;
              }
              .connection-label-bg {
                fill: rgba(255,255,255,0.95);
                stroke: #666;
                stroke-width: 1;
                filter: drop-shadow(1px 1px 2px rgba(0,0,0,0.3));
              }
              .connection-label-text {
                font-family: Arial, sans-serif;
                font-size: 10px;
                font-weight: bold;
                fill: #333;
              }
            </style>
        """.trimIndent()
    }
}
