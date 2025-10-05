# AI辅助开发工作流 - 详细任务清单

> **生成时间**: 2025-10-04
> **更新时间**: 2025-10-04 07:35
> **预计完成时间**: 14个工作日
> **当前完成度**: 13/28 (46%)
> **技术栈**: Spring Boot 3.2 + Java 17 + Vue.js 2.6 + Element UI

---

## 📋 任务概览

| 优先级 | 任务数 | 预计工时 | 完成状态 | 进度 |
|-------|-------|---------|---------|------|
| **P0 (核心)** | 15 | 11.5天 | 11/15 | 73% |
| **P1 (重要)** | 8 | 6.5天 | 0/8 | 0% |
| **P2 (优化)** | 5 | 3.5天 | 0/5 | 0% |
| **总计** | **28** | **21.5天** | **11/28** | **39%** |

---

## 🔥 P0 任务（核心功能，必须完成）

---

### ✅ P0-1: 创建领域层枚举和异常 【已完成】

**工时**: 0.5天
**依赖**: 无
**优先级**: P0
**负责模块**: 领域层 - 基础设施
**状态**: ✅ 已完成

**任务清单**:
- [x] **创建 `WorkflowStatus` 枚举**
  - [x] 定义12个工作流状态常量
  - [x] 添加状态描述字段 `description`
  - [x] 实现 `getDescription()` 方法
  - [x] 添加类级别 Javadoc（@author zhourui(V33215020) @since 2025/10/04）

- [x] **创建 `TaskStatus` 枚举**
  - [x] 定义5个任务状态：PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED
  - [x] 添加状态描述方法
  - [x] 添加 Javadoc 注释

- [x] **创建工作流异常类**
  - [x] `InvalidWorkflowTransitionException` 继承 RuntimeException
    - [x] 包含 `from` 和 `to` 状态字段
    - [x] 自定义错误消息格式
  - [x] `WorkflowNotFoundException` 继承 RuntimeException
    - [x] 包含工作流 ID 字段
    - [x] 自定义错误消息

**验收标准**:
- ✅ 枚举定义完整，包含所有必需状态
- ✅ 所有类包含完整 Javadoc 注释
- ✅ 异常类继承自合适的基类
- ✅ 符合 Alibaba-P3C 规范

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/model/WorkflowStatus.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/TaskStatus.java`
- `src/main/java/com/example/gitreview/domain/workflow/exception/InvalidWorkflowTransitionException.java`
- `src/main/java/com/example/gitreview/domain/workflow/exception/WorkflowNotFoundException.java`

---

### ✅ P0-2: 创建领域层值对象 【已完成】

**工时**: 1天
**依赖**: P0-1
**优先级**: P0
**负责模块**: 领域层 - 值对象
**状态**: ✅ 已完成

**任务清单**:
- [ ] **创建 `Specification` 值对象**
  - [ ] 字段定义：
    - `String prdContent` - PRD内容
    - `List<String> documentPaths` - 文档空间路径列表
    - `String generatedContent` - 生成的spec.md内容
    - `LocalDateTime generatedAt` - 生成时间
  - [ ] 实现业务方法 `validateContent()` - 验证内容完整性
  - [ ] 所有字段使用 final（不可变）
  - [ ] 实现 equals() 和 hashCode()
  - [ ] 添加完整 Javadoc

- [ ] **创建 `TechnicalDesign` 值对象**
  - [ ] 字段定义：
    - `String content` - 技术方案内容
    - `int version` - 版本号（从1开始）
    - `boolean approved` - 是否已批准
    - `LocalDateTime createdAt` - 创建时间
    - `LocalDateTime approvedAt` - 批准时间
  - [ ] 实现业务方法：
    - `createNewVersion(String newContent)` - 创建新版本
    - `approve()` - 批准方案
  - [ ] 实现 equals() 和 hashCode()
  - [ ] 添加完整 Javadoc

- [ ] **创建 `Task` 值对象**
  - [ ] 字段定义：
    - `String id` - 任务ID（如 "P0-1"）
    - `String title` - 任务标题
    - `String description` - 任务描述
    - `TaskStatus status` - 任务状态
    - `List<String> dependencies` - 依赖的任务ID列表
    - `String targetFile` - 目标文件路径
    - `String generatedCode` - 生成的代码
    - `LocalDateTime completedAt` - 完成时间
  - [ ] 实现业务方法：
    - `complete(String code)` - 完成任务
    - `fail(String reason)` - 标记失败
    - `isExecutable(List<Task> allTasks)` - 检查是否可执行（依赖已完成）
  - [ ] 实现 equals() 和 hashCode()（基于 id）
  - [ ] 添加完整 Javadoc

- [ ] **创建 `TaskList` 值对象**
  - [ ] 字段定义：
    - `String content` - tasklist.md完整内容
    - `List<Task> tasks` - 解析后的任务列表
    - `LocalDateTime generatedAt` - 生成时间
  - [ ] 实现业务方法：
    - `getExecutableTasks()` - 获取可执行任务列表
    - `getProgress()` - 计算完成进度（百分比）
    - `getTaskById(String id)` - 根据ID查找任务
  - [ ] 实现 equals() 和 hashCode()
  - [ ] 添加完整 Javadoc

**验收标准**:
- ✅ 所有值对象字段使用 final 修饰（不可变性）
- ✅ 包含必要的业务方法
- ✅ 正确实现 equals() 和 hashCode()
- ✅ 完整的 Javadoc 注释（@param, @return）
- ✅ 符合 DDD 值对象设计原则

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/Specification.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/TechnicalDesign.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/Task.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/TaskList.java`

---

### ✅ P0-3: 创建 DevelopmentWorkflow 聚合根

**工时**: 1.5天
**依赖**: P0-2
**优先级**: P0
**负责模块**: 领域层 - 聚合根

**任务清单**:
- [ ] **创建 `DevelopmentWorkflow` 类基础结构**
  - [ ] 基本字段：
    - `Long id` - 工作流ID
    - `String name` - 工作流名称
    - `Long repositoryId` - 关联仓库ID
    - `WorkflowStatus status` - 当前状态
    - `LocalDateTime createdAt` - 创建时间
    - `LocalDateTime updatedAt` - 更新时间
    - `String createdBy` - 创建者
  - [ ] 关联值对象：
    - `Specification specification`
    - `TechnicalDesign technicalDesign`
    - `TaskList taskList`
    - `List<Task> codeGenerationTasks`
  - [ ] 进度信息：
    - `int progress` (0-100)
    - `String currentStage` - 当前阶段描述

- [ ] **实现业务方法 - 规格文档阶段**
  - [ ] `startSpecGeneration()`
    - 验证状态必须为 DRAFT
    - 更新状态为 SPEC_GENERATING
    - 更新 currentStage
  - [ ] `completeSpecGeneration(Specification spec)`
    - 验证状态必须为 SPEC_GENERATING
    - 保存 specification
    - 更新状态为 SPEC_GENERATED
    - 更新 progress

- [ ] **实现业务方法 - 技术方案阶段**
  - [ ] `startTechDesign()`
    - 验证状态为 SPEC_GENERATED 或 TECH_DESIGN_GENERATED
    - 更新状态为 TECH_DESIGN_GENERATING
  - [ ] `completeTechDesign(TechnicalDesign design)`
    - 保存 technicalDesign
    - 更新状态为 TECH_DESIGN_GENERATED
  - [ ] `updateTechDesign(String content)`
    - 验证状态为 TECH_DESIGN_GENERATED
    - 创建新版本 TechnicalDesign
    - 保留历史版本
  - [ ] `approveTechDesign()`
    - 调用 technicalDesign.approve()
    - 更新状态为 TECH_DESIGN_APPROVED

