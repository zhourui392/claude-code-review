# AI辅助开发工作流 - 任务清单

> 生成时间: 2025-10-03
> 预计完成时间: 14天
> 当前完成度: 0%

---

## 📋 任务概览

| 优先级 | 任务数 | 预计工时 | 状态 |
|-------|-------|---------|------|
| **P0 (核心)** | 15 | 7天 | ⚪ 待开始 |
| **P1 (重要)** | 8 | 4天 | ⚪ 待开始 |
| **P2 (优化)** | 5 | 3天 | ⚪ 待开始 |
| **总计** | **28** | **14天** | **0/28 完成** |

---

## 🔥 P0 任务（核心功能，必须完成）

### ✅ P0-1: 创建领域层枚举和异常

**工时**: 0.5天
**依赖**: 无
**文件**: domain/workflow/model/

**任务清单**:
- [ ] 创建 `WorkflowStatus` 枚举
  - [ ] 定义所有工作流状态（DRAFT, SPEC_GENERATING, SPEC_GENERATED等）
  - [ ] 添加状态描述方法 `getDescription()`
- [ ] 创建 `TaskStatus` 枚举
  - [ ] 定义任务状态（PENDING, IN_PROGRESS, COMPLETED等）
- [ ] 创建工作流异常类
  - [ ] `InvalidWorkflowTransitionException`
  - [ ] `WorkflowNotFoundException`

**验收标准**:
- ✅ 枚举定义完整
- ✅ 包含Javadoc注释
- ✅ 异常类继承自合适的基类

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/model/WorkflowStatus.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/TaskStatus.java`
- `src/main/java/com/example/gitreview/domain/workflow/exception/InvalidWorkflowTransitionException.java`
- `src/main/java/com/example/gitreview/domain/workflow/exception/WorkflowNotFoundException.java`

---

### ✅ P0-2: 创建领域层值对象

**工时**: 1天
**依赖**: P0-1
**文件**: domain/workflow/model/valueobject/

**任务清单**:
- [ ] 创建 `Specification` 值对象
  - [ ] 字段：prdContent, documentPaths, generatedContent, generatedAt
  - [ ] 验证方法：validateContent()
- [ ] 创建 `TechnicalDesign` 值对象
  - [ ] 字段：content, version, approved, createdAt, approvedAt
  - [ ] 业务方法：createNewVersion(), approve()
- [ ] 创建 `Task` 值对象
  - [ ] 字段：id, title, description, status, dependencies, targetFile, generatedCode, completedAt
  - [ ] 业务方法：complete(), fail(), isExecutable()
- [ ] 创建 `TaskList` 值对象
  - [ ] 字段：content, tasks, generatedAt
  - [ ] 业务方法：getExecutableTasks(), getProgress()

**验收标准**:
- ✅ 值对象不可变（final字段）
- ✅ 包含必要的业务方法
- ✅ 实现 equals() 和 hashCode()

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/Specification.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/TechnicalDesign.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/Task.java`
- `src/main/java/com/example/gitreview/domain/workflow/model/valueobject/TaskList.java`

---

### ✅ P0-3: 创建 DevelopmentWorkflow 聚合根

**工时**: 1.5天
**依赖**: P0-2
**文件**: domain/workflow/model/aggregate/

**任务清单**:
- [ ] 创建 `DevelopmentWorkflow` 类
  - [ ] 基本字段：id, name, repositoryId, status, createdAt, updatedAt, createdBy
  - [ ] 关联对象：specification, technicalDesign, taskList, codeGenerationTasks
  - [ ] 进度字段：progress, currentStage
- [ ] 实现业务方法
  - [ ] `startSpecGeneration()`: 开始规格文档生成
  - [ ] `completeSpecGeneration(Specification spec)`: 完成规格文档
  - [ ] `startTechDesign()`: 开始技术方案生成
  - [ ] `completeTechDesign(TechnicalDesign design)`: 完成技术方案
  - [ ] `updateTechDesign(String content)`: 更新技术方案
  - [ ] `approveTechDesign()`: 批准技术方案
  - [ ] `startTaskListGeneration()`: 开始任务列表生成
  - [ ] `completeTaskListGeneration(TaskList tasks)`: 完成任务列表
  - [ ] `startCodeGeneration()`: 开始代码生成
  - [ ] `completeTask(String taskId, String code)`: 完成单个任务
  - [ ] `markAsFailed(String reason)`: 标记失败
  - [ ] `cancel(String reason)`: 取消工作流
  - [ ] `updateProgress(int progress)`: 更新进度
