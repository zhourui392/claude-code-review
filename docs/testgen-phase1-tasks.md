# 单元测试生成功能改造 - 第一阶段任务清单

目标：以稳定性与一致性为优先，收敛职责边界，建立可靠的“生成-验证-修复-提交”闭环与可观测性。

---

## 进度追踪（更新于 2025-10-07）

- 已完成
  - 任务1：控制器瘦身与编排下沉（精简 TestGenerationController；在 TestGenerationApplicationService 内新增批量编排与状态查询）
  - 任务2：任务持久化与状态机统一（基于 TestSuiteRepository，新增 getBatchStatus/getBatchResult 聚合查询）
  - 任务7：多模块与路径解析完善（新增 findPomModules，优化 findJavaClassFile；补充 listAvailableJavaClasses 接口）

- 已交付的基础能力
  - CodeCompilationService：优先使用 mvnw，Maven 参数优化（-T 1C -DskipITs -DfailIfNoTests=false），新增 parseJacocoCoverage() 解析 jacoco.xml
  - ClaudeCodeCliAdapter：移除危险参数 --dangerously-skip-permissions；严格限定修改范围为 src/test/java 与必要 pom.xml
  - 应用层编排：buildPromptWithContext() 与回退方法；批量执行与任务状态聚合查询
  - Git 安全提交：通过 JGit 精确提交 src/test/java 与 pom.xml

- 已知限制与后续待办
  - 任务3：Prompt 标准化与 AST 上下文接入仍需补充更多上下文字段与模板化导入
  - 任务4：覆盖率门禁逻辑待与 TestSuite 结果写回打通（已具备 jacoco.xml 解析能力）
  - 任务5：错误修复回路需串联错误摘要、最大重试策略与验证闭环
  - 任务6：远端推送与失败回退策略待补全（本地安全提交已具备）
  - 当前仓库 Workflow 模块测试存在接口不兼容导致 mvn test 失败，超出本阶段范围；如需打包可临时跳过测试，但受私服/离线源影响可能出现插件依赖解析失败

## 任务1：控制器瘦身与编排下沉
- 目标：将克隆/生成/编译/修复/提交等流程从 Controller 下沉至应用层服务，Controller 仅做参数校验与转发，保持 DDD 分层一致。
- 涉及组件：
  - TestGenerationController（瘦身）
  - TestGenerationApplicationService（新增/完善编排）
  - TestGenerationDomainService（保持领域规则，不做技术编排）
- 具体工作：
  - 从 TestGenerationController 移除仓库克隆、类查找、编译/测试、Claude 修复、Git 提交等实现，改为调用应用服务统一编排。
  - 在应用服务中串联：克隆→类定位→Prompt生成→Claude生成→写文件→编译→测试→必要时修复→最终提交。
  - 统一异常与状态上报接口（进度/消息）。
- 交付物：精简后的 Controller；具备完整编排的 ApplicationService。
- 验收标准：Controller 仅含入参校验与调用；端到端流程可通过 ApplicationService 独立驱动并产出结果。

## 任务2：任务持久化与状态机统一
- 目标：用 TestSuiteRepository 持久化任务与状态，替换内存 Map；统一状态机与幂等查询。
- 涉及组件：
  - TestSuite 聚合根/状态机
  - TestSuiteRepository 实现（JSON/文件或后端存储）
- 具体工作：
  - 创建/更新任务时均落库，移除 Controller 中 AtomicLong/Map 任务表。
  - 提供基于 taskId/suiteId 的查询接口，缓存仅作读优化（可选）。
  - 统一状态流转：PENDING→GENERATING→GENERATED→VALIDATING→VALIDATED/COMPLETED/FAILED。
- 交付物：持久化的任务记录与查询接口；删除内存态实现。
- 验收标准：应用重启后任务可恢复查询；状态机非法流转被拒绝并记录原因。

## 任务3：Prompt 标准化与 AST 上下文接入
- 目标：根据类结构与用户参数生成可复用的 Prompt，增强代码块解析鲁棒性。
- 涉及组件：
  - CodeContextExtractor、JavaParserService
  - ClaudeQueryPort（query）
