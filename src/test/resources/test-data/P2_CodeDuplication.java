package com.example.testdata;

/**
 * P2优先级测试用例：代码重复
 *
 * 预期问题：
 * - Priority: P2
 * - Severity: MAJOR
 * - Category: 代码质量
 * - Description: 存在大量重复代码，违反DRY原则
 * - Impact: 维护成本高，修改时容易遗漏某一处
 */
public class P2_CodeDuplication {

    /**
     * 验证用户 - 第1处重复
     */
    public boolean validateUser1(User user) {
        // ❌ 重复的验证逻辑
        if (user == null) {
            System.err.println("User is null");
            return false;
        }
        if (user.getId() == null) {
            System.err.println("User ID is null");
            return false;
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            System.err.println("Username is empty");
            return false;
        }
        if (user.getUsername().length() < 3 || user.getUsername().length() > 20) {
            System.err.println("Username length invalid");
            return false;
        }
        return true;
    }

    /**
     * 验证用户 - 第2处重复（完全相同的逻辑）
     */
    public boolean validateUser2(User user) {
        // ❌ 完全相同的验证逻辑
        if (user == null) {
            System.err.println("User is null");
            return false;
        }
        if (user.getId() == null) {
            System.err.println("User ID is null");
            return false;
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            System.err.println("Username is empty");
            return false;
        }
        if (user.getUsername().length() < 3 || user.getUsername().length() > 20) {
            System.err.println("Username length invalid");
            return false;
        }
        return true;
    }

    /**
     * 验证用户 - 第3处重复
     */
    public boolean validateUserForUpdate(User user) {
        // ❌ 第三次相同的验证逻辑
        if (user == null) {
            System.err.println("User is null");
            return false;
        }
        if (user.getId() == null) {
            System.err.println("User ID is null");
            return false;
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            System.err.println("Username is empty");
            return false;
        }
        if (user.getUsername().length() < 3 || user.getUsername().length() > 20) {
            System.err.println("Username length invalid");
            return false;
        }
        return true;
    }

    /**
     * 计算折扣 - 重复的计算逻辑
     */
    public double calculateDiscount1(double price, String userType) {
        // ❌ 重复的折扣计算逻辑
        double discount = 0;
        if ("VIP".equals(userType)) {
            discount = price * 0.2;
        } else if ("PREMIUM".equals(userType)) {
            discount = price * 0.15;
        } else if ("REGULAR".equals(userType)) {
            discount = price * 0.1;
        }
        return discount;
    }

    /**
     * 计算折扣 - 第2处相同逻辑
     */
    public double calculateDiscount2(double amount, String level) {
        // ❌ 完全相同的折扣计算，只是参数名不同
        double discount = 0;
        if ("VIP".equals(level)) {
            discount = amount * 0.2;
        } else if ("PREMIUM".equals(level)) {
            discount = amount * 0.15;
        } else if ("REGULAR".equals(level)) {
            discount = amount * 0.1;
        }
        return discount;
    }

    /**
     * 格式化日期 - 重复的格式化逻辑
     */
    public String formatDate1(java.time.LocalDateTime dateTime) {
        // ❌ 重复的日期格式化
        if (dateTime == null) {
            return "";
        }
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond());
    }

    /**
     * 格式化日期 - 第2处相同逻辑
     */
    public String formatDate2(java.time.LocalDateTime dt) {
        // ❌ 完全相同的格式化逻辑
        if (dt == null) {
            return "";
        }
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                dt.getYear(),
                dt.getMonthValue(),
                dt.getDayOfMonth(),
                dt.getHour(),
                dt.getMinute(),
                dt.getSecond());
    }

    static class User {
        private Long id;
        private String username;
        public Long getId() { return id; }
        public String getUsername() { return username; }
    }
}
