package com.kuaishou.kcode.multithread.b_otherThink;

import com.kuaishou.kcode.model.BlockIndexPair;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author wdy
 * @create 2020-06-23 21:05
 */
public class MultiThreadFileProcessor_nosort_BufferedRAF_一万一分 {

    private final long fileLength;
    public List<BlockIndexPair> blockIndexPairList;
    private BufferedRandomAccessFile randomAccessFile;
    private Long splitNum = -1L;

    public Long getSplitNum() {
        return splitNum;
    }

    private String path;

    private ExecutorService executorService;
    private int threadPoolSize;
    private CountDownLatch countDownLatch;

    private AtomicLong atomicLong = new AtomicLong();

    private Long startMin = -1L;
    private Long endMin = -1L;
    private Map<String,Integer> timeFormatMap;

    public Map<String, Integer> getTimeFormatMap() {
        return timeFormatMap;
    }

    public Long getStartMin() {
        return startMin;
    }

    //最终的结果数组
    private Map<Long, List<String>>[] firStageResultArr;
    private Map<Long, Double>[] secStageResultArr;

    public Map<Long, List<String>>[] getFirStageResultArr() {
        return firStageResultArr;
    }

    public Map<Long, Double>[] getSecStageResultArr() {
        return secStageResultArr;
    }

    //供子线程使用
    private ConcurrentHashMap<Long, String> reqMap = new ConcurrentHashMap<>(100);
    private ConcurrentHashMap<Long, String> resMap = new ConcurrentHashMap<>(100);
    private ConcurrentHashMap<Long, String> reqIpMap = new ConcurrentHashMap<>(500);
    private ConcurrentHashMap<Long, String> resIpMap = new ConcurrentHashMap<>(500);

    public ConcurrentHashMap<Long, String> getReqMap() {
        return reqMap;
    }

    public ConcurrentHashMap<Long, String> getResMap() {
        return resMap;
    }

    public ConcurrentHashMap<Long, String> getReqIpMap() {
        return reqIpMap;
    }

    public ConcurrentHashMap<Long, String> getResIpMap() {
        return resIpMap;
    }

