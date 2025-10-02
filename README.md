# Git代码Review服务

基于 Spring Boot + Vue.js(CDN) + Claude Code CLI 的自动化代码审查服务

## 功能特性

- ✅ 仓库管理：新增/查询/更新/删除
- ✅ 远程分支获取（JGit，无需完整克隆）
- ✅ 分支对比与 Git diff 生成
- ✅ Claude Code CLI 管道审查（支持增强提示）
- ✅ 简单网页前端（Vue 2 + Element UI via CDN）
- ✅ RESTful API 接口（含诊断/测试接口）
- ✅ Windows 11 兼容（自动处理 PowerShell/claude.cmd）

## 技术架构

### 后端技术栈
- Spring Boot 2.5.3
- JGit（Git 操作）
- Jackson + 本地 JSON 文件（数据持久化）
- Claude Code CLI 集成（通过进程调用，支持管道/文件）

### 前端技术栈
- Vue.js 2.6（CDN）
- Element UI（CDN）
- Axios（HTTP 客户端）

## 快速开始

### 环境要求
- Windows 11 / macOS / Linux
- JDK 1.8+
- Maven 3.x
- Claude Code CLI 已安装并可用（命令 `claude`）

### 运行
```bash
# 编译项目
mvn clean package

# 开发运行
mvn spring-boot:run

# 生产运行（打包后）
java -jar target/git-review-service-1.0.0.jar
```

### 访问前端
打开浏览器访问: http://localhost:8080/

## API接口

### 仓库管理
- `GET /api/repositories` — 获取所有仓库
- `POST /api/repositories` — 添加仓库
- `GET /api/repositories/{id}` — 获取单个仓库
- `PUT /api/repositories/{id}` — 更新仓库
- `DELETE /api/repositories/{id}` — 删除仓库
- `POST /api/repositories/{id}/test-connection` — 测试连接（当前返回固定成功 TODO）
- `GET /api/repositories/{id}/remote-branches` — 获取远程分支（优先 ls-remote，失败回退 clone）

### Git操作
- `GET /api/git/{repositoryId}/branches` — 获取分支列表（远程）
- `POST /api/git/{repositoryId}/review?targetBranch=xxx` — 生成基础 diff 文本（与 master 对比）

### Claude审查
- `POST /api/review/{repositoryId}/claude?targetBranch=xxx&baseBranch=master` — Claude 审查
- `POST /api/review/{repositoryId}/claude-enhanced?...` — 带自定义上下文/提交信息的增强审查
- `GET /api/review/test-claude` — 测试 Claude CLI 可用性
- `POST /api/review/test-pipeline` — 管道模式示例测试

## 使用流程

1. 在网页添加 Git 仓库配置
2. 选择仓库并拉取远程分支
3. 选择基础分支/目标分支
4. 点击“开始Review”，系统生成 diff
5. 通过 Claude Code CLI 管道将 diff 提交审查
6. 展示审查结果

## 配置说明

### application.properties（关键配置）
```properties
# 服务端口
server.port=8080

# JSON 存储文件（仓库配置）
json.storage.file=data/repositories.json

# Claude CLI
claude.command=claude
claude.timeout=30000           # 可调整（毫秒），默认30s
claude.debug=true              # 调试输出（可选）

# 日志
logging.level.com.example.gitreview.claude=DEBUG
```

## 注意事项

1. 确保 Claude Code CLI 已安装并在 PATH 中（命令 `claude`）
2. Git 仓库凭据当前以明文字段 `encryptedPassword` 存储（尚未实现加密），请谨慎使用
3. Git 与审查过程使用系统临时目录，完成后自动清理
4. 大型仓库/分支对比处理时间较长，建议先使用 `GET /api/repositories/{id}/remote-branches` 预检