- [ ] **实现业务方法 - 任务列表阶段**
  - [ ] `startTaskListGeneration()`
    - 验证状态为 TECH_DESIGN_APPROVED
    - 更新状态为 TASK_LIST_GENERATING
  - [ ] `completeTaskListGeneration(TaskList tasks)`
    - 保存 taskList
    - 更新状态为 TASK_LIST_GENERATED

- [ ] **实现业务方法 - 代码生成阶段**
  - [ ] `startCodeGeneration()`
    - 验证状态为 TASK_LIST_GENERATED
    - 更新状态为 CODE_GENERATING
  - [ ] `completeTask(String taskId, String code)`
    - 查找任务并标记完成
    - 更新 progress
    - 所有任务完成后更新状态为 COMPLETED

- [ ] **实现其他业务方法**
  - [ ] `markAsFailed(String reason)` - 标记失败
  - [ ] `cancel(String reason)` - 取消工作流
  - [ ] `updateProgress(int progress)` - 更新进度
  - [ ] 所有状态转换方法自动更新 `updatedAt`

- [ ] **实现状态转换验证**
  - [ ] 在每个状态转换方法中调用 `validateTransition(from, to)`
  - [ ] 非法转换抛出 `InvalidWorkflowTransitionException`

**验收标准**:
- ✅ 所有业务方法包含状态验证
- ✅ 状态转换时抛出合适的异常
- ✅ 自动更新 updateTime
- ✅ 方法命名清晰，体现业务意图
- ✅ 完整的 Javadoc 注释

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/model/aggregate/DevelopmentWorkflow.java`

---

### ✅ P0-4: 创建 WorkflowDomainService

**工时**: 1天
**依赖**: P0-3
**优先级**: P0
**负责模块**: 领域层 - 领域服务

**任务清单**:
- [ ] **创建 `WorkflowDomainService` 类**
  - [ ] 添加 @Service 注解
  - [ ] 添加类级别 Javadoc

- [ ] **实现状态转换验证**
  - [ ] `isValidTransition(WorkflowStatus from, WorkflowStatus to)`
  - [ ] 定义状态转换矩阵（Map）：
    ```java
    DRAFT -> SPEC_GENERATING
    SPEC_GENERATING -> SPEC_GENERATED, FAILED
    SPEC_GENERATED -> TECH_DESIGN_GENERATING
    TECH_DESIGN_GENERATING -> TECH_DESIGN_GENERATED, FAILED
    TECH_DESIGN_GENERATED -> TECH_DESIGN_GENERATING, TECH_DESIGN_APPROVED
    TECH_DESIGN_APPROVED -> TASK_LIST_GENERATING
    TASK_LIST_GENERATING -> TASK_LIST_GENERATED, FAILED
    TASK_LIST_GENERATED -> CODE_GENERATING
    CODE_GENERATING -> COMPLETED, FAILED
    任意状态 -> CANCELLED
    ```
  - [ ] 返回 boolean 结果

- [ ] **实现验证方法**
  - [ ] `validateSpecification(Specification spec)`
    - 检查 prdContent 非空
    - 检查 generatedContent 非空
    - 抛出 IllegalArgumentException
  - [ ] `validateTechnicalDesign(TechnicalDesign design)`
    - 检查 content 非空
    - 检查 version >= 1
    - 抛出 IllegalArgumentException

- [ ] **实现进度计算**
  - [ ] `calculateProgress(DevelopmentWorkflow workflow)`
  - [ ] 根据当前状态计算进度：
    ```
    DRAFT: 0%
    SPEC_GENERATING/GENERATED: 20%
    TECH_DESIGN_*: 40%
    TASK_LIST_*: 60%
    CODE_GENERATING: 60-99%（根据任务完成情况）
    COMPLETED: 100%
    ```
  - [ ] 代码生成阶段按任务比例计算

**验收标准**:
- ✅ 状态转换规则完整且正确
- ✅ 验证逻辑准确
- ✅ 进度计算符合业务逻辑
- ✅ 完整的 Javadoc 注释
- ✅ 单元测试覆盖所有分支

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/service/WorkflowDomainService.java`

---

### ✅ P0-5: 创建 WorkflowRepository 接口

**工时**: 0.5天
**依赖**: P0-3
**优先级**: P0
**负责模块**: 领域层 - 仓储接口

**任务清单**:
- [ ] **创建 `WorkflowRepository` 接口**
  - [ ] 定义接口（package: domain/workflow/repository）
  - [ ] 添加接口级别 Javadoc

- [ ] **定义 CRUD 方法**
  - [ ] `DevelopmentWorkflow save(DevelopmentWorkflow workflow)` - 保存或更新
  - [ ] `Optional<DevelopmentWorkflow> findById(Long id)` - 根据ID查找
  - [ ] `List<DevelopmentWorkflow> findAll()` - 查找所有
  - [ ] `List<DevelopmentWorkflow> findByRepositoryId(Long repositoryId)` - 根据仓库ID查找
  - [ ] `void delete(Long id)` - 删除工作流

- [ ] **添加方法 Javadoc**
  - [ ] 每个方法包含完整的 @param 和 @return 说明

**验收标准**:
- ✅ 接口定义清晰
- ✅ 返回类型使用 Optional（避免 null）
- ✅ 完整的 Javadoc 注释
- ✅ 符合 DDD 仓储模式

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/repository/WorkflowRepository.java`

---

### ✅ P0-6: 实现 WorkflowStorageAdapter

**工时**: 1天
**依赖**: P0-5
**优先级**: P0
**负责模块**: 基础设施层 - 存储适配器

**任务清单**:
- [ ] **创建 `WorkflowStorageAdapter` 类**
  - [ ] 添加 @Component 注解
  - [ ] 实现 `WorkflowRepository` 接口
  - [ ] 注入配置：`@Value("${workflow.storage.file}")`

- [ ] **实现 JSON 序列化配置**
  - [ ] 创建 ObjectMapper 实例
  - [ ] 配置 JavaTimeModule（支持 LocalDateTime）
  - [ ] 配置 DeserializationFeature（容错）

- [ ] **实现存储方法**
  - [ ] `save(DevelopmentWorkflow workflow)`
    - 如果 ID 为 null，生成新 ID（自增）
    - 读取现有数据
    - 更新或添加工作流
    - 写入 JSON 文件
    - 返回保存的对象

- [ ] **实现查询方法**
  - [ ] `findById(Long id)` - 从 JSON 文件读取并过滤
  - [ ] `findAll()` - 读取所有工作流
  - [ ] `findByRepositoryId(Long repositoryId)` - 按仓库ID过滤

- [ ] **实现删除方法**
  - [ ] `delete(Long id)` - 从列表中移除并保存

- [ ] **实现并发控制**
  - [ ] 使用 synchronized 保护文件读写
  - [ ] 或使用 ReentrantReadWriteLock

- [ ] **实现错误处理**
  - [ ] 文件不存在时返回空列表
  - [ ] JSON 解析失败记录日志
  - [ ] IO 异常包装为 RuntimeException

**验收标准**:
- ✅ JSON 格式正确（可读性好）
- ✅ 支持并发读写（无数据丢失）
- ✅ 错误处理完善
- ✅ ID 生成策略正确（自增）
- ✅ 符合六边形架构适配器模式

**输出文件**:
- `src/main/java/com/example/gitreview/infrastructure/storage/adapter/WorkflowStorageAdapter.java`

---

### ✅ P0-7: 创建 TaskListParser

**工时**: 1天
**依赖**: P0-2
**优先级**: P0
**负责模块**: 基础设施层 - 解析器

**任务清单**:
- [ ] **创建 `TaskListParser` 类**
  - [ ] 添加 @Component 注解
  - [ ] 添加类级别 Javadoc

- [ ] **实现 `parse(String markdownContent)` 方法**
  - [ ] 按行分割 Markdown 内容
  - [ ] 使用状态机模式解析

- [ ] **提取任务信息**
  - [ ] 识别任务标题（正则：`^### .* (P\d+-\d+).*$`）
  - [ ] 提取任务 ID（如 "P0-1", "P1-3"）
  - [ ] 提取任务标题文本
  - [ ] 提取任务清单（`- [ ] ...`）
  - [ ] 提取依赖（`**依赖**: P0-1, P0-2`）
  - [ ] 提取目标文件（`**文件**: src/.../XXX.java`）
  - [ ] 提取预计工时（`**工时**: 1天`）

