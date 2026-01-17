package com.github.wrx886.e2echo.plugin.store;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;

// ECC密钥存储组件
@Component
public final class EccKeyStore {

    // 读写锁
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 公钥 HEX 格式
    private String publicKeyHex;

    // 私钥 HEX 格式
    private String privateKeyHex;

    public void set(String publicKeyHex, String privateKeyHex) {
        lock.writeLock().lock();
        try {
            // 设置密钥对
            this.publicKeyHex = publicKeyHex;
            this.privateKeyHex = privateKeyHex;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取 RAW HEX 格式的公钥
     * 
     * @return RAW HEX 格式的公钥
     */
    public String getPublicKeyHex() {
        lock.readLock().lock();
        try {
            return publicKeyHex;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取 RAW HEX 格式的私钥
     * 
     * @return RAW HEX 格式的私钥
     */
    public String getPrivateKeyHex() {
        lock.readLock().lock();
        try {
            return privateKeyHex;
        } finally {
            lock.readLock().unlock();
        }
    }

}
