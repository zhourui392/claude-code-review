package com.example.testdata;

import java.util.List;
import java.util.Map;

/**
 * P3优先级测试用例：命名问题
 *
 * 预期问题：
 * - Priority: P3
 * - Severity: MINOR
 * - Category: 代码规范
 * - Description: 变量和方法命名不规范，使用缩写降低可读性
 * - Impact: 轻微影响代码可读性和维护性
 */
public class P3_NamingIssue {

    // ❌ 不清晰的缩写
    private String usrNm;
    private String pwd;
    private int cnt;
    private double amt;
    private String addr;
    private String tel;

    // ❌ 单字母变量名（非循环变量）
    private String n;
    private int m;
    private double p;

    /**
     * ❌ 方法名使用缩写
     */
    public void procUsrData(String uid) {
        String reqId = getReqId();
        int usrCnt = getUsrCnt();

        // ❌ 局部变量名不清晰
        String temp = usrNm + "_" + uid;
        int num = usrCnt + 1;
        double val = amt * 1.1;
    }

    /**
     * ❌ 方法名过于简短
     */
    public String getReqId() {
        return "REQ_" + System.currentTimeMillis();
    }

    /**
     * ❌ 方法名使用缩写
     */
    public int getUsrCnt() {
        return cnt;
    }

    /**
     * ❌ 参数名使用缩写
     */
    public void updateUsrInfo(String un, String pw, String em) {
        this.usrNm = un;
        this.pwd = pw;
        // em 是 email 的缩写，不清晰
    }

    /**
     * ❌ 大量使用缩写的变量名
     */
    public double calcTotalAmt(List<Item> itms) {
        double tot = 0;
        int discCnt = 0;

        for (Item itm : itms) {
            double prc = itm.getPrice();
            int qty = itm.getQty();
            double subTot = prc * qty;

            // ❌ 条件判断中的缩写
            if (qty > 10) {
                double disc = subTot * 0.1;
                subTot -= disc;
                discCnt++;
            }

            tot += subTot;
        }

        return tot;
    }

    /**
     * ❌ 不清晰的集合命名
     */
    public void processData(Map<String, Object> data) {
        String str = (String) data.get("name");
        Integer num = (Integer) data.get("count");
        Double val = (Double) data.get("amount");

        // ❌ 临时变量名过于简单
        String tmp1 = str.toUpperCase();
        Integer tmp2 = num + 1;
        Double tmp3 = val * 1.5;
    }

    /**
     * ❌ 布尔变量名不清晰
     */
    public boolean chk(String input) {
        boolean flg = false;
        boolean res = true;

        if (input != null && !input.isEmpty()) {
            flg = true;
        }

        if (flg && input.length() > 5) {
            res = false;
        }

        return res;
    }

    // ❌ 内部类命名使用缩写
    static class Item {
        private double prc;
        private int qty;

        public double getPrice() { return prc; }
        public int getQty() { return qty; }
    }

    // Getters/Setters with poor naming
    public String getUsrNm() { return usrNm; }
    public void setUsrNm(String usrNm) { this.usrNm = usrNm; }
    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public int getCnt() { return cnt; }
    public void setCnt(int cnt) { this.cnt = cnt; }
}
