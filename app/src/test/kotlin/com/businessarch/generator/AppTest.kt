package com.businessarch.generator

import com.businessarch.generator.model.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class AppTest {
    
    @Test 
    fun `test business architecture generation`() = runBlocking {
        val app = App()
        val generator = BusinessArchitectureGenerator()
        
        // Создаем тестовые данные
        val testData = BusinessData(
            AS = listOf(
                AutomatedSystem(
                    id = "test1",
                    name = "Тестовая система 1",
                    platform = "Тестовая платформа",
                    region = "Тестовый регион",
                    role = listOf("test")
                ),
                AutomatedSystem(
                    id = "test2", 
                    name = "Тестовая система 2",
                    platform = "Тестовая платформа",
                    region = "Тестовый регион",
                    role = listOf("test")
                )
            ),
            Function = listOf(
                BusinessFunction(
                    id = "f1",
                    name = "Тестовая функция",
                    type = "test",
                    AS = "Тестовая система 1"
                )
            ),
            Link = listOf(
                SystemLink(
                    source = LinkTarget(AS = "Тестовая система 1"),
                    target = LinkTarget(AS = "Тестовая система 2"),
                    description = "Тестовая связь"
                )
            )
        )
        
        // Генерируем архитектуру
        val result = generator.generateBusinessArchitecture(testData)
        
        // Проверяем результат
        assertTrue(result.success, "Генерация должна быть успешной")
        assertNotNull(result.data, "Данные результата не должны быть null")
        
        val data = result.data!!
        assertEquals(2, data.stats.systems, "Должно быть 2 системы")
        assertEquals(1, data.stats.connections, "Должна быть 1 связь")
        assertEquals(1, data.stats.regions, "Должен быть 1 регион")
        assertTrue(data.svg.isNotEmpty(), "SVG не должен быть пустым")
        assertTrue(data.svg.contains("<svg"), "SVG должен содержать тег svg")
    }
    
    @Test
    fun `test data parser`() {
        val parser = com.businessarch.generator.modules.DataParser()
        
        val testData = BusinessData(
            AS = listOf(
                AutomatedSystem(
                    id = "sys1",
                    name = "Система 1",
                    platform = "Платформа А",
                    region = "Регион 1"
                )
            ),
            Function = listOf(
                BusinessFunction(
                    id = "func1",
                    name = "Функция 1",
                    AS = "Система 1"
                )
            ),
            Link = emptyList()
        )
        
        val result = parser.parseBusinessData(testData)
        
        assertEquals(1, result.regions.size, "Должен быть 1 регион")
        assertEquals(1, result.systemMap.size, "Должна быть 1 система")
        assertEquals(0, result.connections.size, "Должно быть 0 связей")
        assertTrue(result.systemsWithoutRegion.isEmpty(), "Не должно быть систем без региона")
    }
    
    @Test
    fun `test text wrapping`() {
        val parser = com.businessarch.generator.modules.DataParser()
        
        // Тест короткого текста
        val shortText = parser.wrapText("Короткий", 100.0)
        assertEquals(1, shortText.size)
        assertEquals("Короткий", shortText[0])
        
        // Тест длинного текста
        val longText = parser.wrapText("Очень длинный текст который должен быть разбит на несколько строк", 50.0)
        assertTrue(longText.size > 1, "Длинный текст должен быть разбит на несколько строк")
    }
}
