# 深度代码审查功能 - 任务清单

> 基于 `deep-review-spec.md` 规格说明书生成
> 项目: Git Review Service - 深度代码审查功能
> 预计总工时: **14 天**（后端 10.5 天 + 前端 3.5 天）

---

## 📋 任务总览

| 阶段 | 任务数 | 工时 | 优先级 | 状态 |
|-----|-------|------|-------|------|
| **阶段1: 核心功能** | 4 | 3天 | P0 | 🔴 未开始 |
| **阶段2: 上下文分析** | 4 | 4天 | P1 | ⚪ 待启动 |
| **阶段3: 前端优化** | 4 | 3天 | P1 | ⚪ 待启动 |
| **阶段4: 报告导出** | 3 | 2天 | P2 | ⚪ 待启动 |
| **阶段5: 集成测试** | 3 | 2天 | P0 | ⚪ 待启动 |
| **总计** | **18** | **14天** | - | **0% 完成** |

---

## 🎯 阶段 1: 核心功能（3天）⭐ P0

**目标**: 实现 P0-P3 问题分级和深度审查 Prompt
**责任人**: 后端开发
**开始日期**: D+0
**完成日期**: D+3

### Task 1.1: 扩展 Issue 领域模型

**工时**: 0.5天
**优先级**: P0
**依赖**: 无

**详细任务**:
- [ ] 1.1.1 在 `ReviewResult.java` 中新增 `IssuePriority` 枚举
  - [ ] 定义 P0/P1/P2/P3 四个级别
  - [ ] 添加字段: code, displayName, emoji, level
  - [ ] 实现业务方法: `isBlocking()`, `isCritical()`

- [ ] 1.1.2 扩展 `Issue` 内部类
  - [ ] 新增字段: `IssuePriority priority`
  - [ ] 新增字段: `String codeSnippet` (问题代码片段)
  - [ ] 新增字段: `String impact` (影响说明)
  - [ ] 更新构造函数和 Getter

- [ ] 1.1.3 新增 `FixSuggestion` 内部类
  - [ ] 字段: `rootCause`, `fixApproach`, `codeExample`, `testStrategy`
  - [ ] 字段: `estimatedMinutes`, `references`
  - [ ] 实现 toString() 和业务方法

- [ ] 1.1.4 更新 `ReviewMetrics` 计算逻辑
  - [ ] 增加 P0/P1/P2/P3 问题数统计
  - [ ] 调整质量指数计算公式（考虑优先级权重）

**验收标准**:
- ✅ `IssuePriority` 枚举包含完整的 4 个级别
- ✅ `Issue` 类包含所有新增字段
- ✅ 单元测试覆盖率 >80%
- ✅ 通过 `RepositoryTest` 类似的领域模型测试

**输出文件**:
- `ReviewResult.java` (修改)
- `ReviewResultTest.java` (新增/修改)

---

### Task 1.2: 实现 P0-P3 映射逻辑

**工时**: 0.5天
**优先级**: P0
**依赖**: Task 1.1

**详细任务**:
- [ ] 1.2.1 在 `CodeReviewDomainService` 中新增映射方法
  ```java
  public IssuePriority calculateIssuePriority(
      IssueSeverity severity,
      String category,
      CodeDiff codeDiff
  )
  ```

- [ ] 1.2.2 实现映射规则
  - [ ] P0: CRITICAL + (安全|数据|核心业务)
  - [ ] P1: CRITICAL (非核心) + MAJOR (安全/性能)
  - [ ] P2: MAJOR (非安全) + MINOR (重要模块)
  - [ ] P3: MINOR + INFO

- [ ] 1.2.3 实现分类识别逻辑
  - [ ] `isSecurityRelated(category, description)`: 识别安全问题
  - [ ] `isDataRelated(category, description)`: 识别数据问题
  - [ ] `isCoreBusiness(filePath)`: 识别核心业务文件

**验收标准**:
- ✅ 映射逻辑准确率 >90% (基于测试用例)
- ✅ 单元测试覆盖所有映射分支
- ✅ 边界条件测试通过

