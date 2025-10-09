Vue.component('workflow-detail-dialog', {
    template: `
        <el-dialog :title="'工作流详情 - ' + (workflow.name || '')"
                   :visible.sync="dialogVisible"
                   width="90%"
                   top="5vh"
                   @close="handleClose">
            <el-steps :active="getWorkflowStep(workflow.status)" finish-status="success" align-center>
                <el-step title="规格文档" description="生成spec.md"></el-step>
                <el-step title="技术方案" description="设计与批准"></el-step>
                <el-step title="任务列表" description="拆分任务"></el-step>
                <el-step title="代码生成" description="AI生成代码"></el-step>
                <el-step title="完成" description="工作流结束"></el-step>
            </el-steps>

            <el-tabs v-model="activeTab" style="margin-top: 20px;">
                <el-tab-pane label="规格文档" name="spec">
                    <div v-if="workflow.specification">
                        <div style="margin-bottom: 15px;">
                            <el-button-group>
                                <el-button size="small" icon="el-icon-refresh" @click="regenerateSpec"
                                           :loading="specRegenerating"
                                           :disabled="workflow.status !== 'SPEC_GENERATED'">
                                    重新生成规格文档
                                </el-button>
                                <el-button size="small" type="primary" @click="generateTechDesign"
                                           :loading="techDesignLoading"
                                           :disabled="workflow.status !== 'SPEC_GENERATED'">
                                    生成技术方案
                                </el-button>
                            </el-button-group>
                        </div>
                        <div class="code-container">
                            <div class="code-header">
                                <span>spec.md</span>
                                <el-button size="mini" @click="copyContent(workflow.specification.generatedContent)">复制</el-button>
                            </div>
                            <div class="code-content review-result" v-html="renderMarkdown(workflow.specification.generatedContent)"></div>
                        </div>
                    </div>
                    <el-empty v-else description="规格文档未生成"></el-empty>
                </el-tab-pane>

                <el-tab-pane label="技术方案" name="techDesign">
                    <div v-if="workflow.technicalDesign">
                        <div style="margin-bottom: 15px;">
                            <el-button-group>
                                <el-button size="small" @click="editTechDesign"
                                           :disabled="workflow.status !== 'TECH_DESIGN_GENERATED'">
                                    编辑方案
                                </el-button>
                                <el-button size="small" type="success" @click="approveTechDesign"
                                           :loading="approveLoading"
                                           :disabled="workflow.status !== 'TECH_DESIGN_GENERATED' || workflow.technicalDesign.approved">
                                    批准方案
                                </el-button>
                                <el-button size="small" type="primary" @click="generateTaskList"
                                           :loading="taskListLoading"
                                           :disabled="workflow.status !== 'TECH_DESIGN_APPROVED'">
                                    生成任务列表
                                </el-button>
                            </el-button-group>
                            <el-tag v-if="workflow.technicalDesign.approved" type="success" style="margin-left: 10px;">
                                已批准 (版本 {{ workflow.technicalDesign.version }})
                            </el-tag>
                        </div>
                        <div class="code-container">
                            <div class="code-header">
                                <span>tech-design.md (版本: {{ workflow.technicalDesign.version }})</span>
                                <el-button size="mini" @click="copyContent(workflow.technicalDesign.content)">复制</el-button>
                            </div>
                            <div class="code-content review-result" v-html="renderMarkdown(workflow.technicalDesign.content)"></div>
                        </div>
                    </div>
                    <el-empty v-else description="技术方案未生成"></el-empty>
                </el-tab-pane>

                <el-tab-pane label="任务列表" name="taskList">
                    <div v-if="workflow.taskList && workflow.taskList.tasks">
                        <div style="margin-bottom: 15px;">
                            <el-button size="small" type="primary" @click="startCodeGen"
                                       :loading="codeGenLoading"
                                       :disabled="workflow.status !== 'TASK_LIST_GENERATED'">
                                开始代码生成
                            </el-button>
                            <span style="margin-left: 15px; color: #666;">
                                进度: {{ workflow.taskList.completedTasks || 0 }} / {{ workflow.taskList.totalTasks || 0 }}
                            </span>
                        </div>
                        <el-table :data="workflow.taskList.tasks" style="width: 100%">
                            <el-table-column prop="id" label="任务ID" width="100"></el-table-column>
                            <el-table-column prop="title" label="任务标题"></el-table-column>
                            <el-table-column label="状态" width="120">
                                <template slot-scope="scope">
                                    <el-tag :type="getTaskStatusType(scope.row.status)" size="small">
                                        {{ getTaskStatusText(scope.row.status) }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="targetFile" label="目标文件"></el-table-column>
                        </el-table>
                    </div>
                    <el-empty v-else description="任务列表未生成"></el-empty>
                </el-tab-pane>

                <el-tab-pane label="生成代码" name="code">
                    <div v-if="completedTasks.length > 0">
                        <el-collapse accordion>
                            <el-collapse-item v-for="task in completedTasks" :key="task.id" :title="task.id + ' - ' + task.title">
                                <div class="code-container">
                                    <div class="code-header">
                                        <span>{{ task.targetFile }}</span>
                                        <el-button size="mini" @click="copyContent(task.generatedCode)">复制代码</el-button>
                                    </div>
                                    <pre class="code-content">{{ task.generatedCode }}</pre>
                                </div>
                            </el-collapse-item>
                        </el-collapse>
                    </div>
                    <el-empty v-else description="暂无已生成的代码"></el-empty>
                </el-tab-pane>
            </el-tabs>

            <el-dialog title="编辑技术方案" :visible.sync="showEditDialog" width="80%" append-to-body>
                <el-input type="textarea" v-model="editingContent" :rows="20"
                          placeholder="编辑技术方案内容..."></el-input>
                <span slot="footer" class="dialog-footer">
                    <el-button @click="showEditDialog = false">取消</el-button>
                    <el-button type="primary" @click="saveTechDesign" :loading="saveLoading">保存</el-button>
                </span>
            </el-dialog>
        </el-dialog>
    `,
    props: {
        visible: Boolean,
        workflow: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            dialogVisible: this.visible,
            activeTab: 'spec',
            showEditDialog: false,
            editingContent: '',
            specRegenerating: false,
            techDesignLoading: false,
            approveLoading: false,
            taskListLoading: false,
            codeGenLoading: false,
            saveLoading: false
        };
    },
    watch: {
        visible(val) {
            this.dialogVisible = val;
        },
        dialogVisible(val) {
            this.$emit('update:visible', val);
        }
    },
    computed: {
        completedTasks() {
            if (!this.workflow.taskList || !this.workflow.taskList.tasks) return [];
            return this.workflow.taskList.tasks.filter(t => t.status === 'COMPLETED' && t.generatedCode);
        }
    },
    methods: {
        async regenerateSpec() {
            try {
                await this.$confirm('确认重新生成规格文档吗？将使用原有PRD内容重新生成。', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                });

                this.specRegenerating = true;
                await API.workflow.spec.generate(this.workflow.id, {
                    prdContent: this.workflow.specification.prdContent,
                    documentPaths: this.workflow.specification.documentPaths || []
                });
                this.$message.success('规格文档重新生成中，请稍候...');
                this.$emit('refresh');
            } catch (error) {
                if (error !== 'cancel') {
                    this.$message.error('重新生成规格文档失败');
                }
            } finally {
                this.specRegenerating = false;
            }
        },

        async generateTechDesign() {
            this.techDesignLoading = true;
            try {
                await API.workflow.techDesign.generate(this.workflow.id);
                this.$message.success('技术方案生成中，请稍候...');
                this.$emit('refresh');
            } catch (error) {
                this.$message.error('生成技术方案失败');
            } finally {
                this.techDesignLoading = false;
            }
        },

        editTechDesign() {
            this.editingContent = this.workflow.technicalDesign.content;
            this.showEditDialog = true;
        },

        async saveTechDesign() {
            this.saveLoading = true;
            try {
                await API.workflow.techDesign.update(this.workflow.id, this.editingContent);
                this.$message.success('技术方案已更新');
                this.showEditDialog = false;
                this.$emit('refresh');
            } catch (error) {
                this.$message.error('保存失败');
            } finally {
                this.saveLoading = false;
            }
        },

        async approveTechDesign() {
            this.approveLoading = true;
            try {
                await API.workflow.techDesign.approve(this.workflow.id);
                this.$message.success('技术方案已批准');
                this.$emit('refresh');
            } catch (error) {
                this.$message.error('批准失败');
            } finally {
                this.approveLoading = false;
            }
        },

        async generateTaskList() {
            this.taskListLoading = true;
            try {
                await API.workflow.taskList.generate(this.workflow.id);
                this.$message.success('任务列表生成中，请稍候...');
                this.$emit('refresh');
            } catch (error) {
                this.$message.error('生成任务列表失败');
            } finally {
                this.taskListLoading = false;
            }
        },

        async startCodeGen() {
            this.codeGenLoading = true;
            try {
                await API.workflow.codeGeneration.start(this.workflow.id);
                this.$message.success('代码生成中，请稍候...');
                this.$emit('refresh');
            } catch (error) {
                this.$message.error('开始代码生成失败');
            } finally {
                this.codeGenLoading = false;
            }
        },

        copyContent(content) {
            Utils.copyToClipboard(content);
            this.$message.success('已复制到剪贴板');
        },

        renderMarkdown(content) {
            return Utils.renderMarkdown(content);
        },

        getWorkflowStep(status) {
            const stepMap = {
                'DRAFT': 0, 'SPEC_GENERATING': 0, 'SPEC_GENERATED': 1,
                'TECH_DESIGN_GENERATING': 1, 'TECH_DESIGN_GENERATED': 2, 'TECH_DESIGN_APPROVED': 2,
                'TASK_LIST_GENERATING': 2, 'TASK_LIST_GENERATED': 3,
                'CODE_GENERATING': 3, 'COMPLETED': 4, 'FAILED': -1, 'CANCELLED': -1
            };
            return stepMap[status] || 0;
        },

        getTaskStatusType(status) {
            const typeMap = {
                'PENDING': 'info', 'IN_PROGRESS': 'warning',
                'COMPLETED': 'success', 'FAILED': 'danger', 'SKIPPED': 'info'
            };
            return typeMap[status] || 'info';
        },

        getTaskStatusText(status) {
            const textMap = {
                'PENDING': '待执行', 'IN_PROGRESS': '进行中',
                'COMPLETED': '已完成', 'FAILED': '失败', 'SKIPPED': '已跳过'
            };
            return textMap[status] || status;
        },

        handleClose() {
            this.activeTab = 'spec';
        }
    }
});
