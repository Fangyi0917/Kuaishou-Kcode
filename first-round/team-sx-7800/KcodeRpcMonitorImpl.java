package com.kuaishou.kcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.List;

/**
 * @description:
 * @author: wfy
 * @create: 2020-06-29 19:25
 **/
public class KcodeRpcMonitorImpl implements KcodeRpcMonitor{
    private final static int BUFFER_SIZE = Integer.MAX_VALUE;
    private final static int STRAT_BUFFER_SIZE = Integer.MAX_VALUE;

    /* 答案 */
    private Answer answer = new Answer();

    /* 数据区域 */
    private HashSet<String> methodAndIpPairStringSet = new HashSet<>();
    private String[] methodAndIpPairStringArray = null;

    /* 构造函数 */
    public KcodeRpcMonitorImpl() {

    }

    /**
     * 线下数据集信息：
     *  nr_caller   nr_ip_caller    nr_responder    nr_ip_responder     scope_time_minute
     *  78          439             70              388                 30
     */
    public void prepare(String path) throws IOException {

        FileInputStream inputStream = new FileInputStream(path);//2kcodeRpcMonitor.data
        FileChannel fileChannel = (inputStream).getChannel();
        long fileSize  = fileChannel.size();

        /* 初始化当前时间 */
        BufferedReader reader = new BufferedReader(new FileReader(path));
        long currTime = (Long.parseLong(reader.readLine().split(",")[6]) / (1000 * 60));
        reader.close();

        /* 得到所有主调对 */
//        reader = new BufferedReader(new FileReader(path));
//        String line;
//        String[] split;
        String methodAndIpPairString;
        String caller;      /* 调用方 */
        String callerIP;    /* 调用方 IP */
        String responder;      /* 被调用目标方 */
        String responderIP;    /* 被调用目标方 IP */
        boolean success;      /* 调用成功？ */
        int elapsedTime;    /* 调用耗时 */
        long startTime;      /* 调用开始时间 */


        MappedByteBuffer startbuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, STRAT_BUFFER_SIZE);
        Converter startconverter = new Converter(startbuffer, 0);
        int methodAndIpPairSize;
        boolean findOut = false;

        int firstsize = STRAT_BUFFER_SIZE;
        firstsize --;
        while (firstsize >= 0 && (startbuffer.get(firstsize)) != '\n' ) {
            firstsize--;
        }
        firstsize++;
        while ( !findOut && startconverter.offset < firstsize) {
//            line = reader.readLine();
//            /* 先转换 */
//            split = line.split(",");
//            caller = split[0];
//            callerIP = split[1];
//            responder = split[2];
//            responderIP = split[3];
//            success = Boolean.parseBoolean(split[4]);
//            elapsedTime = Integer.parseInt(split[5]);
//            startTime = (Long.parseLong(split[6]) / (1000 * 60));   /* 按分钟算 */

            caller = startconverter.convertString();
            callerIP = startconverter.convertString();
            responder = startconverter.convertString();
            responderIP = startconverter.convertString();
            success = startconverter.convertBoolean();
            elapsedTime = startconverter.convertInt();
            startTime = startconverter.convertLong() / 60000;
            methodAndIpPairString = caller + responder + "|" + callerIP + "," + responderIP;
            if(startTime != currTime) {
                /* 将所有主被调对转换为数组 */
                methodAndIpPairSize = methodAndIpPairStringSet.size();
                methodAndIpPairStringArray
                        = methodAndIpPairStringSet.toArray(new String[methodAndIpPairSize]);

                /* 计算 */
                for(String mp : methodAndIpPairStringArray) {
                    answer.pairAnswerCompute(mp, (int) currTime);
                }
                /* 更新当前时间 */
                currTime = startTime;
                methodAndIpPairStringSet.clear();
                findOut = true;
            }
            /* 将一个主被调对加入集合 */
            methodAndIpPairStringSet.add(methodAndIpPairString);
            /* 加入到输入数据区域 */
            answer.addPairData(methodAndIpPairString, elapsedTime, success);
            answer.addResponderData(responder, (int) startTime, success);
        }

        /* NIO读取并计算 */
        //初始buffer开始偏移量
        long mapOffset = startconverter.offset;
        //long mapOffset = 0;
        //初始buffer大小
        int mapSize = fileSize > BUFFER_SIZE ? BUFFER_SIZE : (int)(fileSize);
        MappedByteBuffer buffer;
        Converter converter;
        while(mapOffset < fileSize) {
            //确定一个2G的buffer
            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, mapOffset, mapSize);
            converter = new Converter(buffer, 0);

            // 找到倒数第一个\n，找到mapsize精确值
            mapSize--;
            while (mapSize >= 0 && (buffer.get(mapSize)) != '\n' ) {
                mapSize--;
            }
            mapSize++;
            //解析当前块的数据
            while(converter.offset < mapSize) {
                caller = converter.convertString();
                callerIP = converter.convertString();
                responder = converter.convertString();
                responderIP = converter.convertString();
                success = converter.convertBoolean();
                elapsedTime = converter.convertInt();
                startTime = converter.convertLong() / 60000;
                methodAndIpPairString = caller + responder + "|" + callerIP + "," + responderIP;
                if(startTime != currTime) {
                    /* 计算 */
                    for(String mp : methodAndIpPairStringArray) {
                        answer.pairAnswerCompute(mp, (int) currTime);
                    }

                    /* 更新当前时间 */
                    currTime = startTime;
                }
                /* 加入到输入数据区域 */
                answer.addPairData(methodAndIpPairString, elapsedTime, success);
                answer.addResponderData(responder, (int) startTime, success);
                //converter.offset ++;
            }
            if(mapSize == 0) {
                caller = converter.convertString();
                callerIP = converter.convertString();
                responder = converter.convertString();
                responderIP = converter.convertString();
                success = converter.convertBoolean();
                elapsedTime = converter.convertInt();
                startTime = converter.convertLong()/ 60000;
                methodAndIpPairString = caller + responder + "|" + callerIP + "," + responderIP;
                if(startTime != currTime) {
                    /* 计算 */
                    for(String mp : methodAndIpPairStringArray) {
                        answer.pairAnswerCompute(mp, (int) currTime);
                    }

                    /* 更新当前时间 */
                    currTime = startTime;
                }
                /* 加入到输入数据区域 */
                answer.addPairData(methodAndIpPairString, elapsedTime, success);
                answer.addResponderData(responder, (int) startTime, success);
                break;
            }
            //更新偏移量，确定下一个buffer的粗略的mapsize大小
            mapOffset += mapSize;
            mapSize = ((fileSize - mapOffset) > BUFFER_SIZE ? BUFFER_SIZE : (int) (fileSize - mapOffset));

        }

        for(String mp : methodAndIpPairStringArray) {
            answer.pairAnswerCompute(mp, (int) currTime);
        }
    }



    public List<String> checkPair(String caller, String responder, String time) {
        return answer.getPairAnswer(caller, responder, time);
    }



    public String checkResponder(String responder, String start, String end) {
        return answer.getResponderAnswer(responder, start, end);
    }


}
