package com.github.wrx886.e2echo.client.srv.store;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;

@Component
public class WebUrlStore {

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private String webUrl;

    public String getWebUrl() {
        try {
            lock.readLock().lock();
            return webUrl;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setWebUrl(String webUrl) {
        try {
            lock.writeLock().lock();
            this.webUrl = webUrl;
        } finally {
            lock.writeLock().unlock();
        }
    }

}