    public MultiThreadFileProcessor_nosort_BufferedRAF_一万一分(String path, int threadPoolSize) {
        this.path = path;
        File file = new File(path);
        this.fileLength = file.length();
        this.blockIndexPairList = new ArrayList<>();
        try {
            randomAccessFile = new BufferedRandomAccessFile(file, "r");
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.executorService = Executors.newFixedThreadPool(threadPoolSize);

        this.firStageResultArr = new ConcurrentHashMap[30];
        this.secStageResultArr = new ConcurrentHashMap[30];

        this.timeFormatMap = new HashMap<>();
    }

    /**
     * @description prepare方法：执行阶段一：分解并处理文件
     */
    public void prapare() throws Exception {
        // 1.先获得分块的索引：blockIndexPairList
        getBlockIndexPairList();


        Future<Integer> timeFormatFuture = executorService.submit(this::computeTimeString);

        int splitNumInt = Math.toIntExact(splitNum);

        countDownLatch = new CountDownLatch(Math.toIntExact(splitNumInt));


        for (BlockIndexPair pair : blockIndexPairList) {
            executorService.execute(new BlockProcessTask(pair));
        }

        countDownLatch.await();
        timeFormatFuture.get();
        stop();

    }

    private Integer computeTimeString(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for(long i = startMin;i<=endMin;i++){
            String timeMin = dateFormat.format(i * 60 * 1000);
            timeFormatMap.put(timeMin,Math.toIntExact(i-startMin));
        }
        return 0;
    }


    // 关闭资源
    public void stop() {
        try {
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
    }


    // 线程任务：处理块----------------------------------------------------------------
    private class BlockProcessTask implements Runnable {
        private long startIndex;
        private long blockSize;
        private long endIndex;
        private long curMinute;

        private BufferedRandomAccessFile rAccessFile;

        MappedByteBuffer mapBuffer;

        ExecutorService computeExecutorService;
        CountDownLatch resLatch;
        List<Future> futureTaskList;

        private Map<Long, Map<Long, FirMidRes>> computFirResMap;//第一、二阶段中间结果
        private Map<Long, SecMidRes> computeSecResMap;
        private Map<Long, List<String>> firFinalResMap;//第一、二阶段最终结果
        private Map<Long, Double> secFinalResMap;

        public BlockProcessTask(BlockIndexPair blockIndexPair) {
            this.startIndex = blockIndexPair.getStart();
            this.endIndex = blockIndexPair.getEnd();
            this.blockSize = blockIndexPair.getEnd() - startIndex;
            this.curMinute = blockIndexPair.getCurMinu();
            this.computeExecutorService = Executors.newFixedThreadPool(3);//单个数据处理线程
            this.resLatch = new CountDownLatch(6);//并行计算结果的latch
            try {//试一下怎么快
                this.rAccessFile = new BufferedRandomAccessFile(path, "r",1<<18);//8M缓冲区
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.computFirResMap = new HashMap<>();
            this.computeSecResMap = new HashMap<>();
            this.firFinalResMap = new ConcurrentHashMap<>();
            this.secFinalResMap = new ConcurrentHashMap<>();
        }

        public void run() {
            try {
//                long t1 = System.currentTimeMillis();
                rAccessFile.seek(startIndex);
                //--------------------进行解析--------------------------
                int bytz;
                int flag = 0;

                int reqNum = 0;//为了保证与真实Hash相同，必须用int
                int resNum = 0;
                long reqIPNum = 0;
                long resIPNum = 0;

                int nameMoveCnt = 0;
                int ipMoveCnt = 1;
                int ipNumCnt = 0;//记录
                int ipPerNum = 0;//IP的每一位

                int isSucces = 0;
                long isSuccesCnt = 0;

                int costTime = 0;//调用耗时

                long keyName = 0;
                long keyIp = 0;

                Map<Long, FirMidRes> keyNameMap;
                FirMidRes firMidRes;
                SecMidRes secMidRes;
                long[] markPosArr = new long[7];//标记字段坐标
                long lineStart = rAccessFile.getFilePointer();//标记每一行开始的坐标

                byte[] bytes = new byte[50];
                byte[] reqIpBytes = new byte[60];
                byte[] resIpBytes = new byte[60];
                int reqIpIndex = 0;
                int resIpIndex = 0;


                while (rAccessFile.getFilePointer() < endIndex) {
                    bytz = rAccessFile.read();
                    if (bytz != ',' && bytz != '\n' && bytz != '\r') {
                        if (flag == 0) {
                            reqNum = reqNum * 31 + bytz;
                        }
                        if (flag == 2) {
                            resNum = resNum * 31 + bytz;
                        }
                        if (flag == 1) {
                            reqIpBytes[reqIpIndex++] = (byte) bytz;
                            if (bytz != '.') {
                                ipPerNum = ipPerNum * 10 + bytz - 48;
                            } else {
                                reqIPNum += ((reqIPNum << 8) + ipPerNum);
                                ipPerNum = 0;
                            }
                        }
                        if (flag == 3) {
                            resIpBytes[resIpIndex++] = (byte) bytz;
                            if (bytz != '.') {
                                ipPerNum = ipPerNum * 10 + bytz - 48;
                            } else {
                                resIPNum += ((resIPNum << 8) + ipPerNum);
                                ipPerNum = 0;
                            }
                        }
                        if (flag == 4) {
                            if (bytz == 116) {//true
                                isSucces = 1;
                                rAccessFile.seek(rAccessFile.getFilePointer() + 3);
                            } else {//false
                                isSucces = 0;
                                rAccessFile.seek(rAccessFile.getFilePointer() + 4);
                            }
                        }
                        if (flag == 5) {
                            costTime = costTime * 10 + bytz - 48;
                        }
                        if (flag == 6) {
                            rAccessFile.seek(rAccessFile.getFilePointer() + 12);
                        }
                    } else if (bytz == ',') {//重置必要的 重用中间 标记状态；并标记字段坐标
                        if (flag == 3) {
                            resIPNum += ((resIPNum << 8) + ipPerNum);
                        }
                        if (flag == 1) {
                            reqIPNum += ((reqIPNum << 8) + ipPerNum);
                        }
                        // hash 防止溢出
                        if (flag == 0) {
                            reqNum = reqNum > 0 ? reqNum : -reqNum;
                        }
                        if (flag == 2) {
                            resNum = resNum > 0 ? resNum : -resNum;
                        }

                        markPosArr[flag++] = rAccessFile.getFilePointer() - 1;
                        ipPerNum = 0;
                        nameMoveCnt = 0;

                    } else { // 为换行符或者结尾-->解析完一条记录，进行处理

                        if (reqIpMap.get(reqIPNum) == null) {
                            String strreqIp = new String(reqIpBytes, 0, reqIpIndex);
                            reqIpMap.put(reqIPNum, strreqIp);
                        }
                        if (resIpMap.get(resIPNum) == null) {
                            String strresIp = new String(resIpBytes, 0, resIpIndex);
                            resIpMap.put(resIPNum, strresIp);
                        }
                        //置位
//                        rAccessFile.seek(mark);
                        //存结果(第一阶段)
                        keyName = (((long) reqNum) << 32) | ((long) resNum);
                        if (computFirResMap.containsKey(keyName)) {
                            keyNameMap = computFirResMap.get(keyName);
                        } else {
                            keyNameMap = new HashMap<>();
                            computFirResMap.put(keyName, keyNameMap);
                        }
                        keyIp = (reqIPNum << 32) | resIPNum;
                        if (keyNameMap.containsKey(keyIp)) {
                            firMidRes = keyNameMap.get(keyIp);
                        } else {
                            firMidRes = new FirMidRes();
                            keyNameMap.put(keyIp, firMidRes);
                        }
                        firMidRes.getList().add(costTime);
                        firMidRes.setIsSuccessCnt(firMidRes.getIsSuccessCnt() + isSucces);
                        //存结果(第二阶段)
                        if (computeSecResMap.containsKey((long)resNum)) {
                            secMidRes = computeSecResMap.get((long)resNum);
                        } else {
                            secMidRes = new SecMidRes();
                            computeSecResMap.put((long) resNum, secMidRes);
                        }
                        //第二阶段计数
                        secMidRes.incrementCount();
                        secMidRes.setSuccessCnt(secMidRes.getSuccessCnt() + isSucces);

                        //重置变量
                        lineStart = rAccessFile.getFilePointer();//下一行的起始坐标
                        reqIPNum = resIPNum = isSucces = costTime = ipPerNum = nameMoveCnt = flag = 0;
                        reqNum = 0;
                        resNum = 0;
                        reqIpIndex = 0;
                        resIpIndex = 0;

                    }
                    // 这里直接计算第二阶段的中间结果（后面试试）

                }
                //转成数组，便于切分线程任务(三线程执行)
                //第一阶段
                Set<Map.Entry<Long, Map<Long, FirMidRes>>> firEntries = computFirResMap.entrySet();
                Object[] firObjects = firEntries.toArray();
//                firEntries = null;
                int firPerNum = firObjects.length / 3;
                computeExecutorService.execute(() -> precessFirFinalRes(firObjects, 0, firPerNum));
                computeExecutorService.execute(() -> precessFirFinalRes(firObjects, firPerNum, 2 * firPerNum));
                computeExecutorService.execute(() -> precessFirFinalRes(firObjects, 2 * firPerNum, firObjects.length));
                //第二阶段
                Set<Map.Entry<Long, SecMidRes>> secEntries = computeSecResMap.entrySet();
                Object[] secObjects = secEntries.toArray();
                int secPerNum = secObjects.length / 3;
                computeExecutorService.execute(() -> precessSecFinalRes(secObjects, 0, secPerNum));
                computeExecutorService.execute(() -> precessSecFinalRes(secObjects, secPerNum, 2 * secPerNum));
                computeExecutorService.execute(() -> precessSecFinalRes(secObjects, 2 * secPerNum, secObjects.length));

                try {
                    resLatch.await();//等待线程计算完
                    computeExecutorService.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //将该分钟结果存入结果数组
                int index = Math.toIntExact(curMinute - startMin);
                firStageResultArr[index] = firFinalResMap;
                secStageResultArr[index] = secFinalResMap;


                countDownLatch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void precessSecFinalRes(Object[] objects, int start, int end) {
            Long resNum;
            SecMidRes secMidRes;
            double successPercent;
            for (int i = start; i < end; i++) {
                Map.Entry<Long, SecMidRes> mapEntry = (Map.Entry<Long, SecMidRes>) objects[i];
                resNum = mapEntry.getKey();
                secMidRes = mapEntry.getValue();
                successPercent = Math.floor((double)secMidRes.getSuccessCnt() / (double)secMidRes.getCount() * 10000)/100;
                secFinalResMap.put(resNum, successPercent);
            }
            resLatch.countDown();
        }

        private void precessFirFinalRes(Object[] objects, int start, int end) {

            String reqIp;
            String resIp;
            int P99;
            double successPercent;
            StringBuilder itemSb = new StringBuilder();
            FirMidRes firMid;
            PriorityQueue<Integer> priorityQueue = new PriorityQueue<>((o1, o2) -> o2 - o1);//大顶堆
            for (int i = start; i < end; i++) {
                Map.Entry<Long, Map<Long, FirMidRes>> mapEntry = (Map.Entry<Long, Map<Long, FirMidRes>>) objects[i];
                Long reqResLong = mapEntry.getKey();

                Map<Long, FirMidRes> midMap = mapEntry.getValue();
                List<String> itemList = new ArrayList<>();//必须内部建
                for (Map.Entry<Long, FirMidRes> midEntry : midMap.entrySet()) {
                    long keyIp = midEntry.getKey();
                    long resIpNum = keyIp & 0XFFFFFFFFL;
                    long reqIpNum = (keyIp >>> 32) & 0XFFFFFFFFL;
                    reqIp = reqIpMap.get(reqIpNum);
                    resIp = resIpMap.get(resIpNum);
                    // 主被调按ip聚合的成功率和P99
                    firMid = midEntry.getValue();
                    priorityQueue.addAll(firMid.getList());
                    int priorityQueueSize = priorityQueue.size();
                    int cnt = priorityQueue.size() - (int) Math.ceil(priorityQueue.size() * 0.99);
                    while (cnt-- > 0) {
                        priorityQueue.poll();
                    }
                    P99 = priorityQueue.poll();
                    successPercent = Math.floor((double) firMid.getIsSuccessCnt()  / (double) priorityQueueSize *10000)/100;
                    priorityQueue.clear();
                    itemSb.append(reqIp).append(",").append(resIp).append(",");
                    if (successPercent > 0) {
                        itemSb.append(String.format("%.2f%%", successPercent)).append("%,").append(P99);
                    } else {
                        itemSb.append(".00%").append(",").append(P99);
                    }
                    itemList.add(itemSb.toString());
                    itemSb.delete(0, itemSb.length());//清空
                }
                firFinalResMap.put(reqResLong, itemList);
            }
            resLatch.countDown();
        }


        public long convertTimeToMinStamp(String time) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                Date date = simpleDateFormat.parse(time);
                long minStamp = date.getTime() / 1000 / 60;
                return minStamp;
            } catch (ParseException e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    // 处理分块索引--------------------------------------------------------------------
    public void getBlockIndexPairList() throws Exception {
        this.startMin = getLineMinute(getThisLine(0));
        this.endMin = getLineMinute(getThisLine(fileLength - 2));
        splitNum = endMin - startMin + 1;

        getBlockPositionPairs(startMin, startMin + 1, 0);
    }

    // 获得分块位置的起始坐标对
    public void getBlockPositionPairs(long srcMin, long targetMin, long srcIndex) throws Exception {
        if (targetMin == endMin + 1) {
            BlockIndexPair endPair = new BlockIndexPair(srcIndex, fileLength, srcMin);
            blockIndexPairList.add(endPair);
//            System.out.println("find ong pire: " + endPair);
            return;
        }
        long midIndex;
        long endIndex = fileLength - 1;
        long startIndex = srcIndex;
        long startMin = srcMin;
        long midMin = 0;

        while (endIndex - startIndex > 1) {
            midIndex = (startIndex + endIndex) / 2;
            String line = getThisLine(midIndex);
            midMin = getLineMinute(line);
            if (midMin - startMin >= 1) {
                endIndex = midIndex;
            } else {
                startIndex = midIndex;
                startMin = getLineMinute(getThisLine(startIndex));
            }
        }
        BlockIndexPair pair = new BlockIndexPair(srcIndex, endIndex, srcMin);
        blockIndexPairList.add(pair);
        getBlockPositionPairs(srcMin + 1, srcMin + 2, endIndex);
    }

    public long getLineMinute(String line) {
        long minute = 0;
        try {
            minute = Long.parseLong(line.split(",")[6]) / 1000 / 60;
        } catch (Exception e) {
            try {
                throw new Exception(line);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return minute;
    }

    // 获得索引位置的当前行
    public String getThisLine(long position) throws Exception {
        if (position < 0) {
            throw new Exception("当前坐标为负数！");
        } else if (position >= fileLength) {
            throw new Exception("当前坐标大于文件总长度！");
        }
        int index = 0;
        //先处理position定位到换行符的问题
        randomAccessFile.seek(position);
        if (randomAccessFile.read() == '\n') {
            position--;
        }
        while (true) {
            if (position - index > 0) {
                randomAccessFile.seek(position - index);
                if (randomAccessFile.read() != '\n') {
                    index++;
                } else {
                    position -= index - 1;
                    break;
                }
            } else {
                position -= index;
                break;
            }
        }
        randomAccessFile.seek(position);
        String line = randomAccessFile.readLine();
        return line;
    }
}
