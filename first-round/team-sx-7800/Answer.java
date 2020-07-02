package com.kuaishou.kcode;

import com.sun.deploy.cache.BaseLocalApplicationProperties;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author plus1s
 * date 2020-06-30
 * @function 处理结果
 */
public class Answer {

    // 降序比较器
    static Comparator<Integer> cmp = (e1, e2) -> e2 - e1;

    // 对于 checkPair 阶段，保存调用耗时列表、调用成功次数、调用总次数
    static class PairData {
        Queue<Integer> callTimeList;    // 调用耗时列表，使用优先队列自动排序
        int callTrueCount;                  // 调用成功次数
        int callTotalCount;                 // 调用总次数

        PairData() {
            this.callTimeList = new PriorityQueue<>(cmp);
        }
    }

    // 对于 checkResponder 阶段，保存调用成功次数、调用总次数
    static class ResponderData {
        int callTrueCount;         // 调用成功次数
        int callTotalCount;        // 调用总次数
    }

    private Map<String, List<String>> pairAnswerMap = new HashMap<>();  // checkPair 答案映射
    private Map<String, PairData> pairDataMap = new HashMap<>();            // checkPair 数据映射


    private Map<String, String> responderAnswerMap = new HashMap<>();        // checkReponder 答案映射
    private Map<String, ResponderData> responderDataMap = new HashMap<>();   // checkReponder 数据映射

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");  // 时间格式化对象

    public Answer() {

    }

    /**
     * 添加此次的耗时以及调用响应情况到该主被调对数据中
     *
     * @param methodAndIpPairString 调用方法和 IP 对组合字符串
     * @param callTime 此次调用耗时(ms)
     * @param success 调用成功否？
     */
    public void addPairData(final String methodAndIpPairString, final int callTime, final boolean success) {
        PairData pairData = this.pairDataMap.computeIfAbsent(methodAndIpPairString, k -> new PairData());
        pairData.callTimeList.add(callTime);   // 耗时
        pairData.callTotalCount++;                   // 调用总次数
        if(success) pairData.callTrueCount++;       // 成功调用总次数
    }

    /**
     * 添加一个响应者在某一时刻(分)的数据
     *
     * @param responder 响应者
     * @param startTime 开始时间(分)
     * @param success 调用成功否？
     */
    public void addResponderData(final String responder, final int startTime, final boolean success) {
        final String responderOnStartTime = responder + startTime;
        ResponderData responderData = this.responderDataMap.computeIfAbsent(responderOnStartTime, k -> new ResponderData());
        responderData.callTotalCount++;              /* 调用总次数 */
        if(success) responderData.callTrueCount++;  /* 成功调用总次数 */
    }

