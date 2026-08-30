# Java Profiling Suite & MCP Demos

Комплекс демонстрационных Java-приложений для сравнительного анализа производительности, поиска узких мест CPU, исследования аллокаций, задержек рантайма и дампов памяти через **JProfiler MCP Server**, **Async-Profiler** и модернизированный **jHiccup**.

---

## 📂 Структура проекта

- [`src/com/demo/DemoApp.java`](src/com/demo/DemoApp.java) — комплекс сценариев нагрузки:
  - **CPU Bottlenecks:** криптографический хеширующий цикл (SHA-256) и квадратичная сортировка $O(N^2)$ (Bubble Sort).
  - **Memory Churn:** неэффективная конкатенация строк (`+`), регулярные выражения и высокочастотная генерация `UUID` / `byte[]`.
  - **Concurrency:** параллельные вычисления в `ThreadPoolExecutor` с синхронизацией через `CountDownLatch`.
- [`src/com/demo/MemoryLeakDemo.java`](src/com/demo/MemoryLeakDemo.java) — сценарий накопления объектов в памяти и генерации `.hprof` дампа кучи (анализ Dominator Tree / Retained Size).
- [`src/com/demo/JHiccupStressTest.java`](src/com/demo/JHiccupStressTest.java) — стресс-тест для профилирования оверхеда самого агента `jHiccup`.
- [`jHiccup.jar`](jHiccup.jar) — готовый скомпилированный агент jHiccup с поддержкой Java 8–25+, `LockSupport.parkNanos` и авто-флашем.
- [`jhiccup_dpx_report.html`](jhiccup_dpx_report.html) — интерактивный отчет статического анализа архитектуры DPX-Java.

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

## 📈 Режим 3: Измерение пауз и задержек через модернизированный jHiccup

[jHiccup](https://github.com/bivex/jHiccup) измеряет задержки (Hiccups), вызванные паузами GC (Stop-The-World), деоптимизациями JIT, троттлингом ОС и переключением контекста ядра.

### 3.1 Запуск с агентом jHiccup
```bash
java -javaagent:jHiccup.jar="-d 0 -i 200 -l hiccup_demo.hlog" -cp bin com.demo.DemoApp
```

### 3.2 Генерация отчета по перцентилям (HdrHistogram)
```bash
java -cp jHiccup.jar org.jhiccup.internal.hdrhistogram.HistogramLogProcessor \
     -i hiccup_demo.hlog -o hiccup_summary.hgrm
```

### 3.3 Визуализация в HTML5 плоттере
Откройте [`/Volumes/External/Code/jHiccup/jHiccupPlotter.html`](file:///Volumes/External/Code/jHiccup/jHiccupPlotter.html) в любом браузере и перетащите файл `hiccup_demo.hlog` или `hiccup_summary.hgrm.hgrm`.

---

## ⚖️ Сравнение инструментов профилирования

| Функция | **JProfiler MCP** | **Async-Profiler** | **jHiccup** |
|---|---|---|---|
| **Главная цель** | Архитектурный анализ, CPU Hotspots, Heap Dominators | CPU сэмплинг, C/C++ стек, скорость аллокаций | Измерение задержек (Latency) и пауз GC (Jitter) |
| **Уровень деталей** | Высокоуровневый Java-код, подсистемы (JDBC/HTTP/Kafka) | Полный стек (Java + JNI + C++ Runtime + JIT) | Точное распределение задержек (p50...p99.99, max) |
| **Анализ памяти** | **Heap Dump (State):** сколько удерживается объектами | **Alloc Profiling (Rate):** сколько выделяется байт | Фиксация Stop-The-World пауз сборщика мусора |
| **Интерфейс** | JSON-RPC через протокол MCP для AI-агентов | FlameGraphs HTML, flat-дампы | HdrHistogram перцентили, веб-дашборд HTML5 |
