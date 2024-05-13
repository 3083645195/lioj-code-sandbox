package com.zhaoli.liojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.zhaoli.liojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;

/**
 * 进程工具类
 *
 * @author 赵立
 */
public class ProcessUtils {
    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();//开始执行时间
            //等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                //正常退出
                System.out.println(opName + "成功");
                //分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuffer compileOutputStringBuffer = new StringBuffer();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuffer.append(compileOutputLine);
                }
                executeMessage.setMessage(compileOutputStringBuffer.toString());
            } else {
                //异常退出
                System.out.println(opName + "失败，错误码：" + exitValue);
                //分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuffer compileOutputStringBuffer = new StringBuffer();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuffer.append(compileOutputLine).append("\n");
                }
                //分批获取进程的错误输出
                BufferedReader erroeBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuffer erroeCompileOutputStringBuffer = new StringBuffer();
                //逐行读取
                String erroeCompileOutputLine;
                while ((erroeCompileOutputLine = erroeBufferedReader.readLine()) != null) {
                    erroeCompileOutputStringBuffer.append(erroeCompileOutputLine).append("\n");
                }
                executeMessage.setErrorMessage(erroeCompileOutputStringBuffer.toString());
            }
            stopWatch.stop();//执行结束时间
            //获取程序执行时间
            long time = stopWatch.getLastTaskTimeMillis();
            executeMessage.setTime(time);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException();
        }
        return executeMessage;
    }

    /**
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String opName, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            //向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            //相当于按下回车键，执行输入发送
            outputStreamWriter.flush();

            //分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer compileOutputStringBuffer = new StringBuffer();
            //逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuffer.append(compileOutputLine).append("\n");
            }
            executeMessage.setMessage(compileOutputStringBuffer.toString());
            //一定要记得资源的回收，否则会卡住
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return executeMessage;
    }
}
