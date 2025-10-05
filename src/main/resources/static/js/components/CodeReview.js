Vue.component('code-review', {
    template: `
        <div class="section">
            <h3>代码审查</h3>

            <el-form :model="reviewForm" label-width="120px">
                <el-form-item label="选择仓库">
                    <el-select v-model="reviewForm.repositoryId" placeholder="请选择仓库" @change="loadBranches">
                        <el-option v-for="repo in repositories"
                                   :key="repo.id"
                                   :label="repo.name"
                                   :value="repo.id">
                        </el-option>
                    </el-select>
                    <el-button style="margin-left: 10px" @click="loadRemoteBranches" :loading="remoteLoading">
                        从远程拉取分支
                    </el-button>
                </el-form-item>

                <el-form-item label="基础分支">
                    <el-select v-model="reviewForm.baseBranch" placeholder="请选择基础分支" filterable allow-create>
                        <el-option v-for="branch in branches"
                                   :key="'base-' + branch"
                                   :label="branch"
                                   :value="branch">
                        </el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="目标分支">
                    <el-select v-model="reviewForm.targetBranch" placeholder="请选择目标分支" filterable allow-create>
                        <el-option v-for="branch in branches"
                                   :key="'target-' + branch"
                                   :label="branch"
                                   :value="branch">
                        </el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="审查模式">
                    <el-select v-model="reviewForm.mode" placeholder="请选择审查模式">
                        <el-option label="⚡ 快速审查 (2-5分钟)" value="quick">
                            <span style="float: left">⚡ 快速审查</span>
                            <span style="float: right; color: #8492a6; font-size: 13px">2-5分钟</span>
                        </el-option>
                        <el-option label="📋 标准审查 (5-10分钟)" value="standard">
                            <span style="float: left">📋 标准审查</span>
                            <span style="float: right; color: #8492a6; font-size: 13px">5-10分钟</span>
                        </el-option>
                        <el-option label="🔍 深度审查 (10-20分钟)" value="deep">
                            <span style="float: left">🔍 深度审查</span>
                            <span style="float: right; color: #8492a6; font-size: 13px">10-20分钟</span>
                        </el-option>
                        <el-option label="🔒 安全审查 (5-10分钟)" value="security">
                            <span style="float: left">🔒 安全审查</span>
                            <span style="float: right; color: #8492a6; font-size: 13px">5-10分钟</span>
                        </el-option>
                        <el-option label="⚡ 性能审查 (5-10分钟)" value="performance">
                            <span style="float: left">⚡ 性能审查</span>
                            <span style="float: right; color: #8492a6; font-size: 13px">5-10分钟</span>
                        </el-option>
                        <el-option label="🏗️ 架构审查 (5-10分钟)" value="architecture">
                            <span style="float: left">🏗️ 架构审查</span>
                            <span style="float: right; color: #8492a6; font-size: 13px">5-10分钟</span>
                        </el-option>
                    </el-select>
                    <el-alert
                        v-if="reviewForm.mode"
                        :title="getModeDescription(reviewForm.mode)"
                        type="info"
                        :closable="false"
                        style="margin-top: 10px">
                    </el-alert>
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="startReview" :loading="reviewLoading">
                        开始Review
                    </el-button>
                </el-form-item>
            </el-form>

            <div v-if="reviewResult" class="review-result">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                    <h4 style="margin: 0;">Review结果：</h4>
                    <el-button-group v-if="currentReviewId">
                        <el-button
                            size="small"
                            icon="el-icon-download"
                            @click="exportMarkdown"
                            :loading="exportLoading">
                            导出 Markdown
                        </el-button>
                        <el-button
                            size="small"
                            icon="el-icon-document"
                            @click="exportJson"
                            :loading="exportLoading">
                            导出 JSON
                        </el-button>
                    </el-button-group>
                </div>
                <div v-html="renderedReviewResult"></div>
            </div>
        </div>
    `,
    props: ['repositories'],
    data() {
        return {
            reviewForm: {
                repositoryId: '',
                baseBranch: 'master',
                targetBranch: '',
                mode: 'deep'
            },
            branches: [],
            reviewLoading: false,
            remoteLoading: false,
            reviewResult: '',
            currentReviewId: null,
            exportLoading: false
        };
    },
    computed: {
        renderedReviewResult() {
            return Utils.renderMarkdown(this.reviewResult);
        }
    },
    methods: {
        async loadBranches() {
            if (!this.reviewForm.repositoryId) return;

            try {
                const response = await API.git.getBranches(this.reviewForm.repositoryId);
                this.branches = response.data.branches || [];
            } catch (error) {
                this.$message.error('加载分支列表失败');
            }
        },

        async loadRemoteBranches() {
            if (!this.reviewForm.repositoryId) {
                this.$message.warning('请先选择仓库');
                return;
            }

            this.remoteLoading = true;
            try {
                const response = await API.repositories.getRemoteBranches(this.reviewForm.repositoryId);
                this.branches = response.data.branches || [];
                this.$message.success('远程分支加载成功');
            } catch (error) {
                this.$message.error('加载远程分支失败: ' + (error.response?.data || error.message));
            } finally {
                this.remoteLoading = false;
            }
        },

        async startReview() {
            if (!this.reviewForm.repositoryId || !this.reviewForm.targetBranch) {
                this.$message.warning('请选择仓库和分支');
                return;
            }

            this.reviewLoading = true;
            this.reviewResult = '';
            this.currentReviewId = null;

            try {
                const response = await API.review.start(this.reviewForm.repositoryId, {
                    baseBranch: this.reviewForm.baseBranch,
                    targetBranch: this.reviewForm.targetBranch,
                    mode: this.reviewForm.mode
                });
                this.reviewResult = response.data;
                this.currentReviewId = this.reviewForm.repositoryId;
                this.$message.success('Review完成');
            } catch (error) {
                this.$message.error('Review失败: ' + (error.response?.data || error.message));
            } finally {
                this.reviewLoading = false;
            }
        },

        getModeDescription(mode) {
            const descriptions = {
                'quick': '快速审查：仅关注关键Bug、安全漏洞和严重性能问题，适合日常PR审查',
                'standard': '标准审查：平衡的审查深度，覆盖常见的代码质量问题，适合功能合并前检查',
                'deep': '深度审查：全面深入的审查，包含详细的问题分级(P0-P3)、修复建议和代码示例，适合版本发布前的严格审查',
                'security': '安全审查：专注于安全漏洞检测，包括SQL注入、XSS、CSRF、认证授权等问题',
                'performance': '性能审查：专注于性能问题，包括N+1查询、慢SQL、内存泄漏、资源未关闭等',
                'architecture': '架构审查：关注架构设计，包括分层耦合、SOLID原则、设计模式应用等'
            };
            return descriptions[mode] || '';
        },

        async exportMarkdown() {
            if (!this.reviewResult) {
                this.$message.warning('没有可导出的审查结果');
                return;
            }

            this.exportLoading = true;
            try {
                const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
                const filename = \`review-\${this.currentReviewId}-\${timestamp}.md\`;
                const repoName = this.repositories.find(r => r.id === this.reviewForm.repositoryId)?.name || 'Unknown';
                const markdown = \`# 代码审查报告

## 基本信息

- **仓库**: \${repoName}
- **分支**: \${this.reviewForm.baseBranch} → \${this.reviewForm.targetBranch}
- **审查时间**: \${new Date().toLocaleString('zh-CN')}

---

## 审查结果

\${this.reviewResult}

---

**报告生成时间**: \${new Date().toLocaleString('zh-CN')}
**生成工具**: Git Review Service
\`;
                Utils.downloadFile(markdown, filename, 'text/markdown;charset=utf-8');
                this.$message.success('Markdown报告已导出');
            } catch (error) {
                this.$message.error('导出失败: ' + error.message);
            } finally {
                this.exportLoading = false;
            }
        },

        async exportJson() {
            if (!this.reviewResult) {
                this.$message.warning('没有可导出的审查结果');
                return;
            }

            this.exportLoading = true;
            try {
                const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
                const filename = \`review-\${this.currentReviewId}-\${timestamp}.json\`;
                const repoName = this.repositories.find(r => r.id === this.reviewForm.repositoryId)?.name || 'Unknown';
                const jsonData = {
                    reviewId: this.currentReviewId,
                    repository: {
                        id: this.reviewForm.repositoryId,
                        name: repoName
                    },
                    branches: {
                        base: this.reviewForm.baseBranch,
                        target: this.reviewForm.targetBranch
                    },
                    reviewTime: new Date().toISOString(),
                    result: this.reviewResult,
                    metadata: {
                        generatedBy: 'Git Review Service',
                        version: '1.0.0'
                    }
                };
                Utils.downloadFile(JSON.stringify(jsonData, null, 2), filename, 'application/json;charset=utf-8');
                this.$message.success('JSON报告已导出');
            } catch (error) {
                this.$message.error('导出失败: ' + error.message);
            } finally {
                this.exportLoading = false;
            }
        }
    }
});
