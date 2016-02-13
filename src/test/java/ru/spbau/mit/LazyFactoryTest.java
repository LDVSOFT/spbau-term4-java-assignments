package ru.spbau.mit;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class LazyFactoryTest {
    private interface CountingSupplier<T> extends Supplier<T> {
        int getCount();
    }

    /*
     * Test set one: just check double getting.
     */
    private void testBasic(Function<Supplier<Integer>, Lazy<Integer>> provider, Integer value) {
        testBasic(provider, value, true);
    }

    private void testBasic(Function<Supplier<Integer>, Lazy<Integer>> provider, final Integer value, boolean checkSingleGet) {
        CountingSupplier<Integer> supplier = new CountingSupplier<Integer>() {
            private int count = 0;

            @Override
            public Integer get() {
                count++;
                return value;
            }

            @Override
            public int getCount() {
                return count;
            }
        };

        Lazy<Integer> lazy = provider.apply(supplier);
        assertEquals(0, supplier.getCount());
        Integer result = lazy.get();
        assertEquals(value, result);
        assertEquals(1, supplier.getCount());
        result = lazy.get();
        assertEquals(value, result);

        if (checkSingleGet) {
            assertEquals(1, supplier.getCount());
        }
    }

    /*
     * With normal values
     */
    @Test
    public void testGetSingleThreadLazyBasic() throws Exception {
        testBasic(LazyFactory::getSingleThreadLazy, 5);
    }

    @Test
    public void testGetConcurrentLazyBasic() {
        testBasic(LazyFactory::getConcurrentLazy, 5);
    }

    @Test
    public void testGetLockFreeLazyBasic() {
        // LockFree is allowed (and actually can) get value more than one time
        testBasic(LazyFactory::getLockFreeLazy, 5, false);
    }

    /*
     * And null values
     */
    @Test
    public void testGetSingleThreadLazyBasicNull() throws Exception {
        testBasic(LazyFactory::getSingleThreadLazy, null);
    }

    @Test
    public void testGetConcurrentLazyBasicNull() {
        testBasic(LazyFactory::getConcurrentLazy, null);
    }

    @Test
    public void testGetLockFreeLazyBasicNull() {
        // LockFree is allowed (and actually will!) get value more than one time
        testBasic(LazyFactory::getLockFreeLazy, null, false);
    }

    /*
     * Test set two: create a bunch of threads, and look them getting the same value
     */
    private void testConcurrency(Function<Supplier<Integer>, Lazy<Integer>> provider) {
        testConcurrency(provider, true);
    }

    private void testConcurrency(Function<Supplier<Integer>, Lazy<Integer>> provider, boolean checkSingleGet) {
        final Integer VALUE = 5;
        final int THREAD_COUNT = 100;

        CountingSupplier<Integer> supplier = new CountingSupplier<Integer>() {
            private int count = 0;

            @Override
            public Integer get() {
                synchronized (this) {
                    count++;
                }
                Thread.yield(); // JUST
                Thread.yield(); // YIELD
                Thread.yield(); // IT
                Thread.yield(); // MAKE
                Thread.yield(); // THIS
                Thread.yield(); // THREAD
                Thread.yield(); // STOP
                return VALUE;
            }

            @Override
            public synchronized int getCount() {
                return count;
            }
        };

        Lazy<Integer> lazy = provider.apply(supplier);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i != THREAD_COUNT; ++i) {
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }

                return lazy.get();
            }));
        }

        for (Future<Integer> future : futures) {
            try {
                Integer result = future.get();
                assertEquals(VALUE, result);
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }

        if (checkSingleGet) {
            assertEquals(1, supplier.getCount());
        }
    }

    @Test
    public void testGetConcurrencyLazyConcurrent() {
        testConcurrency(LazyFactory::getConcurrentLazy);
    }

    @Test
    public void testGetLockFreeLazyConcurrent() {
        // LockFree is allowed (and actually can) get value more than one time
        testConcurrency(LazyFactory::getLockFreeLazy, false);
    }

    /*
     * Test set three: consistency.
     */
    private void testConsistency(Function<Supplier<Integer>, Lazy<Integer>> provider) {
        final int THREAD_COUNT = 100;

        Supplier<Integer> supplier = new Supplier<Integer>() {
            private int count = 0;

            @Override
            public Integer get() {
                int toReturn;
                synchronized (this) {
                    toReturn = count++;
                }
                Thread.yield(); // JUST
                Thread.yield(); // YIELD
                Thread.yield(); // IT
                Thread.yield(); // MAKE
                Thread.yield(); // THIS
                Thread.yield(); // THREAD
                Thread.yield(); // STOP
                return toReturn;
            }
        };

        Lazy<Integer> lazy = provider.apply(supplier);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i != THREAD_COUNT; ++i) {
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }

                return lazy.get();
            }));
        }

        Integer lastValue = null;
        for (Future<Integer> future : futures) {
            try {
                Integer newValue = future.get();
                if (lastValue != null) {
                    assertEquals(lastValue.intValue(), newValue.intValue());
                }
                lastValue = newValue;
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }
    }

    @Test
    public void testGetConcurrencyLazyConsistency() {
        testConsistency(LazyFactory::getConcurrentLazy);
    }

    @Test
    public void testGetLockFreeLazyConsistency() {
        testConsistency(LazyFactory::getLockFreeLazy);
    }
}