- [ ] 实现状态转换验证
  - [ ] 在每个状态转换方法中验证当前状态是否允许转换

**验收标准**:
- ✅ 所有业务方法包含状态验证
- ✅ 抛出合适的异常
- ✅ 更新 updateTime

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/model/aggregate/DevelopmentWorkflow.java`

---

### ✅ P0-4: 创建 WorkflowDomainService

**工时**: 1天
**依赖**: P0-3
**文件**: domain/workflow/service/

**任务清单**:
- [ ] 创建 `WorkflowDomainService` 类
  - [ ] `isValidTransition(WorkflowStatus from, WorkflowStatus to)`: 验证状态转换
  - [ ] `validateSpecification(Specification spec)`: 验证规格文档
  - [ ] `validateTechnicalDesign(TechnicalDesign design)`: 验证技术方案
  - [ ] `calculateProgress(DevelopmentWorkflow workflow)`: 计算总体进度
- [ ] 实现状态转换规则
  - [ ] 定义状态转换矩阵
  - [ ] 验证转换合法性

**验收标准**:
- ✅ 状态转换规则完整
- ✅ 验证逻辑正确

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/service/WorkflowDomainService.java`

---

### ✅ P0-5: 创建 WorkflowRepository 接口

**工时**: 0.5天
**依赖**: P0-3
**文件**: domain/workflow/repository/

**任务清单**:
- [ ] 创建 `WorkflowRepository` 接口
  - [ ] `save(DevelopmentWorkflow workflow)`: 保存工作流
  - [ ] `findById(Long id)`: 根据ID查找
  - [ ] `findAll()`: 查找所有
  - [ ] `findByRepositoryId(Long repositoryId)`: 根据仓库ID查找
  - [ ] `delete(Long id)`: 删除工作流

**验收标准**:
- ✅ 接口定义清晰
- ✅ 返回类型使用Optional

**输出文件**:
- `src/main/java/com/example/gitreview/domain/workflow/repository/WorkflowRepository.java`

---

### ✅ P0-6: 实现 WorkflowStorageAdapter

**工时**: 1天
**依赖**: P0-5
**文件**: infrastructure/storage/adapter/

**任务清单**:
- [ ] 创建 `WorkflowStorageAdapter` 类
  - [ ] 实现 `WorkflowRepository` 接口
  - [ ] 使用 Jackson 进行 JSON 序列化/反序列化
  - [ ] 实现文件读写逻辑
- [ ] 实现 ID 生成策略
  - [ ] 自动生成递增ID
- [ ] 实现并发控制
  - [ ] 使用文件锁或synchronized

**验收标准**:
- ✅ JSON格式正确
- ✅ 支持并发读写
- ✅ 错误处理完善

**输出文件**:
- `src/main/java/com/example/gitreview/infrastructure/storage/adapter/WorkflowStorageAdapter.java`

---

### ✅ P0-7: 创建 TaskListParser

**工时**: 1天
**依赖**: P0-2
**文件**: infrastructure/parser/

**任务清单**:
- [ ] 创建 `TaskListParser` 类
  - [ ] `parse(String markdownContent)`: 解析tasklist.md
  - [ ] 提取任务ID（正则：P\d+-\d+）
  - [ ] 提取任务标题（### 标题）
  - [ ] 提取任务清单（- [ ] ...）
  - [ ] 提取依赖（**依赖**: ...）
  - [ ] 提取目标文件（**文件**: ...）
  - [ ] 提取预计工时（**工时**: ...）
- [ ] 实现容错逻辑
  - [ ] 处理格式不标准的情况
  - [ ] 记录解析警告

**验收标准**:
- ✅ 解析准确率 >90%
- ✅ 处理异常情况不崩溃

**输出文件**:
- `src/main/java/com/example/gitreview/infrastructure/parser/TaskListParser.java`

---

### ✅ P0-8: 创建 workflow-prompts.properties

**工时**: 0.5天
**依赖**: 无
**文件**: resources/

