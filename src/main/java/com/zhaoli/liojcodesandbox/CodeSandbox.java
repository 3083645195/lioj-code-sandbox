package com.zhaoli.liojcodesandbox;


import com.zhaoli.liojcodesandbox.model.ExecuteCodeRequest;
import com.zhaoli.liojcodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 *
 * @author 赵立
 */
public interface CodeSandbox {
    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
