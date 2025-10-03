# 代码审查报告

## 📋 基本信息

- **仓库**: {{repositoryName}}
- **分支**: {{baseBranch}} → {{targetBranch}}
- **审查时间**: {{reviewTime}}
- **审查模式**: {{reviewMode}}
- **质量评分**: {{qualityScore}}/100
- **风险等级**: {{riskLevel}}

---

## 📊 问题统计

| 优先级 | 数量 | 说明 |
|-------|------|------|
| 🔴 **P0 - 阻断性** | {{p0Count}} | 必须立即修复 |
| 🟠 **P1 - 严重** | {{p1Count}} | 下次发布前必须修复 |
| 🟡 **P2 - 一般** | {{p2Count}} | 建议修复 |
| ⚪ **P3 - 建议** | {{p3Count}} | 可延后处理 |
| **总计** | {{totalIssues}} | |

---

{{#hasP0Issues}}
## 🔴 P0 - 阻断性问题 ({{p0Count}})

> **必须立即修复！这些问题会导致严重的安全漏洞、数据丢失或服务崩溃。**

{{#p0Issues}}
### {{index}}. {{description}}

- **文件**: `{{file}}:{{line}}`
- **类别**: {{category}}
- **严重程度**: {{severity}}

**代码片段**:
```{{language}}
{{codeSnippet}}
```

**影响**: {{impact}}

**修复建议**:
- **根本原因**: {{fixSuggestion.rootCause}}
- **修复方案**: {{fixSuggestion.fixApproach}}
- **预计时间**: {{fixSuggestion.estimatedMinutes}}分钟

**修复代码示例**:
```{{language}}
{{fixSuggestion.codeExample}}
```

**测试策略**: {{fixSuggestion.testStrategy}}

{{#fixSuggestion.references}}
**参考资料**: {{.}}
{{/fixSuggestion.references}}

---

{{/p0Issues}}
{{/hasP0Issues}}

{{#hasP1Issues}}
## 🟠 P1 - 严重问题 ({{p1Count}})

> **下次发布前必须修复。这些问题会影响重要功能、性能或用户体验。**

{{#p1Issues}}
### {{index}}. {{description}}

- **文件**: `{{file}}:{{line}}`
- **类别**: {{category}}

{{#codeSnippet}}
**代码片段**:
```{{language}}
{{codeSnippet}}
```
{{/codeSnippet}}

**影响**: {{impact}}

**修复建议**:
- {{fixSuggestion.fixApproach}}
- 预计时间: {{fixSuggestion.estimatedMinutes}}分钟

{{#fixSuggestion.codeExample}}
**修复代码示例**:
```{{language}}
{{fixSuggestion.codeExample}}
```
{{/fixSuggestion.codeExample}}

---

{{/p1Issues}}
{{/hasP1Issues}}

{{#hasP2Issues}}
## 🟡 P2 - 一般问题 ({{p2Count}})

> **建议修复。这些问题影响代码质量和可维护性。**

{{#p2Issues}}
### {{index}}. {{description}}

- **文件**: `{{file}}:{{line}}`
- **类别**: {{category}}
- **建议**: {{fixSuggestion.fixApproach}}

---

{{/p2Issues}}
{{/hasP2Issues}}

{{#hasP3Issues}}
## ⚪ P3 - 改进建议 ({{p3Count}})

{{#p3Issues}}
- **[{{category}}]** {{description}} (`{{file}}:{{line}}`)
  - 建议: {{fixSuggestion.fixApproach}}
{{/p3Issues}}

{{/hasP3Issues}}

---

{{#hasSuggestions}}
## 💡 改进建议

{{#suggestions}}
### {{index}}. [{{category}}] {{description}}

- **优先级**: {{priority}}/10
- **收益**: {{benefit}}

{{/suggestions}}
{{/hasSuggestions}}

---

## 📈 质量分析

### 总体评价
{{summary}}

### 质量指标
- **质量评分**: {{qualityScore}}/100
  - 90-100: 优秀
  - 80-89: 良好
  - 70-79: 合格
  - 60-69: 需改进
  - <60: 不合格

{{#hasCriticalIssues}}
- **⚠️ 存在严重问题**: 是
{{/hasCriticalIssues}}

### 风险等级
- **当前风险**: {{riskLevel}}
{{#riskLevel}}
  - **high**: 存在P0阻断性问题或多个P1严重问题
  - **medium**: 存在P1严重问题但无P0问题
  - **low**: 仅存在P2/P3问题
{{/riskLevel}}

---

## 🎯 修复优先级建议

1. **立即修复** (当天完成):
   - 所有P0问题
   - 安全相关的P1问题

2. **本周修复**:
   - 其他P1问题
   - 重要模块的P2问题

3. **本月修复**:
   - 一般P2问题
   - 高优先级P3建议

4. **计划修复**:
   - 低优先级P3建议

---

**报告生成时间**: {{generatedAt}}
**生成工具**: Git Review Service - Claude Code Review
