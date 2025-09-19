# Business Architecture Generator

Генератор диаграмм бизнес-архитектуры на Kotlin с использованием Gradle.

## Описание

Проект создает SVG диаграммы на основе JSON описания автоматизированных систем, их функций и связей между ними. Использует современную архитектуру на Kotlin с поддержкой пространственного анализа и умной маршрутизации соединений.

## Быстрый старт

### Требования
- Java 21+
- Gradle 8.5+ (включен Gradle Wrapper)

### Установка и запуск

```bash
# Клонирование проекта
git clone <repository-url>
cd graphviz

# Демо режим (встроенные тестовые данные)
./gradlew run

# Обработка JSON файла
./gradlew run --args="../your-file.json"

# Запуск тестов
./gradlew test
```

### Результаты
SVG файлы создаются в папке `app/`:
- `kotlin-business-architecture.svg` (для демо)
- `output-business-architecture.svg` (для JSON файла)

## Формат входных данных

```json
{
  "AS": [
    {
      "id": "unique_id",
      "name": "Название системы",
      "platform": "Название платформы", 
      "region": "Название региона",
      "role": ["роль1", "роль2"]
    }
  ],
  "Function": [
    {
      "id": "func_id",
      "name": "Название функции",
      "type": "business",
      "AS": "Название системы"
    }
  ],
  "Link": [
    {
      "source": {"AS": "Система источник"},
      "target": {"AS": "Система назначение"},
      "description": "Описание связи"
    }
  ]
}
```

## Возможности

- ✅ **Парсинг JSON** данных бизнес-архитектуры
- ✅ **Иерархическая компоновка** (регионы → платформы → системы)
- ✅ **Пространственный анализ** связей между системами
- ✅ **Умная маршрутизация** соединений с обходом препятствий
- ✅ **SVG генерация** с полной стилизацией
- ✅ **Поддержка функций** внутри систем
- ✅ **Различные стили** для типов систем
- ✅ **Асинхронная обработка** с корутинами

## Архитектура

Проект следует принципам **SOLID** и состоит из модулей:

- **DataParser** - парсинг JSON данных и создание внутренних структур
- **LayoutManager** - управление размещением элементов на диаграмме
- **SpatialAnalyzer** - анализ пространственных отношений между системами
- **ConnectionRouter** - создание оптимальных маршрутов для соединений
- **SvgRenderer** - генерация SVG элементов
- **ConnectionManager** - управление отрисовкой связей

## Программное использование

```kotlin
import com.businessarch.generator.*
import kotlinx.coroutines.runBlocking

val generator = BusinessArchitectureGenerator()
val businessData = // ... ваши данные

runBlocking {
    val result = generator.generateBusinessArchitecture(businessData)
    
    if (result.success && result.data != null) {
        generator.exportToSVG(result.data.svg, "output.svg")
        println("Систем: ${result.data.stats.systems}")
        println("Связей: ${result.data.stats.connections}")
    }
}
```

## Преимущества Kotlin реализации

- ✅ **Строгая типизация** - ошибки обнаруживаются на этапе компиляции
- ✅ **Null safety** - защита от NullPointerException
- ✅ **Корутины** - современный подход к асинхронности
- ✅ **Data классы** - иммутабельные структуры данных
- ✅ **Модульная архитектура** - четкое разделение ответственности
- ✅ **JVM экосистема** - совместимость с Java библиотеками

## Тестирование

```bash
./gradlew test --info
```

Проект включает unit тесты для основных компонентов и интеграционные тесты полного цикла генерации.

## Лицензия

MIT License