**输出文件**:
- `CodeReviewDomainService.java` (修改)
- `CodeReviewDomainServiceTest.java` (新增测试方法)

---

### Task 1.3: 设计深度审查 Prompt 模板

**工时**: 1天
**优先级**: P0
**依赖**: Task 1.1

**详细任务**:
- [ ] 1.3.1 更新 `review-prompts.properties`
  - [ ] 新增 `review.prompt.deep` 模板
  - [ ] 包含角色定义、审查任务、问题分级标准
  - [ ] 包含严格的 JSON 输出格式要求

- [ ] 1.3.2 设计 Prompt 结构
  - [ ] 项目上下文占位符: `{projectContext}`
  - [ ] 变更说明占位符: `{commitMessage}`, `{author}`, `{branch}`
  - [ ] 代码变更占位符: `{diffContent}`
  - [ ] 上下文信息占位符: `{contextInfo}`

- [ ] 1.3.3 优化 Few-shot 示例
  - [ ] 添加 2-3 个完整的审查示例
  - [ ] 示例包含各个优先级的问题
  - [ ] 示例展示正确的 JSON 格式

- [ ] 1.3.4 测试 Prompt 有效性
  - [ ] 使用真实代码测试 10 次
  - [ ] 统计 JSON 格式成功率
  - [ ] 调整直到成功率 >90%

**验收标准**:
- ✅ Prompt 引导 Claude 输出 JSON 格式成功率 >90%
- ✅ 问题分级准确率 >85%
- ✅ 包含清晰的输出格式说明和示例

**输出文件**:
- `review-prompts.properties` (修改)
- `prompt-test-results.md` (测试记录)

---

### Task 1.4: 实现 ReviewResultParser

**工时**: 1天
**优先级**: P0
**依赖**: Task 1.1, Task 1.3

**详细任务**:
- [ ] 1.4.1 创建 `ReviewResultParser` 类
  - [ ] 包路径: `com.example.gitreview.infrastructure.parser`
  - [ ] 主方法: `parse(String claudeResponse): ReviewResult`

- [ ] 1.4.2 实现 JSON 解析
  - [ ] 提取 JSON 块（支持 ```json...``` 和 {...} 格式）
  - [ ] 使用 Gson 解析为 DTO
  - [ ] 转换 DTO 为领域模型

- [ ] 1.4.3 实现 Markdown 解析（回退方案）
  - [ ] 正则提取问题列表
  - [ ] 解析优先级、文件、行号
  - [ ] 构建基本的 ReviewResult

- [ ] 1.4.4 实现容错处理
  - [ ] JSON 解析失败 → 回退 Markdown
  - [ ] Markdown 解析失败 → 返回错误 ReviewResult
  - [ ] 记录解析失败日志

- [ ] 1.4.5 创建 DTO 类
  - [ ] `ClaudeReviewResponse`
  - [ ] `ClaudeIssue`
  - [ ] `ClaudeSuggestion`
  - [ ] `ClaudeFixSuggestion`

**验收标准**:
- ✅ JSON 解析成功率 >95%
- ✅ Markdown 回退成功率 >80%
- ✅ 单元测试覆盖所有解析分支
- ✅ 异常情况有明确错误提示

**输出文件**:
- `ReviewResultParser.java` (新增)
- `ClaudeReviewResponse.java` (新增 DTO)
- `ReviewResultParserTest.java` (新增测试)

---

### Task 1.5: 单元测试

**工时**: 0.5天
**优先级**: P0
**依赖**: Task 1.1 - 1.4

**详细任务**:
- [ ] 1.5.1 领域模型测试
  - [ ] `IssuePriorityTest`: 测试优先级枚举
  - [ ] `IssueTest`: 测试 Issue 构造和业务方法
  - [ ] `FixSuggestionTest`: 测试修复建议

- [ ] 1.5.2 领域服务测试
  - [ ] `CodeReviewDomainServiceTest.testCalculateIssuePriority()`: 测试映射逻辑
  - [ ] 覆盖所有 P0-P3 映射场景
  - [ ] 边界条件和异常情况

