# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

基于 Spring Boot 3.2 + Claude CLI 的自动化代码审查与测试生成服务。采用 DDD 六边形架构，提供代码审查和单元测试自动生成两大核心功能。

## 构建和运行命令

### 构建
```bash
mvn clean package                    # 编译和打包
mvn clean package -DskipTests        # 跳过测试打包
mvn compile                          # 仅编译
```

### 运行
```bash
mvn spring-boot:run                                  # 开发环境运行
java -jar target/git-review-service-1.0.0.jar       # 生产环境运行
```

### 测试
```bash
mvn test                                # 运行所有测试
mvn test -Dtest=ClassName               # 运行特定测试类
mvn test -Dtest=ClassName#methodName    # 运行特定测试方法
```

## 架构设计

### 技术栈
- Spring Boot 3.2，Java 17
- JGit 6.7（Git 操作）
- JavaParser 3.25（代码解析与上下文提取）
- Claude CLI（代码审查与测试生成）
- JSON 文件存储（Repository/CodeReview/TestSuite）
- Vue.js 2.6 + Element UI（前端界面）

### DDD 六边形架构分层

**核心设计理念：业务逻辑归领域层，技术细节归基础设施层，应用层仅做编排协调。**

#### 1. Domain Layer (领域层)
领域对象封装核心业务规则和状态转换逻辑：

- **聚合根 (Aggregates)**：
  - `CodeReview`：代码审查聚合，管理审查会话、策略、状态流转
  - `Repository`：Git 仓库聚合，封装凭据验证、URL 规则
  - `TestSuite`：测试套件聚合，管理测试用例生成与验证

- **领域服务 (Domain Services)**：
  - `CodeReviewDomainService`：审查策略执行、结果验证
  - `RepositoryDomainService`：仓库访问验证、分支校验
  - `TestGenerationDomainService`：测试生成规则、质量门禁

- **值对象 (Value Objects)**：
  - `CodeDiff`、`ReviewResult`、`ReviewStrategy`
  - `GitUrl`、`Credential`、`Branch`
  - `JavaClass`、`TestTemplate`、`TestMethod`

#### 2. Application Layer (应用层)
应用服务编排领域对象与基础设施端口，不包含业务规则：

- **应用服务**：
  - `CodeReviewApplicationService`：协调审查流程（仓库获取 → Diff 生成 → Claude 调用 → 结果存储）
  - `GitRepositoryApplicationService`：仓库管理流程编排
  - `TestGenerationApplicationService`：测试生成流程编排（代码解析 → 上下文提取 → Claude 生成 → 编译验证）

- **API Controllers**：
  - `ReviewController`：代码审查接口（支持快速/标准/深度审查模式）
  - `GitRepositoryController`、`GitOperationController`：仓库管理
  - `TestGenerationController`：测试生成接口

- **DTO/Assembler**：DTO 转换与领域对象组装

#### 3. Infrastructure Layer (基础设施层)
实现技术细节和外部依赖适配：

- **端口 (Ports)**：
  - `ClaudeQueryPort`：Claude CLI 调用抽象接口
  - `GitOperationPort`：Git 操作抽象接口
  - `StoragePort`：存储抽象接口

- **适配器 (Adapters)**：
  - `ClaudeCliAdapter`：Claude CLI 进程调用实现（Windows 兼容，支持管道/文件输入）
  - `JGitRepositoryAdapter`：JGit 实现（clone/diff/分支操作，优先 ls-remote）
  - `GitRepositoryStorageAdapter`、`CodeReviewStorageAdapter`、`TestSuiteStorageAdapter`：JSON 文件存储实现

- **核心基础设施服务**：
  - `CodeContextExtractor`：使用 JavaParser 提取类/方法/依赖上下文
  - `JavaParserService`：Java 代码 AST 解析
  - `ReviewResultParser`：解析 Claude 审查结果（JSON 格式）
  - `TempWorkspaceManager`：临时工作空间管理（Git clone 与代码生成）
  - `WorkspaceCleanupService`、`WorkspaceSecurityService`：工作空间清理与安全

## 核心功能与流程

### 1. 代码审查 (Code Review)
**流程**：仓库配置 → 分支选择 → Git Diff 生成 → 上下文提取 → Claude 审查 → 结果解析与存储

**审查模式**（配置于 `review-prompts.properties`）：
- `quick`：快速审查（仅关键 Bug、安全、性能）
- `standard`：标准审查（平衡的审查，含代码质量）
- `deep`：深度审查（结合上下文，JSON 格式输出，含修复建议与预估时间）
- `security`：安全审查（专注注入/认证/加密）
- `architecture`：架构审查（分层/耦合/可扩展性）
- `performance`：性能审查（算法/数据库/并发）

**API**：
- `POST /api/review/{repositoryId}/claude?targetBranch=xxx&baseBranch=master&mode=deep`

