package com.businessarch.generator.modules

import com.businessarch.generator.model.*
import kotlin.math.max

/**
 * Модуль для управления компоновкой и размещением элементов бизнес-архитектуры
 * Отвечает за расчет позиций регионов, платформ и систем
 */
class LayoutManager {
    // Параметры размещения
    private val asSpacing = 150.0
    private val platformSpacing = 250.0
    private val regionSpacing = 300.0

    /**
     * Применяет иерархическую компоновку для бизнес-архитектуры
     */
    fun applyHierarchicalLayout(regions: List<Region>) {
        val regionPadding = 40.0
        val platformPadding = 30.0
        val asPadding = 20.0
        val functionsPadding = 10.0
        
        var currentRegionX = 50.0

        regions.forEach { region ->
            var currentPlatformY = regionPadding + 40 // Место для заголовка региона
            var maxRegionWidth = 0.0

            region.platforms.forEach { platform ->
                var currentASY = currentPlatformY + 40 // Место для заголовка платформы
                var maxPlatformWidth = 0.0
                var totalPlatformHeight = 0.0

                platform.systems.forEach { system ->
                    // Размещаем функции внутри АС с увеличенными отступами
                    if (system.functions.isNotEmpty()) {
                        // Рассчитываем высоту с учетом новых отступов: 
                        // заголовок (35px) + каждая функция (30px) + отступы между функциями (15px)
                        val functionsBlockHeight = 35 + system.functions.size * 30 + (system.functions.size - 1) * 15 + functionsPadding * 2
                        system.height = max(system.height, 80.0 + functionsBlockHeight)
                    }

                    // Размещаем АС вертикально внутри платформы
                    system.x = currentRegionX + platformPadding
                    system.y = currentASY
                    
                    currentASY += system.height + asPadding
                    maxPlatformWidth = max(maxPlatformWidth, system.width + platformPadding * 2)
                    totalPlatformHeight += system.height + asPadding
                }

                platform.x = currentRegionX + regionPadding / 2
                platform.y = currentPlatformY
                platform.width = max(maxPlatformWidth, 200.0)
                platform.height = totalPlatformHeight + 50 // Заголовок + отступы

                currentPlatformY += platform.height + 30
                maxRegionWidth = max(maxRegionWidth, platform.width + regionPadding)
            }

            region.x = currentRegionX
            region.y = 50.0
            region.width = maxRegionWidth
            region.height = currentPlatformY - 50 + regionPadding

            currentRegionX += region.width + 50
        }
    }

    /**
     * Размещает системы без региона, группируя их по платформам
     * @return Список созданных платформ для систем без регионов
     */
    fun layoutSystemsWithoutRegion(systemsWithoutRegion: List<System>, regions: List<Region>): List<Platform> {
        if (systemsWithoutRegion.isEmpty()) return emptyList()

        // Находим максимальную X координату существующих регионов
        var maxRegionX = 50.0 // Минимальное значение по умолчанию
        regions.forEach { region ->
            maxRegionX = max(maxRegionX, region.x + region.width)
        }

        // Группируем системы по платформам
        val systemsByPlatform = systemsWithoutRegion.groupBy { it.platform }

        var currentPlatformX = maxRegionX + 50 // Начальная позиция для первой платформы
        val platformY = 50.0 // Одинаковая Y координата для всех платформ
        val platformPadding = 30.0
        val systemPadding = 20.0

        val createdPlatforms = mutableListOf<Platform>()

        systemsByPlatform.forEach { (platformName, systems) ->
            // Размещаем системы внутри платформы вертикально
            var currentSystemY = platformY + 40 // Место для заголовка платформы
            var maxSystemWidth = 0.0
            var totalPlatformHeight = 0.0

            systems.forEach { system ->
                // Обновляем высоту системы с учетом функций
                if (system.functions.isNotEmpty()) {
                    val functionsBlockHeight = 35 + system.functions.size * 30 + (system.functions.size - 1) * 15 + 10 * 2
                    system.height = max(system.height, 80.0 + functionsBlockHeight)
                }

                system.x = currentPlatformX + platformPadding
                system.y = currentSystemY

                currentSystemY += system.height + systemPadding
                maxSystemWidth = max(maxSystemWidth, system.width)
                totalPlatformHeight += system.height + systemPadding
            }

            // Вычисляем размеры платформы
            val platformWidth = max(maxSystemWidth + platformPadding * 2, 200.0)
            val platformHeight = totalPlatformHeight + 50 // Заголовок + отступы

            // Создаем объект Platform
            val platform = Platform(
                name = platformName,
                systems = systems.toMutableList(),
                x = currentPlatformX,
                y = platformY,
                width = platformWidth,
                height = platformHeight
            )

            createdPlatforms.add(platform)

            // Переходим к следующей платформе
            currentPlatformX += platformWidth + 50
        }

        return createdPlatforms
    }

    /**
     * Группирует системы по платформам
     */
    fun groupByPlatform(systems: List<System>): Map<String, List<System>> {
        return systems.groupBy { it.platform }
    }

    /**
     * Возвращает общее количество платформ
     */
    fun getTotalPlatformCount(regions: List<Region>): Int {
        return regions.sumOf { it.platforms.size }
    }
}