- [ ] 1.5.3 解析器测试
  - [ ] `ReviewResultParserTest.testParseJson()`: JSON 解析
  - [ ] `ReviewResultParserTest.testParseMarkdown()`: Markdown 解析
  - [ ] `ReviewResultParserTest.testFallback()`: 回退逻辑

- [ ] 1.5.4 生成测试报告
  - [ ] 运行 `mvn test`
  - [ ] 确保覆盖率 >80%
  - [ ] 修复所有失败用例

**验收标准**:
- ✅ 测试覆盖率 >80%
- ✅ 所有测试用例通过
- ✅ 无 SonarQube 严重问题

**输出文件**:
- `*Test.java` (多个测试类)
- `test-coverage-report.html`

---

## 🔍 阶段 2: 上下文分析（4天）⭐⭐ P1

**目标**: 实现智能上下文提取
**责任人**: 后端开发
**开始日期**: D+3
**完成日期**: D+7

### Task 2.1: 设计上下文提取策略

**工时**: 1天
**优先级**: P1
**依赖**: 阶段 1 完成

**详细任务**:
- [ ] 2.1.1 定义上下文数据结构
  - [ ] `FileContext`: 文件级上下文
  - [ ] `ClassContext`: 类级上下文（定义、注释、字段）
  - [ ] `MethodContext`: 方法级上下文（完整代码）
  - [ ] `DependencyContext`: 依赖上下文（调用关系）

- [ ] 2.1.2 设计提取规则
  - [ ] 类级上下文: 最多 50 行
  - [ ] 方法级上下文: 完整方法代码
  - [ ] 依赖上下文: 每个方法签名 + 注释（5行）
  - [ ] 总大小限制: 单文件 <2000 行

- [ ] 2.1.3 编写技术设计文档
  - [ ] 上下文提取流程图
  - [ ] AST 解析策略
  - [ ] 性能优化方案
  - [ ] 异常处理策略

**验收标准**:
- ✅ 数据结构定义清晰
- ✅ 提取规则合理（平衡信息量和大小）
- ✅ 技术设计文档完整

**输出文件**:
- `context-extraction-design.md` (设计文档)
- `FileContext.java` (新增)
- `ClassContext.java` (新增)
- `MethodContext.java` (新增)
- `DependencyContext.java` (新增)

---

### Task 2.2: 集成 JavaParser

**工时**: 1.5天
**优先级**: P1
**依赖**: Task 2.1

**详细任务**:
- [ ] 2.2.1 添加 JavaParser 依赖
  ```xml
  <dependency>
    <groupId>com.github.javaparser</groupId>
    <artifactId>javaparser-symbol-solver-core</artifactId>
    <version>3.25.5</version>
  </dependency>
  ```

- [ ] 2.2.2 创建 `JavaParserService`
  - [ ] 包路径: `com.example.gitreview.infrastructure.parser`
  - [ ] 方法: `parseJavaFile(String content): CompilationUnit`
  - [ ] 配置 Symbol Solver (可选，用于高级依赖分析)

- [ ] 2.2.3 实现 AST 遍历器
  - [ ] `ClassVisitor`: 提取类定义、注释、字段
  - [ ] `MethodVisitor`: 提取方法完整代码
  - [ ] `CallVisitor`: 提取方法调用关系

- [ ] 2.2.4 处理边界情况
  - [ ] 解析失败 → 返回空上下文
  - [ ] 内部类 / 匿名类处理
  - [ ] Lambda 表达式处理

**验收标准**:
- ✅ 成功解析 Java 代码并生成 AST
- ✅ 准确提取类、方法、字段信息
- ✅ 异常情况有日志和降级处理

**输出文件**:
- `pom.xml` (修改，添加依赖)
- `JavaParserService.java` (新增)
- `ClassVisitor.java` (新增)
- `MethodVisitor.java` (新增)
- `CallVisitor.java` (新增)

---

### Task 2.3: 实现 CodeContextExtractor

**工时**: 1天
**优先级**: P1
**依赖**: Task 2.2

**详细任务**:
- [ ] 2.3.1 创建 `CodeContextExtractor` 类
  - [ ] 包路径: `com.example.gitreview.infrastructure.context`
  - [ ] 主方法: `extractContext(Repository, String filePath, List<Integer> changedLines): FileContext`

