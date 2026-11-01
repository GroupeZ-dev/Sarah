package fr.maxlego08.sarah;

import fr.maxlego08.sarah.exceptions.DatabaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for AsyncRequestHelper
 */
public class AsyncRequestHelperTest extends DatabaseTestBase {

    private AsyncRequestHelper async;

    @Override
    protected void afterConnectionSetup() throws Exception {
        SchemaBuilder.create(null, "test_async", schema -> {
            schema.autoIncrementBigInt("id");
            schema.string("name", 50);
            schema.integer("score");
        }).execute(connection, testLogger);
        // SQLite allows one writer at a time
        async = new AsyncRequestHelper(requestHelper, 1);
    }

    @AfterEach
    public void shutdownAsync() {
        async.shutdown();
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void testWritesAndReads() throws Exception {
        int firstKey = await(async.insert("test_async", schema -> schema.string("name", "alice").bigInt("score", 10)));
        int secondKey = await(async.insert("test_async", schema -> schema.string("name", "bob").bigInt("score", 20)));
        assertTrue(secondKey > firstKey);

        assertEquals(1, (int) await(async.update("test_async", schema -> schema.bigInt("score", 15).where("name", "alice"))));
        assertEquals(2L, (long) await(async.count("test_async", schema -> {
        })));
        assertEquals(35, await(async.sum("test_async", "score", schema -> {
        })).intValue());
        assertTrue(await(async.exists("test_async", schema -> schema.where("name", "bob"))));

        List<Map<String, Object>> rows = await(async.select("test_async", schema -> schema.where("score", ">", 16)));
        assertEquals(1, rows.size());
        assertEquals("bob", rows.get(0).get("name"));

        assertEquals(1, (int) await(async.delete("test_async", schema -> schema.where("name", "alice"))));
        assertFalse(await(async.exists("test_async", schema -> schema.where("name", "alice"))));
    }

    @Test
    public void testRunsOffTheCallingThread() throws Exception {
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> worker = new AtomicReference<>();
        await(async.count("test_async", schema -> worker.set(Thread.currentThread())));

        assertNotEquals(caller, worker.get());
        assertTrue(worker.get().isDaemon());
        assertTrue(worker.get().getName().startsWith("sarah-async-"));
    }

    @Test
    public void testUsesTheGivenExecutor() throws Exception {
        AtomicInteger submitted = new AtomicInteger();
        Executor counting = runnable -> {
            submitted.incrementAndGet();
            runnable.run();
        };
        AsyncRequestHelper custom = new AsyncRequestHelper(requestHelper, counting);

        await(custom.count("test_async", schema -> {
        }));
        await(custom.execute("UPDATE %prefix%test_async SET score = ?", 1));
        assertEquals(2, submitted.get());
        custom.shutdown();
    }

    @Test
    public void testFailureCompletesExceptionally() {
        CompletableFuture<Integer> future = async.update("test_missing_table", schema -> schema.bigInt("score", 1));
        ExecutionException exception = assertThrows(ExecutionException.class, () -> await(future));
        assertInstanceOf(DatabaseException.class, exception.getCause());
    }

    @Test
    public void testTransaction() throws Exception {
        assertEquals("done", await(async.inTransaction(tx -> {
            tx.insert("test_async", schema -> schema.string("name", "carol").bigInt("score", 1));
            tx.insert("test_async", schema -> schema.string("name", "dave").bigInt("score", 2));
            return "done";
        })));
        assertEquals(2L, (long) await(async.count("test_async", schema -> {
        })));
    }

    @Test
    public void testThreadCountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new AsyncRequestHelper(requestHelper, 0));
    }
}
