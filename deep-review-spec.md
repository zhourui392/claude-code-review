# 深度代码审查功能规格说明书

## 文档信息

| 项目 | 内容 |
|-----|------|
| **文档版本** | v1.0 |
| **创建日期** | 2025-10-01 |
| **项目名称** | Git Review Service - 深度代码审查功能 |
| **负责人** | 开发团队 |
| **审核人** | 技术负责人 |

---

## 1. 功能概述

### 1.1 背景

当前系统已实现基础的代码审查功能，支持标准模式的代码变更审查。为进一步提升代码质量和问题发现能力，需要增强审查深度，实现：
- **结合上下文的深度分析**
- **P0-P3 标准化问题分级**
- **高优先级问题重点标记**
- **可执行的修复建议**

### 1.2 目标

1. **提升问题发现率**: 从当前 60% 提升到 85%+
2. **问题分级标准化**: 统一 P0-P3 优先级标准
3. **降低误报率**: 通过上下文分析减少 30% 误报
4. **提高审查效率**: 人工审查时间减少 50%

### 1.3 适用范围

- **目标用户**: 开发团队、技术 Leader、代码审查负责人
- **适用场景**: 重要功能开发、安全相关变更、架构调整、生产发布前审查
- **不适用场景**: 简单文档修改、配置微调（可使用快速审查模式）

---

## 2. 功能需求

### 2.1 核心功能列表

| 功能编号 | 功能名称 | 优先级 | 说明 |
|---------|---------|-------|------|
| **F-01** | 分支对比 Diff 生成 | P0 | 支持任意两个分支的代码差异对比 |
| **F-02** | 上下文智能提取 | P0 | 提取变更文件的类定义、完整方法、依赖关系 |
| **F-03** | P0-P3 问题分级 | P0 | 按阻断/严重/一般/建议分级 |
| **F-04** | 深度审查模式 | P0 | Claude AI 深度分析，结合上下文 |
| **F-05** | 问题高亮展示 | P1 | P0/P1 问题醒目标记 |
| **F-06** | 修复建议生成 | P1 | 每个问题提供可执行的修复方案 |
| **F-07** | 审查报告导出 | P2 | Markdown/JSON 格式导出 |
| **F-08** | 历史审查对比 | P3 | 对比多次审查结果的改进情况 |

### 2.2 详细功能规格

#### F-01: 分支对比 Diff 生成

**需求描述**
用户选择源分支和目标分支，系统生成标准 Git Diff 格式的代码变更。

**输入参数**
```json
{
  "repositoryId": "repo-001",
  "sourceBranch": "feature/new-payment",
  "targetBranch": "develop",
  "includeContext": true
}
```

**输出示例**
```diff
diff --git a/src/PaymentService.java b/src/PaymentService.java
index 1234567..abcdefg 100644
--- a/src/PaymentService.java
+++ b/src/PaymentService.java
@@ -45,7 +45,10 @@ public class PaymentService {
     public void processPayment(Order order) {
-        // 原有逻辑
+        // 新增风控检查
+        if (!riskControl.check(order)) {
+            throw new RiskException("高风险订单");
+        }
     }
```

**验收标准**
- ✅ 支持跨分支对比
- ✅ Diff 格式符合 Git 标准
- ✅ 包含文件路径、行号、变更类型
- ✅ 处理大型 Diff（>1000行）自动分片

---

#### F-02: 上下文智能提取

**需求描述**
针对每个变更文件，自动提取必要的上下文信息，帮助 AI 理解代码意图。

**提取规则**

| 上下文类型 | 提取内容 | 行数限制 |
|-----------|---------|---------|
| **类级上下文** | 类定义、类注释、字段声明 | 最多 50 行 |
| **方法级上下文** | 变更方法的完整代码 | 完整方法 |
| **依赖上下文** | 被调用方法的签名+注释 | 每个方法 5 行 |
| **业务上下文** | 相关领域模型定义 | 最多 100 行 |

**示例输出**
```java
// === 类级上下文 ===
/**
 * 支付服务，处理订单支付流程
 * 包含风控、支付渠道选择、支付结果通知
 */
public class PaymentService {
    private RiskControlService riskControl;
    private PaymentGateway gateway;
}

// === 变更方法完整代码 ===
public void processPayment(Order order) {
    // 变更前后的完整方法
    if (!riskControl.check(order)) {
        throw new RiskException("高风险订单");
    }
    gateway.pay(order);
}

// === 依赖方法签名 ===
// RiskControlService.check()
/**
 * 风控检查
 * @return true-通过, false-拒绝
 */
boolean check(Order order);
```

**技术实现**
- 使用 JGit API 读取文件完整内容
- 使用 JavaParser 解析 AST（抽象语法树）
- 提取类/方法/字段的范围和依赖关系
- 缓存解析结果，避免重复解析

**验收标准**
- ✅ 准确提取类定义和方法完整代码
- ✅ 识别并提取依赖方法（至少 1 层调用）
- ✅ 单文件上下文不超过 2000 行
- ✅ 处理异常情况（文件删除、二进制文件等）

---

#### F-03: P0-P3 问题分级

**需求描述**
将审查发现的问题按照统一标准分为 P0-P3 四个优先级。

**分级标准**

| 优先级 | 名称 | 定义 | 典型问题 | 处理要求 |
|-------|------|------|---------|---------|
| **P0** | 阻断性 | 影响核心功能、数据安全、生产稳定 | SQL注入、NPE崩溃、数据丢失 | **立即修复，阻止发布** |
| **P1** | 严重 | 重要功能缺陷、性能严重退化 | N+1查询、内存泄漏、逻辑错误 | **下次发布前必须修复** |
| **P2** | 一般 | 代码质量问题、违反最佳实践 | 代码重复、缺少日志、命名不规范 | **建议修复，不阻止发布** |
| **P3** | 建议 | 优化建议、代码风格 | 注释补充、变量命名优化 | **可延后处理** |

