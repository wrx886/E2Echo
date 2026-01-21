package com.github.wrx886.e2echo.plugin.store;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.plugin.util.EccUtil.KeyPairHex;

// ECC密钥存储组件
@Component
public final class EccKeyStore {

    // 读写锁
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 公钥 HEX 格式
    private String publicKeyHex;

    // 私钥 HEX 格式
    private String privateKeyHex;

    /**
     * 设置密钥对
     * 
     * @param publicKeyHex  公钥 HEX 格式
     * @param privateKeyHex 私钥 HEX 格式
     */
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
     * 获取密钥对
     * 
     * @return HEX 格式密钥对
     */
    public KeyPairHex get() {
        lock.readLock().lock();
        try {
            // 获取密钥对
            return new KeyPairHex(publicKeyHex, privateKeyHex);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空密钥对
     */
    public void clear() {
        set(null, null);
    }

}
