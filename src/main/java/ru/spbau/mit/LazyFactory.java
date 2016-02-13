package ru.spbau.mit;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

/**
 * Created by LDVSOFT on 09.02.2016.
 */
public abstract class LazyFactory {
    private static class SingleThreadLazy<T> implements Lazy<T> {
        private Supplier<? extends T> supplier;
        private T data;

        private SingleThreadLazy(Supplier<? extends T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (supplier != null) {
                data = supplier.get();
                supplier = null;
            }
            return data;
        }
    }

    private static class ConcurrentLazy<T> implements Lazy<T> {
        private volatile Supplier<? extends T> supplier;
        private volatile T data;

        public ConcurrentLazy(Supplier<? extends T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (supplier != null) {
                synchronized (this) {
                    if (supplier != null) {
                        data = supplier.get();
                        supplier = null;
                    }
                }
            }
            return data;
        }
    }

    private static class LockFreeLazy<T> implements Lazy<T> {
        private volatile @SuppressWarnings("unused") Supplier<? extends T> supplier;
        private volatile T data;

        private static final AtomicReferenceFieldUpdater<LockFreeLazy, Supplier> SUPPLIER_UPDATER
                = AtomicReferenceFieldUpdater.newUpdater(LockFreeLazy.class, Supplier.class, "supplier");
        private static final Supplier<?> BUSY = () -> null;

        private LockFreeLazy(Supplier<? extends T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            T result;
            Supplier<? extends T> supplier;
            while (true) {
                supplier = SUPPLIER_UPDATER.get(this);
                if (supplier == null) {
                    //Already calculated!
                    return data;
                }
                if (supplier == BUSY) {
                    // Someone is actually writting result, wait a little...
                    Thread.yield();
                    continue;
                }

                result = supplier.get();
                // "Lock" data, writing BUSY to supplier
                if (SUPPLIER_UPDATER.compareAndSet(this, supplier, BUSY)) {
                    data = result;
                    // Release "lock" by writting NULL to supplier
                    SUPPLIER_UPDATER.set(this, null);
                    return result;
                }
            }
        }
    }

    public static <T> Lazy<T> getSingleThreadLazy(Supplier<? extends T> supplier) {
        return new SingleThreadLazy<T>(supplier);
    }

    public static <T> Lazy<T> getConcurrentLazy(final Supplier<? extends T> supplier) {
        return new ConcurrentLazy<T>(supplier);
    }

    public static <T> Lazy<T> getLockFreeLazy(Supplier<? extends T> supplier) {
        return new LockFreeLazy<>(supplier);
    }
}