**映射逻辑**

```java
// Severity → Priority 映射规则
P0 = CRITICAL && (安全问题 || 数据问题 || 核心业务)
P1 = CRITICAL && (非核心) || MAJOR && (性能/安全)
P2 = MAJOR && (非安全) || MINOR && (重要模块)
P3 = MINOR || INFO
```

**领域模型扩展**

```java
public static class Issue {
    private final IssuePriority priority;     // 新增
    private final IssueSeverity severity;     // 原有
    private final String category;
    private final String description;
    private final String suggestion;

    public enum IssuePriority {
        P0("P0", "阻断性", "🔴"),
        P1("P1", "严重", "🟠"),
        P2("P2", "一般", "🟡"),
        P3("P3", "建议", "⚪");
    }
}
```

**验收标准**
- ✅ 所有 Issue 必须包含 priority 字段
- ✅ P0 问题自动触发告警通知
- ✅ 分级规则可配置（支持团队自定义）
- ✅ 统计报表按优先级聚合

---

#### F-04: 深度审查模式

**需求描述**
提供专门的深度审查模式，通过优化的 Prompt 和上下文注入，提升审查深度和准确性。

**审查流程**

```
1. 生成 Diff (F-01)
2. 提取上下文 (F-02)
3. 构建深度审查 Prompt
4. 调用 Claude API
5. 解析结构化结果
6. 问题分级 (F-03)
7. 返回审查报告
```

**Prompt 模板**

```markdown
# 角色定义
你是一位资深的代码审查专家，擅长发现代码中的潜在问题和安全风险。

# 审查任务
请对以下代码变更进行深度审查，结合上下文分析每个变更点的影响。

## 项目上下文
{projectContext}

## 变更说明
Commit: {commitMessage}
Author: {author}
Branch: {sourceBranch} → {targetBranch}

## 代码变更
```diff
{diffContent}
```

## 上下文信息
{contextInfo}

# 审查要求

## 问题分级标准（严格遵守）
- **P0 (阻断性)**: 安全漏洞、数据丢失风险、核心功能崩溃、生产故障
  - 示例: SQL 注入、空指针导致服务崩溃、支付金额计算错误

- **P1 (严重)**: 重要功能缺陷、性能严重退化、资源泄漏、重要逻辑错误
  - 示例: N+1 查询、连接池泄漏、订单状态流转错误

- **P2 (一般)**: 代码质量问题、违反最佳实践、可维护性差
  - 示例: 代码重复、缺少异常处理、日志缺失

- **P3 (建议)**: 命名优化、注释补充、代码风格改进
  - 示例: 变量命名不清晰、缺少方法注释

## 审查重点
1. **安全性**: SQL 注入、XSS、CSRF、权限绕过、敏感数据泄露
2. **正确性**: 逻辑错误、边界条件、空指针、并发问题
3. **性能**: 数据库查询、循环嵌套、资源消耗、缓存使用
4. **可维护性**: SOLID 原则、代码重复、异常处理、日志完整性

## 输出格式（严格 JSON）
{
  "summary": "一句话总体评价",
  "qualityScore": 85,
  "riskLevel": "low|medium|high|critical",
  "issues": [
    {
      "priority": "P0|P1|P2|P3",
      "severity": "CRITICAL|MAJOR|MINOR|INFO",
      "category": "安全|性能|逻辑|设计|测试",
      "file": "src/main/java/PaymentService.java",
      "line": 123,
      "codeSnippet": "问题代码片段",
      "description": "问题的详细描述，说明为什么这是问题",
      "impact": "问题可能造成的影响",
      "suggestion": "具体的修复建议，提供代码示例"
    }
  ],
  "suggestions": [
    {
      "category": "优化|重构|测试|文档",
      "description": "改进建议的详细说明",
      "priority": 8,
      "benefit": "改进后的收益"
    }
  ],
  "metrics": {
    "filesChanged": 5,
    "linesAdded": 120,
    "linesDeleted": 30,
    "complexity": 6
  }
}

# 注意事项
- 每个问题必须指定准确的文件路径和行号
- 修复建议必须具体可执行，最好包含代码示例
- 如果没有发现问题，issues 数组为空，但仍需给出积极的 suggestions
- 严格按照 JSON 格式输出，不要有额外的解释文字
```

**结果解析策略**

```java
public class ReviewResultParser {

    /**
     * 解析 Claude 返回的审查结果
     * 支持 JSON 和 Markdown 两种格式
     */
    public ReviewResult parse(String claudeResponse) {
        // 1. 尝试 JSON 解析（优先）
        try {
            return parseJson(claudeResponse);
        } catch (JsonParseException e) {
            logger.warn("JSON parsing failed, fallback to markdown");
        }

        // 2. 回退到 Markdown 解析
        try {
            return parseMarkdown(claudeResponse);
        } catch (Exception e) {
            logger.error("Markdown parsing failed", e);
        }

        // 3. 兜底：返回原始文本
        return ReviewResult.withError(claudeResponse);
    }

    private ReviewResult parseJson(String json) {
        // 使用 Gson/Jackson 解析
        // 验证必填字段
        // 转换为领域模型
    }

    private ReviewResult parseMarkdown(String markdown) {
        // 正则提取问题列表
        // 解析优先级、文件、行号
        // 构建 ReviewResult
    }
}
```

