# 集成测试数据集

本目录包含用于验证深度代码审查功能准确性的测试数据。

## 📁 文件说明

### 测试用例文件

| 文件 | 优先级 | 问题类型 | 预期问题数 | 说明 |
|------|--------|----------|-----------|------|
| `P0_SqlInjection.java` | P0 | 安全问题 | ≥3 | SQL注入漏洞示例 |
| `P1_N1Query.java` | P1 | 性能问题 | ≥2 | N+1查询问题示例 |
| `P2_CodeDuplication.java` | P2 | 代码质量 | ≥2 | 代码重复示例 |
| `P3_NamingIssue.java` | P3 | 代码规范 | ≥1 | 命名不规范示例 |

### 配置文件

- `test-repository.json` - 测试仓库配置和验收标准
- `verify-review-accuracy.sh` - Linux/Mac验证脚本
- `verify-review-accuracy.bat` - Windows验证脚本
- `README.md` - 本说明文档

## 🎯 使用方法

### 1. 运行验证脚本

**Windows:**
```bash
cd src/test/resources/test-data
verify-review-accuracy.bat
```

**Linux/Mac:**
```bash
cd src/test/resources/test-data
chmod +x verify-review-accuracy.sh
./verify-review-accuracy.sh
```

### 2. 手动测试单个文件

使用Claude CLI直接审查：
```bash
claude --print "请深度审查以下代码，识别所有安全问题：" < P0_SqlInjection.java
```

或通过API：
```bash
curl -X POST http://localhost:8080/api/review/test \
  -F "file=@P0_SqlInjection.java" \
  -F "mode=deep"
```

### 3. 查看测试报告

报告保存在 `target/review-accuracy-results/` 目录：
```
accuracy-report-20251003-145230.txt
```

## 📊 验收标准

| 优先级 | 最低检测率 | 说明 |
|--------|-----------|------|
| P0 (阻断性) | ≥90% | 必须识别出几乎所有严重安全问题 |
| P1 (严重) | ≥85% | 应识别大部分性能和功能问题 |
| P2 (一般) | ≥75% | 识别主要的代码质量问题 |
| P3 (建议) | ≥60% | 识别常见的规范问题 |
| **总体准确率** | **≥80%** | **整体识别准确率** |

## 🧪 测试场景

### 场景1: 全优先级测试
测试所有4个文件，验证能否正确识别P0-P3各类问题。

### 场景2: 安全专项测试
仅测试 `P0_SqlInjection.java`，使用 `security` 审查模式。

### 场景3: 性能专项测试
仅测试 `P1_N1Query.java`，使用 `performance` 审查模式。

## 📝 测试用例详细说明

### P0_SqlInjection.java

**包含的漏洞：**
1. `getUserById()` - 直接拼接用户输入到SQL（行约35）
2. `login()` - 字符串拼接导致认证绕过（行约50）
3. `searchUsers()` - LIKE语句中的注入（行约78）

**预期检测结果：**
- Priority: P0
- Severity: CRITICAL
- Category: 安全问题
- 修复建议应包含 PreparedStatement

### P1_N1Query.java

**包含的问题：**
1. `getUserOrdersWithItems()` - 循环中查询订单项（N+1）
2. `getOrdersWithDetails()` - 嵌套循环多次查询
3. `updateOrderStatuses()` - 未使用批量操作

**预期检测结果：**
- Priority: P1
- Severity: MAJOR
- Category: 性能问题
- 修复建议应包含 JOIN 或批量操作

### P2_CodeDuplication.java

**包含的问题：**
1. 三处相同的 `validateUser` 逻辑
2. 两处相同的 `calculateDiscount` 逻辑
3. 两处相同的 `formatDate` 逻辑

**预期检测结果：**
- Priority: P2
- Severity: MAJOR
- Category: 代码质量
- 修复建议应包含提取公共方法

### P3_NamingIssue.java

**包含的问题：**
1. 大量使用缩写: `usrNm`, `pwd`, `cnt`, `amt`, `reqId`
2. 单字母变量: `n`, `m`, `p`
3. 方法名不清晰: `procUsrData`, `chk`

**预期检测结果：**
- Priority: P3
- Severity: MINOR
- Category: 代码规范
- 修复建议应包含使用完整单词

## 🔧 扩展测试用例

要添加新的测试用例：

1. 在本目录创建新的Java文件（如 `P1_NewIssue.java`）
2. 在文件注释中标注预期问题
3. 更新 `test-repository.json` 配置
4. 运行验证脚本确认识别率

## 📈 持续改进

根据测试结果优化审查Prompt：

1. 如果某类问题识别率低于标准，分析原因
2. 更新 `review-prompts.properties` 中的对应模板
3. 增加Few-shot示例
4. 重新运行验证测试直到达标

## 🎓 参考资料

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- 阿里巴巴Java开发手册
- Effective Java (Third Edition)
- Clean Code: A Handbook of Agile Software Craftsmanship