- [ ] **构建 Task 对象**
  - [ ] 创建 Task 对象
  - [ ] 设置初始状态为 PENDING
  - [ ] 解析依赖列表（逗号分隔）
  - [ ] 组装 description（合并任务清单）

- [ ] **实现容错逻辑**
  - [ ] 格式不标准时记录警告日志
  - [ ] 缺失字段使用默认值
  - [ ] 解析失败不崩溃，返回部分结果

- [ ] **添加单元测试**
  - [ ] 测试标准格式解析
  - [ ] 测试缺失依赖的情况
  - [ ] 测试异常格式处理

**验收标准**:
- ✅ 解析准确率 >90%（针对标准格式）
- ✅ 处理异常情况不崩溃
- ✅ 日志记录完善
- ✅ 单元测试覆盖率 >80%

**输出文件**:
- `src/main/java/com/example/gitreview/infrastructure/parser/TaskListParser.java`
- `src/test/java/com/example/gitreview/infrastructure/parser/TaskListParserTest.java`

---

### ✅ P0-8: 创建 workflow-prompts.properties

**工时**: 0.5天
**依赖**: 无
**优先级**: P0
**负责模块**: 基础设施层 - 配置

**任务清单**:
- [ ] **创建配置文件**
  - [ ] 在 `src/main/resources/` 创建 `workflow-prompts.properties`
  - [ ] 添加文件头注释

- [ ] **编写规格文档生成提示词**
  - [ ] 定义 `workflow.prompt.spec`
  - [ ] 角色设定：资深软件工程师
  - [ ] 输出要求：
    - 明确需求目标和边界
    - 定义核心功能模块
    - 列出关键业务规则
    - 说明外部依赖和接口
    - 使用 Markdown 格式
  - [ ] 包含占位符：`{prd}`, `{documents}`
  - [ ] 提供 Few-shot 示例（可选）

- [ ] **编写技术方案生成提示词**
  - [ ] 定义 `workflow.prompt.tech-design`
  - [ ] 角色设定：资深架构师
  - [ ] 输出要求：
    - 分析现有代码架构
    - 设计领域模型（遵循DDD）
    - 定义 API 接口
    - 规划实现步骤
    - 识别技术风险
    - 使用 Markdown 格式
  - [ ] 包含占位符：`{spec}`, `{repoContext}`

- [ ] **编写任务列表生成提示词**
  - [ ] 定义 `workflow.prompt.tasklist`
  - [ ] 角色设定：项目经理
  - [ ] 输出要求：
    - 按优先级分组（P0/P1/P2/P3）
    - 每个任务包含：ID、标题、描述、依赖、预计工时
    - 任务颗粒度适中（0.5-1天）
    - 明确任务依赖关系
    - 使用 Markdown 格式（固定格式）
  - [ ] 包含占位符：`{techDesign}`
  - [ ] 提供输出格式示例

- [ ] **编写代码生成提示词**
  - [ ] 定义 `workflow.prompt.code`
  - [ ] 角色设定：资深开发工程师
  - [ ] 输出要求：
    - 遵循现有代码风格
    - 遵循 DDD 架构分层
    - 包含必要的注释（@author @since）
    - 符合 Java 最佳实践
    - 仅返回代码，不要额外说明
  - [ ] 包含占位符：`{taskDescription}`, `{codeContext}`

**验收标准**:
- ✅ Prompt 模板完整
- ✅ 格式清晰，易于理解
- ✅ 占位符定义明确
- ✅ 输出格式规范（便于解析）

**输出文件**:
- `src/main/resources/workflow-prompts.properties`

---

### ✅ P0-9: 实现 WorkflowApplicationService（基础）

**工时**: 1.5天
**依赖**: P0-6, P0-7, P0-8
**优先级**: P0
**负责模块**: 应用层 - 应用服务

**任务清单**:
- [ ] **创建 `WorkflowApplicationService` 类**
  - [ ] 添加 @Service 注解
  - [ ] 注入依赖：
    - `WorkflowRepository workflowRepository`
    - `WorkflowDomainService workflowDomainService`
    - `ClaudeQueryPort claudeQueryPort`
    - `GitOperationPort gitOperationPort`
  - [ ] 注入配置：提示词配置

- [ ] **实现创建工作流方法**
  - [ ] `createWorkflow(CreateWorkflowRequest request)`
  - [ ] 创建 DevelopmentWorkflow 聚合根
  - [ ] 设置初始状态为 DRAFT
  - [ ] 保存到仓储
  - [ ] 返回工作流 ID

- [ ] **实现规格文档生成（异步）**
  - [ ] `generateSpecification(Long workflowId, SpecGenerationRequest request)`
  - [ ] 添加 @Async 注解
  - [ ] 流程：
    1. 加载工作流
    2. 验证状态为 DRAFT
    3. 调用 `workflow.startSpecGeneration()`
    4. 保存工作流
    5. 读取 PRD 和文档内容（从仓库）
    6. 替换提示词占位符
    7. 调用 `claudeQueryPort.query(prompt)`
    8. 创建 Specification 对象
    9. 调用 `workflow.completeSpecGeneration(spec)`
    10. 保存工作流
  - [ ] 异常处理：失败时调用 `workflow.markAsFailed(reason)`

- [ ] **实现查询方法**
  - [ ] `getSpecification(Long workflowId)`
  - [ ] 加载工作流
  - [ ] 返回 SpecificationDTO
  - [ ] 使用 DTO Assembler 转换

- [ ] **实现通用方法**
  - [ ] `getWorkflowStatus(Long workflowId)` - 返回 WorkflowStatusDTO
  - [ ] `getProgress(Long workflowId)` - 返回 WorkflowProgressDTO
  - [ ] `cancelWorkflow(Long workflowId, String reason)` - 取消工作流