**验收标准**
- ✅ Prompt 引导 Claude 输出 JSON 格式成功率 >90%
- ✅ 解析器支持 JSON 和 Markdown 两种格式
- ✅ 解析失败时有明确的错误提示
- ✅ 深度审查模式的问题发现率提升 30%+

---

#### F-05: 问题高亮展示

**需求描述**
在前端界面突出显示 P0/P1 高优先级问题，引导用户优先关注。

**展示规则**

| 优先级 | 图标 | 颜色 | 展示位置 | 特殊处理 |
|-------|------|------|---------|---------|
| **P0** | 🔴 | Red | 列表顶部 | 红色边框 + 闪烁动画 |
| **P1** | 🟠 | Orange | P0 之后 | 橙色边框 |
| **P2** | 🟡 | Yellow | 常规位置 | 黄色标记 |
| **P3** | ⚪ | Gray | 折叠显示 | 默认折叠 |

**前端 UI 示例**

```html
<!-- P0 问题卡片 -->
<div class="issue-card p0-issue">
  <div class="issue-header">
    <span class="priority-badge p0">🔴 P0 - 阻断性</span>
    <span class="category">安全问题</span>
  </div>
  <div class="issue-body">
    <h4>SQL 注入风险</h4>
    <p class="location">PaymentService.java:123</p>
    <pre class="code-snippet">
String sql = "SELECT * FROM orders WHERE id=" + orderId;
    </pre>
    <p class="description">
      直接拼接 SQL 语句存在注入风险，攻击者可通过构造特殊 orderId 获取其他用户订单。
    </p>
  </div>
  <div class="issue-footer">
    <button class="btn-view-suggestion">查看修复建议</button>
    <button class="btn-mark-resolved">标记已修复</button>
  </div>
</div>

<!-- 修复建议面板 -->
<div class="suggestion-panel">
  <h5>修复建议</h5>
  <pre class="code-fix">
// 使用参数化查询
String sql = "SELECT * FROM orders WHERE id = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setLong(1, orderId);
  </pre>
  <p class="benefit">彻底避免 SQL 注入风险，符合安全编码规范。</p>
</div>
```

**CSS 样式**

```css
/* P0 问题高亮 */
.p0-issue {
  border: 2px solid #ff4d4f;
  background: #fff1f0;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(255, 77, 79, 0); }
}

/* P1 问题 */
.p1-issue {
  border: 2px solid #ff9800;
  background: #fff7e6;
}

/* 优先级徽章 */
.priority-badge.p0 {
  background: #ff4d4f;
  color: white;
  font-weight: bold;
  padding: 4px 8px;
  border-radius: 4px;
}
```

**验收标准**
- ✅ P0 问题默认置顶显示
- ✅ P0 问题有明显的视觉差异（颜色+动画）
- ✅ 支持按优先级筛选和排序
- ✅ 移动端适配良好

---

#### F-06: 修复建议生成

**需求描述**
为每个发现的问题提供具体的、可执行的修复建议，最好包含代码示例。

**建议内容结构**

```java
public class FixSuggestion {
    private String problemSummary;       // 问题概述
    private String rootCause;            // 根本原因
    private String fixApproach;          // 修复方法
    private String codeExample;          // 代码示例
    private String testStrategy;         // 测试策略
    private List<String> references;     // 参考资料
    private int estimatedMinutes;        // 预计修复时间
}
```

**示例输出**

```json
{
  "problemSummary": "SQL 注入漏洞",
  "rootCause": "直接拼接用户输入到 SQL 语句中，未做参数化处理",
  "fixApproach": "使用 PreparedStatement 或 MyBatis 的参数化查询",
  "codeExample": "String sql = \"SELECT * FROM orders WHERE id = ?\";\nPreparedStatement stmt = conn.prepareStatement(sql);\nstmt.setLong(1, orderId);",
  "testStrategy": "1. 单元测试：验证参数化查询正确性\n2. 安全测试：尝试注入攻击验证防护有效",
  "references": [
    "OWASP Top 10 - Injection",
    "阿里巴巴 Java 开发手册 - 数据库规约"
  ],
  "estimatedMinutes": 10
}
```

**Prompt 引导策略**

```markdown
## 修复建议要求
对于发现的每个问题，必须提供：
1. **问题根本原因**: 为什么会有这个问题
2. **修复方法**: 具体步骤（1, 2, 3...）
3. **代码示例**: 修复后的正确代码（可直接复制使用）
4. **验证方法**: 如何验证修复是否有效
5. **预计工作量**: 修复需要的时间（分钟）

示例格式:
```
**修复建议:**
问题根因: 直接字符串拼接 SQL
修复步骤:
1. 将 SQL 改为参数化查询
2. 使用 PreparedStatement 设置参数
3. 执行查询并获取结果

代码示例:
```java
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM orders WHERE id = ?");
stmt.setLong(1, orderId);
```

验证方法: 单元测试 + SQL 注入测试
预计耗时: 10 分钟
```
```

**验收标准**
- ✅ 90% 的问题包含可执行的代码示例
- ✅ 修复建议符合项目技术栈和规范
- ✅ 提供测试验证方法
- ✅ 预计工作量准确度 ±30%

---

#### F-07: 审查报告导出

**需求描述**
支持将审查结果导出为 Markdown 或 JSON 格式，便于团队分享和存档。

**Markdown 格式示例**

