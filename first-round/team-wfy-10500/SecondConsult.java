package com.kuaishou.kcode;

/**
 * @author kcode
 * @create 2020-06-28 22:38
 */
public class SecondConsult {

    public long count=0;
    public long successCnt=0;

    public void incrementCount(){
        this.count++;
    }

    public long getCount() {
        return count;
    }


    public long getSuccessCnt() {
        return successCnt;
    }

    public void setSuccessCnt(long successCnt) {
        this.successCnt = successCnt;
    }
}