**验收标准**:
- ✅ 异步方法正确标注 @Async
- ✅ 异常处理完善（失败时更新状态）
- ✅ 状态更新正确
- ✅ DTO 转换准确
- ✅ 日志记录关键节点

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`

---

### ✅ P0-10: 实现 WorkflowApplicationService（技术方案）

**工时**: 1天
**依赖**: P0-9
**优先级**: P0
**负责模块**: 应用层 - 应用服务

**任务清单**:
- [ ] **实现技术方案生成（异步）**
  - [ ] `generateTechnicalDesign(Long workflowId)`
  - [ ] 添加 @Async 注解
  - [ ] 流程：
    1. 加载工作流
    2. 验证状态为 SPEC_GENERATED 或 TECH_DESIGN_GENERATED
    3. 调用 `workflow.startTechDesign()`
    4. 保存工作流
    5. 读取 spec.md 内容
    6. 获取代码仓库结构（调用 `gitOperationPort` 或 `CodeContextExtractor`）
    7. 替换提示词占位符
    8. 调用 `claudeQueryPort.query(prompt)`
    9. 创建 TechnicalDesign 对象（version = 当前版本+1）
    10. 调用 `workflow.completeTechDesign(design)`
    11. 保存工作流
  - [ ] 异常处理

- [ ] **实现技术方案更新**
  - [ ] `updateTechnicalDesign(Long workflowId, String content)`
  - [ ] 加载工作流
  - [ ] 验证状态为 TECH_DESIGN_GENERATED
  - [ ] 调用 `workflow.updateTechDesign(content)`
  - [ ] 保存工作流（新版本）

- [ ] **实现技术方案批准**
  - [ ] `approveTechnicalDesign(Long workflowId)`
  - [ ] 加载工作流
  - [ ] 调用 `workflow.approveTechDesign()`
  - [ ] 状态更新为 TECH_DESIGN_APPROVED
  - [ ] 保存工作流

- [ ] **实现查询方法**
  - [ ] `getTechnicalDesign(Long workflowId)` - 返回 TechnicalDesignDTO
  - [ ] 包含版本信息和批准状态

**验收标准**:
- ✅ 版本管理正确（自动递增）
- ✅ 状态流转正确
- ✅ 支持重新生成（版本+1）
- ✅ 批准后状态正确更新

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`（扩展）

---

### ✅ P0-11: 实现 WorkflowApplicationService（任务列表）

**工时**: 1天
**依赖**: P0-10
**优先级**: P0
**负责模块**: 应用层 - 应用服务

**任务清单**:
- [ ] **实现任务列表生成（异步）**
  - [ ] `generateTaskList(Long workflowId)`
  - [ ] 添加 @Async 注解
  - [ ] 流程：
    1. 加载工作流
    2. 验证状态为 TECH_DESIGN_APPROVED
    3. 调用 `workflow.startTaskListGeneration()`
    4. 保存工作流
    5. 读取技术方案内容
    6. 替换提示词占位符
    7. 调用 `claudeQueryPort.query(prompt)`
    8. 使用 `TaskListParser.parse()` 解析 Markdown
    9. 创建 TaskList 对象
    10. 调用 `workflow.completeTaskListGeneration(taskList)`
    11. 保存工作流
  - [ ] 异常处理

- [ ] **实现查询方法**
  - [ ] `getTaskList(Long workflowId)` - 返回 TaskListDTO
  - [ ] 包含任务列表和状态信息
  - [ ] DTO 包含每个任务的详细信息

**验收标准**:
- ✅ 任务列表解析成功
- ✅ 任务依赖关系正确
- ✅ 任务状态初始化为 PENDING
- ✅ DTO 转换准确

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`（扩展）

---

### ✅ P0-12: 实现 WorkflowApplicationService（代码生成）

**工时**: 1.5天
**依赖**: P0-11
**优先级**: P0
**负责模块**: 应用层 - 应用服务

**任务清单**:
- [ ] **实现代码生成（异步）**
  - [ ] `startCodeGeneration(Long workflowId)`
  - [ ] 添加 @Async 注解
  - [ ] 流程：
    1. 加载工作流
    2. 验证状态为 TASK_LIST_GENERATED
    3. 调用 `workflow.startCodeGeneration()`
    4. 保存工作流
    5. 获取任务列表
    6. 遍历任务（按优先级和依赖顺序）：
       - 检查依赖是否完成（调用 `task.isExecutable()`）
       - 跳过不可执行任务
       - 更新任务状态为 IN_PROGRESS
       - 提取代码上下文（使用 `CodeContextExtractor`）
       - 替换提示词占位符
       - 调用 `claudeQueryPort.query(prompt)`
       - 调用 `workflow.completeTask(taskId, code)`
       - 保存工作流
       - 更新进度
    7. 所有任务完成后状态更新为 COMPLETED
  - [ ] 异常处理：单个任务失败时标记为 FAILED，继续其他任务

- [ ] **实现进度更新逻辑**
  - [ ] 每完成一个任务，计算整体进度
  - [ ] 调用 `workflowDomainService.calculateProgress()`
  - [ ] 实时保存工作流状态

- [ ] **实现任务依赖检查**
  - [ ] 在执行任务前检查所有依赖任务是否 COMPLETED
  - [ ] 依赖未完成则跳过，等待下次循环

**验收标准**:
- ✅ 尊重任务依赖关系（依赖未完成不执行）
- ✅ 进度实时更新
- ✅ 单个任务错误不影响其他任务
- ✅ 最终状态正确（COMPLETED 或 FAILED）

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`（扩展）

---

### ✅ P0-13: 创建 DTO 类

**工时**: 0.5天
**依赖**: P0-9
**优先级**: P0
**负责模块**: 应用层 - DTO

**任务清单**:
- [ ] **创建请求 DTO**
  - [ ] `CreateWorkflowRequest`
    - `String name`
    - `Long repositoryId`
    - `String createdBy`
    - 添加 @NotNull, @NotEmpty 验证注解
  - [ ] `SpecGenerationRequest`
    - `String prdContent`
    - `List<String> documentPaths`
    - 添加验证注解

- [ ] **创建响应 DTO**
  - [ ] `SpecificationDTO`
    - `String content`
    - `LocalDateTime generatedAt`
  - [ ] `TechnicalDesignDTO`
    - `String content`
    - `int version`
    - `boolean approved`
    - `LocalDateTime createdAt`
    - `LocalDateTime approvedAt`
  - [ ] `TaskListDTO`
    - `String content`
    - `List<TaskDTO> tasks`
  - [ ] `TaskDTO`
    - `String id`
    - `String title`
    - `String description`
    - `String status`
    - `List<String> dependencies`
    - `String generatedCode`
  - [ ] `WorkflowProgressDTO`
    - `Long workflowId`
    - `String status`
    - `int progress` (0-100)
    - `String currentStage`
    - `int completedTasks`
    - `int totalTasks`
  - [ ] `WorkflowStatusDTO`
    - `Long id`
    - `String name`
    - `String status`
    - `int progress`
    - `LocalDateTime createdAt`
    - `LocalDateTime updatedAt`

- [ ] **添加 DTO Assembler（可选）**
  - [ ] 创建 `WorkflowDtoAssembler` 工具类
  - [ ] 实现 `toSpecificationDTO()`, `toTechnicalDesignDTO()` 等方法

