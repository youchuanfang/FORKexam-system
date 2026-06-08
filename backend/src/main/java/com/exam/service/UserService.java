package com.exam.service;

import java.util.Map;

public interface UserService {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 明文密码
     * @return 包含 token 和 role 的 Map
     */
    Map<String, Object> login(String username, String password, String role);

    /**
     * 用户注册
     * @param username 用户名
     * @param password 明文密码
     * @param role     角色（student / teacher / admin）
     */
    void register(String username, String password, String role);
}
