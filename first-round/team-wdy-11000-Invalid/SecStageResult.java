package com.kuaishou.kcode.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wdy
 * @create 2020-06-22 18:17
 */
public class SecStageResult {

    private Map<String,Long> resulrMap = new HashMap<>();

    public void addResult(long oldTimeMin, String oldResponseForSecond, long succPercent) {
        String key = oldResponseForSecond+"-"+oldTimeMin;
        resulrMap.put(key,succPercent);
    }

    public long getResult(long minStamp, String response) {
        Long succPercent = resulrMap.get(response + "-" + minStamp);
        if(succPercent != null){
            return succPercent;
        }
        return -1;
    }
}
