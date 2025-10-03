# 深度Review功能 - 剩余任务清单

> 生成时间: 2025-10-03
> 当前完成度: 30%
> 剩余工时: 约 9.5 天

---

## 📋 任务概览

| 优先级 | 任务数 | 预计工时 | 状态 |
|-------|-------|---------|------|
| **P0 (阻断)** | 3 | 1.5天 | ⚪ 待开始 |
| **P1 (重要)** | 8 | 5天 | ⚪ 待开始 |
| **P2 (一般)** | 4 | 3天 | ⚪ 待开始 |
| **总计** | **15** | **9.5天** | **0/15 完成** |

---

## 🔥 P0 任务（阻断性，必须完成）

### ✅ P0-1: 实现 P0-P3 优先级映射逻辑

**工时**: 0.5天
**依赖**: 无
**文件**: `CodeReviewDomainService.java`

**任务清单**:
- [ ] 在 `CodeReviewDomainService` 中新增方法:
  ```java
  public IssuePriority calculateIssuePriority(
      IssueSeverity severity,
      String category,
      String description,
      String filePath
  )
  ```
- [ ] 实现映射规则:
  - [ ] P0: CRITICAL + (安全|数据|核心业务)
  - [ ] P1: CRITICAL (非核心) 或 MAJOR (安全/性能)
  - [ ] P2: MAJOR (非安全) 或 MINOR (重要模块)
  - [ ] P3: MINOR 或 INFO
- [ ] 实现辅助方法:
  - [ ] `isSecurityRelated(String category, String description)`: 识别安全问题
  - [ ] `isDataRelated(String category, String description)`: 识别数据问题
  - [ ] `isCoreBusiness(String filePath)`: 识别核心业务文件

**验收标准**:
- ✅ 所有分支逻辑有单元测试覆盖
- ✅ 准确率 >90%（基于10个测试用例）

**输出文件**:
- `src/main/java/.../domain/codereview/service/CodeReviewDomainService.java`

---

### ✅ P0-2: 编写核心功能单元测试

**工时**: 0.5天
**依赖**: P0-1
**文件**: 多个测试类

**任务清单**:
- [ ] 创建 `ReviewResultTest.java`:
  - [ ] 测试 `IssuePriority` 枚举方法
  - [ ] 测试 `Issue` 构造和业务方法
  - [ ] 测试 `FixSuggestion` 字段
  - [ ] 测试 `ReviewMetrics` P0-P3统计
- [ ] 创建 `CodeReviewDomainServiceTest.java`:
  - [ ] 测试 `calculateIssuePriority()` 所有分支
  - [ ] 测试安全/数据/核心业务识别
- [ ] 创建 `ReviewResultParserTest.java`:
  - [ ] 测试 JSON 解析（包含P0-P3问题）
  - [ ] 测试 Markdown 回退解析
  - [ ] 测试解析失败降级

**验收标准**:
- ✅ 测试覆盖率 >80%
- ✅ 所有测试通过
- ✅ 无 SonarQube 严重问题

**输出文件**:
- `src/test/java/.../ReviewResultTest.java`
- `src/test/java/.../CodeReviewDomainServiceTest.java`
- `src/test/java/.../ReviewResultParserTest.java`

---

### ✅ P0-3: 端到端集成测试

**工时**: 0.5天
**依赖**: P0-1, P0-2
**文件**: `DeepReviewE2ETest.java`

**任务清单**:
- [ ] 创建集成测试类 `DeepReviewE2ETest.java`:
  ```java
  @SpringBootTest
  @AutoConfigureMockMvc
  public class DeepReviewE2ETest {
    @Test
    public void testDeepReviewFlow() {
      // 1. 准备测试仓库（包含已知问题）
      // 2. 调用深度审查API
      // 3. 验证返回JSON包含P0-P3问题
      // 4. 验证问题分级准确性
    }
  }
  ```
- [ ] 准备测试数据:
  - [ ] 创建包含SQL注入的代码（P0）
  - [ ] 创建包含N+1查询的代码（P1）
  - [ ] 创建代码重复的代码（P2）
  - [ ] 创建命名不规范的代码（P3）
- [ ] 验证审查结果:
  - [ ] 检查 qualityScore
  - [ ] 检查 riskLevel
  - [ ] 检查 issues 数组包含所有优先级
  - [ ] 检查 fixSuggestion 字段完整

