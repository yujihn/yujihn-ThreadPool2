package threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс кастомного пула потоков, реализующий интерфейс CustomExecutor.
 * Обеспечивает управление задачами через собственную очередь и потоки-воркеры.
 */
public class QueueExecutor implements CustomExecutor {

    private static final Logger logger = LoggerFactory.getLogger(QueueExecutor.class);

    // Основные параметры пула потоков
    private final int corePoolSize;      // минимальное число потоков
    private final int maxPoolSize;       // максимум потоков
    private final int queueSize;         // размер очереди задач
    private final long keepAliveTime;    // время жизни потоков вне ядра
    private final TimeUnit timeUnit;     // единица времени
    private final int minSpareThreads;    // минимальное число свободных потоков (не используется явно в коде)

    // Списки потоков-воркеров и очередей задач
    private final List<QueueWorker> workers;
    private final List<BlockingQueue<Runnable>> taskQueues;

    // Индекс для балансировки задач по очередям
    private final AtomicInteger queueIndex = new AtomicInteger(0);

    // Флаг завершения работы пула
    private volatile boolean isShutdown = false;

    /**
     * Конструктор, инициализирующий пул с заданными параметрами.
     */
    public QueueExecutor(int corePoolSize, int maxPoolSize, int queueSize,
                         long keepAliveTime, TimeUnit timeUnit, int minSpareThreads) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueSize = queueSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.minSpareThreads = minSpareThreads;

        this.taskQueues = new ArrayList<>();
        this.workers = new ArrayList<>();

        logger.info("Инициализация пула потоков: core={}, max={}, queueSize={}, keepAlive={} {}",
                corePoolSize, maxPoolSize, queueSize, keepAliveTime, timeUnit.name());

        // Создаем начальное количество воркеров (по умолчанию corePoolSize)
        for (int i = 0; i < corePoolSize; i++) {
            createWorker(i);
        }
    }

    /**
     * Создает и запускает нового воркера (поток) с собственной очередью.
     */
    private void createWorker(int index) {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
        QueueWorker worker = new QueueWorker(queue, index, keepAliveTime, timeUnit);
        taskQueues.add(queue);
        workers.add(worker);
        new Thread(worker, "Worker-" + index).start(); // старт потока
    }

    /**
     * Метод для отправки задач на выполнение.
     * Балансировка по очередям с помощью round-robin.
     */
    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("Пул потоков завершён, задача отклонена.");
        }

        // Выбираем очередь по кругу
        int index = queueIndex.getAndIncrement() % taskQueues.size();
        BlockingQueue<Runnable> queue = taskQueues.get(index);

        // Пытаемся добавить задачу в очередь
        if (!queue.offer(command)) {
            throw new RejectedExecutionException("Очередь переполнена, задача отклонена.");
        }

        logger.debug("Задача отправлена в очередь {}", index);
    }

    /**
     * Метод для отправки задач с возвратом Future.
     */
    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        if (isShutdown) {
            throw new RejectedExecutionException("Пул потоков завершён, задача отклонена.");
        }

        // Оборачиваем Callable в FutureTask
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask); // отправляем задачу в очередь
        return futureTask; // возвращаем Future для получения результата
    }

    /**
     * Мягкая остановка пула — запрещает новые задачи, но текущие продолжают выполняться.
     */
    @Override
    public void shutdown() {
        isShutdown = true;
        logger.info("Пул переводится в режим завершения (shutdown)");
    }

    /**
     * Принудительное завершение — останавливает все потоки немедленно.
     */
    @Override
    public void shutdownNow() {
        isShutdown = true;
        logger.warn("Принудительное завершение всех потоков (shutdownNow)");
        for (QueueWorker worker : workers) {
            worker.stop(); // остановка каждого воркера
        }
    }

    /**
     * Возвращает количество активных воркеров.
     */
    public int getWorkerCount() {
        return workers.size();
    }
}