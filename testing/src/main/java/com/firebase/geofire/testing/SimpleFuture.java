package com.firebase.geofire.testing;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a simple version of a future that does not implement the interface.
 * It allows you to put a certain value in it and then blockingly getting it.
 */
public final class SimpleFuture<V> {
    private V value;
    private boolean isSet;

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    /** Puts the value into the future. */
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

    /** Allows you to get the value that might be set here in a blocking way. */
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

    /** If the value is set this method returns true and the get method will return it then. */
    public boolean isDone() {
        return isSet;
    }
}
