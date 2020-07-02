package com.kuaishou.kcode;

/**
 * @author kcode
 * @create 2020-06-23 21:01
 */
public class BlockIndexPair {

    private long start;
    private long end;
    private long curMinu;

    public long getCurMinu() {
        return curMinu;
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

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

}
