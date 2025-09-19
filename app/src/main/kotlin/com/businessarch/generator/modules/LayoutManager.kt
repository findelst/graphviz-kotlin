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
     * Размещает системы без региона
     */
    fun layoutSystemsWithoutRegion(systemsWithoutRegion: List<System>, regions: List<Region>) {
        if (systemsWithoutRegion.isEmpty()) return

        // Находим максимальную X координату существующих регионов
        var maxRegionX = 50.0 // Минимальное значение по умолчанию
        regions.forEach { region ->
            maxRegionX = max(maxRegionX, region.x + region.width)
        }

        // Размещаем системы без региона справа от всех регионов
        var currentY = 50.0
        val startX = maxRegionX + 50

        systemsWithoutRegion.forEach { system ->
            system.x = startX
            system.y = currentY
            currentY += system.height + 30 // Отступ между системами
        }
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