**验收标准**:
- ✅ 端到端测试通过
- ✅ 问题识别准确率 >85%
- ✅ 优先级分级准确率 >90%

**输出文件**:
- `src/test/java/.../DeepReviewE2ETest.java`
- `src/test/resources/test-data/` (测试数据)

---

## ⭐ P1 任务（重要，建议完成）

### ✅ P1-1: 完善变更行号提取

**工时**: 0.5天
**依赖**: 无
**文件**: `CodeReviewApplicationService.java`

**任务清单**:
- [ ] 在 `extractContextForReview()` 中实现变更行号提取:
  ```java
  // 从 DiffEntry 解析出变更的行号范围
  List<Integer> changedLines = parseChangedLines(diffContent);
  ```
- [ ] 实现 `parseChangedLines()` 方法:
  - [ ] 解析 Git Diff 格式中的 `@@` 行号标记
  - [ ] 提取新增和修改的行号
  - [ ] 返回行号列表
- [ ] 更新 `CodeContextExtractor.extractContext()` 调用:
  - [ ] 传入实际的变更行号而非空列表

**验收标准**:
- ✅ 正确提取变更方法的完整代码
- ✅ 不提取未变更的方法

**输出文件**:
- `src/main/java/.../CodeReviewApplicationService.java`

---

### ✅ P1-2: 优化深度审查Prompt

**工时**: 0.5天
**依赖**: P0-1
**文件**: `review-prompts.properties`

**任务清单**:
- [ ] 在 `review.prompt.deep` 中添加Few-shot示例:
  ```
  ## 示例

  输入代码:
  String sql = "SELECT * FROM users WHERE id=" + userId;

  输出JSON:
  {
    "issues": [{
      "priority": "P0",
      "severity": "CRITICAL",
      "category": "安全问题",
      "file": "UserService.java",
      "line": 45,
      "description": "SQL注入风险",
      ...
    }]
  }
  ```
- [ ] 优化输出格式说明，强调:
  - [ ] priority 必须是 "P0"/"P1"/"P2"/"P3"
  - [ ] severity 必须是 "CRITICAL"/"MAJOR"/"MINOR"/"INFO"
  - [ ] fixSuggestion 必须包含 codeExample
- [ ] 测试 Prompt 有效性:
  - [ ] 使用真实代码测试10次
  - [ ] 统计JSON格式成功率
  - [ ] 调整直到成功率 >90%

**验收标准**:
- ✅ Claude 返回JSON格式成功率 >90%
- ✅ 问题分级符合预期

**输出文件**:
- `src/main/resources/review-prompts.properties`

---

### ✅ P1-3: 实现异步审查机制

**工时**: 1天
**依赖**: 无
**文件**: `CodeReviewApplicationService.java`

**任务清单**:
- [ ] 修改 `executeReviewAsync()` 为真正的异步:
  ```java
  @Async
  public CompletableFuture<Void> executeReviewAsync(Long reviewId) {
    return CompletableFuture.runAsync(() -> {
      // 审查逻辑
    }, reviewExecutor);
  }
  ```
- [ ] 配置异步线程池:
  ```java
  @Bean
  public Executor reviewExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("review-");
    executor.initialize();
    return executor;
  }
  ```
- [ ] 添加进度更新逻辑:
  - [ ] 在审查过程中更新 `CodeReview.progress`
  - [ ] 0% → 克隆仓库
  - [ ] 30% → 生成Diff
  - [ ] 50% → 提取上下文
  - [ ] 70% → 调用Claude
  - [ ] 100% → 完成

**验收标准**:
- ✅ 审查任务异步执行，不阻塞接口
- ✅ 进度实时更新

**输出文件**:
- `src/main/java/.../CodeReviewApplicationService.java`
- `src/main/java/.../config/AsyncConfig.java`

---

### ✅ P1-4: 实现进度查询API

**工时**: 0.5天
**依赖**: P1-3
**文件**: `ReviewController.java`

