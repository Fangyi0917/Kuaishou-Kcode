package com.kuaishou.kcode;

import java.io.*;
import java.util.*;
import java.util.ArrayList;


public class KcodeQuestion {

    private static Operation[] operations = new Operation[1024*1024];
    private static final Result result = new Result();
    /**
     * prepare() 方法用来接受输入数据集，数据集格式参考README.md
     *
     * @param inputStream
     */
    /*输入格式如下(132610000行，按照时间戳升序)
        1587987945950,mockUser2,43
        1587987945398,mockUser8,35
        1587987945321,getInfo7,60
        1587987945010,checkPass7,10
     */
    //测试数据1s最多20W条，一共69个方法，时间跨度4201s
    public void prepare(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        String[] strs;
        int idx = 0;
        long oldTimeStamp = -1;
        long invokeTime = 0;


        //while ((line = reader.readLine()) != null){
        while (reader.ready()){
            line = reader.readLine();
            strs = line.split(",");
            invokeTime = Long.parseLong(strs[0]);
            if(invokeTime/1000 != oldTimeStamp){
                prepareResult(oldTimeStamp, idx);
                oldTimeStamp = invokeTime / 1000;
                idx = 0;
            }
            if(operations[idx] == null) operations[idx] = new Operation();
            //timeStampMs not set
            operations[idx++].setMethodName(strs[1])
                    .setDuration(Integer.parseInt(strs[2]));
        }
        prepareResult(oldTimeStamp, idx);
    }

    /**
     * getResult() 方法是由kcode评测系统调用，是评测程序正确性的一部分，请按照题目要求返回正确数据
     * 输入格式和输出格式参考 README.md
     *
     * @param timestamp 秒级时间戳
     * @param methodName 方法名称
     */
    //测试数据共289800条
    public String getResult(Long timestamp, String methodName) {
//        return "QPS,P99,P50,AVG,MAX";
        return result.getResult(timestamp,methodName);
    }

    /**
     * 按照方法-时间排序，在对应的方法内加入对应时间戳的结果
     * @param timestamp 秒级时间戳
     * @param cnt 当前秒的操作个数
     */
    public void prepareResult(long timestamp,int cnt){
        if(timestamp < 0 || cnt == 0) return;
        //System.out.println("=="+cnt);
        Arrays.sort(operations, 0, cnt, (o1, o2) -> {
            int ret = o1.getMethodName().compareTo(o2.getMethodName());
            if(ret == 0){
                return Integer.compare(o1.getDuration(), o2.getDuration());
            }
            return ret < 0 ? -1 : 1;
        });
        String oldMethodName = "";
        int lastIdx = -1;
        for(int i = 0; i < cnt; i++){
            if(!operations[i].getMethodName().equals(oldMethodName)){
                if(lastIdx >= 0){
                    result.addResult(timestamp, oldMethodName, computeAns(lastIdx, i));
                }
                oldMethodName = operations[i].getMethodName();
                lastIdx = i;
            }
            //System.out.println(timestamp+" "+operations[i].getMethodName()+" "+operations[i].getDuration());
        }
        result.addResult(timestamp, oldMethodName, computeAns(lastIdx, cnt));
    }

    public String computeAns(int st,int ed){
        //"QPS,P99,P50,AVG,MAX"
        int QPS = ed - st;
        int P99 = operations[st + (int) Math.ceil(QPS * 0.99) - 1].getDuration();
        int P50 = operations[st + (int) Math.ceil(QPS * 0.50) - 1].getDuration();
        int sum = 0;
        for(int i = st; i < ed; i++){
            sum += operations[i].getDuration();
        }
        int AVG = (sum + ed - st - 1) / QPS;
        int MAX = operations[ed - 1].getDuration();
        return QPS+","+P99+","+P50+","+AVG+","+MAX;
    }
}