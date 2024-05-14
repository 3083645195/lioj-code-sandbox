package com.zhaoli.liojcodesandbox;

import com.zhaoli.liojcodesandbox.model.ExecuteCodeRequest;
import com.zhaoli.liojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * java 原生代码沙箱实现（直接复用模板方法）
 *
 * @author 赵立
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
