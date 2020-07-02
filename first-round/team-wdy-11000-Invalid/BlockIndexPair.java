package com.kuaishou.kcode.model;

/**
 * @author wdy
 * @create 2020-06-23 21:01
 */
public class BlockIndexPair {

    private long start;
    private long end;
    private long curMinu;

    public long getCurMinu() {
        return curMinu;
    }

    public void setCurMinu(long curMinu) {
        this.curMinu = curMinu;
    }

    @Override
    public String toString() {
        return "BlockIndexPair{" +
                "start=" + start +
                ", end=" + end +
                ", curMinu=" + curMinu +
                '}';
    }

    public BlockIndexPair(long start, long end, long curMinu) {
        this.start = start;
        this.end = end;
        this.curMinu = curMinu;
    }

    public BlockIndexPair(long start, long end) {
        this.start = start;
        this.end = end;
    }



    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}
