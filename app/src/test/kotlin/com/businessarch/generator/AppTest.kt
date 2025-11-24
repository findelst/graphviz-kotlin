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
        val testData = ArchResult(
            Var = emptyMap(),
            AS = listOf(
                AsObject(
                    id = "test1",
                    name = "Тестовая система 1",
                    platform = "Тестовая платформа",
                    role = listOf("test")
                ),
                AsObject(
                    id = "test2",
                    name = "Тестовая система 2",
                    platform = "Тестовая платформа",
                    role = listOf("test")
                )
            ),
            function = listOf(
                FunctionObject(
                    id = "f1",
                    name = "Тестовая функция",
                    asName = "Тестовая система 1"
                )
            ),
            link = listOf(
                LinkObject(
                    id = "l1",
                    source = LinkEnd(type = "AS", name = "Тестовая система 1"),
                    target = LinkEnd(type = "AS", name = "Тестовая система 2")
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
        assertEquals(0, data.stats.regions, "Регионы больше не используются")
        assertTrue(data.svg.isNotEmpty(), "SVG не должен быть пустым")
        assertTrue(data.svg.contains("<svg"), "SVG должен содержать тег svg")
    }
    
    @Test
    fun `test data parser`() {
        val parser = com.businessarch.generator.modules.DataParser()
        
        val testData = ArchResult(
            Var = emptyMap(),
            AS = listOf(
                AsObject(
                    id = "sys1",
                    name = "Система 1",
                    platform = "Платформа А"
                )
            ),
            function = listOf(
                FunctionObject(
                    id = "func1",
                    name = "Функция 1",
                    asName = "Система 1"
                )
            ),
            link = emptyList()
        )

        val result = parser.parseBusinessData(testData)

        assertEquals(1, result.platforms.size, "Должна быть 1 платформа")
        assertEquals(1, result.systemMap.size, "Должна быть 1 система")
        assertEquals(0, result.connections.size, "Должно быть 0 связей")
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