- [ ] 2.3.2 实现上下文提取逻辑
  ```java
  public FileContext extractContext(...) {
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
  ```

- [ ] 2.3.3 实现辅助方法
  - [ ] `extractClassContext(CompilationUnit)`: 提取类上下文
  - [ ] `extractMethodContexts(CompilationUnit, List<Integer>)`: 提取方法上下文
  - [ ] `extractDependencies(CompilationUnit, List<MethodContext>)`: 提取依赖

- [ ] 2.3.4 实现大小限制
  - [ ] 类上下文 ≤ 50 行
  - [ ] 单文件总上下文 ≤ 2000 行
  - [ ] 超出时智能截断（保留关键部分）

**验收标准**:
- ✅ 准确提取类、方法、依赖上下文
- ✅ 遵守大小限制
- ✅ 单元测试覆盖率 >75%

**输出文件**:
- `CodeContextExtractor.java` (新增)
- `CodeContextExtractorTest.java` (新增)

---

### Task 2.4: 集成到审查流程

**工时**: 0.5天
**优先级**: P1
**依赖**: Task 2.3

**详细任务**:
- [ ] 2.4.1 修改 `ClaudeCliAdapter.buildReviewPrompt()`
  - [ ] 新增参数: `FileContext context`
  - [ ] 在 Prompt 中注入上下文信息
  - [ ] 格式化上下文为易读形式

- [ ] 2.4.2 修改 `CodeReviewApplicationService`
  - [ ] 在审查前调用 `CodeContextExtractor`
  - [ ] 提取每个变更文件的上下文
  - [ ] 传递上下文到 `ClaudeCliAdapter`

- [ ] 2.4.3 添加上下文开关
  - [ ] 配置项: `review.includeContext=true/false`
  - [ ] 允许用户选择是否使用上下文

**验收标准**:
- ✅ 上下文成功注入到 Prompt
- ✅ 审查结果包含上下文相关的分析
- ✅ 配置开关生效

**输出文件**:
- `ClaudeCliAdapter.java` (修改)
- `CodeReviewApplicationService.java` (修改)
- `application.properties` (修改)

---

## 🎨 阶段 3: 前端优化（3天）⭐⭐⭐ P1

**目标**: 优化用户体验和问题展示
**责任人**: 前端开发
**开始日期**: D+7
**完成日期**: D+10

### Task 3.1: 设计 UI 界面

**工时**: 0.5天
**优先级**: P1
**依赖**: 无

**详细任务**:
- [ ] 3.1.1 设计审查配置页面原型
  - [ ] 分支选择器
  - [ ] 审查模式选择（快速/标准/深度）
  - [ ] 上下文选项开关
  - [ ] 启动审查按钮

- [ ] 3.1.2 设计问题列表页面原型
  - [ ] 问题卡片布局（P0-P3 不同样式）
  - [ ] 筛选器（按优先级/类别）
  - [ ] 排序器（优先级/文件/行号）
  - [ ] 问题详情展开

- [ ] 3.1.3 设计修复建议面板原型
  - [ ] 代码对比视图（问题代码 vs 修复代码）
  - [ ] 修复步骤说明
  - [ ] 相关参考资料链接

- [ ] 3.1.4 创建交互流程图
  - [ ] 发起审查 → 进度展示 → 结果查看
  - [ ] 问题筛选 → 详情查看 → 标记修复

**验收标准**:
- ✅ 原型图清晰美观
- ✅ 交互流程合理
- ✅ 通过 UI/UX 评审

**输出文件**:
- `ui-prototype.fig` (Figma 原型)
- `interaction-flow.png` (交互流程图)

---

### Task 3.2: 实现问题高亮展示

**工时**: 1天
**优先级**: P1
**依赖**: Task 3.1, 阶段 1 完成

**详细任务**:
- [ ] 3.2.1 创建问题卡片组件 `IssueCard.vue`
  - [ ] Props: `issue` (问题对象)
  - [ ] 根据 `priority` 应用不同样式
  - [ ] 显示: 优先级徽章、类别、文件位置、描述

