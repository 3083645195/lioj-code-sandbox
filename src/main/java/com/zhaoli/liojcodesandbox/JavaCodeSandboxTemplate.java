package com.zhaoli.liojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zhaoli.liojcodesandbox.model.*;
import com.zhaoli.liojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * java 代码沙箱模板方法的实现
 *
 * @author 赵立
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    /**
     * 全局代码目录名称
     */
    private static final String GLOBAL_CODE_DIR_NAME = "tmoCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    /**
     * 用户工作目录路径
     */
    private static String USER_DIR;
    /**
     * 全局代码路径
     */
    private static String GLOBAL_CODE_PATH_NAME;
    /**
     * 用户代码所在的父路径
     */
    public static String USER_CODE_PARENT_PATH;
    /**
     * 超时时间
     */
    private static final long TIME_OUT = 5000l;

    /**
     * security 所在的路径
     */
    private static final String SECURITY_MANAGER_PATH = "D:\\java\\鱼皮\\项目\\OJ判题系统\\lioj-code-sandbox\\src\\main\\resources\\security";
    /**
     * SecurityManager.class 的名称
     */
    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";
    /**
     * 返回响应
     */
    private static ExecuteCodeResponse executeCodeResponse;

    /**
     * 判题信息
     */
    private static JudgeInfo judgeInfo;

    static {
        USER_DIR = System.getProperty("user.dir");// 获取当前用户工作目录路径
        // 构建全局代码路径名，使用文件分隔符以便在不同操作系统上正确组合路径
        GLOBAL_CODE_PATH_NAME = USER_DIR + File.separator + GLOBAL_CODE_DIR_NAME;
        executeCodeResponse = new ExecuteCodeResponse();
        judgeInfo = new JudgeInfo();
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        try {
            List<String> inputList = executeCodeRequest.getInputList();
            String code = executeCodeRequest.getCode();
            String language = executeCodeRequest.getLanguage();

            //1.把用户的代码保存为文件
            File userCodeFile = saveCodeToFile(code);

            // 2.编译代码，得到 class 文件
            ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
            System.out.println(compileFileExecuteMessage);


            // 3.执行代码，得到输出结果
            List<ExecuteMessage> executeMessageList = runFile(inputList);

            // 4.收集整理输出结果
            ExecuteCodeResponse executeCodeResponse = getOutputResponse(executeMessageList);
        } catch (Exception e) {
            throw new RuntimeException("异常");
        } finally {
            boolean result = deleteFile();
            if (!result) {
                log.error("deleteFile error,userCodeFilePath{}" + USER_CODE_PARENT_PATH);
            }
            return executeCodeResponse;
        }
    }

    /**
     * 1.把用户的代码保存为文件
     *
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        //判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(GLOBAL_CODE_PATH_NAME)) {
            FileUtil.mkdir(GLOBAL_CODE_PATH_NAME);
        }
        //把用户的代码隔离存放
        USER_CODE_PARENT_PATH = GLOBAL_CODE_PATH_NAME + File.separator + UUID.randomUUID();
        String userCodePath = USER_CODE_PARENT_PATH + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2.编译代码，得到 class 文件
     *
     * @param userCodeFile 编译代码的路径
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String comoileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            Process compileProcess = Runtime.getRuntime().exec(comoileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                judgeInfo.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getText());
                executeCodeResponse.setJudgeInfo(judgeInfo);
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3.执行代码，得到输出结果
     *
     * @param inputList 输入用例
     * @return
     */
    public List<ExecuteMessage> runFile(List<String> inputList) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx512m -Dfile.encoding=UTF-8 -cp %s Main %s", USER_CODE_PARENT_PATH, inputArgs);
            //加上java安全管理器
//            String runCmd = String.format("java -Xmx512m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //执行外部命令，并设置了一个超时时间，在超时之后如果进程仍未执行完毕，则销毁这个进程
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                    } catch (InterruptedException e) {
                        judgeInfo.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getText());
                        executeCodeResponse.setJudgeInfo(judgeInfo);
                        throw new IllegalStateException();
                    }
                    if (runProcess.isAlive()) {
                        System.out.println("超时了，中断！");
                        judgeInfo.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getText());
                        executeCodeResponse.setJudgeInfo(judgeInfo);
                        runProcess.destroy();
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
                if (executeMessage.getErrorMessage() != null) {
                    judgeInfo.setMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getText());
                    executeCodeResponse.setJudgeInfo(judgeInfo);
                }
            } catch (IOException e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4.收集整理输出结果
     *
     * @param executeMessageList 程序执行输出结果
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        //取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotEmpty(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                //用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        //正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMessage(JudgeInfoMessageEnum.ACCEPTED.getText());
        //要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;

    }

    /**
     * 5.文件清理，释放空间
     *
     * @return
     */
    public boolean deleteFile() {
        if (FileUtil.exist(USER_CODE_PARENT_PATH)) {
            boolean delResult = FileUtil.del(USER_CODE_PARENT_PATH);
            System.out.println("删除" + (delResult ? "成功" : "失败"));
            return delResult;
        }
        return true;
    }

    /**
     * 6.获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        // 6.错误处理，提升程序健壮性
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        //表示代码沙箱出现错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
