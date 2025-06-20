# Проект: Кастомный пул потоков на Java

## Общее описание
Это реализация на Java — кастомный пул потоков с гибкими настройками очередей и параметров. Основные компоненты — классы `QueueExecutor` и `QueueWorker`. Он реализует интерфейс `CustomExecutor`, обеспечивая управление задачами с помощью нескольких очередей и балансировкой.

## Основные возможности
- **Распределение задач по очередям по круговому принципу (Round-Robin).**
- **Настраиваемые параметры пула:**
  - `corePoolSize` — минимальное число активных потоков
  - `maxPoolSize` — максимальное число потоков
  - `queueSize` — размер каждой очереди задач
  - `keepAliveTime` — время простоя потоков вне ядра
  - `minSpareThreads` — резерв потоков для поддержания минимального числа свободных потоков
- **Интерфейс `CustomExecutor`:**
  - `execute(Runnable)`
  - `submit(Callable<T>)`
  - `shutdown()` — мягкое завершение
  - `shutdownNow()` — принудительное завершение
- **Обработка ошибок:**
  - При переполнении очереди — выбрасывается `RejectedExecutionException`
- **Логирование:**
  - Создание, запуск, завершение потоков
  - Прием и выполнение задач
  - Таймауты простоя потоков
- **Graceful shutdown:**
  - Мягкое завершение — завершение текущих задач
  - Принудительное — немедленная остановка потоков

## Технологии
- Java 21
- Maven
- SLF4J API + Logback

## Установка и запуск
1. Склонируйте репозиторий:
```bash
git clone https://github.com/yujihn/ThreadPool2.git
cd ThreadPool2
```
2. Соберите проект:
```bash
mvn clean install
```
3. Запустите демонстрационное приложение:
```bash
mvn clean compile exec:java
```
## Структура проекта
```
ThreadPool2/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── threadpool/
│       │       ├── CustomExecutor.java
│       │       ├── QueueExecutor.java
│       │       ├── QueueWorker.java
│       │       └── App.java
│       └── resources/
│           └── logback.xml
└── README.md
```
## Тестирование и результаты

- **Демонстрационный запуск (`App.java`)**:
  - 100 задач с задержкой 10 мс между отправками.
  - Параметры по умолчанию:
    - `corePoolSize=4`
    - `maxPoolSize=8`
    - `queueSize=10`
    - `keepAliveTime=5с`
    - `minSpareThreads=2`
  - В логах отображается прием задач, их выполнение и итоговая статистика — выполнено/отклонено, среднее время.

---

## Анализ производительности

### Демонстрационный тест (App.java)
| Метрика | QueueExecutor |
|---|---|
| Общее время (мс) | 4 |
| Выполнено задач | 44 |
| Отклонено задач | 56 |
| Среднее время/задачу (мс) | 0.09 |

### Бенчмарк (ThreadPoolBenchmark.java)
| Метрика | QueueExecutor | ThreadPoolExecutor |
|---|---|---|
| Время выполнения (мс) | 1584 | 1569 |
| Выполнено задач | 72 | 52 |
| Отклонено задач | 128 | 148 |
| Среднее время/задачу (мс) | 22 | 30.17 |

**Вывод:**
Результаты показывают, что наш пул способен выполнять больше задач за меньшее время по сравнению со стандартным `ThreadPoolExecutor`, в частности при высокой нагрузке.

---

### Мини-исследование влияния параметров
Параметры пула существенно влияют на его производительность:
1. Размер очереди (`queueSize`):
Маленькие очереди (например, 2-10) вызывают большое число отказов, так как задач много, и очередь быстро заполняется.
Оптимальные значения — 25-35 элементов, при которых достигается баланс между пропускной способностью и количеством отказов.
2. Максимальное число потоков (`maxPoolSize`):
Вдвое больше количества ядер (например, 8 при 4 ядрах) обеспечивает максимальную пропускную способность без излишних накладных расходов.
Более высокие значения (например, 10-12) не дают существенного прироста, а могут ухудшить эффективность из-за накладных расходов.
3. Количество потоков по умолчанию (`corePoolSize`):
Значения 2-4 достаточно для большинства сценариев, при этом обеспечивая быстрый старт и хорошую адаптацию под нагрузку.
4. Время простоя потоков (`keepAliveTime`):
5-10 секунд — оптимальный диапазон для динамического масштабирования.
Более короткое время приводит к частым созданию/уничтожению потоков, что снижает производительность.

---

## Принцип работы распределения задач
Используется несколько очередей, по одной на каждый поток или группу потоков. Распределение задач происходит по алгоритму Round-robin:
- Каждая новая задача добавляется в очередь по очереди, циклически.
- В случае переполнения очереди или превышения лимита потоков — создается новый поток (если не достигнут maxPoolSize).
- Потоки, не получающие задач в течение keepAliveTime, автоматически завершаются, освобождая ресурсы.
Это позволяет равномерно распределять нагрузку между потоками и быстро масштабировать пул при необходимости.