```markdown
# 代码审查报告

## 基本信息
- **仓库**: user-service
- **分支**: feature/payment → develop
- **审查时间**: 2025-10-01 14:30:00
- **审查模式**: 深度审查
- **质量评分**: 78/100

## 审查总结
本次变更新增支付功能，发现 2 个 P0 问题、3 个 P1 问题，需要优先修复后才能合并。

## 风险等级
🔴 **高风险** - 发现阻断性问题

---

## 问题列表

### 🔴 P0 - 阻断性问题 (2个)

#### 1. SQL 注入漏洞
- **文件**: src/main/java/PaymentService.java:123
- **类别**: 安全问题
- **严重性**: CRITICAL

**问题描述**:
直接拼接 SQL 语句存在注入风险，攻击者可通过构造特殊 orderId 获取其他用户订单。

**问题代码**:
```java
String sql = "SELECT * FROM orders WHERE id=" + orderId;
```

**影响**:
- 可能导致数据库被恶意查询或篡改
- 影响所有订单数据安全
- 违反 OWASP Top 10 安全规范

**修复建议**:
使用 PreparedStatement 参数化查询:
```java
String sql = "SELECT * FROM orders WHERE id = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setLong(1, orderId);
```

**预计修复时间**: 10 分钟

---

### 🟠 P1 - 严重问题 (3个)

#### 1. N+1 查询问题
...

---

## 改进建议

### 1. 增加单元测试覆盖
- **优先级**: 8/10
- **类别**: 测试
- **描述**: 支付核心逻辑缺少单元测试，建议覆盖率达到 80%
- **收益**: 降低 Bug 引入风险，提升代码可维护性

---

## 统计数据

| 指标 | 数值 |
|-----|------|
| 文件变更数 | 5 |
| 新增代码行 | 120 |
| 删除代码行 | 30 |
| P0 问题 | 2 |
| P1 问题 | 3 |
| P2 问题 | 5 |
| P3 建议 | 8 |
| 质量评分 | 78/100 |
| 风险等级 | 高 |

---

## 审查结论

❌ **不建议合并** - 请修复所有 P0 和 P1 问题后重新审查

## 下一步行动
1. 立即修复 2 个 P0 安全问题
2. 修复 3 个 P1 性能问题
3. 增加单元测试覆盖
4. 修复完成后重新提交审查
```

**JSON 格式示例**

```json
{
  "reviewId": "review-20251001-001",
  "repository": {
    "id": "repo-001",
    "name": "user-service",
    "sourceBranch": "feature/payment",
    "targetBranch": "develop"
  },
  "metadata": {
    "reviewTime": "2025-10-01T14:30:00Z",
    "reviewMode": "DEEP",
    "reviewer": "Claude AI",
    "duration": 320
  },
  "summary": {
    "qualityScore": 78,
    "riskLevel": "HIGH",
    "recommendation": "REJECT",
    "description": "发现 2 个阻断性安全问题，需要立即修复"
  },
  "issues": [
    {
      "id": "issue-001",
      "priority": "P0",
      "severity": "CRITICAL",
      "category": "安全问题",
      "file": "src/main/java/PaymentService.java",
      "line": 123,
      "codeSnippet": "String sql = \"SELECT * FROM orders WHERE id=\" + orderId;",
      "description": "SQL 注入漏洞",
      "impact": "可能导致数据库被恶意查询或篡改",
      "suggestion": {
        "rootCause": "直接拼接用户输入到 SQL",
        "fixApproach": "使用 PreparedStatement 参数化查询",
        "codeExample": "PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM orders WHERE id = ?\");\nstmt.setLong(1, orderId);",
        "testStrategy": "单元测试 + SQL 注入攻击测试",
        "estimatedMinutes": 10
      }
    }
  ],
  "suggestions": [
    {
      "id": "suggestion-001",
      "category": "测试",
      "priority": 8,
      "description": "增加单元测试覆盖，建议达到 80%",
      "benefit": "降低 Bug 引入风险"
    }
  ],
  "metrics": {
    "filesChanged": 5,
    "linesAdded": 120,
    "linesDeleted": 30,
    "p0Issues": 2,
    "p1Issues": 3,
    "p2Issues": 5,
    "p3Suggestions": 8,
    "complexity": 6
  }
}
```

**API 接口**

```java
@RestController
@RequestMapping("/api/reviews")
public class ReviewExportController {

    /**
     * 导出审查报告为 Markdown
     */
    @GetMapping("/{reviewId}/export/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable String reviewId) {
        ReviewReport report = reviewService.getReport(reviewId);
        String markdown = markdownExporter.export(report);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_MARKDOWN);
        headers.setContentDispositionFormData("attachment",
            "review-" + reviewId + ".md");

        return new ResponseEntity<>(markdown, headers, HttpStatus.OK);
    }

    /**
     * 导出审查报告为 JSON
     */
    @GetMapping("/{reviewId}/export/json")
    public ResponseEntity<ReviewReport> exportJson(@PathVariable String reviewId) {
        ReviewReport report = reviewService.getReport(reviewId);
        return ResponseEntity.ok(report);
    }
}
```

**验收标准**
- ✅ 支持 Markdown 和 JSON 两种格式导出
- ✅ Markdown 格式清晰易读，包含所有关键信息
- ✅ JSON 格式可被其他系统集成
- ✅ 导出文件命名规范，包含时间戳

---

## 3. 非功能需求

### 3.1 性能需求

| 指标 | 目标值 | 说明 |
|-----|-------|------|
| **审查响应时间** | < 10 秒 (小型变更) | Diff < 500 行 |
| **深度审查时间** | < 60 秒 (中型变更) | Diff 500-2000 行 |
| **大型变更处理** | < 300 秒 (大型变更) | Diff > 2000 行，自动分片 |
| **上下文提取** | < 5 秒/文件 | 单文件上下文提取 |
| **并发支持** | 10 个并发审查 | 多用户同时使用 |

