package com.zhaoli.liojcodesandbox;

import cn.hutool.core.io.FileUtil;
import com.zhaoli.liojcodesandbox.model.ExecuteCodeRequest;
import com.zhaoli.liojcodesandbox.model.ExecuteCodeResponse;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * @author 赵立
 */
public class JavaNativeCodeSandbox implements CodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String userDir = System.getProperty("user.dir");// 获取当前用户工作目录路径
        // 构建全局代码路径名，使用文件分隔符以便在不同操作系统上正确组合路径
        String globalCodePathName = userDir + File.separator + "tmoCode";
        //判断全局代码目录是否存在，没有则新建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodePath = globalCodePathName + File.separator + UUID.randomUUID();

        return null;
    }
}