**验收标准**:
- ✅ DTO 定义完整
- ✅ 包含必要的验证注解
- ✅ 字段命名清晰
- ✅ 与领域对象解耦

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/dto/CreateWorkflowRequest.java`
- `src/main/java/com/example/gitreview/application/workflow/dto/SpecGenerationRequest.java`
- `src/main/java/com/example/gitreview/application/workflow/dto/SpecificationDTO.java`
- `src/main/java/com/example/gitreview/application/workflow/dto/TechnicalDesignDTO.java`
- `src/main/java/com/example/gitreview/application/workflow/dto/TaskListDTO.java`
- `src/main/java/com/example/gitreview/application/workflow/dto/TaskDTO.java`
- `src/main/java/com/example/gitreview/application/workflow/dto/WorkflowProgressDTO.java`
- `src/main/java/com/example/gitreview/application/workflow/dto/WorkflowStatusDTO.java`
- `src/main/java/com/example/gitreview/application/workflow/assembler/WorkflowDtoAssembler.java`（可选）

---

### ✅ P0-14: 实现 WorkflowController

**工时**: 1天
**依赖**: P0-13
**优先级**: P0
**负责模块**: 应用层 - API

**任务清单**:
- [ ] **创建 `WorkflowController` 类**
  - [ ] 添加 @RestController 注解
  - [ ] 添加 @RequestMapping("/api/workflow")
  - [ ] 注入 `WorkflowApplicationService`

- [ ] **实现工作流管理接口**
  - [ ] `POST /api/workflow` - 创建工作流
    - 参数：CreateWorkflowRequest
    - 返回：`{"workflowId": 1}`
  - [ ] `GET /api/workflow` - 获取所有工作流
    - 返回：`List<WorkflowStatusDTO>`
  - [ ] `GET /api/workflow/{id}/status` - 获取工作流状态
    - 返回：WorkflowStatusDTO
  - [ ] `GET /api/workflow/{id}/progress` - 获取进度
    - 返回：WorkflowProgressDTO
  - [ ] `POST /api/workflow/{id}/cancel` - 取消工作流
    - 参数：`{"reason": "..."}`
    - 返回：HTTP 200

- [ ] **实现规格文档接口**
  - [ ] `POST /api/workflow/{id}/spec/generate` - 生成规格文档
    - 参数：SpecGenerationRequest
    - 返回：HTTP 202 Accepted
  - [ ] `GET /api/workflow/{id}/spec` - 获取规格文档
    - 返回：SpecificationDTO

- [ ] **实现技术方案接口**
  - [ ] `POST /api/workflow/{id}/tech-design/generate` - 生成技术方案
    - 返回：HTTP 202 Accepted
  - [ ] `GET /api/workflow/{id}/tech-design` - 获取技术方案
    - 返回：TechnicalDesignDTO
  - [ ] `PUT /api/workflow/{id}/tech-design` - 更新技术方案
    - 参数：`{"content": "..."}`
    - 返回：HTTP 200
  - [ ] `POST /api/workflow/{id}/tech-design/approve` - 批准技术方案
    - 返回：HTTP 200

- [ ] **实现任务列表接口**
  - [ ] `POST /api/workflow/{id}/tasklist/generate` - 生成任务列表
    - 返回：HTTP 202 Accepted
  - [ ] `GET /api/workflow/{id}/tasklist` - 获取任务列表
    - 返回：TaskListDTO

- [ ] **实现代码生成接口**
  - [ ] `POST /api/workflow/{id}/code-generation/start` - 开始代码生成
    - 返回：HTTP 202 Accepted

- [ ] **添加异常处理**
  - [ ] 添加 @ExceptionHandler 方法
  - [ ] 捕获 WorkflowNotFoundException
  - [ ] 捕获 InvalidWorkflowTransitionException
  - [ ] 返回统一错误格式

**验收标准**:
- ✅ 所有接口可正常调用
- ✅ 返回格式符合 RESTful 规范
- ✅ 异常处理完善
- ✅ 异步接口返回 202 Accepted
- ✅ 添加 Swagger 注解（可选）

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/api/WorkflowController.java`

---

### ✅ P0-15: 更新配置文件

**工时**: 0.5天
**依赖**: P0-8
**优先级**: P0
**负责模块**: 配置层

**任务清单**:
- [ ] **更新 `application.properties`**
  - [ ] 添加工作流配置：
    ```properties
    # 工作流配置
    workflow.storage.file=data/workflows.json
    workflow.claude.timeout=180000
    workflow.max.concurrent.workflows=5
    workflow.code.generation.timeout=60000

    # 提示词配置文件
    workflow.prompts.file=workflow-prompts.properties
    ```

- [ ] **更新或创建 `AsyncConfig`**
  - [ ] 添加 @Configuration 和 @EnableAsync 注解
  - [ ] 创建 `workflowExecutor` Bean：
    ```java
    @Bean("workflowExecutor")
    public Executor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("workflow-");
        executor.initialize();
        return executor;
    }
    ```
  - [ ] 在 `WorkflowApplicationService` 的 @Async 方法指定 executor

- [ ] **创建数据目录**
  - [ ] 确保 `data/` 目录存在
  - [ ] 如不存在，应用启动时自动创建

**验收标准**:
- ✅ 配置项完整
- ✅ 线程池配置合理
- ✅ 异步执行器正常工作
- ✅ 数据目录可正常读写

**输出文件**:
- `src/main/resources/application.properties`（更新）
- `src/main/java/com/example/gitreview/config/AsyncConfig.java`（创建或更新）

---

## ⭐ P1 任务（重要功能，建议完成）

---

### ✅ P1-1: 扩展 CodeContextExtractor

**工时**: 0.5天
**依赖**: P0-12
**优先级**: P1
**负责模块**: 基础设施层

**任务清单**:
- [ ] **扩展 `CodeContextExtractor` 类**
  - [ ] 添加方法 `extractRepositoryStructure(String repoPath)`
  - [ ] 遍历项目目录（使用 Files.walk）
  - [ ] 过滤 Java 文件
  - [ ] 提取主要包结构
  - [ ] 提取类列表（包括类名和路径）
  - [ ] 返回树形结构文本（Markdown 格式）
  - [ ] 示例输出：
    ```
    ## Repository Structure
    - com.example.gitreview
      - domain
        - workflow
          - model (DevelopmentWorkflow.java, Task.java)
          - service (WorkflowDomainService.java)
      - application
        - workflow (WorkflowApplicationService.java)
      - infrastructure
        - ...
    ```

**验收标准**:
- ✅ 能够提取完整的仓库结构
- ✅ 输出格式清晰易读
- ✅ 性能良好（大型项目不超时）

**输出文件**:
- `src/main/java/com/example/gitreview/infrastructure/context/CodeContextExtractor.java`（扩展）

---

### ✅ P1-2: 编写领域层单元测试

**工时**: 1天
**依赖**: P0-4
**优先级**: P1
**负责模块**: 测试

**任务清单**:
- [ ] **创建 `DevelopmentWorkflowTest`**
  - [ ] 测试状态转换方法（正常流程）
  - [ ] 测试非法状态转换（抛出异常）
  - [ ] 测试业务规则验证
  - [ ] 测试进度更新

- [ ] **创建 `WorkflowDomainServiceTest`**
  - [ ] 测试状态转换验证（所有合法和非法转换）
  - [ ] 测试进度计算（各个状态）
  - [ ] 测试 Specification 验证
  - [ ] 测试 TechnicalDesign 验证

- [ ] **创建值对象测试**
  - [ ] `SpecificationTest` - 测试验证方法
  - [ ] `TechnicalDesignTest` - 测试版本创建和批准
  - [ ] `TaskTest` - 测试完成、失败、可执行性判断
  - [ ] `TaskListTest` - 测试可执行任务获取、进度计算