**性能优化策略**:
- 使用异步处理 + 进度通知
- 缓存已解析的 AST 和上下文
- 大型 Diff 自动分片处理
- Claude API 调用超时控制

### 3.2 可用性需求

- **服务可用性**: 99.5%
- **Claude API 降级**: API 失败时回退到简单模式
- **错误恢复**: 审查失败后支持重试（最多 3 次）
- **数据持久化**: 审查结果保存 90 天

### 3.3 安全需求

- **访问控制**: 只有仓库成员可以发起审查
- **敏感数据**: Diff 中的密码、Token 自动脱敏
- **审计日志**: 记录所有审查请求和结果
- **数据隔离**: 不同项目的审查结果完全隔离

### 3.4 兼容性需求

- **Git 版本**: JGit 支持的所有 Git 版本
- **Java 版本**: Java 8+
- **浏览器**: Chrome 90+, Firefox 88+, Safari 14+
- **Claude API**: 兼容 Claude 3 Sonnet/Opus

---

## 4. 技术设计

### 4.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 Vue.js                           │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 审查配置页  │  │ 问题列表页   │  │ 报告导出页   │       │
│  └─────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
                            ↓ REST API
┌─────────────────────────────────────────────────────────────┐
│                   应用层 Application Layer                   │
│  ┌──────────────────────────────────────────────────┐       │
│  │     DeepReviewApplicationService                 │       │
│  │  - startDeepReview()                             │       │
│  │  - getReviewProgress()                           │       │
│  │  - exportReport()                                │       │
│  └──────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    领域层 Domain Layer                       │
│  ┌──────────────────┐  ┌──────────────────────────┐        │
│  │ CodeReview       │  │ ReviewResult             │        │
│  │  - reviewId      │  │  - issues (P0-P3)        │        │
│  │  - strategy      │  │  - suggestions           │        │
│  │  - status        │  │  - qualityScore          │        │
│  └──────────────────┘  └──────────────────────────┘        │
│                                                              │
│  ┌──────────────────────────────────────────────────┐       │
│  │     CodeReviewDomainService                      │       │
│  │  - calculatePriority()  // P0-P3 映射            │       │
│  │  - validateContext()    // 上下文验证            │       │
│  │  - recommendStrategy()  // 策略推荐              │       │
│  └──────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  基础设施层 Infrastructure                    │
│  ┌─────────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ ClaudeAdapter   │  │ ContextExtrac│  │ JGitAdapter  │   │
│  │  - deepReview() │  │  - extract() │  │  - getDiff() │   │
│  └─────────────────┘  └──────────────┘  └──────────────┘   │
│                                                              │
│  ┌─────────────────┐  ┌──────────────────────────────┐     │
│  │ ResultParser    │  │ MarkdownExporter             │     │
│  │  - parseJson()  │  │  - export()                  │     │
│  │  - parseMd()    │  │                              │     │
│  └─────────────────┘  └──────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 核心类设计

#### 领域层扩展

```java
// 1. 扩展 Issue 优先级
package com.example.gitreview.domain.codereview.model.valueobject;

public class ReviewResult {

    public static class Issue {
        private final IssuePriority priority;     // 新增
        private final IssueSeverity severity;
        private final String category;
        private final String file;
        private final int line;
        private final String codeSnippet;         // 新增
        private final String description;
        private final String impact;              // 新增
        private final FixSuggestion suggestion;   // 增强

        public enum IssuePriority {
            P0("P0", "阻断性", "🔴", 1),
            P1("P1", "严重", "🟠", 2),
            P2("P2", "一般", "🟡", 3),
            P3("P3", "建议", "⚪", 4);

            private final String code;
            private final String displayName;
            private final String emoji;
            private final int level;

            public boolean isBlocking() {
                return this == P0;
            }

            public boolean isCritical() {
                return this == P0 || this == P1;
            }
        }
    }

    public static class FixSuggestion {
        private final String rootCause;
        private final String fixApproach;
        private final String codeExample;
        private final String testStrategy;
        private final int estimatedMinutes;
        private final List<String> references;
    }
}
```

#### 基础设施层新增

```java
// 2. 上下文提取器
package com.example.gitreview.infrastructure.context;

/**
 * 代码上下文提取器
 * 提取变更文件的类定义、方法、依赖关系等上下文信息
 */
public class CodeContextExtractor {

    private final GitOperationPort gitAdapter;
    private final JavaParser javaParser;

    /**
     * 提取变更文件的上下文
     * @param repository 仓库
     * @param filePath 文件路径
     * @param changedLines 变更的行号列表
     * @return 上下文信息
     */
    public FileContext extractContext(Repository repository,
                                     String filePath,
                                     List<Integer> changedLines) {
        // 1. 读取完整文件内容
        String fileContent = gitAdapter.readFile(repository, filePath);

        // 2. 解析 AST
        CompilationUnit cu = javaParser.parse(fileContent);

        // 3. 提取类级上下文
        ClassContext classContext = extractClassContext(cu);

        // 4. 提取变更方法的完整代码
        List<MethodContext> methodContexts = extractMethodContexts(cu, changedLines);

        // 5. 提取依赖方法
        List<DependencyContext> dependencies = extractDependencies(cu, methodContexts);

        return new FileContext(filePath, classContext, methodContexts, dependencies);
    }

    private ClassContext extractClassContext(CompilationUnit cu) {
        // 提取类定义、类注释、字段声明
        // 限制最多 50 行
    }

    private List<MethodContext> extractMethodContexts(
            CompilationUnit cu, List<Integer> changedLines) {
        // 找到包含变更行的方法
        // 提取方法完整代码
    }

    private List<DependencyContext> extractDependencies(
            CompilationUnit cu, List<MethodContext> methods) {
        // 分析方法内的调用
        // 提取被调用方法的签名和注释
    }
}

// 上下文数据结构
public class FileContext {
    private String filePath;
    private ClassContext classContext;
    private List<MethodContext> methodContexts;
    private List<DependencyContext> dependencies;

    public String toPromptString() {
        // 转换为适合 Prompt 的格式
        StringBuilder sb = new StringBuilder();
        sb.append("// === 类级上下文 ===\n");
        sb.append(classContext.toString());
        sb.append("\n// === 变更方法 ===\n");
        methodContexts.forEach(m -> sb.append(m.toString()));
        sb.append("\n// === 依赖方法 ===\n");
        dependencies.forEach(d -> sb.append(d.toString()));
        return sb.toString();
    }
}
```

