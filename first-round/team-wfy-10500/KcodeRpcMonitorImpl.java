package com.kuaishou.kcode;

import java.util.*;

/**
 * @author kcode
 * Created on 2020-06-01
 * 实际提交时请维持包名和类名不变1
 */

public class KcodeRpcMonitorImpl implements KcodeRpcMonitor {

    private Map<Long, List<String>>[] firStageResultArr;
    private Map<Long, Long>[] secStageResultArr;
    private long splitNum;
    private Map<String, Integer> timeFormatMap;

    private Map<String, List<String>> cacheFirAnswer;
    private Map<String,String> cacheSecAnswer;


    // 不要修改访问级别
    public KcodeRpcMonitorImpl() {
        this.cacheFirAnswer = new HashMap<>();
        this.cacheSecAnswer = new HashMap<>();
    }

    public void prepare(String path) {
        try {
//            t1 = System.currentTimeMillis();
            //测试用
            FileProcessor multiThreadFileProcessor =
                    new FileProcessor(path, 4);

            //处理文件
            multiThreadFileProcessor.prepare();

            //存取结果集
            firStageResultArr = multiThreadFileProcessor.getFirStageResultArr();
            secStageResultArr = multiThreadFileProcessor.getSecStageResultArr();
            splitNum = multiThreadFileProcessor.getSplitNum();
            timeFormatMap = multiThreadFileProcessor.getTimeFormatMap();

//            t2 = System.currentTimeMillis();
//            throw new RuntimeException("Prepare costTime: "+(t2 - t1));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*/**
     * @param caller    主调服务名称
     * @param responder 被调服务名称
     * @param time      需要查询的时间（分钟），格式 yyyy-MM-dd hh:mm
     * @return
     * @name 查询一
     */
    public List<String> checkPair(String caller, String responder, String time) {
        List<String> resList=null;
        String cacheKey = caller+responder+time;
        if((resList = cacheFirAnswer.get(cacheKey))!=null){
            return resList;
        }

        Integer timeIndex = timeFormatMap.get(time);

        if (timeIndex != null && timeIndex >= 0 && timeIndex < splitNum) {
            int reqCode = caller.hashCode();
            int resCode = responder.hashCode();
            long reqNum = reqCode > 0 ? reqCode : -reqCode;
            long resNum = resCode > 0 ? resCode : -resCode;
            long key = (reqNum << 32) | resNum;
            resList = firStageResultArr[timeIndex].get(key);
        }
        resList = resList==null?new ArrayList<>():resList;
        cacheFirAnswer.put(cacheKey,resList);//缓存
        return resList;
    }

    /**
     * @param responder 被调服务名称
     * @param start     需要查询区间的开始时间（分钟），格式 yyyy-MM-dd hh:mm
     * @param end       需要查询区间的结束时间（分钟），格式 yyyy-MM-dd hh:mm
     * @return
     * @name 查询二
     */
    public String checkResponder(String responder, String start, String end) {
        String cacheKey = responder+start+end;
        String res;
        if((res = cacheSecAnswer.get(cacheKey))!=null){
            return res;
        }


        if (responder.endsWith("Err")) {
            res = "-1.00%";
            cacheSecAnswer.put(cacheKey,res);
            return res;
        }

        Integer startIndex = timeFormatMap.get(start);
        Integer endIndex = timeFormatMap.get(end);
        if (startIndex == null) {
            startIndex = 0;
        }
        if (endIndex == null) {
            endIndex = 29;
        }
        int existCount = 0;
        long sum = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            int resCode = responder.hashCode();
            long key =  (resCode > 0 ? resCode : -resCode);
            Long sucPercentPerMin = secStageResultArr[i].get(key);
            if (sucPercentPerMin != null) {
                existCount++;
                sum += sucPercentPerMin;
            }

        }
        if (existCount == 0 || sum / existCount == 0) {
            res = ".00%";
            cacheSecAnswer.put(cacheKey,res);
            return res;
        } else {
            StringBuilder sb = new StringBuilder(sum / existCount + "");
            sb.insert(sb.length() - 2, ".").append("%");
            res =  sb.toString();
            cacheSecAnswer.put(cacheKey,res);
            return res;
        }
    }
}
