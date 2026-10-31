package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;
import fr.maxlego08.sarah.transaction.TransactionCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs {@link RequestHelper} operations off the calling thread and returns a
 * {@link CompletableFuture}, so a server thread never waits for the database.
 *
 * <p>Each method does the same as the {@link RequestHelper} method of the same name; writes
 * complete with the row count or generated key. A failure completes the future exceptionally
 * with the {@link fr.maxlego08.sarah.exceptions.DatabaseException} the helper throws. Reads follow
 * the helper's {@link RequestHelper#setStrictReads(boolean) strict reads} setting.</p>
 *
 * <pre>
 * AsyncRequestHelper async = new AsyncRequestHelper(requestHelper);
 * async.select("users", User.class, schema -&gt; schema.where("uuid", uuid))
 *      .thenAccept(users -&gt; ...);
 * </pre>
 *
 * <p>Futures complete on the executor's threads. To go back to a main thread, such as a
 * Minecraft server's, hand the result to its scheduler.</p>
 */
public class AsyncRequestHelper {

    private static final AtomicInteger POOL_COUNT = new AtomicInteger();

    private final RequestHelper requestHelper;
    private final Executor executor;
    private final ExecutorService ownedExecutor;

    /**
     * Runs the operations on the given executor, for example a server's async scheduler. The
     * executor is not shut down by {@link #shutdown()}.
     *
     * @param requestHelper the helper that runs the operations
     * @param executor      where the operations run
     */
    public AsyncRequestHelper(RequestHelper requestHelper, Executor executor) {
        this.requestHelper = requestHelper;
        this.executor = executor;
        this.ownedExecutor = null;
    }

    /**
     * Runs the operations on a pool of {@code threads} daemon threads named
     * {@code sarah-async-<pool>-<thread>}, owned by this helper and stopped by {@link #shutdown()}.
     *
     * @param requestHelper the helper that runs the operations
     * @param threads       the number of threads, at most as many as the connection pool
     *                      allows at once
     */
    public AsyncRequestHelper(RequestHelper requestHelper, int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1");
        }
        this.requestHelper = requestHelper;
        this.ownedExecutor = Executors.newFixedThreadPool(threads, daemonThreadFactory());
        this.executor = this.ownedExecutor;
    }

    /**
     * Runs the operations on a pool of 4 daemon threads, stopped by {@link #shutdown()}.
     *
     * @param requestHelper the helper that runs the operations
     */
    public AsyncRequestHelper(RequestHelper requestHelper) {
        this(requestHelper, 4);
    }

    private static ThreadFactory daemonThreadFactory() {
        int pool = POOL_COUNT.incrementAndGet();
        AtomicInteger threadCount = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "sarah-async-" + pool + "-" + threadCount.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, this.executor);
    }

    public <T> CompletableFuture<List<T>> select(String tableName, Class<T> clazz, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.select(tableName, clazz, consumer));
    }

    public CompletableFuture<List<Map<String, Object>>> select(String tableName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.select(tableName, consumer));
    }

    public <T> CompletableFuture<List<T>> selectAll(String tableName, Class<T> clazz) {
        return supply(() -> this.requestHelper.selectAll(tableName, clazz));
    }

    public CompletableFuture<Long> count(String tableName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.count(tableName, consumer));
    }

    public CompletableFuture<Number> sum(String tableName, String columnName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.sum(tableName, columnName, consumer));
    }

    public CompletableFuture<Number> max(String tableName, String columnName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.max(tableName, columnName, consumer));
    }

    public CompletableFuture<Number> min(String tableName, String columnName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.min(tableName, columnName, consumer));
    }

    public CompletableFuture<Number> avg(String tableName, String columnName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.avg(tableName, columnName, consumer));
    }

    public CompletableFuture<Boolean> exists(String tableName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.exists(tableName, consumer));
    }

    /**
     * @return a future completed with the generated key, or 0 when the table has no
     * auto-increment column
     */
    public CompletableFuture<Integer> insert(String tableName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.insertAndGetKey(tableName, consumer));
    }

    public <T> CompletableFuture<Integer> insert(String tableName, Class<T> clazz, T data) {
        return supply(() -> this.requestHelper.insertAndGetKey(tableName, clazz, data));
    }

    /**
     * @return a future completed with the number of rows changed
     */
    public CompletableFuture<Integer> update(String tableName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.updateAndCount(tableName, consumer));
    }

    /**
     * @return a future completed with the number of rows affected, as reported by the driver
     */
    public CompletableFuture<Integer> upsert(String tableName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.upsertAndCount(tableName, consumer));
    }

    public <T> CompletableFuture<Integer> upsert(String tableName, Class<T> clazz, T data) {
        return supply(() -> this.requestHelper.upsertAndCount(tableName, clazz, data));
    }

    /**
     * @return a future completed with the number of rows deleted
     */
    public CompletableFuture<Integer> delete(String tableName, Consumer<Schema> consumer) {
        return supply(() -> this.requestHelper.deleteAndCount(tableName, consumer));
    }

    public CompletableFuture<Integer> insertMultiple(List<Schema> schemas) {
        return supply(() -> this.requestHelper.insertMultipleAndGetKey(schemas));
    }

    public <T> CompletableFuture<Integer> insertMultiple(String tableName, Class<T> clazz, List<T> dataList) {
        return supply(() -> this.requestHelper.insertMultipleAndGetKey(tableName, clazz, dataList));
    }

    public CompletableFuture<Integer> upsertMultiple(List<Schema> schemas) {
        return supply(() -> this.requestHelper.upsertMultipleAndCount(schemas));
    }

    public <T> CompletableFuture<Integer> upsertMultiple(String tableName, Class<T> clazz, List<T> dataList) {
        return supply(() -> this.requestHelper.upsertMultipleAndCount(tableName, clazz, dataList));
    }

    public CompletableFuture<Integer> updateMultiple(List<Schema> schemas) {
        return supply(() -> this.requestHelper.updateMultipleAndCount(schemas));
    }

    public <T> CompletableFuture<Integer> updateMultiple(String tableName, Class<T> clazz, List<T> dataList) {
        return supply(() -> this.requestHelper.updateMultipleAndCount(tableName, clazz, dataList));
    }

    public CompletableFuture<Integer> execute(String sql, Object... parameters) {
        return supply(() -> this.requestHelper.execute(sql, parameters));
    }

    public <T> CompletableFuture<List<T>> query(String sql, RowMapper<T> mapper, Object... parameters) {
        return supply(() -> this.requestHelper.query(sql, mapper, parameters));
    }

    /**
     * Runs {@link RequestHelper#inTransaction(TransactionCallback)} on one thread of the executor.
     * The callback receives a synchronous helper: its calls are part of the transaction and must
     * not be handed to another thread.
     */
    public <T> CompletableFuture<T> inTransaction(TransactionCallback<T> callback) {
        return supply(() -> this.requestHelper.inTransaction(callback));
    }

    /**
     * @return the synchronous helper the operations run on
     */
    public RequestHelper getRequestHelper() {
        return this.requestHelper;
    }

    /**
     * Stops the pool this helper created, after the operations already submitted. Does nothing
     * when the helper was given its own executor.
     */
    public void shutdown() {
        if (this.ownedExecutor != null) {
            this.ownedExecutor.shutdown();
        }
    }
}