```java
// 3. 审查结果解析器
package com.example.gitreview.infrastructure.parser;

/**
 * Claude 审查结果解析器
 * 支持 JSON 和 Markdown 两种格式
 */
public class ReviewResultParser {

    private final Gson gson;
    private final MarkdownParser markdownParser;

    /**
     * 解析 Claude 返回的结果
     */
    public ReviewResult parse(String claudeResponse) {
        // 1. 提取 JSON 块（如果有）
        String jsonContent = extractJsonBlock(claudeResponse);
        if (jsonContent != null) {
            try {
                return parseJson(jsonContent);
            } catch (JsonParseException e) {
                logger.warn("JSON parsing failed", e);
            }
        }

        // 2. 尝试 Markdown 解析
        try {
            return parseMarkdown(claudeResponse);
        } catch (Exception e) {
            logger.error("Markdown parsing failed", e);
        }

        // 3. 兜底处理
        return ReviewResult.withError("解析失败：" + claudeResponse);
    }

    private String extractJsonBlock(String response) {
        // 提取 ```json ... ``` 或 { ... } 块
        Pattern jsonPattern = Pattern.compile(
            "```json\\s*(.+?)\\s*```|\\{.+\\}",
            Pattern.DOTALL
        );
        Matcher matcher = jsonPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group();
        }
        return null;
    }

    private ReviewResult parseJson(String json) {
        ClaudeReviewResponse response = gson.fromJson(json, ClaudeReviewResponse.class);

        // 转换为领域模型
        List<Issue> issues = response.issues.stream()
            .map(this::convertToIssue)
            .collect(Collectors.toList());

        List<Suggestion> suggestions = response.suggestions.stream()
            .map(this::convertToSuggestion)
            .collect(Collectors.toList());

        return ReviewResult.complete(
            response.summary,
            response.detailedReport,
            response.qualityScore,
            issues,
            suggestions
        );
    }

    private Issue convertToIssue(ClaudeIssue claudeIssue) {
        IssuePriority priority = IssuePriority.valueOf(claudeIssue.priority);
        IssueSeverity severity = IssueSeverity.valueOf(claudeIssue.severity);

        FixSuggestion suggestion = new FixSuggestion(
            claudeIssue.suggestion.rootCause,
            claudeIssue.suggestion.fixApproach,
            claudeIssue.suggestion.codeExample,
            claudeIssue.suggestion.testStrategy,
            claudeIssue.suggestion.estimatedMinutes,
            claudeIssue.suggestion.references
        );

        return new Issue(
            claudeIssue.file,
            claudeIssue.line,
            severity,
            priority,
            claudeIssue.category,
            claudeIssue.description,
            claudeIssue.impact,
            claudeIssue.codeSnippet,
            suggestion
        );
    }
}

// Claude 响应 DTO
class ClaudeReviewResponse {
    String summary;
    int qualityScore;
    String riskLevel;
    List<ClaudeIssue> issues;
    List<ClaudeSuggestion> suggestions;
    ReviewMetrics metrics;
}

class ClaudeIssue {
    String priority;       // "P0", "P1", "P2", "P3"
    String severity;       // "CRITICAL", "MAJOR", "MINOR", "INFO"
    String category;
    String file;
    int line;
    String codeSnippet;
    String description;
    String impact;
    ClaudeFixSuggestion suggestion;
}
```

### 4.3 数据库设计

#### 扩展 code_reviews 表

```sql
ALTER TABLE code_reviews ADD COLUMN review_mode VARCHAR(20) DEFAULT 'STANDARD';
ALTER TABLE code_reviews ADD COLUMN risk_level VARCHAR(20);
ALTER TABLE code_reviews ADD COLUMN has_context BOOLEAN DEFAULT FALSE;
ALTER TABLE code_reviews ADD COLUMN context_size INT DEFAULT 0;

-- 索引优化
CREATE INDEX idx_reviews_risk_level ON code_reviews(risk_level);
CREATE INDEX idx_reviews_mode ON code_reviews(review_mode);
```

#### 扩展 review_issues 表

```sql
ALTER TABLE review_issues ADD COLUMN priority VARCHAR(10) NOT NULL DEFAULT 'P2';
ALTER TABLE review_issues ADD COLUMN code_snippet TEXT;
ALTER TABLE review_issues ADD COLUMN impact TEXT;
ALTER TABLE review_issues ADD COLUMN root_cause TEXT;
ALTER TABLE review_issues ADD COLUMN fix_approach TEXT;
ALTER TABLE review_issues ADD COLUMN code_example TEXT;
ALTER TABLE review_issues ADD COLUMN test_strategy TEXT;
ALTER TABLE review_issues ADD COLUMN estimated_minutes INT;

-- 索引优化
CREATE INDEX idx_issues_priority ON review_issues(priority);
CREATE INDEX idx_issues_severity_priority ON review_issues(severity, priority);
```