**任务清单**:
- [ ] 创建配置文件 `workflow-prompts.properties`
  - [ ] `workflow.prompt.spec`: 规格文档生成提示词
  - [ ] `workflow.prompt.tech-design`: 技术方案生成提示词
  - [ ] `workflow.prompt.tasklist`: 任务列表生成提示词
  - [ ] `workflow.prompt.code`: 代码生成提示词
- [ ] 优化Prompt模板
  - [ ] 包含Few-shot示例
  - [ ] 明确输出格式要求
  - [ ] 包含占位符（{prd}, {spec}, {techDesign}等）

**验收标准**:
- ✅ Prompt模板完整
- ✅ 格式清晰易懂

**输出文件**:
- `src/main/resources/workflow-prompts.properties`

---

### ✅ P0-9: 实现 WorkflowApplicationService（基础）

**工时**: 1.5天
**依赖**: P0-6, P0-7, P0-8
**文件**: application/workflow/

**任务清单**:
- [ ] 创建 `WorkflowApplicationService` 类
  - [ ] 注入依赖：WorkflowRepository, WorkflowDomainService, ClaudeQueryPort, GitOperationPort
- [ ] 实现基础方法
  - [ ] `createWorkflow(CreateWorkflowRequest request)`: 创建工作流
  - [ ] `getWorkflowStatus(Long workflowId)`: 获取状态
  - [ ] `getProgress(Long workflowId)`: 获取进度
  - [ ] `cancelWorkflow(Long workflowId, String reason)`: 取消工作流
- [ ] 实现规格文档生成（异步）
  - [ ] `generateSpecification(Long workflowId, SpecGenerationRequest request)`
  - [ ] 读取PRD和文档内容
  - [ ] 调用Claude生成spec.md
  - [ ] 保存结果
- [ ] 实现查询方法
  - [ ] `getSpecification(Long workflowId)`: 获取规格文档

**验收标准**:
- ✅ 异步方法正确标注 @Async
- ✅ 异常处理完善
- ✅ 状态更新正确

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`

---

### ✅ P0-10: 实现 WorkflowApplicationService（技术方案）

**工时**: 1天
**依赖**: P0-9
**文件**: application/workflow/

**任务清单**:
- [ ] 实现技术方案生成（异步）
  - [ ] `generateTechnicalDesign(Long workflowId)`
  - [ ] 读取spec.md内容
  - [ ] 获取代码仓库结构（调用GitOperationPort）
  - [ ] 调用Claude生成技术方案
  - [ ] 保存TechnicalDesign（版本+1）
- [ ] 实现技术方案更新
  - [ ] `updateTechnicalDesign(Long workflowId, String content)`
  - [ ] 创建新版本
  - [ ] 保存工作流
- [ ] 实现技术方案批准
  - [ ] `approveTechnicalDesign(Long workflowId)`
  - [ ] 更新approved标志
  - [ ] 更新状态为TECH_DESIGN_APPROVED
- [ ] 实现查询方法
  - [ ] `getTechnicalDesign(Long workflowId)`: 获取技术方案

**验收标准**:
- ✅ 版本管理正确
- ✅ 状态流转正确

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`

---

### ✅ P0-11: 实现 WorkflowApplicationService（任务列表）

**工时**: 1天
**依赖**: P0-10
**文件**: application/workflow/

**任务清单**:
- [ ] 实现任务列表生成（异步）
  - [ ] `generateTaskList(Long workflowId)`
  - [ ] 读取技术方案内容
  - [ ] 调用Claude生成tasklist.md
  - [ ] 使用TaskListParser解析
  - [ ] 保存TaskList
- [ ] 实现查询方法
  - [ ] `getTaskList(Long workflowId)`: 获取任务列表

