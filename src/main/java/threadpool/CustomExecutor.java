package threadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Расширенный интерфейс Executor с поддержкой завершения работы и отправки задач с результатом.
 */
public interface CustomExecutor extends Executor {

    /**
     * Отправляет задачу на выполнение.
     * @param command задача в виде Runnable
     */
    @Override
    void execute(Runnable command);

    /**
     * Отправляет задачу в виде Callable и возвращает Future для получения результата.
     * @param callable задача с результатом
     * @param <T> тип результата
     * @return Future для получения результата выполнения задачи
     */
    <T> Future<T> submit(Callable<T> callable);

    /**
     * Мягкое завершение работы — запрещает новые задачи, текущие продолжают выполняться.
     */
    void shutdown();

    /**
     * Принудительное завершение — останавливает все задачи немедленно.
     */
    void shutdownNow();
}