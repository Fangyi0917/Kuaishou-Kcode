package com.kuaishou.kcode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;


public class FirstConsult {

    public PriorityQueue<Integer> list;
    public int isSuccessCnt=0;

    public FirstConsult() {
        this.list = new PriorityQueue<>(100,(o1, o2) -> o2 - o1);
    }


    public PriorityQueue<Integer> getList() {
        return list;
    }

    public int getIsSuccessCnt() {
        return isSuccessCnt;
    }

    public void setIsSuccessCnt(int isSuccessCnt) {
        this.isSuccessCnt = isSuccessCnt;
    }
}