**验收标准**:
- ✅ 任务列表解析成功
- ✅ 任务依赖关系正确

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`

---

### ✅ P0-12: 实现 WorkflowApplicationService（代码生成）

**工时**: 1.5天
**依赖**: P0-11
**文件**: application/workflow/

**任务清单**:
- [ ] 实现代码生成（异步）
  - [ ] `startCodeGeneration(Long workflowId)`
  - [ ] 获取可执行任务列表（检查依赖）
  - [ ] 遍历任务
  - [ ] 对每个任务：
    - [ ] 提取代码上下文（使用CodeContextExtractor）
    - [ ] 调用Claude生成代码
    - [ ] 保存生成的代码
    - [ ] 更新任务状态为COMPLETED
    - [ ] 更新工作流进度
  - [ ] 所有任务完成后更新状态为COMPLETED
- [ ] 实现进度更新逻辑
  - [ ] 实时更新progress字段

**验收标准**:
- ✅ 尊重任务依赖关系
- ✅ 进度实时更新
- ✅ 错误不影响其他任务

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`

---

### ✅ P0-13: 创建DTO类

**工时**: 0.5天
**依赖**: P0-9
**文件**: application/workflow/dto/

**任务清单**:
- [ ] 创建请求DTO
  - [ ] `CreateWorkflowRequest`
  - [ ] `SpecGenerationRequest`
- [ ] 创建响应DTO
  - [ ] `SpecificationDTO`
  - [ ] `TechnicalDesignDTO`
  - [ ] `TaskListDTO`
  - [ ] `TaskDTO`
  - [ ] `WorkflowProgressDTO`
  - [ ] `WorkflowStatusDTO`
- [ ] 添加DTO验证注解
  - [ ] @NotNull, @NotEmpty等

