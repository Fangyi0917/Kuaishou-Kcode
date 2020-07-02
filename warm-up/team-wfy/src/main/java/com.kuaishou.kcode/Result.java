package com.kuaishou.kcode;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: wfy
 * @create: 2020-06-06 18:12
 **/

public class Result {
    private Map<String,Map<Long,String>> resultsOfMethods = new HashMap<>();

    public void addResult(Long timestamp, String methodName, String result){
//        System.out.println(timestamp+" "+methodName+" "+result);
        Map<Long, String> resultsInSeconds = resultsOfMethods.computeIfAbsent(methodName, k -> new HashMap<>());
        resultsInSeconds.put(timestamp,result);
    }

    public String getResult(Long timestamp, String methodName){
        return resultsOfMethods.get(methodName).get(timestamp);
    }
}
