package com.businessarch.generator

import com.businessarch.generator.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File

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
            val businessData = Json.decodeFromString<BusinessData>(jsonContent)
            
            println("📁 Загружены данные из: $jsonFilePath")
            
            val result = generator.generateBusinessArchitecture(businessData)
            
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
    private fun createTestBusinessData(): BusinessData {
        return BusinessData(
            AS = listOf(
                AutomatedSystem(
                    id = "crm",
                    name = "CRM Система",
                    platform = "Клиентская платформа",
                    region = "Фронт-офис",
                    role = listOf("sales_channel")
                ),
                AutomatedSystem(
                    id = "erp",
                    name = "ERP Система",
                    platform = "Корпоративная платформа", 
                    region = "Бэк-офис",
                    role = listOf("product_fabric")
                ),
                AutomatedSystem(
                    id = "billing",
                    name = "Биллинговая система",
                    platform = "Финансовая платформа",
                    region = "Бэк-офис",
                    role = listOf()
                ),
                AutomatedSystem(
                    id = "external_bank",
                    name = "Банковская система",
                    platform = "Внешняя АС",
                    region = null,
                    role = listOf()
                )
            ),
            Function = listOf(
                BusinessFunction(
                    id = "f1",
                    name = "Управление клиентами",
                    type = "business",
                    AS = "CRM Система"
                ),
                BusinessFunction(
                    id = "f2", 
                    name = "Обработка заказов",
                    type = "business",
                    AS = "CRM Система"
                ),
                BusinessFunction(
                    id = "f3",
                    name = "Управление складом",
                    type = "business", 
                    AS = "ERP Система"
                ),
                BusinessFunction(
                    id = "f4",
                    name = "Финансовый учет",
                    type = "business",
                    AS = "ERP Система"
                ),
                BusinessFunction(
                    id = "f5",
                    name = "Выставление счетов",
                    type = "business",
                    AS = "Биллинговая система"
                )
            ),
            Link = listOf(
                SystemLink(
                    source = LinkTarget(AS = "CRM Система"),
                    target = LinkTarget(AS = "ERP Система"),
                    description = "Передача заказов"
                ),
                SystemLink(
                    source = LinkTarget(AS = "ERP Система"),
                    target = LinkTarget(AS = "Биллинговая система"),
                    description = "Данные для биллинга"
                ),
                SystemLink(
                    source = LinkTarget(AS = "Биллинговая система"),
                    target = LinkTarget(AS = "Банковская система"),
                    description = "Платежные операции"
                )
            )
        )
    }
}

fun main(args: Array<String>) = runBlocking {
    val app = App()
    
    if (args.isNotEmpty()) {
        // Если передан аргумент - обрабатываем файл
        app.loadAndProcess(args[0])
    } else {
        // Иначе запускаем демо
        app.runDemo()
    }
}