**验收标准**:
- ✅ DTO定义完整
- ✅ 包含必要的验证注解

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/dto/*.java`

---

### ✅ P0-14: 实现 WorkflowController

**工时**: 1天
**依赖**: P0-13
**文件**: application/workflow/api/

**任务清单**:
- [ ] 创建 `WorkflowController` 类
  - [ ] 注入 `WorkflowApplicationService`
- [ ] 实现所有API接口
  - [ ] `POST /api/workflow`: 创建工作流
  - [ ] `POST /api/workflow/{id}/spec/generate`: 生成规格文档
  - [ ] `GET /api/workflow/{id}/spec`: 获取规格文档
  - [ ] `POST /api/workflow/{id}/tech-design/generate`: 生成技术方案
  - [ ] `GET /api/workflow/{id}/tech-design`: 获取技术方案
  - [ ] `PUT /api/workflow/{id}/tech-design`: 更新技术方案
  - [ ] `POST /api/workflow/{id}/tech-design/approve`: 批准技术方案
  - [ ] `POST /api/workflow/{id}/tasklist/generate`: 生成任务列表
  - [ ] `GET /api/workflow/{id}/tasklist`: 获取任务列表
  - [ ] `POST /api/workflow/{id}/code-generation/start`: 开始代码生成
  - [ ] `GET /api/workflow/{id}/progress`: 获取进度
  - [ ] `GET /api/workflow/{id}/status`: 获取状态
  - [ ] `POST /api/workflow/{id}/cancel`: 取消工作流
  - [ ] `GET /api/workflow`: 获取所有工作流
- [ ] 添加异常处理
  - [ ] @ExceptionHandler

**验收标准**:
- ✅ 所有接口可正常调用
- ✅ 返回格式符合规范
- ✅ 异常处理完善

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/api/WorkflowController.java`

---

### ✅ P0-15: 配置文件更新

**工时**: 0.5天
**依赖**: P0-8
**文件**: resources/

**任务清单**:
- [ ] 更新 `application.properties`
  - [ ] 添加工作流配置
    - [ ] `workflow.storage.file=data/workflows.json`
    - [ ] `workflow.claude.timeout=180000`
    - [ ] `workflow.max.concurrent.workflows=5`
    - [ ] `workflow.code.generation.timeout=60000`
  - [ ] 添加Prompt配置
    - [ ] `workflow.prompts.file=workflow-prompts.properties`
- [ ] 更新 `AsyncConfig`
  - [ ] 添加 `workflowExecutor` Bean
  - [ ] 配置线程池参数

**验收标准**:
- ✅ 配置项完整
- ✅ 线程池配置合理

**输出文件**:
- `src/main/resources/application.properties`
- `src/main/java/com/example/gitreview/config/AsyncConfig.java`

---

## ⭐ P1 任务（重要功能，建议完成）

### ✅ P1-1: 扩展 CodeContextExtractor

**工时**: 0.5天
**依赖**: P0-12
**文件**: infrastructure/context/

**任务清单**:
- [ ] 在 `CodeContextExtractor` 添加方法
  - [ ] `extractRepositoryStructure(String repoPath)`: 提取仓库目录结构
  - [ ] 遍历项目目录
  - [ ] 提取主要包和类列表
  - [ ] 返回树形结构文本

**验收标准**:
- ✅ 能够提取完整的仓库结构
- ✅ 输出格式清晰易读

**输出文件**:
- `src/main/java/com/example/gitreview/infrastructure/context/CodeContextExtractor.java`

---

### ✅ P1-2: 编写领域层单元测试

**工时**: 1天
**依赖**: P0-4
**文件**: test/domain/workflow/

**任务清单**:
- [ ] 创建 `DevelopmentWorkflowTest.java`
  - [ ] 测试状态转换方法
  - [ ] 测试业务规则验证
  - [ ] 测试异常抛出
- [ ] 创建 `WorkflowDomainServiceTest.java`
  - [ ] 测试状态转换验证
  - [ ] 测试进度计算
- [ ] 创建值对象测试
  - [ ] `SpecificationTest.java`
  - [ ] `TechnicalDesignTest.java`
  - [ ] `TaskTest.java`
  - [ ] `TaskListTest.java`

**验收标准**:
- ✅ 测试覆盖率 >80%
- ✅ 所有测试通过

**输出文件**:
- `src/test/java/com/example/gitreview/domain/workflow/model/aggregate/DevelopmentWorkflowTest.java`
- `src/test/java/com/example/gitreview/domain/workflow/service/WorkflowDomainServiceTest.java`
- `src/test/java/com/example/gitreview/domain/workflow/model/valueobject/*.java`

---

### ✅ P1-3: 编写应用层集成测试

**工时**: 1天
**依赖**: P0-14
**文件**: test/application/workflow/

**任务清单**:
- [ ] 创建 `WorkflowApplicationServiceTest.java`
  - [ ] Mock所有外部依赖
  - [ ] 测试创建工作流
  - [ ] 测试规格文档生成流程
  - [ ] 测试技术方案生成流程
  - [ ] 测试任务列表生成流程
  - [ ] 测试代码生成流程
- [ ] 创建 `WorkflowControllerTest.java`
  - [ ] 使用MockMvc测试所有API
  - [ ] 测试异常处理

**验收标准**:
- ✅ 测试覆盖主要流程
- ✅ 所有测试通过

**输出文件**:
- `src/test/java/com/example/gitreview/application/workflow/WorkflowApplicationServiceTest.java`
- `src/test/java/com/example/gitreview/application/workflow/api/WorkflowControllerTest.java`

---

### ✅ P1-4: 创建前端工作流列表页面

**工时**: 0.5天
**依赖**: P0-14
**文件**: resources/static/

**任务清单**:
- [ ] 创建 `WorkflowList.vue`
  - [ ] 使用Element UI Table展示工作流列表
  - [ ] 列：名称、仓库、状态、进度、创建时间、操作
  - [ ] 状态筛选器
  - [ ] 创建工作流按钮
  - [ ] 操作按钮：查看详情、取消、删除
- [ ] 实现API调用
  - [ ] GET /api/workflow

**验收标准**:
- ✅ 列表正确展示
- ✅ 状态筛选正常

**输出文件**:
- `src/main/resources/static/components/WorkflowList.vue`

---

### ✅ P1-5: 创建前端工作流创建页面

**工时**: 0.5天
**依赖**: P1-4
**文件**: resources/static/

**任务清单**:
- [ ] 创建 `WorkflowCreate.vue`
  - [ ] 表单：工作流名称、选择仓库
  - [ ] PRD内容输入框（Textarea）
  - [ ] 文档空间文件选择器（Multi-select）
  - [ ] 提交按钮
- [ ] 实现表单验证
  - [ ] 必填字段验证
- [ ] 实现API调用
  - [ ] POST /api/workflow
  - [ ] POST /api/workflow/{id}/spec/generate

**验收标准**:
- ✅ 表单验证完整
- ✅ 提交成功后跳转

**输出文件**:
- `src/main/resources/static/components/WorkflowCreate.vue`

---

### ✅ P1-6: 创建前端规格文档页面

**工时**: 0.5天
**依赖**: P1-5
**文件**: resources/static/

**任务清单**:
- [ ] 创建 `SpecEditor.vue`
  - [ ] Markdown预览组件（使用marked.js）
  - [ ] 生成状态展示
  - [ ] 生成技术方案按钮
- [ ] 实现API调用
  - [ ] GET /api/workflow/{id}/spec
  - [ ] POST /api/workflow/{id}/tech-design/generate
- [ ] 实现轮询逻辑
  - [ ] 生成中时轮询状态

**验收标准**:
- ✅ Markdown正确渲染
- ✅ 实时显示生成状态

**输出文件**:
- `src/main/resources/static/components/SpecEditor.vue`

---

### ✅ P1-7: 创建前端技术方案编辑页面

**工时**: 1天
**依赖**: P1-6
**文件**: resources/static/

**任务清单**:
- [ ] 创建 `TechDesignEditor.vue`
  - [ ] 集成Monaco Editor
  - [ ] 编辑模式切换（预览/编辑）
  - [ ] 版本历史查看（下拉选择）
  - [ ] 保存按钮
  - [ ] 批准按钮
- [ ] 实现API调用
  - [ ] GET /api/workflow/{id}/tech-design
  - [ ] PUT /api/workflow/{id}/tech-design
  - [ ] POST /api/workflow/{id}/tech-design/approve

**验收标准**:
- ✅ Monaco Editor正常工作
- ✅ 版本管理正常
- ✅ 编辑和保存流畅

**输出文件**:
- `src/main/resources/static/components/TechDesignEditor.vue`

---

### ✅ P1-8: 创建前端任务列表和代码生成页面

**工时**: 1天
**依赖**: P1-7
**文件**: resources/static/

**任务清单**:
- [ ] 创建 `TaskListView.vue`
  - [ ] 任务列表展示（按优先级分组）
  - [ ] 任务状态图标
  - [ ] 依赖关系展示
  - [ ] 开始代码生成按钮
- [ ] 创建 `CodeGenerationProgress.vue`
  - [ ] 进度条（Element UI Progress）
  - [ ] 当前任务展示
  - [ ] 完成任务列表
  - [ ] 生成代码预览（Modal）
- [ ] 实现API调用
  - [ ] GET /api/workflow/{id}/tasklist
  - [ ] POST /api/workflow/{id}/code-generation/start
  - [ ] GET /api/workflow/{id}/progress（轮询）

**验收标准**:
- ✅ 任务列表清晰展示
- ✅ 进度实时更新
- ✅ 代码可预览

**输出文件**:
- `src/main/resources/static/components/TaskListView.vue`
- `src/main/resources/static/components/CodeGenerationProgress.vue`

---

## 📦 P2 任务（优化项，可选）

### ✅ P2-1: 添加全局异常处理

**工时**: 0.5天
**依赖**: P0-14
**文件**: exception/

**任务清单**:
- [ ] 在 `GlobalExceptionHandler` 添加工作流异常处理
  - [ ] `InvalidWorkflowTransitionException`
  - [ ] `WorkflowNotFoundException`
  - [ ] 返回统一错误格式

**验收标准**:
- ✅ 异常正确捕获
- ✅ 错误信息友好

**输出文件**:
- `src/main/java/com/example/gitreview/exception/GlobalExceptionHandler.java`

---

### ✅ P2-2: 添加日志记录

**工时**: 0.5天
**依赖**: P0-12
**文件**: application/workflow/

**任务清单**:
- [ ] 在关键方法添加日志
  - [ ] 工作流创建
  - [ ] 每个阶段开始/完成
  - [ ] Claude调用（请求/响应）
  - [ ] 异常发生
- [ ] 使用合适的日志级别
  - [ ] INFO：正常流程
  - [ ] DEBUG：详细信息
  - [ ] ERROR：异常

**验收标准**:
- ✅ 日志信息完整
- ✅ 便于问题排查

**输出文件**:
- `src/main/java/com/example/gitreview/application/workflow/WorkflowApplicationService.java`

---

### ✅ P2-3: 添加前端路由和导航

**工时**: 0.5天
**依赖**: P1-8
**文件**: resources/static/

**任务清单**:
- [ ] 在 `index.html` 添加工作流导航菜单
  - [ ] 工作流列表入口
- [ ] 配置路由
  - [ ] `/workflow` → WorkflowList
  - [ ] `/workflow/create` → WorkflowCreate
  - [ ] `/workflow/:id/spec` → SpecEditor
  - [ ] `/workflow/:id/design` → TechDesignEditor
  - [ ] `/workflow/:id/tasklist` → TaskListView
  - [ ] `/workflow/:id/code` → CodeGenerationProgress

**验收标准**:
- ✅ 导航菜单正常
- ✅ 路由跳转正常

**输出文件**:
- `src/main/resources/static/index.html`

---

### ✅ P2-4: 添加进度步骤条组件

**工时**: 0.5天
**依赖**: P1-8
**文件**: resources/static/

**任务清单**:
- [ ] 创建 `WorkflowSteps.vue` 组件
  - [ ] 使用Element UI Steps
  - [ ] 展示5个步骤：规格文档、技术方案、任务列表、代码生成、完成
  - [ ] 根据工作流状态高亮当前步骤
- [ ] 在各页面集成此组件

**验收标准**:
- ✅ 步骤条清晰展示
- ✅ 当前步骤高亮

**输出文件**:
- `src/main/resources/static/components/WorkflowSteps.vue`

---

### ✅ P2-5: 端到端测试

**工时**: 1天
**依赖**: P1-8
**文件**: test/integration/

**任务清单**:
- [ ] 创建 `WorkflowE2ETest.java`
  - [ ] 使用 @SpringBootTest
  - [ ] Mock Claude CLI（使用TestContainers或Mock）
  - [ ] 测试完整流程：
    - [ ] 创建工作流
    - [ ] 生成规格文档
    - [ ] 生成技术方案
    - [ ] 更新技术方案
    - [ ] 批准技术方案
    - [ ] 生成任务列表
    - [ ] 开始代码生成
    - [ ] 验证最终状态

**验收标准**:
- ✅ 端到端测试通过
- ✅ 覆盖主要流程

**输出文件**:
- `src/test/java/com/example/gitreview/integration/WorkflowE2ETest.java`

---

## 📈 进度跟踪

### 里程碑

| 里程碑 | 日期 | 交付物 | 状态 |
|-------|------|-------|------|
| **M1: 领域层完成** | D+3 | P0-1 ~ P0-5 | ⚪ 未开始 |
| **M2: 基础设施层完成** | D+5 | P0-6 ~ P0-8 | ⚪ 未开始 |
| **M3: 应用层完成** | D+9 | P0-9 ~ P0-14 | ⚪ 未开始 |
| **M4: 前端完成** | D+11 | P1-4 ~ P1-8 | ⚪ 未开始 |
| **M5: 测试和优化** | D+14 | P1-2, P1-3, P2-5 | ⚪ 未开始 |

---

## 🎯 建议执行顺序

### 第一周（7天）- 后端核心
1. **Day 1-3**: P0-1 ~ P0-5（领域层）
2. **Day 4-5**: P0-6 ~ P0-8（基础设施层）
3. **Day 6-7**: P0-9 ~ P0-11（应用层前半）

### 第二周（7天）- 后端完善 + 前端
4. **Day 8-9**: P0-12 ~ P0-15（应用层后半 + 配置）
5. **Day 10-11**: P1-4 ~ P1-8（前端全部）
6. **Day 12-13**: P1-2, P1-3（测试）
7. **Day 14**: P2-1 ~ P2-5（优化和E2E测试）

---

## ✅ 验收标准总览

### 功能验收
- [ ] 能够完整走通 PRD → spec → 技术方案 → tasklist → 代码 的全流程
- [ ] 技术方案支持在线编辑和版本管理
- [ ] 任务列表解析准确率 >90%
- [ ] 代码生成成功率 >80%
- [ ] 前端操作流畅，进度实时更新

### 质量验收
- [ ] 所有核心功能有单元测试覆盖
- [ ] 测试覆盖率 >80%
- [ ] 端到端测试通过
- [ ] 代码符合Alibaba-P3C规范

### 文档验收
- [ ] API文档完整
- [ ] 技术方案文档完整
- [ ] 代码注释清晰

---

**任务清单结束**

👉 **下一步**: 开始执行 P0-1 任务