**验收标准**:
- ✅ 测试覆盖率 >80%
- ✅ 所有测试通过
- ✅ 测试用例清晰，命名规范
- ✅ 使用 JUnit 5 + AssertJ

**输出文件**:
- `src/test/java/com/example/gitreview/domain/workflow/model/aggregate/DevelopmentWorkflowTest.java`
- `src/test/java/com/example/gitreview/domain/workflow/service/WorkflowDomainServiceTest.java`
- `src/test/java/com/example/gitreview/domain/workflow/model/valueobject/SpecificationTest.java`
- `src/test/java/com/example/gitreview/domain/workflow/model/valueobject/TechnicalDesignTest.java`
- `src/test/java/com/example/gitreview/domain/workflow/model/valueobject/TaskTest.java`
- `src/test/java/com/example/gitreview/domain/workflow/model/valueobject/TaskListTest.java`

---

### ✅ P1-3: 编写应用层集成测试

**工时**: 1天
**依赖**: P0-14
**优先级**: P1
**负责模块**: 测试

**任务清单**:
- [ ] **创建 `WorkflowApplicationServiceTest`**
  - [ ] 使用 Mockito Mock 所有外部依赖
    - Mock WorkflowRepository
    - Mock ClaudeQueryPort
    - Mock GitOperationPort
  - [ ] 测试创建工作流
  - [ ] 测试规格文档生成流程
  - [ ] 测试技术方案生成流程
  - [ ] 测试技术方案更新和批准
  - [ ] 测试任务列表生成流程
  - [ ] 测试代码生成流程
  - [ ] 测试异常场景（状态错误、Claude 失败等）

- [ ] **创建 `WorkflowControllerTest`**
  - [ ] 使用 MockMvc 测试所有 API
  - [ ] 测试请求参数验证
  - [ ] 测试响应格式
  - [ ] 测试异常处理（404, 400 等）

**验收标准**:
- ✅ 测试覆盖主要流程
- ✅ 所有测试通过
- ✅ Mock 验证调用次数和参数
- ✅ 使用 @SpringBootTest 或 @WebMvcTest

**输出文件**:
- `src/test/java/com/example/gitreview/application/workflow/WorkflowApplicationServiceTest.java`
- `src/test/java/com/example/gitreview/application/workflow/api/WorkflowControllerTest.java`

---

### ✅ P1-4: 创建前端工作流列表页面

**工时**: 0.5天
**依赖**: P0-14
**优先级**: P1
**负责模块**: 前端

**任务清单**:
- [ ] **创建 `WorkflowList.vue` 组件**
  - [ ] 使用 Element UI Table 展示工作流列表
  - [ ] 表格列：
    - 工作流名称
    - 关联仓库
    - 状态（Tag 组件，不同颜色）
    - 进度（Progress 组件）
    - 创建时间
    - 操作（按钮组）
  - [ ] 状态筛选器（Select）
  - [ ] 创建工作流按钮（跳转到创建页面）
  - [ ] 操作按钮：
    - 查看详情（跳转到对应阶段页面）
    - 取消（弹窗确认）
    - 删除（弹窗确认）

- [ ] **实现 API 调用**
  - [ ] `GET /api/workflow` - 获取工作流列表
  - [ ] `POST /api/workflow/{id}/cancel` - 取消工作流

- [ ] **实现数据刷新**
  - [ ] 定时刷新（每5秒）
  - [ ] 手动刷新按钮

**验收标准**:
- ✅ 列表正确展示
- ✅ 状态筛选正常
- ✅ 操作按钮功能正常
- ✅ 进度条实时更新

**输出文件**:
- `src/main/resources/static/components/WorkflowList.vue`

---

### ✅ P1-5: 创建前端工作流创建页面

**工时**: 0.5天
**依赖**: P1-4
**优先级**: P1
**负责模块**: 前端

**任务清单**:
- [ ] **创建 `WorkflowCreate.vue` 组件**
  - [ ] 表单字段：
    - 工作流名称（Input）
    - 选择关联仓库（Select，调用仓库列表 API）
    - PRD 内容（Textarea，可扩展）
    - 文档空间文件选择器（Multi-select 或 Tree）
  - [ ] 提交按钮
  - [ ] 取消按钮（返回列表）

- [ ] **实现表单验证**
  - [ ] 必填字段验证（使用 Element UI Form 验证）
  - [ ] PRD 内容长度验证

- [ ] **实现 API 调用**
  - [ ] `GET /api/repositories` - 获取仓库列表
  - [ ] `POST /api/workflow` - 创建工作流
  - [ ] `POST /api/workflow/{id}/spec/generate` - 生成规格文档

- [ ] **提交流程**
  - [ ] 创建工作流
  - [ ] 自动触发规格文档生成
  - [ ] 跳转到规格文档页面

**验收标准**:
- ✅ 表单验证完整
- ✅ 提交成功后正确跳转
- ✅ 错误提示友好

**输出文件**:
- `src/main/resources/static/components/WorkflowCreate.vue`

---

### ✅ P1-6: 创建前端规格文档页面

**工时**: 0.5天
**依赖**: P1-5
**优先级**: P1
**负责模块**: 前端

**任务清单**:
- [ ] **创建 `SpecEditor.vue` 组件**
  - [ ] Markdown 预览组件（使用 marked.js 或 vue-markdown）
  - [ ] 生成状态展示（Loading、成功、失败）
  - [ ] 生成技术方案按钮（规格文档生成完成后可用）

- [ ] **实现 API 调用**
  - [ ] `GET /api/workflow/{id}/spec` - 获取规格文档
  - [ ] `GET /api/workflow/{id}/status` - 获取工作流状态
  - [ ] `POST /api/workflow/{id}/tech-design/generate` - 生成技术方案

- [ ] **实现轮询逻辑**
  - [ ] 生成中时每3秒轮询状态
  - [ ] 生成完成后停止轮询，显示内容
  - [ ] 生成失败时显示错误信息

**验收标准**:
- ✅ Markdown 正确渲染
- ✅ 实时显示生成状态
- ✅ 按钮状态正确（生成中禁用）

**输出文件**:
- `src/main/resources/static/components/SpecEditor.vue`

---

### ✅ P1-7: 创建前端技术方案编辑页面

**工时**: 1天
**依赖**: P1-6
**优先级**: P1
**负责模块**: 前端

**任务清单**:
- [ ] **创建 `TechDesignEditor.vue` 组件**
  - [ ] 集成 Monaco Editor（使用 vue-monaco）
  - [ ] 编辑模式切换（预览/编辑）
  - [ ] 版本历史查看（下拉选择，显示版本号和时间）
  - [ ] 保存按钮（编辑模式下）
  - [ ] 批准按钮（预览模式下，未批准时）
  - [ ] 生成任务列表按钮（批准后可用）

- [ ] **实现 API 调用**
  - [ ] `GET /api/workflow/{id}/tech-design` - 获取技术方案
  - [ ] `PUT /api/workflow/{id}/tech-design` - 更新技术方案
  - [ ] `POST /api/workflow/{id}/tech-design/approve` - 批准技术方案
  - [ ] `POST /api/workflow/{id}/tasklist/generate` - 生成任务列表

- [ ] **实现版本管理**
  - [ ] 显示当前版本号
  - [ ] 切换版本时加载对应内容
  - [ ] 保存时创建新版本