### 2. 测试生成 (Test Generation)
**流程**：代码解析 → 上下文提取（依赖/方法签名） → Claude 生成测试 → 编译验证 → 质量门禁检查

**核心组件**：
- `CodeContextExtractor`：提取类上下文（字段、方法、依赖）
- `JavaParserService`：AST 解析，获取方法签名与调用关系
- `TestGenerationDomainService`：应用质量门禁（覆盖率、复杂度）
- `TestSuite` 聚合根：管理测试用例状态（PENDING/GENERATING/COMPLETED/FAILED）

**配置**：
```properties
test.generation.max-class-size=50000        # 超大类跳过
test.generation.target-coverage=80          # 目标覆盖率
quality.gates.min-coverage=70               # 最低覆盖率门禁
quality.gates.max-complexity=10             # 最大圈复杂度
test.validation.compile-timeout=30000       # 编译超时
```

**API**：
- `POST /api/test-generation/generate` - 生成测试
- `GET /api/test-generation/{suiteId}` - 查询测试套件状态

## 关键配置

### application.properties 核心配置
```properties
# 服务端口
server.port=8080

# 数据存储（JSON 文件）
json.storage.repository.file=data/repositories.json
json.storage.codereview.file=data/code-reviews.json
json.storage.testsuite.file=data/test-suites.json

# Claude CLI
claude.command=claude
claude.cli.timeout=120000                   # Claude CLI 超时（代码审查）
claude.agent.timeout=60000                  # Claude Agent 超时（测试生成）
claude.agent.max-retries=3                  # 重试次数

# Git 操作
git.temp.dir=C:\\tmp\\git-review            # 临时工作空间（Windows 路径）

# 代码审查
review.context.enabled=true                 # 启用上下文提取
review.context.maxLines=2000                # 上下文最大行数

# 测试生成
test.generation.timeout=300000              # 测试生成总超时（5分钟）
test.generation.max.concurrent.tasks=10     # 最大并发任务数
test.validation.enabled=true                # 启用编译验证

# 质量门禁
quality.gates.min-coverage=70
quality.gates.compilation-required=true

# 日志
logging.level.com.example.gitreview.claude=DEBUG
```

### Claude CLI 集成关键点
- **Windows 兼容**：`ClaudeCliAdapter` 自动处理 `.cmd` 后缀，支持 PowerShell 回退
- **管道模式**：将 Git Diff 通过标准输入传递给 Claude（避免命令行长度限制）
- **超时控制**：代码审查 120s，测试生成 60s（可配置）
- **临时文件管理**：使用 `TempWorkspaceManager` 创建隔离工作空间，结束后自动清理

## 开发工作流程

### 添加新功能（遵循 DDD 模式）
1. **领域层**：创建/修改聚合根、值对象、领域服务（封装业务规则）
2. **应用层**：创建应用服务编排流程，添加 Controller 接口，定义 DTO
3. **基础设施层**：实现端口适配器（Git/Claude/Storage）
4. **测试**：领域对象单元测试（纯 Java，无 Spring）+ 应用服务协作测试（Mock 端口）

### 扩展 Claude 审查提示词
1. 编辑 `src/main/resources/review-prompts.properties`
2. 添加新模式，如 `review.prompt.custom=...`
3. 在 `ReviewController` 添加对应的请求参数处理
4. 提示词设计建议：简洁、聚焦、明确输出格式（推荐 JSON）

### 扩展 Git 操作
- 修改 `JGitRepositoryAdapter`（实现 `GitOperationPort`）
- 新增方法时，先在 Port 接口定义抽象，再在 Adapter 实现
- 注意临时目录清理（使用 `WorkspaceCleanupService`）

## API 接口

### 代码审查 API
- `GET /api/repositories` - 获取所有仓库
- `POST /api/repositories` - 创建仓库
- `PUT /api/repositories/{id}` - 更新仓库
- `DELETE /api/repositories/{id}` - 删除仓库
- `GET /api/repositories/{id}/remote-branches` - 获取远程分支
- `POST /api/review/{repositoryId}/claude?targetBranch=xxx&baseBranch=master&mode=deep` - 执行审查

### 测试生成 API
- `POST /api/test-generation/generate` - 生成测试（请求体：repositoryId, branch, classPath）
- `GET /api/test-generation/{suiteId}` - 查询测试套件
- `GET /api/test-generation/{suiteId}/status` - 查询生成状态

### 前端访问
- 静态资源：http://localhost:8080/ （`src/main/resources/static/index.html`）
- 使用 Vue.js 2 + Element UI + Axios

## 环境要求
- JDK 17+
- Maven 3.x
- Claude CLI（命令 `claude` 可用）
- Windows 11（已适配）或 macOS/Linux
- 临时目录写权限（`C:\tmp\git-review` 或 `/tmp/git-review`）
