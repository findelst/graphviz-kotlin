package com.businessarch.generator

import com.businessarch.generator.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Главное приложение для генерации бизнес-архитектуры на Kotlin
 */
class App {
    private val generator = BusinessArchitectureGenerator()
    
    /**
     * Демонстрационный пример работы генератора
     */
    fun runDemo() {
        println("🚀 Запуск генератора бизнес-архитектуры (Kotlin)")
        
        // Создаем тестовые данные
        val testData = createTestBusinessData()
        
        // Генерируем архитектуру
        val result = generator.generateBusinessArchitecture(testData)
        
        if (result.success && result.data != null) {
            println("✅ Генерация успешно завершена!")
            println("📊 Статистика:")
            println("  - Систем: ${result.data.stats.systems}")
            println("  - Связей: ${result.data.stats.connections}")
            println("  - Регионов: ${result.data.stats.regions}")
            println("  - Платформ: ${result.data.stats.platforms}")
            
            // Сохраняем SVG
            val filename = generator.exportToSVG(result.data.svg, "kotlin-business-architecture.svg")
            println("💾 SVG сохранен: $filename")
            
        } else {
            println("❌ Ошибка генерации: ${result.error}")
        }
    }
    
    /**
     * Загружает данные из JSON файла
     */
    fun loadAndProcess(jsonFilePath: String) {
        try {
            val jsonContent = File(jsonFilePath).readText()
            val archResult = json.decodeFromString<ArchResult>(jsonContent)

            println("📁 Загружены данные из: $jsonFilePath")

            val result = generator.generateBusinessArchitecture(archResult)

            if (result.success && result.data != null) {
                println("✅ Обработка завершена!")
                val filename = generator.exportToSVG(result.data.svg, "output-business-architecture.svg")
                println("💾 Результат сохранен: $filename")
            } else {
                println("❌ Ошибка обработки: ${result.error}")
            }

        } catch (e: Exception) {
            println("❌ Ошибка загрузки файла: ${e.message}")
        }
    }
    
    /**
     * Создает тестовые данные для демонстрации
     */
    private fun createTestBusinessData(): ArchResult {
        return ArchResult(
            Var = emptyMap(),
            AS = listOf(
                AsObject(
                    id = "crm",
                    name = "CRM Система",
                    platform = "Клиентская платформа",
                    role = listOf("sales_channel")
                ),
                AsObject(
                    id = "erp",
                    name = "ERP Система",
                    platform = "Корпоративная платформа",
                    role = listOf("product_fabric")
                ),
                AsObject(
                    id = "billing",
                    name = "Биллинговая система",
                    platform = "Финансовая платформа",
                    role = listOf()
                ),
                AsObject(
                    id = "external_bank",
                    name = "Банковская система",
                    platform = "Внешняя АС",
                    role = null
                )
            ),
            function = listOf(
                FunctionObject(
                    id = "f1",
                    name = "Управление клиентами",
                    asName = "CRM Система"
                ),
                FunctionObject(
                    id = "f2",
                    name = "Обработка заказов",
                    asName = "CRM Система"
                ),
                FunctionObject(
                    id = "f3",
                    name = "Управление складом",
                    asName = "ERP Система"
                ),
                FunctionObject(
                    id = "f4",
                    name = "Финансовый учет",
                    asName = "ERP Система"
                ),
                FunctionObject(
                    id = "f5",
                    name = "Выставление счетов",
                    asName = "Биллинговая система"
                )
            ),
            link = listOf(
                LinkObject(
                    id = "l1",
                    source = LinkEnd(type = "AS", name = "CRM Система"),
                    target = LinkEnd(type = "AS", name = "ERP Система")
                ),
                LinkObject(
                    id = "l2",
                    source = LinkEnd(type = "AS", name = "ERP Система"),
                    target = LinkEnd(type = "AS", name = "Биллинговая система")
                ),
                LinkObject(
                    id = "l3",
                    source = LinkEnd(type = "AS", name = "Биллинговая система"),
                    target = LinkEnd(type = "AS", name = "Банковская система")
                )
            )
        )
    }
}

fun main(args: Array<String>) = runBlocking {
    val app = App()
    val filePath = if (args.isNotEmpty()) args[0] else "../answers_transformed_full_direct_result.json"
    app.loadAndProcess(filePath)
}
