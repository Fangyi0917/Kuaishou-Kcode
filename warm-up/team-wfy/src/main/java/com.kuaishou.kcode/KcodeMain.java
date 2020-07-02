package com.kuaishou.kcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author kcode
 * Created on 2020-05-20
 */
public class KcodeMain {

    public static void main(String[] args) throws Exception {
        long startReadTime =  System.currentTimeMillis();
        //InputStream fileInputStream = new FileInputStream("G:\\warmup-test.data");
        InputStream fileInputStream = new FileInputStream("E:/Projects/kuaishou/warmup-test.data");


         KcodeQuestion question = new KcodeQuestion();
         //KcodeQuestionNew1 question = new KcodeQuestionNew1();
         // 准备数据
         question.prepare(fileInputStream);

        //int n = 0;

        // 验证正确性
        long endReadTime =  System.currentTimeMillis();
        long usedReadTime = (endReadTime-startReadTime);

        //InputStream testdata = new FileInputStream("G:\\result-test.data");
        InputStream testdata = new FileInputStream("E:/Projects/kuaishou/result-test.data");
        BufferedReader reader = new BufferedReader(new InputStreamReader(testdata));
        while(reader.ready()){
            String line = reader.readLine();

            String testinputs = line.split("\\|")[0];
            //String ourRes = question.getResult(1589761895L,"getUserName");
            //System.out.println(Long.parseLong(testinputs.split(",")[0]));
            String ourRes = question.getResult(Long.parseLong(testinputs.split(",")[0]), testinputs.split(",")[1]);
            String accRes = line.split("\\|")[1];

            if (!ourRes.equals(accRes)){
                System.out.println("fail      测试数据：" + line + "      ours：" +ourRes);
                //n ++;
                break;
            }
        }


        long endTime = System.currentTimeMillis();
        long usedComputeTime = endTime - endReadTime;
        long totalTime = (endTime-startReadTime);
        System.out.println("读数据用时：  " + usedReadTime);
        System.out.println("查询计算用时：  " + usedComputeTime);
        System.out.println("总用时：  " + totalTime);
        //System.out.println("错误个数" + n);

    }
}