- [ ] 3.2.2 实现优先级样式
  ```css
  /* P0 问题 - 红色 + 闪烁动画 */
  .p0-issue {
    border: 2px solid #ff4d4f;
    background: #fff1f0;
    animation: pulse 2s infinite;
  }

  /* P1 问题 - 橙色 */
  .p1-issue {
    border: 2px solid #ff9800;
    background: #fff7e6;
  }

  /* P2 问题 - 黄色 */
  .p2-issue {
    border: 1px solid #faad14;
    background: #fffbe6;
  }

  /* P3 建议 - 灰色 */
  .p3-issue {
    border: 1px solid #d9d9d9;
    background: #fafafa;
  }
  ```

- [ ] 3.2.3 实现问题列表组件 `IssueList.vue`
  - [ ] 按优先级排序（P0 → P1 → P2 → P3）
  - [ ] 支持筛选（优先级、类别、文件）
  - [ ] 支持搜索（描述关键词）
  - [ ] 分页显示（每页 20 条）

- [ ] 3.2.4 实现优先级徽章组件 `PriorityBadge.vue`
  - [ ] 显示优先级图标和文字
  - [ ] 不同优先级不同颜色

**验收标准**:
- ✅ P0 问题有明显视觉差异（红色边框+动画）
- ✅ 问题默认按优先级排序
- ✅ 筛选和搜索功能正常
- ✅ 移动端适配良好

**输出文件**:
- `IssueCard.vue` (新增)
- `IssueList.vue` (新增)
- `PriorityBadge.vue` (新增)
- `issue-styles.css` (新增)

---

### Task 3.3: 实现审查配置页面

**工时**: 1天
**优先级**: P1
**依赖**: Task 3.1, 阶段 1 完成

**详细任务**:
- [ ] 3.3.1 创建审查配置组件 `ReviewConfig.vue`
  - [ ] 仓库选择下拉框
  - [ ] 源分支/目标分支选择器
  - [ ] 审查模式选择（Radio Group）
  - [ ] 上下文选项（Switch）
  - [ ] 启动审查按钮

- [ ] 3.3.2 实现分支选择器
  - [ ] 调用 API 获取分支列表
  - [ ] 支持搜索过滤
  - [ ] 显示最近使用的分支

- [ ] 3.3.3 实现审查模式选择
  ```html
  <el-radio-group v-model="reviewMode">
    <el-radio label="QUICK">
      <i class="el-icon-lightning"></i> 快速审查
      <span class="mode-desc">仅关注严重问题，2-5分钟</span>
    </el-radio>
    <el-radio label="STANDARD">
      <i class="el-icon-document-checked"></i> 标准审查
      <span class="mode-desc">平衡深度和速度，5-10分钟</span>
    </el-radio>
    <el-radio label="DEEP">
      <i class="el-icon-zoom-in"></i> 深度审查
      <span class="mode-desc">结合上下文，全面分析，10-20分钟</span>
    </el-radio>
  </el-radio-group>
  ```

- [ ] 3.3.4 实现表单验证
  - [ ] 必填字段验证
  - [ ] 分支不能相同验证
  - [ ] 提交前确认对话框

**验收标准**:
- ✅ 所有配置项正常工作
- ✅ 表单验证完整
- ✅ 用户体验流畅

**输出文件**:
- `ReviewConfig.vue` (新增)
- `BranchSelector.vue` (新增)

---

### Task 3.4: 实现异步进度展示

**工时**: 0.5天
**优先级**: P1
**依赖**: Task 3.3

**详细任务**:
- [ ] 3.4.1 创建进度组件 `ReviewProgress.vue`
  - [ ] 进度条（Element UI Progress）
  - [ ] 当前步骤描述
  - [ ] 预计剩余时间
  - [ ] 取消按钮

- [ ] 3.4.2 实现轮询逻辑
  ```javascript
  // 每 2 秒轮询一次进度
  const pollProgress = async () => {
    const response = await axios.get(`/api/reviews/${reviewId}/progress`);
    this.progress = response.data.progress;
    this.currentStep = response.data.currentStep;

    if (response.data.status === 'COMPLETED') {
      clearInterval(this.pollingTimer);
      this.showResult();
    }
  };

  this.pollingTimer = setInterval(pollProgress, 2000);
  ```

