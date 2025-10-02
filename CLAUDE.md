# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于 Spring Boot + Vue.js(CDN) + Claude Code CLI 的自动化 Git 代码审查服务。提供 RESTful API 与 Web 界面，支持仓库管理、远程分支获取、分支对比（Git diff），并通过 Claude CLI 进行智能审查（支持管道与文件输入）。

## 构建和运行命令

### 构建项目
```bash
# 编译和打包
mvn clean package

# 编译但跳过测试
mvn clean package -Dmaven.test.skip=true

# 仅编译
mvn compile
```

### 运行应用
```bash
# 运行打包后的jar文件
java -jar target/git-review-service-1.0.0.jar

# 使用Maven运行（开发环境）
mvn spring-boot:run
```

### 测试
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ClassName

# 运行特定测试方法
mvn test -Dtest=ClassName#methodName
```

### 依赖管理
```bash
# 查看依赖树
mvn dependency:tree

# 解决依赖冲突
mvn dependency:resolve
```

## 项目架构

### 技术栈
- 后端：Spring Boot 2.5.3，JGit 5.13.0，Jackson + JSON 文件存储
- 前端：Vue.js 2.6 + Element UI（CDN）
- 构建：Maven；Java 8

### 核心模块结构

#### Controller层
- **GitRepositoryController**: Git仓库CRUD管理，连接测试，远程分支获取
- **GitOperationController**: Git操作相关接口（分支列表等）
- **ReviewController**: Claude AI代码审查接口

#### Service层
- GitRepositoryService：仓库管理，使用 JSON 文件持久化
- GitService：JGit 实现 clone/diff/分支处理（含 ls-remote 回退 clone）
- ClaudeReviewService：封装审查流程，走 ClaudeService 的管道/文件调用
- JsonStorageService：JSON 文件读写

#### Entity层
- **GitRepository**: Git仓库实体，包含URL、凭据、描述等字段

### 前端架构
- 单页页面，位于 `src/main/resources/static/index.html`（由 Spring Boot 静态资源服务，根路径 `/`）
- 使用 Element UI 组件库与 Axios
- 模块：仓库管理、代码审查

## 关键配置

### application.properties（关键配置）
```properties
server.port=8080                           # 服务端口
json.storage.file=data/repositories.json   # 仓库数据存储文件
claude.command=claude                      # Claude CLI 命令
claude.timeout=30000                       # CLI 超时（毫秒，可调）
logging.level.com.example.gitreview.claude=DEBUG
```

### Claude 集成说明
- 通过 `ClaudeCliExecutor` 创建进程执行 Claude CLI
- Windows 兼容：优先直接调用 `claude.cmd`，失败时自动回退 PowerShell 包装（含参数转义与编码处理）
- 审查采用管道/文件输入：`pipeQueryFromFile(diff, prompt)`
- 自动清理临时 diff 文件与临时目录

## 开发工作流程

### 添加新的API接口
1. 在对应的Controller中添加@RequestMapping方法
2. 在相应的Service中实现业务逻辑
3. 如需数据持久化，使用JsonStorageService
4. 在前端Vue应用中添加对应的axios调用

### Git操作扩展
1. 基于 JGit 支持 clone/diff/分支操作，远程分支优先 ls-remote
2. 使用系统临时目录（`Files.createTempDirectory`），完成后自动清理
3. 支持用户名/密码访问私有仓库

### Claude审查功能
1. `ClaudeReviewService` 负责审查流程与提示词构建
2. 审查内容限制约 200KB（超出自动截断并标注）
3. 使用标准化的审查提示模板（可带项目上下文与提交说明）
4. Windows 环境自动处理 PowerShell/claude.cmd 调用细节

## 数据存储

### JSON 文件存储
- 仓库配置存储在 `data/repositories.json`
- 使用 JsonStorageService 进行序列化/反序列化
- 支持基本 CRUD
- 密码字段名为 `encryptedPassword`（当前未加密实现）

### 临时文件管理
- Git克隆操作使用系统临时目录
- Claude审查使用临时文件传递prompt
- 所有临时文件在操作完成后自动清理

## API设计模式

### RESTful接口规范
- GET `/api/repositories` - 获取所有仓库
- POST `/api/repositories` - 创建仓库
- PUT `/api/repositories/{id}` - 更新仓库
- DELETE `/api/repositories/{id}` - 删除仓库
- POST `/api/repositories/{id}/test-connection` - 测试连接
- POST `/api/review/{repositoryId}/claude` - Claude审查

### 错误处理
- 使用ResponseEntity统一返回格式
- HTTP状态码标准化（200/400/404）
- 异常信息通过响应体返回

## 环境要求

### 开发环境
- JDK 1.8+
- Maven 3.x
- Claude Code CLI已安装并配置
- Windows环境（PowerShell支持）

### 运行时依赖
- 临时目录写权限（默认C:\tmp\git-review）
- 网络访问权限（用于Git仓库clone）
- Claude CLI命令行工具可用

## 前端开发

### 访问方式
- 开发/运行: http://localhost:8080/
- 静态资源由 Spring Boot 提供

### 主要功能模块
1. **仓库管理**: 添加、删除、测试Git仓库连接
2. **代码审查**: 选择仓库和分支，执行Claude AI审查

### Vue组件结构
- 使用Element UI的Tab、Table、Dialog、Form组件
- axios异步请求处理
- 响应式数据绑定和状态管理
