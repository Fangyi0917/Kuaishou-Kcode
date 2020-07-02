package com.kuaishou.kcode;

/**
 * @description:
 * @author: wfy
 * @create: 2020-06-06 18:12
 **/
public class Operation {

    private long timeStampMs;
    private String methodName;
    private int duration;

    public Operation() {
    }

    public Operation(long timeStampMs, String methodName, int duration) {
        this.timeStampMs = timeStampMs;
        this.methodName = methodName;
        this.duration = duration;
    }

    public long getTimeStampMs() {
        return timeStampMs;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getDuration() {
        return duration;
    }

    public Operation setTimeStampMs(long timeStampMs) {
        this.timeStampMs = timeStampMs;
        return this;
    }

    public Operation setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public Operation setDuration(int duration) {
        this.duration = duration;
        return this;
    }


}
