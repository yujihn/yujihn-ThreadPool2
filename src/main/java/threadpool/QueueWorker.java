package threadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Воркеры, которые берут задачи из очереди и выполняют их.
 */
public class QueueWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(QueueWorker.class);

    private final BlockingQueue<Runnable> taskQueue; // очередь задач
    private final int workerId; // идентификатор воркера

    private final long keepAliveTime; // время ожидания перед завершением
    private final TimeUnit timeUnit; // единица измерения времени

    private volatile boolean running = true; // флаг, управляющий жизненным циклом воркера

    /**
     * Конструктор воркера.
     * @param taskQueue очередь задач
     * @param workerId идентификатор воркера
     * @param keepAliveTime время ожидания задач
     * @param timeUnit единица времени
     */
    public QueueWorker(BlockingQueue<Runnable> taskQueue, int workerId, long keepAliveTime, TimeUnit timeUnit) {
        this.taskQueue = taskQueue;
        this.workerId = workerId;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
    }

    /**
     * Основной цикл воркера.
     */
    @Override
    public void run() {
        logger.info("Worker-{} запущен", workerId);
        try {
            while (running) {
                // Пытаемся получить задачу с тайм-аутом
                Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                if (task != null) {
                    logger.debug("Worker-{} получил задачу", workerId);
                    try {
                        task.run(); // выполнение задачи
                    } catch (Exception e) {
                        logger.error("Ошибка при выполнении задачи Worker-{}: {}", workerId, e.getMessage());
                    }
                } else {
                    // Если задач не было в течение времени ожидания — завершаемся
                    logger.info("Worker-{} завершает работу после простоя", workerId);
                    running = false;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Worker-{} был прерван", workerId);
        }
    }

    /**
     * Остановка воркера извне.
     */
    public void stop() {
        this.running = false;
    }
}