- [ ] 3.4.3 实现 WebSocket 推送（可选优化）
  - [ ] 服务端推送进度更新
  - [ ] 前端实时接收并更新

**验收标准**:
- ✅ 进度实时更新
- ✅ 完成后自动跳转到结果页
- ✅ 支持取消审查

**输出文件**:
- `ReviewProgress.vue` (新增)

---

## 📄 阶段 4: 报告导出（2天）⭐⭐ P2

**目标**: 支持 Markdown/JSON 格式报告导出
**责任人**: 后端开发 + 前端开发
**开始日期**: D+10
**完成日期**: D+12

### Task 4.1: 设计报告模板

**工时**: 0.5天
**优先级**: P2
**依赖**: 阶段 1 完成

**详细任务**:
- [ ] 4.1.1 设计 Markdown 模板结构
  - [ ] 基本信息部分
  - [ ] 审查总结部分
  - [ ] 问题列表（按优先级分组）
  - [ ] 改进建议部分
  - [ ] 统计数据部分
  - [ ] 审查结论和下一步行动

- [ ] 4.1.2 创建模板文件
  - [ ] `review-report-template.md`
  - [ ] 使用占位符: `{{reviewId}}`, `{{qualityScore}}`, `{{issues}}` 等

- [ ] 4.1.3 设计 JSON Schema
  - [ ] 定义完整的 JSON 结构
  - [ ] 包含所有审查数据
  - [ ] 符合 OpenAPI 规范

**验收标准**:
- ✅ Markdown 模板清晰美观
- ✅ JSON Schema 完整规范
- ✅ 通过评审

**输出文件**:
- `review-report-template.md` (新增)
- `review-report-schema.json` (新增)

---

### Task 4.2: 实现 MarkdownExporter

**工时**: 1天
**优先级**: P2
**依赖**: Task 4.1

**详细任务**:
- [ ] 4.2.1 创建 `MarkdownExporter` 类
  - [ ] 包路径: `com.example.gitreview.infrastructure.export`
  - [ ] 主方法: `export(ReviewResult result): String`

- [ ] 4.2.2 实现模板渲染
  - [ ] 读取模板文件
  - [ ] 替换占位符
  - [ ] 格式化问题列表
  - [ ] 格式化统计数据