**任务清单**:
- [ ] 在 `ReviewController` 中添加进度查询接口:
  ```java
  @GetMapping("/api/review/{reviewId}/progress")
  public ResponseEntity<Map<String, Object>> getReviewProgress(@PathVariable Long reviewId) {
    CodeReviewStatusInfo status = codeReviewApplicationService.getReviewStatus(reviewId);

    Map<String, Object> response = new HashMap<>();
    response.put("reviewId", reviewId);
    response.put("status", status.getStatus());
    response.put("progress", status.getProgress()); // 0-100
    response.put("currentStep", getCurrentStepDescription(status));
    response.put("estimatedRemainingSeconds", estimateRemainingTime(status));

    return ResponseEntity.ok(response);
  }
  ```
- [ ] 实现 `getCurrentStepDescription()`:
  - [ ] 根据 progress 返回当前步骤描述

**验收标准**:
- ✅ 前端可轮询此接口获取进度
- ✅ 返回估算剩余时间

**输出文件**:
- `src/main/java/.../ReviewController.java`

---

### ✅ P1-5: 前端 - 问题高亮展示组件

**工时**: 1天
**依赖**: 无
**文件**: Vue组件

**任务清单**:
- [ ] 创建 `IssueCard.vue` 组件:
  - [ ] Props: `issue` (问题对象)
  - [ ] 根据 `priority` 应用不同样式
  - [ ] 显示: 优先级徽章、类别、文件位置、描述
- [ ] 创建 CSS 样式:
  ```css
  .p0-issue {
    border: 2px solid #ff4d4f;
    background: #fff1f0;
    animation: pulse 2s infinite;
  }
  .p1-issue { border: 2px solid #ff9800; background: #fff7e6; }
  .p2-issue { border: 1px solid #faad14; background: #fffbe6; }
  .p3-issue { border: 1px solid #d9d9d9; background: #fafafa; }
  ```
- [ ] 创建 `IssueList.vue` 组件:
  - [ ] 按优先级排序（P0 → P1 → P2 → P3）
  - [ ] 支持筛选（优先级、类别、文件）
  - [ ] 支持搜索（描述关键词）
  - [ ] 分页显示（每页20条）
- [ ] 创建 `PriorityBadge.vue` 徽章组件

**验收标准**:
- ✅ P0 问题有明显视觉差异（红色+动画）
- ✅ 问题默认按优先级排序
- ✅ 筛选和搜索功能正常

**输出文件**:
- `src/main/resources/static/components/IssueCard.vue`
- `src/main/resources/static/components/IssueList.vue`
- `src/main/resources/static/components/PriorityBadge.vue`

---

### ✅ P1-6: 前端 - 审查配置页面

**工时**: 1天
**依赖**: 无
**文件**: Vue组件

**任务清单**:
- [ ] 创建 `ReviewConfig.vue` 组件:
  - [ ] 仓库选择下拉框
  - [ ] 源分支/目标分支选择器
  - [ ] 审查模式选择（Radio Group）:
    ```html
    <el-radio label="quick">⚡ 快速审查 (2-5分钟)</el-radio>
    <el-radio label="standard">📋 标准审查 (5-10分钟)</el-radio>
    <el-radio label="deep">🔍 深度审查 (10-20分钟)</el-radio>
    ```
  - [ ] 上下文选项开关（仅深度模式可用）
  - [ ] 启动审查按钮
- [ ] 实现分支选择器:
  - [ ] 调用 API 获取分支列表
  - [ ] 支持搜索过滤
  - [ ] 显示最近使用的分支
- [ ] 实现表单验证:
  - [ ] 必填字段验证
  - [ ] 分支不能相同验证
  - [ ] 提交前确认对话框

**验收标准**:
- ✅ 所有配置项正常工作
- ✅ 表单验证完整
- ✅ 用户体验流畅

**输出文件**:
- `src/main/resources/static/components/ReviewConfig.vue`
- `src/main/resources/static/components/BranchSelector.vue`

---

### ✅ P1-7: 前端 - 异步进度展示

**工时**: 0.5天
**依赖**: P1-4
**文件**: Vue组件

**任务清单**:
- [ ] 创建 `ReviewProgress.vue` 组件:
  - [ ] Element UI Progress 进度条
  - [ ] 当前步骤描述
  - [ ] 预计剩余时间
  - [ ] 取消按钮
- [ ] 实现轮询逻辑:
  ```javascript
  const pollProgress = async () => {
    const response = await axios.get(`/api/review/${reviewId}/progress`);
    this.progress = response.data.progress;
    this.currentStep = response.data.currentStep;

    if (response.data.status === 'COMPLETED') {
      clearInterval(this.pollingTimer);
      this.showResult();
    }
  };

  this.pollingTimer = setInterval(pollProgress, 2000);
  ```