### 4.4 API 接口设计

#### 深度审查接口

```java
/**
 * 发起深度代码审查
 */
@PostMapping("/api/reviews/deep")
public ResponseEntity<DeepReviewResponse> startDeepReview(
    @RequestBody DeepReviewRequest request) {

    // 请求参数
    {
      "repositoryId": "repo-001",
      "sourceBranch": "feature/payment",
      "targetBranch": "develop",
      "reviewMode": "DEEP",
      "includeContext": true,
      "contextStrategy": "SMART_WINDOW",
      "async": true
    }

    // 响应
    {
      "reviewId": "review-20251001-001",
      "status": "IN_PROGRESS",
      "estimatedTime": 120,
      "progressUrl": "/api/reviews/review-20251001-001/progress"
    }
}

/**
 * 查询审查进度
 */
@GetMapping("/api/reviews/{reviewId}/progress")
public ResponseEntity<ReviewProgress> getProgress(@PathVariable String reviewId) {
    {
      "reviewId": "review-20251001-001",
      "status": "IN_PROGRESS",
      "progress": 65,
      "currentStep": "分析第 3 个文件",
      "startTime": "2025-10-01T14:30:00Z",
      "estimatedComplete": "2025-10-01T14:32:00Z"
    }
}

/**
 * 获取审查结果
 */
@GetMapping("/api/reviews/{reviewId}")
public ResponseEntity<ReviewDetailResponse> getReview(@PathVariable String reviewId) {
    {
      "reviewId": "review-20251001-001",
      "status": "COMPLETED",
      "summary": {
        "qualityScore": 78,
        "riskLevel": "HIGH",
        "recommendation": "REJECT"
      },
      "issues": [...],
      "suggestions": [...],
      "metrics": {...}
    }
}
```

---

## 5. 实施计划

### 5.1 开发阶段划分

#### 阶段 1: 核心功能（3天）⭐ P0

**目标**: 实现 P0-P3 分级和深度审查 Prompt

| 任务 | 负责人 | 工时 | 输出 |
|-----|-------|------|------|
| 扩展 Issue 领域模型 | 后端 | 0.5天 | IssuePriority 枚举 + 映射逻辑 |
| 设计深度审查 Prompt | 后端 | 1天 | review-prompts.properties 更新 |
| 实现 ReviewResultParser | 后端 | 1天 | JSON/Markdown 解析器 |
| 单元测试 | 后端 | 0.5天 | 测试覆盖率 >80% |

**验收标准**:
- ✅ Issue 包含 P0-P3 优先级
- ✅ Prompt 引导 JSON 输出成功率 >90%
- ✅ 解析器支持容错

---

#### 阶段 2: 上下文分析（4天）⭐⭐ P1

**目标**: 实现智能上下文提取

| 任务 | 负责人 | 工时 | 输出 |
|-----|-------|------|------|
| 设计 ContextExtractor | 后端 | 1天 | 上下文提取策略文档 |
| 实现 JavaParser 集成 | 后端 | 1.5天 | AST 解析 + 上下文提取 |
| 集成到审查流程 | 后端 | 1天 | ClaudeAdapter 调用上下文 |
| 性能优化 + 测试 | 后端 | 0.5天 | 提取耗时 <5秒/文件 |

**验收标准**:
- ✅ 准确提取类/方法/依赖上下文
- ✅ 单文件上下文 <2000行
- ✅ 支持 Java 文件（后续扩展其他语言）

---

#### 阶段 3: 前端优化（3天）⭐⭐⭐ P1

**目标**: 优化用户体验和问题展示

| 任务 | 负责人 | 工时 | 输出 |
|-----|-------|------|------|
| 设计 UI 界面 | 前端 | 0.5天 | 原型图 + 交互设计 |
| 实现问题高亮展示 | 前端 | 1天 | P0/P1 特殊样式 |
| 审查配置页面 | 前端 | 1天 | 深度审查选项 |
| 异步进度展示 | 前端 | 0.5天 | 进度条 + 实时更新 |

**验收标准**:
- ✅ P0 问题醒目标记（红色+动画）
- ✅ 支持按优先级筛选排序
- ✅ 异步审查进度实时显示

---

#### 阶段 4: 报告导出（2天）⭐⭐ P2

**目标**: 支持 Markdown/JSON 导出

| 任务 | 负责人 | 工时 | 输出 |
|-----|-------|------|------|
| 设计报告模板 | 后端 | 0.5天 | Markdown 模板 |
| 实现 MarkdownExporter | 后端 | 1天 | 导出功能 |
| 前端导出按钮 | 前端 | 0.5天 | 下载功能 |

**验收标准**:
- ✅ Markdown 格式美观易读
- ✅ JSON 格式符合规范
- ✅ 支持文件下载

---

#### 阶段 5: 集成测试（2天）

| 任务 | 负责人 | 工时 | 输出 |
|-----|-------|------|------|
| 端到端测试 | QA | 1天 | 测试用例 + 报告 |
| 性能测试 | QA | 0.5天 | 性能基准 |
| 文档编写 | 全员 | 0.5天 | 用户手册 + API 文档 |

---

### 5.2 里程碑

