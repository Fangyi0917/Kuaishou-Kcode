package com.kuaishou.kcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description:
 * @author: wfy
 * @create: 2020-05-31 15:26
 **/
public class ReadOnlyTask implements Runnable{
    private InputStream inputStream;
    private AtomicInteger n;
    private String line;
    public ReadOnlyTask(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
    }
    @Override
    public void run(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while(true){
            try {
                if (!reader.ready()) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                line  = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            n.getAndAdd(1);
        }
    }


}
