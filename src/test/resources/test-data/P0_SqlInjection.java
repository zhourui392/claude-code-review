package com.example.testdata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * P0优先级测试用例：SQL注入漏洞
 *
 * 预期问题：
 * - Priority: P0
 * - Severity: CRITICAL
 * - Category: 安全问题
 * - Description: SQL注入漏洞
 * - Impact: 攻击者可通过构造特殊输入获取其他用户数据或执行恶意SQL
 */
public class P0_SqlInjection {

    private Connection connection;

    /**
     * 严重的SQL注入漏洞 - 直接拼接用户输入
     *
     * @param userId 用户输入的ID（未验证、未转义）
     * @return 用户信息
     */
    public List<User> getUserById(String userId) {
        List<User> users = new ArrayList<>();

        try {
            Statement stmt = connection.createStatement();

            // ❌ 严重安全漏洞：直接拼接用户输入到SQL语句
            String sql = "SELECT * FROM users WHERE id = " + userId;

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password")); // 密码未加密
                users.add(user);
            }

            rs.close();
            stmt.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }

    /**
     * 另一个SQL注入漏洞 - 字符串拼接
     *
     * @param username 用户名
     * @param password 密码
     * @return 是否登录成功
     */
    public boolean login(String username, String password) {
        try {
            Statement stmt = connection.createStatement();

            // ❌ SQL注入漏洞：攻击者可通过 username = "admin' OR '1'='1" 绕过认证
            String sql = "SELECT * FROM users WHERE username = '" + username +
                        "' AND password = '" + password + "'";

            ResultSet rs = stmt.executeQuery(sql);
            boolean success = rs.next();

            rs.close();
            stmt.close();

            return success;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 第三个SQL注入 - 搜索功能
     */
    public List<User> searchUsers(String keyword) {
        List<User> results = new ArrayList<>();

        try {
            Statement stmt = connection.createStatement();

            // ❌ SQL注入：LIKE语句中的用户输入未转义
            String sql = "SELECT * FROM users WHERE username LIKE '%" + keyword + "%'";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                results.add(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    static class User {
        private Long id;
        private String username;
        private String password;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
