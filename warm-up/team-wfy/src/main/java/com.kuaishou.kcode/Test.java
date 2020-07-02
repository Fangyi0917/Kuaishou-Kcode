package com.kuaishou.kcode;

import sun.util.resources.cldr.zh.CalendarData_zh_Hans_HK;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: wfy
 * @create: 2020-06-01 09:53
 **/
public class Test {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        List<Integer> list = new ArrayList();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(null);
        pool.submit(() -> {
            list.parallelStream().map(a -> a.toString()).collect(Collectors.toList());
        }).get();


//        pool.execute(() -> {
//            list.parallelStream().map(a -> a.toString()).collect(Collectors.toList());
//        });
        TimeUnit.SECONDS.sleep(3);

    }





}

