# Использование

## Команды

```bash
# Демо (встроенные данные)
./gradlew run

# JSON файл из корня проекта
./gradlew run --args="../your-file.json"

# JSON файл в папке app
./gradlew run --args="your-file.json"

# Полный путь
./gradlew run --args="/full/path/to/file.json"

# Тесты
./gradlew test
```

## Результат

SVG файлы создаются в `app/`:
- `kotlin-business-architecture.svg` (демо)
- `output-business-architecture.svg` (JSON файл)

## Пример JSON

```json
{
  "AS": [
    {
      "id": "crm",
      "name": "CRM Система",
      "platform": "Клиентская платформа",
      "region": "Фронт-офис",
      "role": ["sales_channel"]
    }
  ],
  "Function": [
    {
      "id": "f1",
      "name": "Управление клиентами",
      "AS": "CRM Система"
    }
  ],
  "Link": [
    {
      "source": {"AS": "CRM Система"},
      "target": {"AS": "ERP Система"},
      "description": "Передача данных"
    }
  ]
}
```

## Возможности

✅ Автоматическая компоновка по регионам/платформам  
✅ Пространственный анализ связей  
✅ Умная маршрутизация соединений  
✅ Поддержка функций в системах  
✅ Различные стили для типов систем  
✅ Подробная статистика