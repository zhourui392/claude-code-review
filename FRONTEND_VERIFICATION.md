# 前端重构验证指南

## 🚀 快速验证（已启动HTTP服务器）

### 访问地址
```
http://localhost:9090/index.html
```

## ✅ 验证检查清单

### 1. 页面基础功能
- [ ] 页面能正常加载，无404错误
- [ ] 页面标题显示"Git代码Review服务"
- [ ] 四个Tab页签都能正常显示：
  - 仓库管理
  - 代码Review
  - 单元测试生成
  - AI辅助开发

### 2. 浏览器控制台检查（F12）
打开浏览器开发者工具，检查：

#### 2.1 Network（网络）面板
- [ ] 所有JS文件加载成功（200状态码）：
  - `/js/utils/api.js`
  - `/js/utils/common.js`
  - `/js/components/RepositoryManagement.js`
  - `/js/components/CodeReview.js`
  - `/js/components/TestGeneration.js`
  - `/js/components/WorkflowManagement.js`
  - `/js/components/WorkflowCreateDialog.js`
  - `/js/components/WorkflowDetailDialog.js`
- [ ] CSS文件加载成功：`/css/styles.css`

#### 2.2 Console（控制台）面板
- [ ] 无红色错误信息
- [ ] 无组件注册失败错误
- [ ] 无"Cannot read property"等运行时错误

### 3. 功能验证（模拟测试）

#### 3.1 仓库管理Tab
- [ ] 点击"添加仓库"按钮，弹出对话框
- [ ] 对话框包含所有表单字段：名称、地址、用户名、密码、描述
- [ ] 对话框"取消"按钮能正常关闭

#### 3.2 代码Review Tab
- [ ] "选择仓库"下拉框能展开
- [ ] "审查模式"下拉框显示6种模式（快速/标准/深度/安全/性能/架构）
- [ ] 选择审查模式后，下方显示模式说明

#### 3.3 单元测试生成Tab
- [ ] "选择仓库"下拉框能展开
- [ ] "Java类名"输入框能输入文本
- [ ] "测试要求"文本域能输入内容

#### 3.4 AI辅助开发Tab
- [ ] 点击"创建工作流"按钮，弹出对话框
- [ ] 对话框包含基本信息和代码风格配置两部分
- [ ] 架构模式下拉框显示5种架构
- [ ] 编码规范下拉框显示4种规范

### 4. 样式验证
- [ ] 页面居中显示（max-width: 1200px）
- [ ] 标题样式正常（居中、有间距）
- [ ] 按钮样式正常（Element UI蓝色主题）
- [ ] 表格样式正常（带边框、行高合适）

### 5. 对比验证（与旧版本对比）

访问旧版本（如果需要）：
```bash
# 在static目录下创建临时访问旧版本
cp index-old.html index-temp.html
# 访问 http://localhost:9090/index-temp.html
```

对比检查：
- [ ] 新版本功能与旧版本完全一致
- [ ] 新版本无功能缺失
- [ ] 新版本UI样式一致

## 🔍 深度验证（需要后端API）

以下验证需要Spring Boot后端运行：

### 1. API交互验证
- [ ] 加载仓库列表（GET /api/repositories）
- [ ] 创建仓库（POST /api/repositories）
- [ ] 开始Review（POST /api/review/{id}/claude）
- [ ] 生成测试（POST /api/test-generation/generate）

### 2. 组件交互验证
- [ ] 仓库管理组件刷新后，其他Tab能获取到最新仓库列表
- [ ] 工作流创建后，能正确触发轮询刷新
- [ ] 导出Markdown/JSON功能正常下载文件

## 🐛 常见问题排查

### 问题1：页面空白
**检查**：
1. 浏览器控制台是否有JS加载错误
2. 是否所有外部依赖CDN正常加载（Vue/Element UI/Axios/Marked）

### 问题2：组件不显示
**检查**：
1. 控制台是否有"Unknown custom element"错误
2. 组件JS文件是否正确加载
3. Vue组件注册是否成功

### 问题3：API调用失败
**原因**：后端未启动
**解决**：这是正常的，前端静态页面可以独立验证UI和交互

### 问题4：样式错乱
**检查**：
1. `/css/styles.css`是否加载成功
2. Element UI的CSS是否加载成功

## 📊 验证报告模板

```markdown
# 前端重构验证报告

**验证时间**：2025-10-05
**验证人**：[你的名字]

## 验证结果

### 基础功能
- 页面加载：✅/❌
- 资源加载：✅/❌（__/11个文件成功）
- 控制台无错误：✅/❌

### UI组件
- 仓库管理：✅/❌
- 代码Review：✅/❌
- 测试生成：✅/❌
- 工作流管理：✅/❌

### 样式展示
- 整体布局：✅/❌
- Element UI主题：✅/❌
- 响应式设计：✅/❌

## 发现问题
1. [描述问题1]
2. [描述问题2]

## 总体评价
[✅ 重构成功 / ❌ 需要修复]
```

## 🎯 快速验证命令

```bash
# 1. 启动HTTP服务器（已启动）
python -m http.server 9090

# 2. 打开浏览器
start http://localhost:9090/index.html

# 3. 查看文件结构
cd D:\agent_workspace\claude-code-review\src\main\resources\static
dir /s
```

## 📝 验证记录

- [x] HTTP服务器已启动（端口9090）
- [ ] 浏览器访问验证
- [ ] 功能完整性验证
- [ ] 样式一致性验证
