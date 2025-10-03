# Git Review Service

> 基于 Claude AI 的智能代码审查与测试生成服务

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Git Review Service** 是一个基于 **Spring Boot 3.2** + **Claude AI** 的智能代码审查与单元测试自动生成服务，采用DDD六边形架构，提供企业级代码质量保障解决方案。

## ✨ 核心特性

### 🔍 智能代码审查

- **6种审查模式**: 快速/标准/深度/安全/性能/架构
- **P0-P3问题分级**: 阻断性、严重、一般、建议四级分类
- **详细修复建议**: 根因分析、修复方案、代码示例、测试策略
- **质量评分**: 0-100分质量评分 + 风险等级评估
- **异步执行**: 支持长时间审查，实时进度跟踪
- **上下文提取**: 基于JavaParser的智能代码上下文提取

### 🧪 自动化测试生成

- **单元测试生成**: 基于代码结构自动生成JUnit 5测试
- **质量门禁**: 编译验证、覆盖率检查、复杂度控制
- **测试验证**: 自动编译验证生成的测试代码

### 📊 报告导出

- **Markdown格式**: 适用于GitHub/GitLab PR描述
- **JSON格式**: 结构化数据，便于集成和分析
- **优先级排序**: 问题按P0→P1→P2→P3自动排序

### 🏗️ 技术架构

- **DDD六边形架构**: 领域驱动设计，清晰的分层结构
- **Spring Boot 3.2**: 现代化Java应用框架
- **JGit 6.7**: Git操作（无需本地Git客户端）
- **JavaParser 3.25**: Java代码AST解析
- **Claude CLI**: AI驱动的代码审查引擎

## 🚀 快速开始

### 环境要求

- **JDK 17+**
- **Maven 3.x**
- **Claude CLI** (命令 `claude` 可用)
- **Windows 11** / macOS / Linux

### 安装运行

```bash
# 1. 克隆项目
git clone https://github.com/your-org/git-review-service.git
cd git-review-service

# 2. 编译项目
mvn clean package

# 3. 运行服务
mvn spring-boot:run

# 或使用jar包运行
java -jar target/git-review-service-1.0.0.jar
```

### 访问系统

- **前端界面**: http://localhost:8080
- **API文档**: http://localhost:8080/swagger-ui.html (TODO)
- **健康检查**: http://localhost:8080/actuator/health

### 第一次使用

1. 添加Git仓库（仓库管理 → 添加仓库）
2. 选择要审查的分支
3. 选择审查模式（推荐：深度审查）
4. 开始审查并查看结果

## 📖 文档

- **📘 用户手册**: [docs/user-manual.md](docs/user-manual.md) - 详细使用指南
- **👨‍💻 开发者文档**: [docs/developer-guide.md](docs/developer-guide.md) - 架构设计与扩展
- **📋 API文档**: [docs/api-docs.yaml](docs/api-docs.yaml) - OpenAPI 3.0规范
- **🧪 测试数据**: [src/test/resources/test-data/README.md](src/test/resources/test-data/README.md)

## 🎯 深度审查功能

### 问题优先级 (P0-P3)

| 优先级 | 说明 | 修复时间 | 示例 |
|--------|------|---------|------|
| 🔴 **P0 - 阻断性** | 严重安全漏洞、数据丢失、核心功能崩溃 | 当天 | SQL注入、XSS、支付错误 |
| 🟠 **P1 - 严重** | 重要功能缺陷、性能问题、资源泄漏 | 本周 | N+1查询、内存泄漏 |
| 🟡 **P2 - 一般** | 代码质量问题、违反规范 | 本月 | 代码重复、高耦合 |
| ⚪ **P3 - 建议** | 优化建议、命名问题 | 计划 | 变量命名、注释缺失 |

### 审查模式对比

```bash
# 快速审查 (2-5分钟) - 日常PR
POST /api/review/1/claude?mode=quick&baseBranch=main&targetBranch=feature/xyz

# 标准审查 (5-10分钟) - 功能合并
POST /api/review/1/claude?mode=standard&baseBranch=main&targetBranch=feature/xyz

# 深度审查 (10-20分钟) - 版本发布
POST /api/review/1/claude?mode=deep&baseBranch=main&targetBranch=feature/xyz

# 安全审查 (5-10分钟) - 安全审计
POST /api/review/1/claude?mode=security&baseBranch=main&targetBranch=feature/xyz
```

