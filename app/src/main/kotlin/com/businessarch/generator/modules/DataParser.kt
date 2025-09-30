package com.businessarch.generator.modules

import com.businessarch.generator.model.*
import kotlin.math.floor
import kotlin.math.max

/**
 * Модуль для парсинга и обработки входных данных бизнес-архитектуры
 * Отвечает за преобразование JSON данных в структуру для генерации диаграммы
 */
class DataParser {
    // Параметры размещения для расчета размеров АС
    private val asFixedWidth = 320.0
    private val asMinHeight = 80.0
    private val maxFunctionTextWidth = 280.0
    private val functionHeight = 20.0

    /**
     * Парсит JSON данные бизнес-архитектуры
     */
    fun parseBusinessData(data: BusinessData): ParsedBusinessData {
        val regions = mutableListOf<Region>()
        val connections = mutableListOf<Connection>()
        val systemsWithoutRegion = mutableListOf<System>() // Системы без региона

        // Создаем структуру регионов
        val regionMap = mutableMapOf<String, Region>()
        val platformMap = mutableMapOf<String, Platform>()
        val systemMap = mutableMapOf<String, System>()

        // Парсим автоматизированные системы
        data.AS.forEach { asData ->
            val system = System(
                id = asData.id,
                name = asData.name,
                platform = asData.platform ?: "Не указана",
                region = asData.region, // Не создаем автоматически "Общий регион"
                role = asData.role,
                functions = mutableListOf(),
                functionalPlatforms = mutableListOf(),
                x = 0.0,
                y = 0.0,
                width = asFixedWidth,
                height = asMinHeight
            )

            // Добавляем прямые функции к системе (функции без FP)
            data.Function.forEach { func ->
                if (func.AS == asData.name && func.FP.isEmpty()) {
                    system.functions.add(
                        Function(
                            id = func.id,
                            name = func.name,
                            type = func.type
                        )
                    )
                }
            }

            // Парсим функциональные платформы для данной AS
            data.FP.forEach { fpData ->
                if (fpData.AS == asData.name) {
                    val functionalPlatform = FunctionalPlatform(
                        id = fpData.id,
                        name = fpData.name,
                        functions = mutableListOf()
                    )

                    // Добавляем функции к функциональной платформе
                    data.Function.forEach { func ->
                        if (func.FP == fpData.name) {
                            functionalPlatform.functions.add(
                                Function(
                                    id = func.id,
                                    name = func.name,
                                    type = func.type
                                )
                            )
                        }
                    }

                    system.functionalPlatforms.add(functionalPlatform)
                }
            }

            // Рассчитываем размер АС на основе функций и FP
            calculateSystemSize(system)
            systemMap[system.name] = system

            // Только для систем с указанным регионом создаем платформы и регионы
            if (system.region != null) {
                // Создаем или находим платформу
                val platformKey = "${system.region}::${system.platform}"
                val platform = platformMap.getOrPut(platformKey) {
                    Platform(
                        name = system.platform,
                        region = system.region,
                        systems = mutableListOf(),
                        x = 0.0,
                        y = 0.0,
                        width = 0.0,
                        height = 0.0
                    )
                }
                platform.systems.add(system)

                // Создаем или находим регион
                val region = regionMap.getOrPut(system.region) {
                    Region(
                        name = system.region,
                        platforms = mutableListOf(),
                        x = 0.0,
                        y = 0.0,
                        width = 0.0,
                        height = 0.0
                    )
                }
            } else {
                // Добавляем систему без региона в отдельный список
                systemsWithoutRegion.add(system)
            }
        }

        // Группируем платформы по регионам
        platformMap.values.forEach { platform ->
            val region = regionMap[platform.region]
            if (region != null && region.platforms.none { it.name == platform.name }) {
                region.platforms.add(platform)
            }
        }

        // Преобразуем в список
        regions.addAll(regionMap.values)

        // Парсим связи (поддерживаем и Link и Links)
        val linksData = data.Link + data.Links
        
        linksData.forEach { link ->
            if (link.source.AS.isNotEmpty() && link.target.AS.isNotEmpty()) {
                connections.add(
                    Connection(
                        source = link.source.AS,
                        target = link.target.AS,
                        type = "business",
                        description = link.description
                    )
                )
            }
        }

        return ParsedBusinessData(regions, connections, systemMap, systemsWithoutRegion)
    }

    /**
     * Рассчитывает размер АС на основе количества функций и FP
     */
    private fun calculateSystemSize(system: System) {
        // Используем фиксированную ширину для всех АС
        system.width = asFixedWidth

        // Рассчитываем высоту заголовка с учетом переноса
        val titleLines = wrapText(system.name, asFixedWidth - 20)
        val titleHeight = titleLines.size * 16 + 10

        // Рассчитываем высоту блока прямых функций системы
        var directFunctionsBlockHeight = 0.0
        if (system.functions.isNotEmpty()) {
            directFunctionsBlockHeight = 25.0 // заголовок "Функции:"

            system.functions.forEach { func ->
                val lines = wrapText(func.name, maxFunctionTextWidth)
                directFunctionsBlockHeight += lines.size * functionHeight + 8 // 8px между функциями
            }

            directFunctionsBlockHeight += 10 // отступ снизу блока функций
        }

        // Рассчитываем высоту функциональных платформ
        var fpBlockHeight = 0.0
        if (system.functionalPlatforms.isNotEmpty()) {
            system.functionalPlatforms.forEach { fp ->
                fpBlockHeight += 25.0 // заголовок FP

                fp.functions.forEach { func ->
                    val lines = wrapText(func.name, maxFunctionTextWidth - 20) // уменьшаем ширину для отступа
                    fpBlockHeight += lines.size * (functionHeight - 2) + 6 // чуть меньше высоты для функций в FP
                }

                fpBlockHeight += 15.0 // отступ между FP
            }
        }

        // Общая высота = заголовок + прямые функции + FP + минимальные отступы
        val totalHeight = titleHeight + directFunctionsBlockHeight + fpBlockHeight + 20 // 20px - общие отступы
        
        system.height = max(totalHeight, asMinHeight)
    }

    /**
     * Разбивает текст на строки для переноса
     */
    fun wrapText(text: String, maxWidth: Double): List<String> {
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
}

/**
 * Результат парсинга бизнес-данных
 */
data class ParsedBusinessData(
    val regions: List<Region>,
    val connections: List<Connection>,
    val systemMap: Map<String, System>,
    val systemsWithoutRegion: List<System>
)
