package com.kuaishou.kcode;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @description:
 * @author: wfy
 * @create: 2020-05-30 23:20
 **/
public class MyTask implements Runnable {

    InputStream inputStream;
    MyObject data;
    Object lock = new Object();

    public MyTask(InputStream inputStream, MyObject data) {
        this.data = data;
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while (true){
            try {
                if (!reader.ready()) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String line = reader.readLine();
                String[] splits = line.split(",");
                String key = splits[0].substring(0,10) + splits[1];
                if (!data.containsKey(key)) {
                    data.put(key, new CopyOnWriteArrayList<>());
                }
                data.get(key).add(Integer.valueOf(splits[2]));

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}