### 深度审查输出示例

```json
{
  "summary": "发现1个严重安全问题、2个性能问题和若干代码质量建议",
  "qualityScore": 65,
  "riskLevel": "high",
  "issues": [
    {
      "priority": "P0",
      "severity": "CRITICAL",
      "category": "安全问题",
      "file": "src/main/java/PaymentService.java",
      "line": 123,
      "description": "SQL注入漏洞",
      "impact": "攻击者可通过构造特殊输入获取其他用户数据",
      "fixSuggestion": {
        "rootCause": "直接拼接用户输入到SQL语句",
        "fixApproach": "使用PreparedStatement参数化查询",
        "codeExample": "PreparedStatement stmt = conn.prepareStatement(...)",
        "estimatedMinutes": 10
      }
    }
  ]
}
```

## 📡 API 接口

### 代码审查

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/review/{repositoryId}/claude` | POST | 执行代码审查 |
| `/api/review/{reviewId}/progress` | GET | 查询审查进度 |
| `/api/review/{reviewId}/export/markdown` | GET | 导出Markdown报告 |
| `/api/review/{reviewId}/export/json` | GET | 导出JSON报告 |
| `/api/code-review/{reviewId}/status` | GET | 获取审查状态 |

### 仓库管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/repositories` | GET | 获取所有仓库 |
| `/api/repositories` | POST | 添加仓库 |
| `/api/repositories/{id}` | GET | 获取单个仓库 |
| `/api/repositories/{id}` | PUT | 更新仓库 |
| `/api/repositories/{id}` | DELETE | 删除仓库 |
| `/api/repositories/{id}/remote-branches` | GET | 获取远程分支 |

### 测试生成

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/test-generation/generate` | POST | 生成单元测试 |
| `/api/test-generation/{suiteId}` | GET | 查询测试套件 |
| `/api/test-generation/{suiteId}/status` | GET | 查询生成状态 |

完整API文档请查看: [docs/api-docs.yaml](docs/api-docs.yaml)

## ⚙️ 配置说明

### application.properties 核心配置

```properties
# 服务端口
server.port=8080

# 数据存储（JSON文件）
json.storage.repository.file=data/repositories.json
json.storage.codereview.file=data/code-reviews.json
json.storage.testsuite.file=data/test-suites.json

# Claude CLI
claude.command=claude
claude.cli.timeout=120000                   # Claude CLI超时（代码审查）
claude.agent.timeout=60000                  # Claude Agent超时（测试生成）
claude.agent.max-retries=3                  # 重试次数

# Git操作
git.temp.dir=C:\\tmp\\git-review            # 临时工作空间（Windows路径）

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

# 异步执行
async.review.core-pool-size=5               # 异步审查核心线程数
async.review.max-pool-size=10               # 异步审查最大线程数
async.review.queue-capacity=50              # 异步审查队列容量

# 日志
logging.level.com.example.gitreview.claude=DEBUG
```

### 审查模式Prompt配置

编辑 `src/main/resources/review-prompts.properties` 可自定义审查规则：

```properties
# 深度审查模式
review.prompt.deep=你是资深代码审查专家，请对以下代码变更进行深度审查...

# 自定义审查模式
review.prompt.custom=请审查以下代码，重点检查：\
1. 线程安全问题\
2. 分布式事务一致性\
3. 缓存穿透风险
```

## 🏛️ 架构设计

### DDD六边形架构分层

```
├── Domain Layer (领域层)
│   ├── Aggregates (聚合根): CodeReview, Repository, TestSuite
│   ├── Entities (实体): ReviewSession
│   ├── Value Objects (值对象): CodeDiff, ReviewResult, ReviewStrategy
│   └── Domain Services (领域服务): CodeReviewDomainService
│
├── Application Layer (应用层)
│   ├── Application Services: CodeReviewApplicationService
│   ├── API Controllers: ReviewController, ReviewExportController
│   └── DTOs & Assemblers
│
└── Infrastructure Layer (基础设施层)
    ├── Ports (端口): ClaudeQueryPort, GitOperationPort, StoragePort
    ├── Adapters (适配器): ClaudeCliAdapter, JGitRepositoryAdapter
    └── Services: CodeContextExtractor, ReviewResultParser