**验收标准**:
- ✅ Monaco Editor 正常工作
- ✅ 版本管理正常
- ✅ 编辑和保存流畅
- ✅ 批准后状态正确更新

**输出文件**:
- `src/main/resources/static/components/TechDesignEditor.vue`

---

### ✅ P1-8: 创建前端任务列表和代码生成页面

**工时**: 1天
**依赖**: P1-7
**优先级**: P1
**负责模块**: 前端

**任务清单**:
- [ ] **创建 `TaskListView.vue` 组件**
  - [ ] 任务列表展示（按优先级分组）
  - [ ] 任务卡片设计：
    - 任务 ID 和标题
    - 任务状态图标（待执行、进行中、已完成、失败）
    - 依赖关系展示（Tag）
    - 预计工时
  - [ ] 开始代码生成按钮

- [ ] **创建 `CodeGenerationProgress.vue` 组件**
  - [ ] 进度条（Element UI Progress）
  - [ ] 当前任务展示（高亮）
  - [ ] 完成任务列表（Collapse）
  - [ ] 生成代码预览（Modal + Monaco Editor）
  - [ ] 完成后提示（成功/部分失败）

- [ ] **实现 API 调用**
  - [ ] `GET /api/workflow/{id}/tasklist` - 获取任务列表
  - [ ] `POST /api/workflow/{id}/code-generation/start` - 开始代码生成
  - [ ] `GET /api/workflow/{id}/progress` - 获取进度（轮询）

- [ ] **实现进度轮询**
  - [ ] 代码生成中每2秒轮询进度
  - [ ] 更新任务状态和进度条
  - [ ] 完成后停止轮询

**验收标准**:
- ✅ 任务列表清晰展示
- ✅ 进度实时更新
- ✅ 代码可预览
- ✅ 依赖关系直观展示

**输出文件**:
- `src/main/resources/static/components/TaskListView.vue`
- `src/main/resources/static/components/CodeGenerationProgress.vue`

---

## 📦 P2 任务（优化项，可选）

---

### ✅ P2-1: 添加全局异常处理

**工时**: 0.5天
**依赖**: P0-14
**优先级**: P2
**负责模块**: 应用层

**任务清单**:
- [ ] **扩展 `GlobalExceptionHandler`**
  - [ ] 添加工作流异常处理：
    - `@ExceptionHandler(InvalidWorkflowTransitionException.class)`
    - `@ExceptionHandler(WorkflowNotFoundException.class)`
  - [ ] 返回统一错误格式：
    ```json
    {
      "error": "INVALID_TRANSITION",
      "message": "Cannot transition from DRAFT to COMPLETED",
      "timestamp": "2025-10-04T10:00:00"
    }
    ```

**验收标准**:
- ✅ 异常正确捕获
- ✅ 错误信息友好
- ✅ 返回格式统一

**输出文件**:
- `src/main/java/com/example/gitreview/exception/GlobalExceptionHandler.java`（扩展）

---

### ✅ P2-2: 添加日志记录

**工时**: 0.5天
**依赖**: P0-12
**优先级**: P2
**负责模块**: 应用层

**任务清单**:
- [ ] **在关键方法添加日志**
  - [ ] 工作流创建（INFO）
  - [ ] 每个阶段开始/完成（INFO）
  - [ ] Claude 调用（DEBUG：请求/响应）
  - [ ] 异常发生（ERROR：堆栈信息）

- [ ] **使用合适的日志级别**
  - [ ] INFO：正常流程节点
  - [ ] DEBUG：详细调试信息
  - [ ] ERROR：异常和错误

- [ ] **日志格式**
  - [ ] 包含工作流 ID
  - [ ] 包含操作类型
  - [ ] 包含耗时（可选）

