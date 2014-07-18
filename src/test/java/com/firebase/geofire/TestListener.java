package com.firebase.geofire;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TestListener {
    private final List<String> events = new ArrayList<String>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    protected void addEvent(String event) {
        lock.lock();
        try {
            this.events.add(event);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void expectEvents(Set<String> events) throws InterruptedException {
        boolean stillWaiting = true;
        lock.lock();
        try {
            while (!new HashSet(this.events).equals(events)) {
                if (!stillWaiting) {
                    Assert.assertEquals(events, new HashSet(this.events));
                    Assert.fail("Timeout occured");
                    return;
                }
                stillWaiting = condition.await(10, TimeUnit.SECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    public void expectEvents(List<String> events) throws InterruptedException {
        boolean stillWaiting = true;
        lock.lock();
        try {
            while (!this.events.equals(events)) {
                if (!stillWaiting) {
                    Assert.assertEquals(events, this.events);
                    Assert.fail("Timeout occured");
                    return;
                }
                stillWaiting = condition.await(10, TimeUnit.SECONDS);
            }
        } finally {
            lock.unlock();
        }
    }
}
