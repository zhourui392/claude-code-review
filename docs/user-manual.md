# Git Review Service 用户手册

> 基于 Claude AI 的智能代码审查与测试生成服务

**版本**: 1.0.0
**更新时间**: 2025-10-03

---

## 📚 目录

- [快速开始](#快速开始)
- [代码审查功能](#代码审查功能)
- [测试生成功能](#测试生成功能)
- [问题优先级说明](#问题优先级说明)
- [报告导出](#报告导出)
- [常见问题](#常见问题)

---

## 🚀 快速开始

### 1. 访问系统

在浏览器中打开：
```
http://localhost:8080
```

### 2. 添加Git仓库

1. 点击「仓库管理」→「添加仓库」
2. 填写仓库信息：
   - **仓库名称**: 项目名称
   - **Git URL**: `https://github.com/username/repo.git`
   - **用户名**: Git账号（公开仓库可留空）
   - **密码/Token**: 访问凭证（公开仓库可留空）
3. 点击「保存」

### 3. 发起代码审查

1. 选择仓库
2. 选择分支：
   - **基础分支**: 通常是 `main` 或 `master`
   - **目标分支**: 要审查的功能分支
3. 选择审查模式（见下文）
4. 点击「开始审查」

### 4. 查看结果

审查完成后，可以：
- 在线查看问题列表（按优先级排序）
- 导出Markdown/JSON格式报告
- 查看详细的修复建议

---

## 🔍 代码审查功能

### 审查模式说明

| 模式 | 用时 | 适用场景 | 输出格式 |
|------|------|---------|---------|
| **⚡ 快速审查 (quick)** | 2-5分钟 | 日常代码提交、Pull Request | 文本列表 |
| **📋 标准审查 (standard)** | 5-10分钟 | 功能开发完成、合并前检查 | 结构化文本 |
| **🔍 深度审查 (deep)** | 10-20分钟 | 重要版本发布、安全审计 | JSON（含详细建议） |
| **🔒 安全审查 (security)** | 5-10分钟 | 专注安全漏洞检测 | 安全报告 |
| **⚡ 性能审查 (performance)** | 5-10分钟 | 性能优化、数据库查询 | 性能分析 |
| **🏗️ 架构审查 (architecture)** | 5-10分钟 | 设计重构、架构评审 | 架构分析 |

### 深度审查详解

**深度审查**是最强大的模式，提供：

#### 1. P0-P3 问题分级

所有问题按优先级分类：

- 🔴 **P0 - 阻断性问题**
  - SQL注入、XSS等严重安全漏洞
  - 数据丢失、数据泄露风险
  - 支付、订单等核心业务逻辑错误
  - **必须立即修复！**

- 🟠 **P1 - 严重问题**
  - 重要功能缺陷
  - N+1查询、连接池泄漏等性能问题
  - 资源泄漏（内存、文件句柄）
  - **下次发布前必须修复**

- 🟡 **P2 - 一般问题**
  - 代码质量问题（重复、高耦合）
  - 违反编码规范
  - 缺少异常处理或日志
  - **建议修复**

- ⚪ **P3 - 改进建议**
  - 变量命名优化
  - 注释补充
  - 代码格式调整
  - **可延后处理**

#### 2. 详细的修复建议

每个问题都包含：

```json
{
  "priority": "P0",
  "severity": "CRITICAL",
  "category": "安全问题",
  "file": "src/main/java/PaymentService.java",
  "line": 123,
  "codeSnippet": "String sql = \"SELECT * FROM orders WHERE id=\" + orderId;",
  "description": "SQL注入漏洞",
  "impact": "攻击者可通过构造特殊orderId获取其他用户订单数据",
  "fixSuggestion": {
    "rootCause": "直接拼接用户输入到SQL语句",
    "fixApproach": "使用PreparedStatement参数化查询",
    "codeExample": "PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM orders WHERE id = ?\");\nstmt.setLong(1, orderId);",
    "testStrategy": "单元测试验证参数化查询，尝试SQL注入攻击测试",
    "estimatedMinutes": 10,
    "references": ["OWASP Top 10", "阿里巴巴Java开发手册"]
  }
}
```

#### 3. 质量评分与风险等级

- **质量评分**: 0-100分
  - 90-100: 优秀 ✨
  - 80-89: 良好 👍
  - 70-79: 合格 ✓
  - 60-69: 需改进 ⚠️
  - <60: 不合格 ❌

- **风险等级**:
  - **high**: 存在P0问题或3个以上P1问题
  - **medium**: 存在P1问题但无P0问题
  - **low**: 仅存在P2/P3问题

### 异步审查与进度跟踪

深度审查采用异步执行，可实时查看进度：

1. **发起审查后**，系统返回审查ID
2. **轮询进度API**: `GET /api/review/{reviewId}/progress`
   ```json
   {
     "reviewId": 123,
     "status": "IN_PROGRESS",
     "progress": 50,
     "currentStep": "Claude正在分析代码",
     "estimatedRemainingSeconds": 60
   }
   ```
3. **审查完成**后，`status` 变为 `COMPLETED`，可查看结果

### 审查步骤说明

| 进度 | 步骤 | 说明 |
|------|------|------|
| 0% | 初始化 | 创建审查任务 |
| 10% | 检查服务 | 验证Claude CLI可用性 |
| 30% | 提取上下文 | 解析Git Diff，提取变更方法的上下文 |
| 50% | Claude分析 | Claude AI深度分析代码 |
| 80% | 解析结果 | 解析JSON结果，计算优先级 |
| 90% | 保存结果 | 持久化审查结果 |
| 100% | 完成 | 审查完成 |

---

## 🧪 测试生成功能

### 自动生成单元测试

系统可为Java类自动生成单元测试：

#### 1. 生成测试

**API请求**:
```bash
curl -X POST http://localhost:8080/api/test-generation/generate \
  -H "Content-Type: application/json" \
  -d '{
    "repositoryId": 1,
    "branch": "main",
    "classPath": "src/main/java/com/example/UserService.java"
  }'
```

**返回**:
```json
{
  "taskId": "TG_1_1696334567890",
  "status": "PENDING",
  "message": "测试生成任务已创建"
}
```

#### 2. 查询进度

```bash
curl http://localhost:8080/api/test-generation/TG_1_1696334567890
```

**返回**:
```json
{
  "taskId": "TG_1_1696334567890",
  "status": "COMPLETED",
  "testCode": "package com.example;\n\nimport org.junit.jupiter.api.Test;\n...",
  "testFilePath": "src/test/java/com/example/UserServiceTest.java",
  "coverage": 85,
  "complexity": 8
}
```

#### 3. 状态说明

- `PENDING`: 等待生成
- `GENERATING`: 正在生成
- `VALIDATING`: 编译验证
- `COMPLETED`: 生成成功
- `FAILED`: 生成失败

### 质量门禁

生成的测试必须通过以下检查：

- ✅ **编译通过**: 测试代码可编译
- ✅ **覆盖率达标**: 目标覆盖率 ≥80%
- ✅ **复杂度合理**: 圈复杂度 ≤10
- ✅ **命名规范**: 符合测试命名约定

---

## 📊 问题优先级说明

### 如何理解优先级？

#### P0 - 阻断性问题 🔴

**定义**: 必须立即修复，否则会导致严重后果

**典型问题**:
- **安全漏洞**: SQL注入、XSS、CSRF
- **数据风险**: 数据丢失、泄露、越权访问
- **核心业务**: 支付失败、订单错误、登录崩溃

**修复时间**: 当天完成

**示例**:
```java
// ❌ P0问题：SQL注入
String sql = "SELECT * FROM users WHERE id=" + userId;

// ✅ 正确做法
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id=?");
stmt.setLong(1, userId);
```

#### P1 - 严重问题 🟠

**定义**: 影响重要功能或性能，需在下次发布前修复

**典型问题**:
- **功能缺陷**: 重要流程报错、数据不一致
- **性能问题**: N+1查询、慢查询、内存泄漏
- **资源泄漏**: 数据库连接未关闭、文件句柄泄漏

**修复时间**: 本周完成

**示例**:
```java
// ❌ P1问题：N+1查询
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    // 每个订单都查询一次！
    List<Item> items = itemRepository.findByOrderId(order.getId());
}

// ✅ 正确做法：使用JOIN
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items")
List<Order> findAllWithItems();
```

#### P2 - 一般问题 🟡

**定义**: 代码质量问题，建议修复但不影响发布

**典型问题**:
- **代码重复**: 违反DRY原则
- **高耦合**: 违反SOLID原则
- **缺少日志/异常处理**

**修复时间**: 本月完成

**示例**:
```java
// ❌ P2问题：代码重复
public void method1() {
    if (user == null || user.getId() == null) throw new Exception();
    // ... 业务逻辑
}
public void method2() {
    if (user == null || user.getId() == null) throw new Exception();
    // ... 相同验证！
}

// ✅ 正确做法：提取公共方法
private void validateUser(User user) {
    if (user == null || user.getId() == null) throw new ValidationException();
}
```

#### P3 - 改进建议 ⚪

**定义**: 优化建议，可延后处理

**典型问题**:
- **命名问题**: 使用缩写、命名不清晰
- **注释缺失**: 复杂逻辑缺少说明
- **格式问题**: 代码格式不统一

**修复时间**: 计划修复

**示例**:
```java
// ❌ P3问题：命名不清晰
String reqId = request.getId();
int usrCnt = getUserCount();

// ✅ 正确做法：使用完整单词
String requestId = request.getId();
int userCount = getUserCount();
```

### 修复优先级建议

1. **立即修复** (当天):
   - 所有 P0 问题
   - 安全相关的 P1 问题

2. **本周修复**:
   - 其他 P1 问题
   - 重要模块的 P2 问题

3. **本月修复**:
   - 一般 P2 问题
   - 高优先级 P3 建议

4. **计划修复**:
   - 低优先级 P3 建议

---

## 📥 报告导出

### Markdown 格式导出

**适用场景**: GitHub/GitLab PR描述、团队分享

**API**:
```bash
GET /api/review/{reviewId}/export/markdown?repositoryName=MyProject&baseBranch=main&targetBranch=feature/new
```

**下载文件**: `review-123-20251003-145230.md`

**特点**:
- ✅ 支持GitHub Flavored Markdown
- ✅ 问题按优先级分组
- ✅ 包含代码片段高亮
- ✅ 修复建议格式化

### JSON 格式导出

**适用场景**: 数据分析、自动化处理、集成其他工具

**API**:
```bash
GET /api/review/{reviewId}/export/json
```

**下载文件**: `review-123-20251003-145230.json`

**数据结构**:
```json
{
  "reviewId": 123,
  "status": "COMPLETED",
  "createTime": "2025-10-03T14:30:00",
  "updateTime": "2025-10-03T14:35:00",
  "progress": 100,
  "result": {
    "summary": "发现1个严重安全问题、2个性能问题和若干代码质量建议",
    "qualityScore": 65,
    "issues": [...],
    "suggestions": [...]
  }
}
```

**特点**:
- ✅ 完整的审查元数据
- ✅ 结构化问题列表
- ✅ 易于编程处理
- ✅ 支持数据导入

---

## ❓ 常见问题

### Q1: 审查速度慢怎么办？

**A**: 深度审查需要10-20分钟，这是正常的。如果超过30分钟：
1. 检查网络连接
2. 查看Claude CLI是否正常：`claude --version`
3. 检查日志：`logs/spring.log`

### Q2: 为什么有些问题没有检测到？

**A**: 可能原因：
- **审查模式不对**: 使用 `deep` 模式可提高检出率
- **代码复杂度高**: 极度复杂的代码可能漏检
- **上下文不足**: 确保变更代码的依赖都在仓库中

**提高准确率的方法**:
- ✅ 使用深度审查模式
- ✅ 确保代码可编译
- ✅ 提供清晰的变更范围

### Q3: 如何处理误报（False Positive）？

**A**: 如果审查报告了不存在的问题：
1. **验证问题**: 仔细检查，可能确实存在隐患
2. **上下文问题**: AI可能缺少某些业务上下文
3. **反馈改进**: 记录误报case，帮助优化Prompt

### Q4: P0问题一定要修复吗？

**A**: 是的！P0问题通常是：
- **严重安全漏洞**: 可能导致数据泄露
- **核心功能错误**: 影响用户正常使用
- **数据一致性**: 可能丢失或损坏数据

**不修复的风险极高**，强烈建议立即处理。

### Q5: 支持哪些Git托管平台？

**A**: 支持所有Git托管平台：
- ✅ GitHub
- ✅ GitLab
- ✅ Bitbucket
- ✅ Gitee (码云)
- ✅ 私有Git服务器

只要能通过HTTPS克隆即可。

### Q6: 私有仓库如何配置？

**A**: 使用Personal Access Token：

**GitHub**:
1. Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. 勾选 `repo` 权限
4. 将token作为密码填入系统

**GitLab**:
1. User Settings → Access Tokens
2. 选择 `read_repository` scope
3. 将token作为密码填入系统

### Q7: 审查结果可以保存多久？

**A**: 默认永久保存（存储在JSON文件中）。

如需清理旧数据：
```bash
# 删除30天前的审查记录
find data/code-reviews.json -mtime +30 -delete
```

### Q8: 如何自定义审查规则？

**A**: 编辑 `src/main/resources/review-prompts.properties`：

```properties
# 添加自定义审查模式
review.prompt.custom=请审查以下代码，重点检查：\
1. 线程安全问题\
2. 分布式事务一致性\
3. 缓存穿透风险
```

然后使用 `mode=custom` 参数调用API。

### Q9: 支持哪些编程语言？

**A**: 当前版本专注于 **Java**，未来计划支持：
- JavaScript/TypeScript
- Python
- Go
- C#

### Q10: 如何集成到CI/CD流程？

**A**: 示例（GitHub Actions）：

```yaml
name: Code Review

on: [pull_request]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Run Code Review
        run: |
          curl -X POST http://review-server:8080/api/review/1/claude \
            -d "baseBranch=main" \
            -d "targetBranch=${{ github.head_ref }}" \
            -d "mode=deep"

      - name: Check Results
        run: |
          # 检查是否有P0问题
          # 如果有，则失败CI
```

---

## 📞 技术支持

- **问题反馈**: [GitHub Issues](https://github.com/your-org/git-review-service/issues)
- **使用交流**: [Discussions](https://github.com/your-org/git-review-service/discussions)
- **文档主页**: [README.md](../README.md)

---

**文档版本**: 1.0.0
**最后更新**: 2025-10-03
**维护者**: Git Review Service Team