| 里程碑 | 日期 | 交付物 | 标准 |
|-------|------|-------|------|
| **M1: 核心功能完成** | D+3 | P0-P3 分级 + 深度 Prompt | 功能可用 |
| **M2: 上下文分析完成** | D+7 | 上下文提取器 | 准确率 >85% |
| **M3: 前端优化完成** | D+10 | 问题高亮 + 异步审查 | 用户体验达标 |
| **M4: 功能全量发布** | D+14 | 报告导出 + 文档 | 验收通过 |

---

## 6. 测试计划

### 6.1 单元测试

**领域层测试**
```java
@Test
public void testIssuePriorityMapping() {
    Issue issue = new Issue(
        "PaymentService.java", 123,
        IssueSeverity.CRITICAL,
        IssuePriority.P0,
        "安全问题",
        "SQL 注入漏洞",
        "可能导致数据泄露",
        "...",
        fixSuggestion
    );

    assertTrue(issue.getPriority().isBlocking());
    assertEquals("🔴", issue.getPriority().getEmoji());
}

@Test
public void testContextExtraction() {
    String javaCode = "public class Foo { ... }";
    FileContext context = contextExtractor.extractContext(repo, "Foo.java", Arrays.asList(10, 20));

    assertNotNull(context.getClassContext());
    assertEquals(2, context.getMethodContexts().size());
    assertTrue(context.toString().length() < 2000);
}
```

**基础设施层测试**
```java
@Test
public void testReviewResultParser_Json() {
    String jsonResponse = "{\"summary\":\"...\", \"issues\":[...]}";
    ReviewResult result = parser.parse(jsonResponse);

    assertEquals(85, result.getQualityScore());
    assertEquals(3, result.getIssues().size());
    assertEquals(IssuePriority.P0, result.getIssues().get(0).getPriority());
}

@Test
public void testReviewResultParser_Markdown_Fallback() {
    String markdownResponse = "## Issues\n- **P0**: SQL injection...";
    ReviewResult result = parser.parse(markdownResponse);

    assertNotNull(result);
    assertTrue(result.getIssues().size() > 0);
}
```

### 6.2 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
public class DeepReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testDeepReview_EndToEnd() throws Exception {
        // 1. 发起深度审查
        String request = "{\"repositoryId\":\"repo-001\", \"sourceBranch\":\"feature/test\", \"targetBranch\":\"main\", \"reviewMode\":\"DEEP\"}";

        MvcResult result = mockMvc.perform(post("/api/reviews/deep")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").exists())
            .andReturn();

        String reviewId = JsonPath.read(result.getResponse().getContentAsString(), "$.reviewId");

        // 2. 查询进度（轮询）
        await().atMost(120, TimeUnit.SECONDS).until(() -> {
            MvcResult progress = mockMvc.perform(get("/api/reviews/" + reviewId + "/progress"))
                .andReturn();
            String status = JsonPath.read(progress.getResponse().getContentAsString(), "$.status");
            return "COMPLETED".equals(status);
        });

        // 3. 获取结果
        mockMvc.perform(get("/api/reviews/" + reviewId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.qualityScore").isNumber())
            .andExpect(jsonPath("$.issues").isArray())
            .andExpect(jsonPath("$.issues[0].priority").exists());
    }
}
```

### 6.3 性能测试

**测试场景**

| 场景 | Diff 大小 | 文件数 | 目标时间 | 验收标准 |
|-----|---------|-------|---------|---------|
| 小型变更 | <500 行 | 1-3 | <10 秒 | 95% 请求达标 |
| 中型变更 | 500-2000 行 | 3-10 | <60 秒 | 90% 请求达标 |
| 大型变更 | >2000 行 | >10 | <300 秒 | 85% 请求达标 |

**工具**: JMeter + Grafana 监控

---

## 7. 风险与应对

### 7.1 技术风险

| 风险 | 概率 | 影响 | 应对措施 |
|-----|------|------|---------|
| **Claude 输出格式不稳定** | 高 | 中 | Prompt 优化 + 容错解析 + 重试机制 |
| **上下文提取失败** | 中 | 中 | 异常处理 + 降级到无上下文模式 |
| **性能不达标** | 中 | 高 | 异步处理 + 分片 + 缓存优化 |
| **Claude API 限流** | 低 | 高 | 本地限流 + 队列 + 降级方案 |

### 7.2 业务风险

| 风险 | 概率 | 影响 | 应对措施 |
|-----|------|------|---------|
| **误报率过高** | 中 | 高 | 持续优化 Prompt + 用户反馈机制 |
| **审查时间过长** | 中 | 中 | 提供快速模式 + 进度提示 |
| **用户不接受 AI 审查** | 低 | 高 | 作为辅助工具，不替代人工 |

---

## 8. 附录

### 8.1 术语表

| 术语 | 定义 |
|-----|------|
| **P0-P3** | 问题优先级分级，P0 最高（阻断），P3 最低（建议） |
| **上下文** | 变更代码周边的类定义、方法、依赖等信息 |
| **深度审查** | 结合上下文的 AI 深度分析模式 |
| **Diff** | Git 代码差异，显示新增/删除/修改的代码 |
| **AST** | 抽象语法树，代码的结构化表示 |

### 8.2 参考资料

- [OWASP Top 10](https://owasp.org/Top10/)
- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [JGit Documentation](https://www.eclipse.org/jgit/)
- [Claude API Documentation](https://docs.anthropic.com/claude/reference)

### 8.3 变更记录

| 版本 | 日期 | 修改人 | 变更内容 |
|-----|------|-------|---------|
| v1.0 | 2025-10-01 | 开发团队 | 初始版本 |

---

**文档结束**
