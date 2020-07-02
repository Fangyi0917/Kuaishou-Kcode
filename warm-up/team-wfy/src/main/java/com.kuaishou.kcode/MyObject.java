package com.kuaishou.kcode;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @description:
 * @author: wfy
 * @create: 2020-05-30 23:58
 **/
class MyObject {

    ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> map;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public MyObject(ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> data_) {
        this.map = data_;
    }


    public CopyOnWriteArrayList<Integer> get(String K) {
        writeLock.lock();
        try {
             return map.get(K);
        } finally {
            writeLock.unlock();
        }
    }

    public void put(String K, CopyOnWriteArrayList<Integer> V) {
        writeLock.lock();
        try {
            map.put(K, V);
        } finally {
            writeLock.unlock();
        }
    }

    public boolean containsKey(String key) {
        writeLock.lock();
        try {
            return map.containsKey(key);
        } finally {
            writeLock.unlock();
        }

    }
}
