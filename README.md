# JProfiler Demos & MCP Profiling Suite

Комплекс демонстрационных Java-приложений для профилирования производительности, анализа узких мест CPU и исследования дампов памяти через **JProfiler MCP Server**.

---

## 📂 Структура проекта

- [`src/com/demo/DemoApp.java`](src/com/demo/DemoApp.java) — сценарии CPU-нагрузки:
  - Неэффективная сборка строк и регулярные выражения
  - Вычисление SHA-256 хешей
  - Квадратичная сортировка $O(N^2)$ (Bubble Sort)
  - Интенсивная аллокация объектов
  - Многопоточная нагрузка (`ThreadPoolExecutor` + `CountDownLatch`)
- [`src/com/demo/MemoryLeakDemo.java`](src/com/demo/MemoryLeakDemo.java) — сценарий накопления объектов в памяти и генерации `.hprof` дампа (Dominator Tree / Retained Size).

---

## 🚀 Быстрый старт

### 1. Компиляция
```bash
mkdir -p bin
javac -d bin src/com/demo/*.java
```

### 2. Запуск CPU-профилирования (JFR)
```bash
java -XX:StartFlightRecording=filename=demo_profile.jfr,settings=profile -cp bin com.demo.DemoApp
```

### 3. Генерация Heap Dump (.hprof)
```bash
java -cp bin com.demo.MemoryLeakDemo
```

---

## 🤖 Профилирование через JProfiler MCP

### Анализ CPU
1. **`load_snapshot`**: `{ "filePath": "/path/to/demo_profile.jfr" }`
2. **`check_status`**: ожидание статуса `data_ready`.
3. **`get_performance_hotspots`**: `{ "subsystem": "cpu", "view": "hotspots", "packageFilter": "com.demo" }`
4. **`expand_performance_hotspot`**: детализация трассировок по `id`.

### Анализ Памяти (Heap Dump)
1. **`load_snapshot`**: `{ "filePath": "/path/to/heap_demo.hprof" }`
2. **`check_status`**: подтверждение загрузки.
3. **`get_heap_data`**: `{ "view": "biggest_objects" }`
4. **`get_heap_data`**: `{ "view": "retained_classes", "biggestObjectId": 1 }`
