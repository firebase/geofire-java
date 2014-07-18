package com.firebase.geofire.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleFuture<V> implements Future<V> {

    private V object = null;
    private boolean isSet = false;

    private final Lock lock = new ReentrantLock();
    private final Condition setCondition = lock.newCondition();

    public synchronized void put(V object) {
        this.lock.lock();
        try {
            this.object = object;
            this.isSet = true;
            this.setCondition.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        this.lock.lock();
        try {
            return this.isSet;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        this.lock.lock();
        try {
            while (!this.isSet) {
                setCondition.await();
            }
            return this.object;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        this.lock.lock();
        try {
            while (!this.isSet) {
                if (!setCondition.await(timeout, unit)) {
                    throw new TimeoutException();
                }
            }
            return this.object;
        } finally {
            this.lock.unlock();
        }
    }
}
