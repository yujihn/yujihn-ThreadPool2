package threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class App {

    // Создаем логгер для вывода логов
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    // Общее количество задач для выполнения
    private static final int TASK_COUNT = 100;

    public static void main(String[] args) {
        // Инициализация кастомного исполнителя задач
        QueueExecutor executor = new QueueExecutor(
                4, 8, 10, 5, TimeUnit.SECONDS, 2
        );

        int rejected = 0; // счетчик отклоненных задач
        long startTime = System.currentTimeMillis(); // время начала выполнения

        // Цикл для отправки задач в очередь
        for (int i = 1; i <= TASK_COUNT; i++) {
            final int taskId = i; // идентификатор задачи
            try {
                // Отправляем задачу на выполнение
                executor.execute(() -> {
                    // Логирование начала выполнения задачи
                    logger.info("Задача #{} начата", taskId);
                    try {
                        Thread.sleep(100); // имитация работы задачи
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // обработка прерывания
                    }
                    // Логирование завершения задачи
                    logger.info("Задача #{} завершена", taskId);
                });
            } catch (RejectedExecutionException e) {
                // Обработка ситуации, когда задача отклонена
                logger.warn("Задача #{} отклонена", taskId);
                rejected++;
            }
        }

        long endTime = System.currentTimeMillis(); // время окончания выполнения
        // Завершаем работу исполнителя
        executor.shutdown();

        // Ждем, чтобы все задачи завершились
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // обработка прерывания
        }

        // Выводим статистику выполнения задач
        logStats(startTime, endTime, rejected);
    }

    /**
     * Метод для логирования статистики выполнения задач
     *
     * @param startTime время начала выполнения
     * @param endTime   время окончания выполнения
     * @param rejectedTasks количество отклоненных задач
     */
    private static void logStats(long startTime, long endTime, int rejectedTasks) {
        long totalTime = endTime - startTime; // общее время выполнения
        int executedTasks = TASK_COUNT - rejectedTasks; // выполненные задачи
        // расчет среднего времени выполнения одной задачи
        double avgTime = executedTasks > 0 ? (double) totalTime / executedTasks : 0.0;
        // формат для отображения среднего времени
        DecimalFormat df = new DecimalFormat("#.##");

        // Вывод результатов
        logger.info("Результаты выполнения:");
        logger.info("Общее время: {} мс", totalTime);
        logger.info("Выполнено задач: {}", executedTasks);
        logger.info("Отклонено задач: {}", rejectedTasks);
        logger.info("Среднее время выполнения: {} мс", df.format(avgTime));
    }
}