```

**核心设计理念**: 业务逻辑归领域层，技术细节归基础设施层，应用层仅做编排协调。

详见：[docs/developer-guide.md](docs/developer-guide.md)

## 🧪 测试

### 运行所有测试

```bash
mvn test
```

### 测试覆盖率

```bash
mvn clean test jacoco:report
# 报告: target/site/jacoco/index.html
```

### 集成测试数据

使用测试数据集验证准确率：

```bash
cd src/test/resources/test-data

# Windows
verify-review-accuracy.bat

# Linux/Mac
chmod +x verify-review-accuracy.sh
./verify-review-accuracy.sh
```

**验收标准**:
- P0检测率 ≥90%
- P1检测率 ≥85%
- P2检测率 ≥75%
- P3检测率 ≥60%
- 总体准确率 ≥80%

## 🔧 开发指南

### 添加新的审查模式

1. 编辑 `review-prompts.properties`:
   ```properties
   review.prompt.mymode=请审查以下代码，重点关注...
   ```

2. 调用API时使用新模式:
   ```bash
   POST /api/review/1/claude?mode=mymode&...
   ```

### 扩展问题优先级逻辑

修改 `CodeReviewDomainService.calculateIssuePriority()`:

```java
public IssuePriority calculateIssuePriority(
    IssueSeverity severity, String category,
    String description, String filePath) {

    // 自定义映射规则
    if (isMyCustomRule(category, description)) {
        return IssuePriority.P0;
    }

    // ... 现有逻辑
}
```

### 集成CI/CD

**GitHub Actions示例**:

```yaml
name: Code Review

on: [pull_request]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run Deep Review
        run: |
          RESULT=$(curl -X POST http://review-server:8080/api/review/1/claude \
            -d "baseBranch=main" \
            -d "targetBranch=${{ github.head_ref }}" \
            -d "mode=deep")

          # 检查是否有P0问题
          P0_COUNT=$(echo $RESULT | jq '.issues | map(select(.priority=="P0")) | length')
          if [ "$P0_COUNT" -gt 0 ]; then
            echo "发现 $P0_COUNT 个P0问题，终止合并！"
            exit 1
          fi
```

## 📊 项目统计

- **代码行数**: ~15,000 行
- **测试用例**: 162 个
- **测试覆盖率**: >80%
- **支持语言**: Java (未来扩展: JavaScript, Python, Go)

## 🤝 贡献指南

欢迎贡献！请遵循以下流程：

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交代码 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

### 代码规范

- 遵循阿里巴巴Java开发手册（P3C规范）
- 方法长度 <50行，单一职责
- 类级注释包含 `@author` 和 `@since`
- 单元测试覆盖核心业务逻辑

## 📝 更新日志

### v1.0.0 (2025-10-03)

**新增功能**:
- ✅ P0-P3问题优先级分级
- ✅ 6种审查模式（快速/标准/深度/安全/性能/架构）
- ✅ 详细修复建议（根因、方案、代码示例、预估时间）
- ✅ 异步审查执行与进度跟踪
- ✅ Markdown/JSON报告导出
- ✅ 智能代码上下文提取（基于JavaParser）
- ✅ 单元测试自动生成
- ✅ 质量评分与风险等级计算

**架构升级**:
- ✅ 升级到Spring Boot 3.2 + Java 17
- ✅ 采用DDD六边形架构
- ✅ 162个单元/集成测试

**测试与文档**:
- ✅ 集成测试数据集（P0-P3示例代码）
- ✅ 准确率验证脚本
- ✅ 完整的用户手册和开发者文档

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [Claude AI](https://www.anthropic.com/claude) - AI驱动的代码审查引擎
- [Spring Boot](https://spring.io/projects/spring-boot) - 强大的Java应用框架
- [JGit](https://www.eclipse.org/jgit/) - Java实现的Git库
- [JavaParser](https://javaparser.org/) - Java代码解析工具

## 📧 联系方式

- **项目主页**: https://github.com/your-org/git-review-service
- **问题反馈**: [GitHub Issues](https://github.com/your-org/git-review-service/issues)
- **使用交流**: [GitHub Discussions](https://github.com/your-org/git-review-service/discussions)

---

**⭐ 如果这个项目对您有帮助，请给个Star！**
