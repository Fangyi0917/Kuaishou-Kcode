package com.kuaishou.kcode;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.Executors.*;

public class FileProcessor {

    private final long fileLength;
    public List<BlockIndexPair> blockIndexPairList;
    private BufferedRandomAccessFile randomAccessFile;
    private Long splitNum = -1L;

    public Long getSplitNum() {
        return splitNum;
    }

    private String path;

    private ExecutorService pool;
    private CountDownLatch countDownLatch;


    private Long startMin = -1L;
    private Long endMin = -1L;
    private Map<String,Integer> timeFormatMap;

    public Map<String, Integer> getTimeFormatMap() {
        return timeFormatMap;
    }


    //最终的结果数组
    private Map<Long, List<String>>[] firStageResultArr;
    private Map<Long, Long>[] secStageResultArr;

    public Map<Long, List<String>>[] getFirStageResultArr() {
        return firStageResultArr;
    }

    public Map<Long, Long>[] getSecStageResultArr() {
        return secStageResultArr;
    }
    private ConcurrentHashMap<Long, String> reqIpMap = new ConcurrentHashMap<>(512);
    private ConcurrentHashMap<Long, String> resIpMap = new ConcurrentHashMap<>(512);


    public FileProcessor(String path, int threadPoolSize) {
        this.path = path;
        File file = new File(path);
        this.fileLength = file.length();
        this.blockIndexPairList = new ArrayList<>();
        try {
            randomAccessFile = new BufferedRandomAccessFile(file, "r");
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.pool = newFixedThreadPool(threadPoolSize);
        //this.executorService = Executors.newCachedThreadPool();

        this.firStageResultArr = new ConcurrentHashMap[30];
        this.secStageResultArr = new ConcurrentHashMap[30];

        this.timeFormatMap = new HashMap<>();
    }

    /**
     * @description 处理文件
     */
    public void prepare() throws Exception {
        // 索引
        getBlockIndexPairList();

        // 时间set
        Future<Integer> timeFormatFuture = pool.submit(this::computeTimeString);

        int splitNumInt = Math.toIntExact(splitNum);

        countDownLatch = new CountDownLatch(Math.toIntExact(splitNumInt));


        for (BlockIndexPair pair : blockIndexPairList) {
            pool.execute(new BlockProcessTask(pair));
        }

        countDownLatch.await();
        timeFormatFuture.get();
        stop();

    }

    private Integer computeTimeString(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for(long i = startMin;i<=endMin;i++){
            String timeMin = dateFormat.format(i*60000);
            timeFormatMap.put(timeMin,Math.toIntExact(i-startMin));
        }
        return 0;
    }

//    public String toDate(long minutes){
//        final int baseMin = 26515680; // 2020-06-01 00:00
//        StringBuilder date = new StringBuilder("2020-06-");
//        long day = (minutes - baseMin) / (60 * 24) + 1;
//        long hour = (minutes - baseMin - (day - 1 ) * 60 * 24) / 60;
//        long min = (minutes - baseMin) % 60;
//        if(day < 10) {
//            date.append('0');
//        }
//        date.append(day);
//        if(hour < 10) {
//            date.append('0');
//        }
//        date.append(hour);
//        if(min < 10) {
//            date.append('0');
//        }
//        date.append(min);
//        return date.toString();
//    }

    // 关闭资源
    public void stop() {
        try {
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pool.shutdown();
    }


    //
    private class BlockProcessTask implements Runnable {
        private long startIndex;
        private long endIndex;
        private long curMinute;

        private BufferedRandomAccessFile rAccessFile;

        ExecutorService computeExecutorService;
        CountDownLatch resLatch;

        private Map<Long, Map<Long, FirstConsult>> computFirResMap;
        private Map<Long, SecondConsult> computeSecResMap;
        private Map<Long, List<String>> firFinalResMap;
        private Map<Long, Long> secFinalResMap;

        public BlockProcessTask(BlockIndexPair blockIndexPair) {
            this.startIndex = blockIndexPair.getStart();
            this.endIndex = blockIndexPair.getEnd();
            this.curMinute = blockIndexPair.getCurMinu();
            this.computeExecutorService = newFixedThreadPool(3);//单个数据处理线程
            this.resLatch = new CountDownLatch(6);//并行计算结果的latch
            try {
                //this.rAccessFile = new BufferedRandomAccessFile(path, "r",1<<24);//8M缓冲区
                this.rAccessFile = new BufferedRandomAccessFile(path, "r",1<<27);
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
                rAccessFile.seek(startIndex);
                int bytz;
                int flag = 0;

                int reqNum = 0;//为了保证与真实Hash相同，必须用int
                int resNum = 0;
                long reqIPNum = 0;
                long resIPNum = 0;

                int ipPerNum = 0;//IP的每一位

                int isSucces = 0;

                int costTime = 0;//调用耗时

                long keyName ;
                long keyIp ;

                Map<Long, FirstConsult> keyNameMap;
                FirstConsult firMidRes;
                SecondConsult secMidRes;
                long[] markPosArr = new long[7];//标记字段坐标

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
                        if (flag == 4) {//是否成功
                            if (bytz == 116) {//true
                                isSucces = 1;
                                rAccessFile.seek(rAccessFile.getFilePointer() + 3);
                            } else {//false
                                isSucces = 0;
                                rAccessFile.seek(rAccessFile.getFilePointer() + 4);
                            }
                        }
                        if (flag == 5) {//调用耗时
                            costTime = costTime * 10 + bytz - 48;
                        }
                        if (flag == 6) {//时间戳，直接跳过
                            rAccessFile.seek(rAccessFile.getFilePointer() + 12);
                        }
                    } else if (bytz == ',') {//重置必要的 重用中间 标记状态；并标记字段坐标
                        //（IP较多，转换必须正确）
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

                    } else {

                        if (reqIpMap.get(reqIPNum) == null) {
                            String strreqIp = new String(reqIpBytes, 0, reqIpIndex);
                            reqIpMap.put(reqIPNum, strreqIp);
                        }
                        if (resIpMap.get(resIPNum) == null) {
                            String strresIp = new String(resIpBytes, 0, resIpIndex);
                            resIpMap.put(resIPNum, strresIp);
                        }
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
                            firMidRes = new FirstConsult();
                            keyNameMap.put(keyIp, firMidRes);
                        }
                        firMidRes.getList().add(costTime);
                        firMidRes.setIsSuccessCnt(firMidRes.getIsSuccessCnt() + isSucces);
                        //存结果(第二阶段)
                        if (computeSecResMap.containsKey((long)resNum)) {
                            secMidRes = computeSecResMap.get((long)resNum);
                        } else {
                            secMidRes = new SecondConsult();
                            computeSecResMap.put((long) resNum, secMidRes);
                        }
                        //第二阶段计数
                        secMidRes.incrementCount();
                        secMidRes.setSuccessCnt(secMidRes.getSuccessCnt() + isSucces);

                        //重置变量
                        reqIPNum = resIPNum = isSucces = costTime = ipPerNum  = flag = 0;
                        reqNum = 0;
                        resNum = 0;
                        reqIpIndex = 0;
                        resIpIndex = 0;

                    }
                    // 这里直接计算第二阶段的中间结果（后面试试）

                }
                //第一阶段
                Set<Map.Entry<Long, Map<Long, FirstConsult>>> firEntries = computFirResMap.entrySet();
                Object[] firObjects = firEntries.toArray();
                int firPerNum = firObjects.length / 3;
                computeExecutorService.execute(() -> precessFirFinalRes(firObjects, 0, firPerNum));
                computeExecutorService.execute(() -> precessFirFinalRes(firObjects, firPerNum, 2 * firPerNum));
                computeExecutorService.execute(() -> precessFirFinalRes(firObjects, 2 * firPerNum, firObjects.length));
                //第二阶段
                Set<Map.Entry<Long, SecondConsult>> secEntries = computeSecResMap.entrySet();
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
            SecondConsult secMidRes;
            long successPercent;
            for (int i = start; i < end; i++) {
                Map.Entry<Long, SecondConsult> mapEntry = (Map.Entry<Long, SecondConsult>) objects[i];
                resNum = mapEntry.getKey();
                secMidRes = mapEntry.getValue();
                successPercent = secMidRes.getSuccessCnt() * 10000 / secMidRes.getCount();
                secFinalResMap.put(resNum, successPercent);
            }
            resLatch.countDown();
        }

        private void precessFirFinalRes(Object[] objects, int start, int end) {

            String reqIp;
            String resIp;
            int P99;
            long successPercent;
            StringBuilder itemSb = new StringBuilder();
            FirstConsult firMid;
            PriorityQueue<Integer> priorityQueue = new PriorityQueue<>((o1, o2) -> o2 - o1);//大顶堆
            for (int i = start; i < end; i++) {
                Map.Entry<Long, Map<Long, FirstConsult>> mapEntry = (Map.Entry<Long, Map<Long, FirstConsult>>) objects[i];
                Long reqResLong = mapEntry.getKey();

                Map<Long, FirstConsult> midMap = mapEntry.getValue();
                List<String> itemList = new ArrayList<>();//必须内部建
                for (Map.Entry<Long, FirstConsult> midEntry : midMap.entrySet()) {
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
                    successPercent = firMid.getIsSuccessCnt() * 10000 / priorityQueueSize;
                    priorityQueue.clear();
                    itemSb.append(reqIp).append(",").append(resIp).append(",");
                    if (successPercent > 0) {
                        itemSb.append(successPercent).insert(itemSb.length() - 2, ".").append("%,").append(P99);
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

    }

    // 处理分块索引
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
            return;
        }
        long midIndex;
        long endIndex = fileLength - 1;
        long startIndex = srcIndex;
        long startMin = srcMin;
        long midMin;

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
            minute = Long.parseLong(line.split(",")[6]) / (1000 * 60);
        } catch (Exception e) {
            try {
                throw new Exception(line);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return minute;
    }
    public String getThisLine(long position) throws Exception {
        int index = 0;
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
        return randomAccessFile.readLine();
    }
}