**验收标准**:
- ✅ 日志信息完整
- ✅ 便于问题排查
- ✅ 不泄露敏感信息

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`（添加日志）

---

### ✅ P2-3: 添加前端路由和导航

**工时**: 0.5天
**依赖**: P1-8
**优先级**: P2
**负责模块**: 前端

**任务清单**:
- [ ] **在 `index.html` 添加导航菜单**
  - [ ] 工作流管理菜单项
  - [ ] 子菜单：工作流列表

- [ ] **配置路由（Vue Router）**
  - [ ] `/workflow` → WorkflowList
  - [ ] `/workflow/create` → WorkflowCreate
  - [ ] `/workflow/:id/spec` → SpecEditor
  - [ ] `/workflow/:id/design` → TechDesignEditor
  - [ ] `/workflow/:id/tasklist` → TaskListView
  - [ ] `/workflow/:id/code` → CodeGenerationProgress

**验收标准**:
- ✅ 导航菜单正常
- ✅ 路由跳转正常
- ✅ URL 参数正确传递

**输出文件**:
- `src/main/resources/static/index.html`（更新）
- `src/main/resources/static/router.js`（如有独立路由文件）

---

### ✅ P2-4: 添加进度步骤条组件

**工时**: 0.5天
**依赖**: P1-8
**优先级**: P2
**负责模块**: 前端

**任务清单**:
- [ ] **创建 `WorkflowSteps.vue` 组件**
  - [ ] 使用 Element UI Steps 组件
  - [ ] 展示5个步骤：
    1. 规格文档
    2. 技术方案
    3. 任务列表
    4. 代码生成
    5. 完成
  - [ ] 根据工作流状态高亮当前步骤
  - [ ] Props：`currentStatus` (WorkflowStatus)

- [ ] **在各页面集成此组件**
  - [ ] SpecEditor、TechDesignEditor、TaskListView、CodeGenerationProgress
  - [ ] 显示在页面顶部

**验收标准**:
- ✅ 步骤条清晰展示
- ✅ 当前步骤高亮
- ✅ 响应式布局

**输出文件**:
- `src/main/resources/static/components/WorkflowSteps.vue`

---

### ✅ P2-5: 端到端测试

**工时**: 1天
**依赖**: P1-8
**优先级**: P2
**负责模块**: 测试

**任务清单**:
- [ ] **创建 `WorkflowE2ETest` 类**
  - [ ] 使用 @SpringBootTest（webEnvironment = RANDOM_PORT）
  - [ ] Mock Claude CLI（使用 WireMock 或 Mockito）

- [ ] **测试完整流程**
  - [ ] 创建工作流
  - [ ] 生成规格文档（Mock Claude 返回）
  - [ ] 验证规格文档内容
  - [ ] 生成技术方案
  - [ ] 验证技术方案内容
  - [ ] 更新技术方案
  - [ ] 批准技术方案
  - [ ] 生成任务列表
  - [ ] 验证任务列表解析正确
  - [ ] 开始代码生成
  - [ ] 验证所有任务完成
  - [ ] 验证最终状态为 COMPLETED

- [ ] **测试异常场景**
  - [ ] Claude 调用失败
  - [ ] 状态转换错误
  - [ ] 任务依赖未满足

**验收标准**:
- ✅ 端到端测试通过
- ✅ 覆盖主要流程
- ✅ 异常场景处理正确
- ✅ 测试可重复执行

**输出文件**:
- `src/test/java/com/example/gitreview/integration/WorkflowE2ETest.java`

---

## 📈 里程碑和进度跟踪

### 里程碑定义

| 里程碑 | 目标日期 | 交付物 | 验收标准 | 状态 |
|-------|---------|-------|---------|------|
| **M1: 领域层完成** | D+3 | P0-1 ~ P0-5 | 领域模型完整，单元测试通过 | ⚪ 未开始 |
| **M2: 基础设施层完成** | D+5 | P0-6 ~ P0-8 | 存储、解析器、提示词配置完成 | ⚪ 未开始 |
| **M3: 应用层完成** | D+9 | P0-9 ~ P0-14 | 所有API可用，集成测试通过 | ⚪ 未开始 |
| **M4: 前端完成** | D+11 | P1-4 ~ P1-8 | 所有页面可用，交互流畅 | ⚪ 未开始 |
| **M5: 测试和优化** | D+14 | P1-2, P1-3, P2-5 | E2E测试通过，性能优化 | ⚪ 未开始 |

---

## 🎯 建议执行顺序

### 第一周（后端核心 - 7天）

**Day 1**:
- ✅ P0-1: 创建领域层枚举和异常（0.5天）
- ✅ P0-2: 创建领域层值对象（0.5天，开始）

**Day 2**:
- ✅ P0-2: 创建领域层值对象（完成）
- ✅ P0-3: 创建 DevelopmentWorkflow 聚合根（0.5天，开始）

**Day 3**:
- ✅ P0-3: 创建 DevelopmentWorkflow 聚合根（完成）
- ✅ P0-4: 创建 WorkflowDomainService（0.5天）
- ✅ P0-5: 创建 WorkflowRepository 接口（0.5天）

**Day 4**:
- ✅ P0-6: 实现 WorkflowStorageAdapter（1天）

**Day 5**:
- ✅ P0-7: 创建 TaskListParser（1天）

**Day 6**:
- ✅ P0-8: 创建 workflow-prompts.properties（0.5天）
- ✅ P0-9: 实现 WorkflowApplicationService 基础（0.5天，开始）

**Day 7**:
- ✅ P0-9: 实现 WorkflowApplicationService 基础（完成）
- ✅ P0-10: 实现技术方案生成逻辑（0.5天，开始）

---

### 第二周（后端完善 + 前端 - 7天）

**Day 8**:
- ✅ P0-10: 实现技术方案生成逻辑（完成）
- ✅ P0-11: 实现任务列表生成逻辑（1天）

**Day 9**:
- ✅ P0-12: 实现代码生成逻辑（1天）
- ✅ P0-13: 创建 DTO 类（0.5天）

**Day 10**:
- ✅ P0-14: 实现 WorkflowController（1天）
- ✅ P0-15: 更新配置文件（0.5天）

**Day 11**:
- ✅ P1-4: 创建前端工作流列表页面（0.5天）
- ✅ P1-5: 创建前端工作流创建页面（0.5天）
- ✅ P1-6: 创建前端规格文档页面（0.5天）

**Day 12**:
- ✅ P1-7: 创建前端技术方案编辑页面（1天）

**Day 13**:
- ✅ P1-8: 创建前端任务列表和代码生成页面（1天）

**Day 14**:
- ✅ P1-1: 扩展 CodeContextExtractor（0.5天）
- ✅ P2-1: 添加全局异常处理（0.5天）
- ✅ P2-2: 添加日志记录（0.5天）

---

### 第三周（测试和优化 - 可选）

**Day 15-16**:
- ✅ P1-2: 编写领域层单元测试（1天）
- ✅ P1-3: 编写应用层集成测试（1天）

**Day 17**:
- ✅ P2-3: 添加前端路由和导航（0.5天）
- ✅ P2-4: 添加进度步骤条组件（0.5天）

**Day 18**:
- ✅ P2-5: 端到端测试（1天）

---

## ✅ 验收标准总览

### 功能验收
- [ ] **完整流程可用**: 能够走通 PRD → spec → 技术方案 → tasklist → 代码 的全流程
- [ ] **技术方案编辑**: 支持在线编辑和版本管理
- [ ] **任务列表解析**: 准确率 >90%
- [ ] **代码生成**: 成功率 >80%
- [ ] **前端交互**: 操作流畅，进度实时更新
- [ ] **状态管理**: 状态流转正确，无非法转换
- [ ] **异常处理**: 异常场景有友好提示

### 质量验收
- [ ] **单元测试**: 领域层和应用层有单元测试覆盖
- [ ] **测试覆盖率**: >80%
- [ ] **集成测试**: 应用层集成测试通过
- [ ] **端到端测试**: E2E 测试通过
- [ ] **代码规范**: 符合 Alibaba-P3C 规范
- [ ] **DDD 设计**: 严格遵循 DDD 分层架构
- [ ] **性能**: Claude 调用超时正确处理，并发控制有效

### 文档验收
- [ ] **API 文档**: 完整的 API 接口文档（Swagger 或 Markdown）
- [ ] **技术方案文档**: 完整且准确
- [ ] **代码注释**: 所有类和方法包含 Javadoc
- [ ] **README**: 更新项目 README，包含工作流功能说明

---

## 📊 风险和依赖管理

### 关键依赖
- **Claude CLI 可用性**: 所有生成功能依赖 Claude CLI 正常工作
- **Git 仓库访问**: 代码上下文提取依赖 Git 仓库可访问
- **文件系统权限**: JSON 存储需要文件读写权限
- **前端库**: Monaco Editor、marked.js 等库需提前引入

### 风险识别
| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| Claude 调用超时 | 高 | 中 | 重试机制、超时配置、降级处理 |
| 任务列表解析失败 | 中 | 中 | 容错逻辑、格式规范、人工调整接口 |
| 代码生成质量低 | 中 | 高 | 提供预览、支持手动修改、编译验证 |
| 并发冲突 | 中 | 低 | 文件锁、并发数限制 |
| 前端兼容性 | 低 | 低 | 测试主流浏览器 |

---

## 📝 开发规范

### 命名规范
- **类名**: 大驼峰，体现业务意图（如 `DevelopmentWorkflow`）
- **方法名**: 小驼峰，动词开头（如 `startSpecGeneration()`）
- **常量**: 全大写，下划线分隔（如 `SPEC_GENERATING`）
- **包名**: 全小写，单数形式（如 `domain.workflow.model`）

### 注释规范
- **类级注释**: 必须包含 @author zhourui(V33215020) 和 @since 2025/10/04
- **方法注释**: 必须包含完整的 @param 和 @return 说明
- **业务逻辑**: 复杂逻辑添加行内注释说明意图
- **禁止**: 行尾注释、无意义注释

### 代码规范
- **方法长度**: 不超过 50 行
- **参数个数**: 不超过 5 个
- **嵌套层级**: 不超过 3 层
- **异常处理**: 明确的异常类型，避免捕获 Exception
- **日志记录**: 关键节点 INFO，调试信息 DEBUG

---

## 🚀 快速开始

### 环境准备
1. JDK 17+
2. Maven 3.x
3. Claude CLI（`claude` 命令可用）
4. Git
5. Node.js + npm（前端依赖，如需本地开发）

### 启动步骤
1. 克隆项目：`git clone ...`
2. 配置 `application.properties`
3. 创建 `data/` 目录
4. 运行：`mvn spring-boot:run`
5. 访问：`http://localhost:8080`

---

**任务清单结束**

👉 **下一步**: 开始执行 P0-1 任务 - 创建领域层枚举和异常

---

**文档版本**: v1.0
**创建日期**: 2025-10-04
**维护者**: AI 辅助开发团队