- [ ] 4.2.3 实现特殊处理
  - [ ] 代码块高亮（使用 ``` 语法）
  - [ ] 表格格式化
  - [ ] Emoji 图标插入（🔴 🟠 🟡）

- [ ] 4.2.4 创建 REST API
  ```java
  @GetMapping("/api/reviews/{reviewId}/export/markdown")
  public ResponseEntity<String> exportMarkdown(@PathVariable String reviewId) {
    ReviewResult result = reviewService.getResult(reviewId);
    String markdown = markdownExporter.export(result);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_MARKDOWN);
    headers.setContentDispositionFormData("attachment",
        "review-" + reviewId + ".md");

    return new ResponseEntity<>(markdown, headers, HttpStatus.OK);
  }
  ```

**验收标准**:
- ✅ 导出的 Markdown 格式正确
- ✅ 可以直接在 GitHub/GitLab 查看
- ✅ API 返回正确的 Content-Type 和文件名

**输出文件**:
- `MarkdownExporter.java` (新增)
- `ReviewExportController.java` (新增)
- `MarkdownExporterTest.java` (新增)

---

### Task 4.3: 实现前端导出按钮

**工时**: 0.5天
**优先级**: P2
**依赖**: Task 4.2

**详细任务**:
- [ ] 4.3.1 在审查结果页面添加导出按钮
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

- [ ] 4.3.2 实现下载逻辑
  ```javascript
  async exportMarkdown() {
    const response = await axios.get(
      `/api/reviews/${this.reviewId}/export/markdown`,
      { responseType: 'blob' }
    );

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `review-${this.reviewId}.md`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  }
  ```

- [ ] 4.3.3 添加导出成功提示
  - [ ] Toast 提示
  - [ ] 下载进度（可选）

**验收标准**:
- ✅ 点击按钮成功下载文件
- ✅ 文件名正确
- ✅ 有成功提示

**输出文件**:
- `ReviewResult.vue` (修改)

---

## 🧪 阶段 5: 集成测试（2天）⭐ P0

**目标**: 完整的端到端测试和性能测试
**责任人**: QA + 开发团队
**开始日期**: D+12
**完成日期**: D+14

### Task 5.1: 端到端测试

**工时**: 1天
**优先级**: P0
**依赖**: 阶段 1-4 完成

**详细任务**:
- [ ] 5.1.1 编写集成测试用例
  ```java
  @SpringBootTest
  @AutoConfigureMockMvc
  public class DeepReviewE2ETest {

    @Test
    public void testFullReviewFlow() {
      // 1. 发起深度审查
      // 2. 轮询进度直到完成
      // 3. 获取审查结果
      // 4. 验证问题分级
      // 5. 导出报告
    }
  }
  ```

- [ ] 5.1.2 准备测试数据
  - [ ] 创建测试仓库（包含已知问题的代码）
  - [ ] P0 问题: SQL 注入
  - [ ] P1 问题: N+1 查询
  - [ ] P2 问题: 代码重复
  - [ ] P3 建议: 命名优化

- [ ] 5.1.3 执行测试并验证
  - [ ] 验证所有问题被正确识别
  - [ ] 验证优先级分级准确
  - [ ] 验证上下文提取正确
  - [ ] 验证报告导出完整

- [ ] 5.1.4 修复发现的 Bug
  - [ ] 记录所有 Bug
  - [ ] 优先修复 P0/P1 Bug
  - [ ] 重新测试直到通过

**验收标准**:
- ✅ 端到端测试全部通过
- ✅ 问题识别准确率 >85%
- ✅ 优先级分级准确率 >90%
- ✅ 无阻断性 Bug

**输出文件**:
- `DeepReviewE2ETest.java` (新增)
- `test-data/` (测试数据目录)
- `e2e-test-report.md` (测试报告)

---

### Task 5.2: 性能测试

**工时**: 0.5天
**优先级**: P0
**依赖**: Task 5.1

**详细任务**:
- [ ] 5.2.1 准备性能测试场景
  - [ ] 小型变更: <500 行，1-3 文件
  - [ ] 中型变更: 500-2000 行，3-10 文件
  - [ ] 大型变更: >2000 行，>10 文件

- [ ] 5.2.2 使用 JMeter 进行压力测试
  - [ ] 配置并发用户: 1, 5, 10
  - [ ] 测试审查接口响应时间
  - [ ] 测试系统资源占用

- [ ] 5.2.3 验证性能指标
  - [ ] 小型变更: <10 秒（95% 请求）
  - [ ] 中型变更: <60 秒（90% 请求）
  - [ ] 大型变更: <300 秒（85% 请求）
  - [ ] 并发 10 用户: 无超时错误

- [ ] 5.2.4 性能优化（如不达标）
  - [ ] 识别性能瓶颈
  - [ ] 优化慢查询
  - [ ] 增加缓存
  - [ ] 调整超时配置

**验收标准**:
- ✅ 所有性能指标达标
- ✅ 无内存泄漏
- ✅ 并发场景稳定

**输出文件**:
- `performance-test.jmx` (JMeter 测试脚本)
- `performance-test-report.html` (性能报告)

---

### Task 5.3: 文档编写

**工时**: 0.5天
**优先级**: P1
**依赖**: 阶段 1-4 完成

**详细任务**:
- [ ] 5.3.1 编写用户手册
  - [ ] 如何发起深度审查
  - [ ] 如何理解问题优先级
  - [ ] 如何查看和导出报告
  - [ ] 常见问题 FAQ

- [ ] 5.3.2 编写 API 文档
  - [ ] 使用 Swagger 注解
  - [ ] 生成 OpenAPI 规范文件
  - [ ] 提供请求/响应示例

- [ ] 5.3.3 更新 README.md
  - [ ] 添加深度审查功能说明
  - [ ] 更新配置说明
  - [ ] 添加截图

- [ ] 5.3.4 编写开发者文档
  - [ ] 架构设计说明
  - [ ] 核心类和接口说明
  - [ ] 扩展指南（如何支持其他语言）

**验收标准**:
- ✅ 文档完整清晰
- ✅ 包含足够的示例和截图
- ✅ 通过评审

**输出文件**:
- `user-manual.md` (新增)
- `api-docs.yaml` (新增)
- `README.md` (更新)
- `developer-guide.md` (新增)

---

## 📊 进度跟踪

### 里程碑

| 里程碑 | 日期 | 交付物 | 状态 |
|-------|------|-------|------|
| **M1: 核心功能完成** | D+3 | P0-P3 分级 + 深度 Prompt + 解析器 | ⚪ 未开始 |
| **M2: 上下文分析完成** | D+7 | 上下文提取器 + 集成 | ⚪ 未开始 |
| **M3: 前端优化完成** | D+10 | 问题高亮 + 异步审查 + 配置页 | ⚪ 未开始 |
| **M4: 报告导出完成** | D+12 | Markdown/JSON 导出 | ⚪ 未开始 |
| **M5: 测试完成发布** | D+14 | 集成测试 + 性能测试 + 文档 | ⚪ 未开始 |

### 每日站会检查点

**每日更新**:
- [ ] 今日完成的任务
- [ ] 遇到的阻塞问题
- [ ] 明日计划

**每周回顾**:
- [ ] 本周完成情况（任务数、工时）
- [ ] 下周计划调整
- [ ] 风险和依赖更新

---

## 🚨 风险管理

### 技术风险

| 风险 | 优先级 | 应对措施 | 负责人 |
|-----|-------|---------|-------|
| **Claude 输出格式不稳定** | 高 | Prompt 优化 + 容错解析 + 重试 | 后端 |
| **上下文提取失败** | 中 | 异常处理 + 降级到无上下文 | 后端 |
| **性能不达标** | 中 | 异步处理 + 分片 + 缓存 | 后端 |
| **JavaParser 解析异常** | 中 | Try-catch + 日志 + 空上下文返回 | 后端 |
| **前端兼容性问题** | 低 | 浏览器测试 + Polyfill | 前端 |

### 进度风险

| 风险 | 概率 | 影响 | 应对措施 |
|-----|------|------|---------|
| **上下文提取超期** | 中 | 高 | 简化规则，仅提取类和方法 |
| **测试发现严重 Bug** | 中 | 中 | 预留 1 天 Buffer 时间 |
| **Prompt 优化耗时** | 高 | 中 | 并行进行其他任务 |

---

## ✅ 验收标准

### 功能验收

- [ ] 所有 P0 任务 100% 完成
- [ ] 所有 P1 任务 >90% 完成
- [ ] 端到端测试通过率 100%
- [ ] 性能测试达标率 >90%

### 质量验收

- [ ] 代码覆盖率 >80%
- [ ] SonarQube 质量门禁通过（无 Blocker/Critical）
- [ ] API 文档完整
- [ ] 用户手册清晰

### 业务验收

- [ ] 问题识别准确率 >85%
- [ ] 优先级分级准确率 >90%
- [ ] JSON 解析成功率 >90%
- [ ] 用户满意度 >80%

---

## 📞 联系方式

| 角色 | 姓名 | 邮箱 | 职责 |
|-----|------|------|------|
| **项目经理** | - | - | 进度跟踪、风险管理 |
| **后端负责人** | - | - | 阶段 1/2/4 开发 |
| **前端负责人** | - | - | 阶段 3 开发 |
| **QA 负责人** | - | - | 阶段 5 测试 |
| **技术负责人** | - | - | 技术评审、难点攻关 |

---

## 📝 更新日志

| 日期 | 版本 | 修改人 | 变更内容 |
|-----|------|-------|---------|
| 2025-10-01 | v1.0 | 系统 | 初始版本，基于 spec 生成 |

---

**任务清单结束**

👉 **下一步**: 召开启动会议，分配任务负责人，开始阶段 1 开发
