package com.zhaoli.liojcodesandbox.security;


import java.security.Permission;

/**
 * @author 赵立
 */
public class DefaultSecurityManager extends SecurityManager {
    /**
     * 检查所有的权限
     */
    @Override
    public void checkPermission(Permission perm) {
//        System.out.println("默认不做任何权限");
//        System.out.println(perm);
//        super.checkPermission(perm);
//        throw new SecurityException("权限不足"+perm.getActions());
    }
}
