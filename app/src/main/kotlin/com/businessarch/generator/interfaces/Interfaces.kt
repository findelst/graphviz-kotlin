package com.businessarch.generator.interfaces

import com.businessarch.generator.model.*
import com.businessarch.generator.modules.ParsedBusinessData
import com.businessarch.generator.modules.SpatialAnalysisResult

/**
 * Интерфейсы для улучшения архитектуры и тестируемости
 */

/**
 * Стратегия размещения элементов на диаграмме
 */
interface LayoutStrategy {
    /**
     * Применяет компоновку к списку регионов
     * @param regions Список регионов для размещения
     */
    fun applyLayout(regions: List<Region>)

    /**
     * Размещает системы без региона
     * @param systems Системы без региона
     * @param regions Существующие регионы для определения позиции
     */
    fun layoutSystemsWithoutRegion(systems: List<System>, regions: List<Region>)
}

/**
 * Рендерер соединений между системами
 */
interface ConnectionRenderer {
    /**
     * Отрисовывает соединение между двумя системами
     * @param source Система-источник
     * @param target Система-назначение
     * @param connection Данные о соединении
     * @return SVG строка с отрисованным соединением
     */
    fun renderConnection(source: System, target: System, connection: Connection): String

    /**
     * Анализирует все соединения для группировки параллельных связей
     * @param connections Список всех соединений
     * @param systemMap Карта систем для поиска
     * @return Результат группировки соединений
     */
    fun analyzeConnections(connections: List<Connection>, systemMap: Map<String, System>): ConnectionGroups
}

/**
 * Парсер входных данных
 */
interface DataProcessor {
    /**
     * Обрабатывает входные данные бизнес-архитектуры
     * @param data Сырые данные из JSON
     * @return Обработанные данные для генерации диаграммы
     */
    fun processBusinessData(data: BusinessData): ParsedBusinessData

    /**
     * Валидирует корректность входных данных
     * @param data Данные для валидации
     * @return Результат валидации с ошибками, если есть
     */
    fun validateData(data: BusinessData): ValidationResult
}

/**
 * Анализатор пространственных отношений
 */
interface SpatialProcessor {
    /**
     * Анализирует пространственные отношения всех соединений
     * @param systems Список всех систем
     * @param connections Список всех соединений
     * @return Результат пространственного анализа
     */
    fun analyzeSpatialRelations(systems: List<System>, connections: List<Connection>): SpatialAnalysisResult

    /**
     * Выводит отчет о пространственном анализе
     * @param analysis Результаты анализа
     */
    fun printAnalysisReport(analysis: SpatialAnalysisResult)
}

/**
 * Результат валидации данных
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Группы соединений для параллельной отрисовки
 */
data class ConnectionGroups(
    val incoming: Map<String, ConnectionGroup>,
    val outgoing: Map<String, ConnectionGroup>
)

/**
 * Группа соединений для одной системы
 */
data class ConnectionGroup(
    val connections: List<Connection>,
    val totalIncoming: Int,
    val totalOutgoing: Int,
    val side: String?
)
