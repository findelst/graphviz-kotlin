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
    fun parseBusinessData(data: ArchResult): ParsedBusinessData {
        val platforms = mutableListOf<Platform>()
        val connections = mutableListOf<Connection>()

        val platformMap = mutableMapOf<String, Platform>()
        val systemMap = mutableMapOf<String, System>()

        // Парсим автоматизированные системы
        data.AS.forEach { asData ->
            val system = System(
                id = asData.id,
                name = asData.name,
                platform = asData.platform ?: "Не указана",
                role = asData.role ?: emptyList(),
                functions = mutableListOf(),
                functionalPlatforms = mutableListOf(),
                x = 0.0,
                y = 0.0,
                width = asFixedWidth,
                height = asMinHeight
            )

            // Добавляем прямые функции к системе (функции без FP)
            data.function.forEach { func ->
                if (func.asName == asData.name && (func.fpName == null || func.fpName.isEmpty())) {
                    system.functions.add(
                        Function(
                            id = func.id,
                            name = func.name,
                            type = null
                        )
                    )
                }
            }

            // Парсим функциональные платформы для данной AS
            data.FP.forEach { fpData ->
                if (fpData.asName == asData.name) {
                    val functionalPlatform = FunctionalPlatform(
                        id = fpData.id,
                        name = fpData.name,
                        functions = mutableListOf()
                    )

                    // Добавляем функции к функциональной платформе
                    data.function.forEach { func ->
                        // Проверяем что функция принадлежит данной FP и данной AS
                        if (func.fpName == fpData.name && func.asName == asData.name) {
                            functionalPlatform.functions.add(
                                Function(
                                    id = func.id,
                                    name = func.name,
                                    type = null
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

            // Создаем или находим платформу (без региона)
            val platform = platformMap.getOrPut(system.platform) {
                Platform(
                    name = system.platform,
                    systems = mutableListOf(),
                    x = 0.0,
                    y = 0.0,
                    width = 0.0,
                    height = 0.0
                )
            }
            platform.systems.add(system)
        }

        // Преобразуем в список
        platforms.addAll(platformMap.values)

        // Парсим связи
        data.link.forEach { link ->
            val sourceName = getSystemName(link.source, data)
            val targetName = getSystemName(link.target, data)

            if (sourceName != null && targetName != null) {
                connections.add(
                    Connection(
                        source = sourceName,
                        target = targetName,
                        type = "business",
                        description = null
                    )
                )
            }
        }

        return ParsedBusinessData(platforms, connections, systemMap)
    }

    /**
     * Получает имя системы из LinkEnd
     */
    private fun getSystemName(linkEnd: LinkEnd, data: ArchResult): String? {
        return when (linkEnd.type) {
            "AS" -> linkEnd.name ?: linkEnd.asName
            "FP" -> {
                // Находим AS для данной FP
                val fp = data.FP.find { it.name == linkEnd.name }
                fp?.asName
            }
            else -> null
        }
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
            directFunctionsBlockHeight = 35.0 // заголовок "Функции:" + отступ (синхронизировано с SvgRenderer)

            system.functions.forEach { func ->
                val lines = wrapText(func.name, maxFunctionTextWidth)
                val funcHeight = lines.size * functionHeight + 10 // высота одной функции
                directFunctionsBlockHeight += funcHeight + 15 // 15px между функциями (синхронизировано с SvgRenderer)
            }

            directFunctionsBlockHeight += 10 // отступ снизу блока функций
        }

        // Рассчитываем высоту функциональных платформ (синхронизировано с SvgRenderer)
        var fpBlockHeight = 0.0
        if (system.functionalPlatforms.isNotEmpty()) {
            system.functionalPlatforms.forEach { fp ->
                // Рассчитываем высоту заголовка FP с учетом переноса
                val titleLines = wrapText(fp.name, maxFunctionTextWidth - 10)
                val titleHeight = titleLines.size * 16 + 10

                // Общая высота FP блока = заголовок + дополнительный отступ + функции
                var totalFpHeight = titleHeight + 10.0

                fp.functions.forEach { func ->
                    val lines = wrapText(func.name, maxFunctionTextWidth - 40) // больше отступ внутри FP
                    totalFpHeight += lines.size * (functionHeight - 2) + 15 // высота функции + отступ (синхронизировано с SvgRenderer)
                }

                if (fp.functions.isNotEmpty()) {
                    totalFpHeight += 10 // дополнительный отступ снизу
                }

                fpBlockHeight += totalFpHeight + 15.0 // отступ между FP (синхронизировано с SvgRenderer)
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
    val platforms: List<Platform>,
    val connections: List<Connection>,
    val systemMap: Map<String, System>
)