- 具体工作：
  - 提取类的包名、类名、公共方法签名、依赖、注释等上下文（限制大小），按 testType/qualityLevel/Mock/断言框架动态注入 Prompt。
  - 增强响应解析：支持```java/```代码块、无语言代码块与“public class”起始三种路径；补全缺失包名与 imports（模板化）。
  - 统一测试类命名与模板（JUnit5+MockitoExtension），为空/脆弱场景给出兜底模板。
- 交付物：标准化 Prompt 生成器与响应解析器。
- 验收标准：多类输入下稳定生成合法 Java 测试代码，解析成功率显著提升。

## 任务4：构建验证与覆盖率门禁（JaCoCo）
- 目标：让验证链路产出真实覆盖率并执行质量门禁。
- 涉及组件：
  - CodeCompilationService
  - POM（JaCoCo、Surefire 配置）
- 具体工作：
  - 优先使用 mvnw（存在时），Maven 参数优化：`-T 1C -DskipITs -DfailIfNoTests=false -q`。
  - 引入 JaCoCo（report XML），在 runTests 后解析 jacoco.xml，产出 class/line 覆盖率。
  - 按配置（quality.gates.min-coverage、quality.gates.compilation-required）判定通过/失败并写回 TestSuite。
- 交付物：可解析覆盖率的编译/测试服务；质量门禁判定与结果结构化输出。
- 验收标准：覆盖率数值可在结果查询中呈现；低于阈值时任务进入 FAILED 并给出明确原因。

## 任务5：错误修复回路收敛与安全
- 目标：Claude 修复仅作用于测试代码与必要的依赖声明，输入聚焦、次数可控。
- 涉及组件：
  - ClaudeCodePort.fixCompilationError
  - CodeCompilationService（错误摘要提取）
- 具体工作：
  - 将错误输出摘要化（文件/行/片段），Prompt 强约束“仅修改 src/test/java 与必要 pom.xml”。
  - 限制最大重试次数（配置化），记录每次修复摘要与 diff。
  - 修复后必经重新 compile/test 校验。
- 交付物：受限范围的修复流程与重试策略。
- 验收标准：非测试文件不会被修改；超过重试上限时明确失败原因并退出。

## 任务6：Git 提交安全化与最小变更
- 目标：精确提交仅测试文件与必要的 pom 变更，提交信息服务端生成，避免危险参数。
- 涉及组件：
  - GitCommitService / JGit
  - ClaudeCodePort.gitCommitAndPush（降权或替换为本地提交）
- 具体工作：
  - 用 JGit 或精确 `git add` 路径方式，仅添加 `src/test/java/**` 与必要 `pom.xml`。
  - 提交信息统一由服务侧生成（含 gateId、方案、建议），避免 `--dangerously-skip-permissions`。
  - 推送失败时提供可重试/人工处理路径，不强制。
- 交付物：精确提交与推送实现，安全的提交信息生成。
- 验收标准：提交 diff 仅包含测试/必要依赖变更；远端历史整洁可回溯。

## 任务7：多模块与路径解析完善
- 目标：在多模块/Gradle 项目中稳健定位源类与对应测试路径。
- 涉及组件：
  - 类文件查找与路径转换逻辑
- 具体工作：
  - 解析父 POM `modules` 或递归检测子模块（含 Gradle），建立模块索引。
  - `findJavaClassFile` 先按包名直达，失败再走索引/递归；增强日志与候选提示。
  - `convertToTestPath` 校验包名与目录一致性，缺失目录时自动创建；统一 Windows/Unix 路径处理。
- 交付物：稳健的类定位与测试路径转换实现。
- 验收标准：在单模块与多模块仓库中均可正确生成到 `src/test/java` 对应包路径。

---

### 验收与回归
- 提供端到端集成用例（单类/多类/多模块），覆盖：生成→编译→测试→门禁→（必要）修复→提交。
- 输出统一的任务状态/进度/覆盖率/失败原因；重启后可恢复查询。
- 变更不影响现有代码审查与工作流模块的对外 API 行为。
