package com.businessarch.generator.model

/**
 * Константы и перечисления для улучшения читаемости кода
 */

/**
 * Типы соединений между системами
 */
enum class ConnectionType {
    /** Связь внутри одной системы */
    INTERNAL,
    /** Связь с внешней системой */
    EXTERNAL,
    /** Связь между системами разных регионов */
    INTER_REGION,
    /** Связь между системами разных платформ */
    INTER_PLATFORM,
    /** Связь внутри одной платформы */
    INTRA_PLATFORM
}

/**
 * Константы для размещения элементов
 */
enum class LayoutConstants(val value: Double) {
    AS_SPACING(150.0),
    PLATFORM_SPACING(250.0),
    REGION_SPACING(300.0),
    REGION_PADDING(40.0),
    PLATFORM_PADDING(30.0),
    AS_PADDING(20.0),
    FUNCTIONS_PADDING(10.0),
    REGION_HEADER_HEIGHT(40.0),
    PLATFORM_HEADER_HEIGHT(40.0),
    SYSTEM_HEADER_HEIGHT(35.0),
    FUNCTION_HEIGHT(30.0),
    FUNCTION_SPACING(15.0),
    MIN_PLATFORM_WIDTH(200.0),
    MIN_SYSTEM_HEIGHT(80.0),
    INITIAL_REGION_X(50.0),
    INITIAL_REGION_Y(50.0),
    REGION_MARGIN(50.0),
    SYSTEM_VERTICAL_MARGIN(30.0)
}

/**
 * Направления для пространственного анализа
 */
enum class Direction {
    NORTH, SOUTH, EAST, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST
}

/**
 * Маркеры для стрелок в SVG
 */
enum class ArrowMarker(val id: String) {
    BUSINESS("arrowhead-business"),
    EXTERNAL("arrowhead-external"),
    INTER_REGION("arrowhead-inter-region"),
    INTER_PLATFORM("arrowhead-inter-platform"),
    INTRA_PLATFORM("arrowhead-intra-platform"),
    HORIZONTAL("arrowhead-horizontal"),
    VERTICAL("arrowhead-vertical")
}
