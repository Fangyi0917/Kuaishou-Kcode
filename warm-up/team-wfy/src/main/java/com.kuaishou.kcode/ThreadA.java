package com.kuaishou.kcode;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @description:
 * @author: wfy
 * @create: 2020-06-01 11:05
 **/
public class ThreadA extends Thread{
    private Object lock;
    private ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> MyHashMap;
    private List<String> MyLineList;
    public ThreadA(Object lock) {
        super();
        this.lock = lock;
    }
    @Override
    public void run(){
        try {
            synchronized (lock) {
                if (MyLineList.size() != 1000) {
                   
                    lock.wait();
                    System.out.println("Interruption!!!");

                    lock.notify();
                    lock.wait();

                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}
