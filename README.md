# Java Profiling Suite & MCP Demos

Комплекс демонстрационных Java-приложений для сравнительного анализа производительности, поиска узких мест CPU, исследования аллокаций и дампов памяти через **JProfiler MCP Server** и **Async-Profiler**.

---

## 📂 Структура проекта

- [`src/com/demo/DemoApp.java`](src/com/demo/DemoApp.java) — комплекс сценариев нагрузки:
  - **CPU Bottlenecks:** криптографический хеширующий цикл (SHA-256) и квадратичная сортировка $O(N^2)$ (Bubble Sort).
  - **Memory Churn:** неэффективная конкатенация строк (`+`), регулярные выражения и высокочастотная генерация `UUID` / `byte[]`.
  - **Concurrency:** параллельные вычисления в `ThreadPoolExecutor` с синхронизацией через `CountDownLatch`.
- [`src/com/demo/MemoryLeakDemo.java`](src/com/demo/MemoryLeakDemo.java) — сценарий накопления объектов в памяти и генерации `.hprof` дампа кучи (анализ Dominator Tree / Retained Size).

---

## 🚀 Сборка

```bash
mkdir -p bin
javac -d bin src/com/demo/*.java
```

---

## 🔬 Режим 1: Профилирование через JProfiler MCP

JProfiler MCP Server (`jpmcp`) поддерживает анализ снимков Java Flight Recorder (`.jfr`), дампов кучи (`.hprof`) и собственных снимков JProfiler (`.jps`).

### 1.1 CPU Профилирование (JFR)
Запуск с генерацией JFR-снимка:
```bash
java -XX:StartFlightRecording=filename=demo_profile.jfr,settings=profile -cp bin com.demo.DemoApp
```

Последовательность вызовов MCP:
1. **`load_snapshot`**:
   ```json
   { "filePath": "/absolute/path/to/demo_profile.jfr" }
   ```
2. **`check_status`**: опрос до получения `"status": "data_ready"`.
3. **`get_performance_hotspots`** (поиск узких мест в коде проекта):
   ```json
   { "subsystem": "cpu", "view": "hotspots", "packageFilter": "com.demo" }
   ```
4. **`expand_performance_hotspot`** (детализация обратного стека вызовов):
   ```json
   { "id": 1 }
   ```
5. **Дерево вызовов (Call Tree)**:
   ```json
   { "subsystem": "cpu", "view": "call_tree" }
   ```

### 1.2 Анализ утечек памяти (Heap Dump)
Генерация дампа кучи:
```bash
java -cp bin com.demo.MemoryLeakDemo
```

Последовательность вызовов MCP:
1. **`load_snapshot`**: `{ "filePath": "/absolute/path/to/heap_demo.hprof" }`
2. **`check_status`**: проверка готовности подсистемы `heap_dump`.
3. **`get_heap_data` (Доминаторы кучи)**:
   ```json
   { "view": "biggest_objects" }
   ```
4. **`get_heap_data` (Удерживаемая память конкретного объекта)**:
   ```json
   { "view": "retained_classes", "biggestObjectId": 1 }
   ```

---

## 🔥 Режим 2: Профилирование через Async-Profiler

[Async-Profiler](https://github.com/async-profiler/async-profiler) использует `AsyncGetCallTrace` и аппаратные счетчики, захватывая нативный стек (C/C++, JNI, JVM runtime) без Safepoint Bias.

### 2.1 CPU Профилирование (Текстовый отчет и сэмплы)
```bash
java -agentpath:/opt/homebrew/Cellar/async-profiler/4.5/lib/libasyncProfiler.dylib=start,event=cpu,file=async_cpu.txt,flat=25 \
     -cp bin com.demo.DemoApp
```

### 2.2 Генерация интерактивного FlameGraph (HTML)
```bash
java -agentpath:/opt/homebrew/Cellar/async-profiler/4.5/lib/libasyncProfiler.dylib=start,event=cpu,file=flamegraph.html \
     -cp bin com.demo.DemoApp
```

### 2.3 Профилирование потока аллокаций памяти (`event=alloc`)
```bash
java -agentpath:/opt/homebrew/Cellar/async-profiler/4.5/lib/libasyncProfiler.dylib=start,event=alloc,file=async_alloc.txt \
     -cp bin com.demo.DemoApp
```

---

## ⚖️ Сравнение JProfiler MCP и Async-Profiler

| Функция | **JProfiler MCP** | **Async-Profiler** |
|---|---|---|
| **Уровень детализации** | Высокоуровневый Java-стек, фильтрация по пакетам, бизнес-подсистемы | Низкоуровневый стек (Java + JNI + C/C++ runtime + JVM internals) |
| **Safepoint Bias** | Минимален при использовании JFR | Полностью отсутствует |
| **Анализ памяти** | **Heap Dump (State):** сколько памяти *удерживается живыми объектами* прямо сейчас | **Alloc Profiling (Rate):** сколько памяти *было выделено суммарно* в рантайме |
| **Интерфейс** | JSON-RPC через протокол MCP для AI-агентов | CLI, FlameGraphs HTML, JFR, консольные дампы |
| **Сравнительный анализ** | Поддержка `baselineFilePath` для A/B сравнения дельт | `jfr-conv` / FlameGraph diff |
