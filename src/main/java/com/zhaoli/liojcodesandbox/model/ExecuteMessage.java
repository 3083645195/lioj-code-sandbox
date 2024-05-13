package com.zhaoli.liojcodesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 *
 * @author 赵立
 */
@Data
public class ExecuteMessage {
    /**
     * 错误码
     */
    private Integer exitValue;
    /**
     * 正常信息
     */
    private String message;
    /**
     * 异常信息
     */
    private String errorMessage;
    /**
     * 执行用时
     */
    private Long time;

    private Long memory;
}