**验收标准**:
- ✅ 进度实时更新
- ✅ 完成后自动跳转到结果页
- ✅ 支持取消审查

**输出文件**:
- `src/main/resources/static/components/ReviewProgress.vue`

---

### ✅ P1-8: 集成测试数据准备

**工时**: 0.5天
**依赖**: P0-3
**文件**: 测试数据

**任务清单**:
- [ ] 在 `src/test/resources/test-data/` 创建测试代码:
  - [ ] `P0_SqlInjection.java`: SQL注入示例
  - [ ] `P1_N1Query.java`: N+1查询示例
  - [ ] `P2_CodeDuplication.java`: 代码重复示例
  - [ ] `P3_NamingIssue.java`: 命名问题示例
- [ ] 创建测试仓库配置:
  - [ ] `test-repository.json`: 包含测试仓库信息
- [ ] 编写测试用例验证脚本:
  - [ ] `verify-review-accuracy.sh`: 验证准确率

**验收标准**:
- ✅ 测试数据覆盖所有优先级
- ✅ 可重复执行测试

**输出文件**:
- `src/test/resources/test-data/` (目录及文件)

---

## 📦 P2 任务（一般优先级，可选）

### ✅ P2-1: Markdown报告导出

**工时**: 1天
**依赖**: P0-1
**文件**: `MarkdownExporter.java`

**任务清单**:
- [ ] 创建报告模板 `review-report-template.md`:
  ```markdown
  # 代码审查报告

  ## 基本信息
  - 仓库: {{repositoryName}}
  - 分支: {{baseBranch}} → {{targetBranch}}
  - 审查时间: {{reviewTime}}
  - 质量评分: {{qualityScore}}/100

  ## 问题列表

  ### 🔴 P0 - 阻断性问题 ({{p0Count}})
  {{#p0Issues}}
  - **[{{category}}]** {{description}}
    - 文件: `{{file}}:{{line}}`
    - 影响: {{impact}}
    - 修复建议: {{fixApproach}}
    - 预计时间: {{estimatedMinutes}}分钟
  {{/p0Issues}}

  ### 🟠 P1 - 严重问题 ({{p1Count}})
  ...
  ```
- [ ] 创建 `MarkdownExporter.java`:
  ```java
  public class MarkdownExporter {
    public String export(ReviewResult result) {
      // 读取模板
      // 替换占位符
      // 格式化问题列表
      // 返回Markdown字符串
    }
  }
  ```
- [ ] 创建导出API:
  ```java
  @GetMapping("/api/review/{reviewId}/export/markdown")
  public ResponseEntity<String> exportMarkdown(@PathVariable Long reviewId) {
    // ...
  }
  ```

**验收标准**:
- ✅ 导出的Markdown格式正确
- ✅ 可在GitHub/GitLab直接查看

**输出文件**:
- `src/main/resources/review-report-template.md`
- `src/main/java/.../infrastructure/export/MarkdownExporter.java`
- `src/main/java/.../api/ReviewExportController.java`

---

### ✅ P2-2: JSON报告导出

**工时**: 0.5天
**依赖**: P2-1
**文件**: `ReviewExportController.java`

**任务清单**:
- [ ] 在 `ReviewExportController` 添加JSON导出:
  ```java
  @GetMapping("/api/review/{reviewId}/export/json")
  public ResponseEntity<ReviewResult> exportJson(@PathVariable Long reviewId) {
    ReviewResult result = reviewService.getResult(reviewId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setContentDispositionFormData("attachment",
        "review-" + reviewId + ".json");

    return new ResponseEntity<>(result, headers, HttpStatus.OK);
  }
  ```

**验收标准**:
- ✅ 导出完整的JSON数据
- ✅ 文件名正确

**输出文件**:
- `src/main/java/.../api/ReviewExportController.java`

---

### ✅ P2-3: 前端导出按钮

**工时**: 0.5天
**依赖**: P2-1, P2-2
**文件**: `ReviewResult.vue`