    /**
     * 计算一个主被调对在某一时刻(分)的答案
     *
     * @param methodAndIpPairString 调用方法和 IP 对组合字符串
     * @param startTime 开始时间(分)
     */
    public void pairAnswerCompute(final String methodAndIpPairString, int startTime) {
        PairData data = this.pairDataMap.get(methodAndIpPairString);
        if(data.callTotalCount == 0) return;    /* 本次时间该主被调对未出现调用 */

        /* 首先排序 */
        Queue<Integer> callTimeList = data.callTimeList;
        //Collections.sort(elapsedTimes);

        /* P99 */
        int index = (int)Math.ceil(callTimeList.size() * 0.99) - 1;
        while(callTimeList.size() != index + 1){
            callTimeList.poll();
        }
        int P99 = callTimeList.peek();

        /* 调用成功率，截取后两位小数，不进位 */
        double SR = Math.floor((double) data.callTrueCount / (double) data.callTotalCount * 10000) / 100;

        //        String successPercent = ".00%";
//        int tmp = data.callTrueCount * 10000 / data.callTotalCount;
//        if(tmp != 0){
//            StringBuilder sb = new StringBuilder(tmp + "");
//            successPercent = sb.insert(sb.length() - 2, ".").append("%").toString();
        /* 存入答案 */
        String[] methodAndIpPair = methodAndIpPairString.split("\\|");
        String key = methodAndIpPair[0] + dateFormat.format((long)startTime * 60 * 1000);
        String SRS;
        if(SR == 0.0) SRS = ".00%"; /* 0% ？那么直接是 ".00%" */
        else {
            SRS = String.format("%.2f%%", SR);
        }
        List<String> list = this.pairAnswerMap.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(methodAndIpPair[1] + "," + SRS + "," + P99);

        /* 为了复用 */
        callTimeList.clear();
        data.callTrueCount = data.callTotalCount = 0;

    }
    /**
     * 计算输入字符串表示时间对应的 unix 时间戳分钟数
     *
     * @param time 响应者
     * @return unix 时间戳分钟数
     */
    public int calMin(String time){
        final int baseMin = 26515680; // 2020-06-01 00:00

        String yyMMdd = time.split(" ")[0];
        String[] HHmm = time.split(" ")[1].split(":");

        int day = Integer.valueOf(yyMMdd.split("-")[2]);
        int hour = Integer.valueOf(HHmm[0]);
        int min = Integer.valueOf(HHmm[1]);
        return baseMin + (day - 1) * 1440 + hour * 60 + min;
    }
    /**
     * 计算响应者在某个时间范围内的答案
     *
     * @param responder 响应者
     * @param start 开始时间(分)
     * @param end 结束时间(分)
     * @param responderAndTimeScope 响应者与时间范围组合字符串，作为答案的 key
     * @return 答案字符串(平均成功率)
     */
    public String responderAnswerCompute(String responder, String start, String end,
                                         final String responderAndTimeScope) {
        double TSR = 0;
        int totalExist = 0;
        ResponderData data;
        String AVG_RS_STRING = null;

            /* 得到开始时间戳(分)和结束时间戳(分) */
            //int startTime = (int) (dateFormat.parse(start).getTime() / (1000 * 60));
            //int endTime = (int) (dateFormat.parse(end).getTime() / (1000 * 60));
            int startTime = calMin(start);
            int endTime = calMin(end);
            /* 遍历闭区间内时间范围的所有调用情况并计算平均成功率 */
            while ( startTime <= endTime ) {
                /* 计算当前时间的成功率 */
                data = this.responderDataMap.get(responder + startTime);
                /* 假设该分钟不存在调用 */
                if(data != null) {
                    TSR += Math.floor((double) data.callTrueCount / (double) data.callTotalCount * 10000) / 100;
                    totalExist++;   /* 当前分钟存在调用 */
                }

                /* 下一分钟 */
                startTime++;
            }

            /* 好的，现在计算平均成功率 */
            AVG_RS_STRING = "-1.00%";
            if(totalExist > 0) {
                /* 转换为字符串 */
                AVG_RS_STRING = String.format("%.2f%%", Math.floor(TSR / totalExist * 100) / 100);
            }

            /* 存入答案 */
            this.responderAnswerMap.put(responderAndTimeScope, AVG_RS_STRING);

        return (totalExist > 0 ? AVG_RS_STRING : "-1.00%");
    }

    /**
     * 获取一个主被调对在某一时刻(分)的答案
     *
     * @param caller 调用者
     * @param responder 响应者
     * @param time 时间
     * @return 答案
     */
    public List<String> getPairAnswer(String caller, String responder, String time) {
        String key = caller + responder + time;
        List<String> res = this.pairAnswerMap.get(key);
        return (res != null ? res : new ArrayList<>());
    }

    /**
     * 获取一个响应者在某个时间范围内的答案(平均调用成功率)
     *
     * @param responder 响应者
     * @param start 开始时间(分)
     * @param end 结束时间(分)
     * @return 答案
     */
    public String getResponderAnswer(String responder, String start, String end) {
        /* 如果该区间未被计算过，先进行计算 */
        String key = responder + start + end;
        String ans = this.responderAnswerMap.get(key);
        if(ans == null) {
            ans = this.responderAnswerCompute(responder, start, end, key);
        }
        return ans;
    }

}