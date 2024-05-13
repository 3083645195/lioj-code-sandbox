package com.zhaoli.liojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.zhaoli.liojcodesandbox.model.ExecuteCodeRequest;
import com.zhaoli.liojcodesandbox.model.ExecuteCodeResponse;
import com.zhaoli.liojcodesandbox.model.ExecuteMessage;
import com.zhaoli.liojcodesandbox.model.JudgeInfo;
import com.zhaoli.liojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * java 原生代码沙箱实现（老版本）
 *
 * @author 赵立
 */
public class JavaNativeCodeSandboxOld implements CodeSandbox {
    /**
     * 全局代码目录名称
     */
    private static final String GLOBAL_CODE_DIR_NAME = "tmoCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    /**
     * 超时时间
     */
    private static final long TIME_OUT = 5000l;

    /**
     * 用户代码执行黑名单
     */
    private static final List<String> BLACK_LIST = Arrays.asList("Files", "exec");
    /**
     * security 所在的路径
     */
    private static final String SECURITY_MANAGER_PATH = "D:\\java\\鱼皮\\项目\\OJ判题系统\\lioj-code-sandbox\\src\\main\\resources\\security";
    /**
     * SecurityManager.class 的名称
     */
    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";
    private static final WordTree WORD_TREE;

    static {
        //初始化字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(BLACK_LIST);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        //校验用户代码是否有黑名单中的操作
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if(foundWord!=null){
//            System.out.println("包含敏感词："+foundWord.getFoundWord());
//            return null;
//        }

        // 1.把用户的代码保存为文件
        String userDir = System.getProperty("user.dir");// 获取当前用户工作目录路径
        // 构建全局代码路径名，使用文件分隔符以便在不同操作系统上正确组合路径
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2.编译代码，得到 class 文件
        String comoileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            Process compileProcess = Runtime.getRuntime().exec(comoileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (IOException e) {
            return getErrorResponse(e);
        }
        // 3.执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx512m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            //加上java安全管理器
//            String runCmd = String.format("java -Xmx512m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //执行外部命令，并设置了一个超时时间，在超时之后如果进程仍未执行完毕，则销毁这个进程
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException();
                    }
                    if (runProcess.isAlive()) {
                        System.out.println("超时了，中断！");
                        runProcess.destroy();
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }
        // 4.收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
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
        //要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 5.文件清理，释放空间
        if (userCodeFile.getParentFile() != null) {
            boolean delResult = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (delResult ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }

    /**
     * 获取错误响应
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

    public static void main(String[] args) {
        JavaNativeCodeSandboxOld javaNativeCodeSandboxOld = new JavaNativeCodeSandboxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandboxOld.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);

    }
}