**任务清单**:
- [ ] 在审查结果页面添加导出按钮:
  ```html
  <el-button-group>
    <el-button @click="exportMarkdown">
      <i class="el-icon-download"></i> 导出 Markdown
    </el-button>
    <el-button @click="exportJson">
      <i class="el-icon-document"></i> 导出 JSON
    </el-button>
  </el-button-group>
  ```
- [ ] 实现下载逻辑:
  ```javascript
  async exportMarkdown() {
    const response = await axios.get(
      `/api/review/${this.reviewId}/export/markdown`,
      { responseType: 'blob' }
    );

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `review-${this.reviewId}.md`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  }
  ```

**验收标准**:
- ✅ 点击按钮成功下载文件
- ✅ 文件名正确

**输出文件**:
- `src/main/resources/static/components/ReviewResult.vue`

---

### ✅ P2-4: 编写用户文档

**工时**: 1天
**依赖**: 所有功能完成
**文件**: 文档文件

**任务清单**:
- [ ] 编写用户手册 `user-manual.md`:
  - [ ] 如何发起深度审查
  - [ ] 如何理解问题优先级
  - [ ] 如何查看和导出报告
  - [ ] 常见问题 FAQ
- [ ] 更新 `README.md`:
  - [ ] 添加深度审查功能说明
  - [ ] 更新配置说明
  - [ ] 添加截图
- [ ] 编写 API 文档 `api-docs.yaml`:
  - [ ] 使用 OpenAPI 3.0 规范
  - [ ] 包含请求/响应示例
- [ ] 编写开发者文档 `developer-guide.md`:
  - [ ] 架构设计说明
  - [ ] 核心类和接口说明
  - [ ] 扩展指南

**验收标准**:
- ✅ 文档完整清晰
- ✅ 包含足够的示例和截图

**输出文件**:
- `docs/user-manual.md`
- `README.md` (更新)
- `docs/api-docs.yaml`
- `docs/developer-guide.md`

---

## 📈 进度跟踪

### 每日更新模板

```markdown
## 2025-10-XX 进度更新

### 今日完成
- [ ] 任务ID: 任务名称

### 遇到的问题
- 问题描述
- 解决方案

### 明日计划
- [ ] 任务ID: 任务名称
```

### 里程碑

| 里程碑 | 日期 | 交付物 | 状态 |
|-------|------|-------|------|
| **M1: P0任务完成** | D+2 | 映射逻辑 + 单元测试 + E2E测试 | ⚪ 未开始 |
| **M2: 后端完成** | D+5 | 异步审查 + 进度查询 + 导出 | ⚪ 未开始 |
| **M3: 前端完成** | D+8 | 问题展示 + 配置页 + 进度条 | ⚪ 未开始 |
| **M4: 文档完成** | D+9 | 用户手册 + API文档 | ⚪ 未开始 |
| **M5: 全部完成** | D+9.5 | 所有任务完成 | ⚪ 未开始 |

---

## 🎯 建议执行顺序

### 第一阶段（2天）- 核心功能闭环
1. P0-1: 实现P0-P3映射逻辑
2. P0-2: 编写单元测试
3. P0-3: 端到端集成测试
4. P1-1: 完善变更行号提取
5. P1-2: 优化深度审查Prompt

### 第二阶段（3天）- 异步和前端
6. P1-3: 实现异步审查机制
7. P1-4: 实现进度查询API
8. P1-5: 前端问题高亮展示
9. P1-6: 前端审查配置页面
10. P1-7: 前端异步进度展示

### 第三阶段（2天）- 导出和测试
11. P2-1: Markdown报告导出
12. P2-2: JSON报告导出
13. P2-3: 前端导出按钮
14. P1-8: 集成测试数据准备

### 第四阶段（2.5天）- 文档和优化
15. P2-4: 编写用户文档
16. 代码review和优化
17. 性能测试和调优

---

## ✅ 验收标准总览

### 功能验收
- [ ] 所有 P0 任务 100% 完成
- [ ] 所有 P1 任务 >90% 完成
- [ ] 端到端测试通过率 100%

### 质量验收
- [ ] 代码覆盖率 >80%
- [ ] 所有单元测试通过
- [ ] 编译无警告

### 业务验收
- [ ] 问题识别准确率 >85%
- [ ] 优先级分级准确率 >90%
- [ ] JSON 解析成功率 >90%
- [ ] 深度审查完整流程可用

---

**任务清单结束**

👉 **下一步**: 开始执行 P0-1 任务
