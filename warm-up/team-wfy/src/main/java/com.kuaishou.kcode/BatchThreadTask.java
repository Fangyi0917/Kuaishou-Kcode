package com.kuaishou.kcode;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: wfy
 * @create: 2020-06-01 10:07
 **/
public class BatchThreadTask implements Runnable {

    private List dataList;
    private Map paraMap;
    public BatchThreadTask(List<String> onceList, Map<String, Object> pMap) {
        this.dataList = onceList;
        this.paraMap = pMap;
    }


    @Override
    public void run() {

        for (int y = 0; y < dataList.size(); y++) {

            String s = (String) dataList.get(y);
//            System.out.println("--t--线程名: " + Thread.currentThread().getName() + "--当前批次处理总数据" + dataList.size() + "--当前数据---" + s);
            try {
                Thread.sleep(60);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
