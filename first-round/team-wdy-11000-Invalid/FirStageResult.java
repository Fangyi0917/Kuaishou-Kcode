package com.kuaishou.kcode.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wdy
 * @create 2020-06-22 18:17
 */
public class FirStageResult {

    private Map<String, List<String>> resultMap = new HashMap<>();


    public void addResult(Long oldTimeMin, String oldRequest, String oldResponse, List<String> itemList){
//        List<String> innerList = new ArrayList<>(itemList);
        String key = oldTimeMin+","+oldRequest+","+oldResponse;
        resultMap.put(key,itemList);
    }

    public List<String> getResult(String request, String response, long minStamp){
        List<String> innerList;
        innerList = resultMap.get(minStamp+","+request+","+response);
        if(innerList!=null){
            return innerList;
        }
        return new ArrayList<>();
    }

}
