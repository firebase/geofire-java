package com.firebase.geofire.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class SimpleFuture<V> {
    private V value;
    private boolean isSet;

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public synchronized void put(final V valueToPut) {
        lock.lock();

        try {
            value = valueToPut;
            isSet = true;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public V get(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
        lock.lock();

        try {
            while (!isSet) {
                if (!condition.await(timeout, unit)) {
                    throw new TimeoutException();
                }
            }

            return value;
        } finally {
            lock.unlock();
        }
    }

    public boolean isDone() {
        return isSet;
    }
}
