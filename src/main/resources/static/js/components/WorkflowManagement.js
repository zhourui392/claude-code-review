Vue.component('workflow-management', {
    template: `
        <div class="section">
            <h3>AI辅助开发工作流</h3>
            <el-button type="primary" @click="showCreateDialog = true" icon="el-icon-plus">
                创建工作流
            </el-button>

            <el-table :data="workflows" style="width: 100%; margin-top: 20px;" v-loading="loading">
                <el-table-column prop="name" label="工作流名称" width="200"></el-table-column>
                <el-table-column label="状态" width="150">
                    <template slot-scope="scope">
                        <el-tag :type="getStatusType(scope.row.status)" size="small">
                            {{ getStatusText(scope.row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="进度" width="200">
                    <template slot-scope="scope">
                        <el-progress :percentage="scope.row.progress" :color="getProgressColor(scope.row.progress)"></el-progress>
                    </template>
                </el-table-column>
                <el-table-column prop="currentStage" label="当前阶段"></el-table-column>
                <el-table-column label="创建时间" width="180">
                    <template slot-scope="scope">
                        {{ formatDate(scope.row.createdAt) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="280">
                    <template slot-scope="scope">
                        <el-button-group>
                            <el-button size="mini" @click="viewDetail(scope.row)" icon="el-icon-view">
                                查看
                            </el-button>
                            <el-button size="mini" type="primary" @click="viewDetail(scope.row)"
                                       :disabled="!canContinue(scope.row)" icon="el-icon-right">
                                继续
                            </el-button>
                            <el-button size="mini" type="danger" @click="cancel(scope.row)"
                                       :disabled="!canCancel(scope.row)" icon="el-icon-close">
                                取消
                            </el-button>
                        </el-button-group>
                    </template>
                </el-table-column>
            </el-table>

            <workflow-create-dialog
                :visible.sync="showCreateDialog"
                :repositories="repositories"
                @created="handleCreated">
            </workflow-create-dialog>

            <workflow-detail-dialog
                :visible.sync="showDetailDialog"
                :workflow="currentWorkflow"
                @refresh="loadWorkflows">
            </workflow-detail-dialog>
        </div>
    `,
    props: ['repositories'],
    data() {
        return {
            workflows: [],
            loading: false,
            showCreateDialog: false,
            showDetailDialog: false,
            currentWorkflow: {},
            pollingTimer: null
        };
    },
    mounted() {
        this.loadWorkflows();
    },
    beforeDestroy() {
        if (this.pollingTimer) {
            clearInterval(this.pollingTimer);
        }
    },
    methods: {
        async loadWorkflows() {
            this.loading = true;
            try {
                const response = await API.workflow.list();
                this.workflows = response.data;
            } catch (error) {
                this.$message.error('加载工作流列表失败');
            } finally {
                this.loading = false;
            }
        },

        async viewDetail(workflow) {
            try {
                const promises = [API.workflow.getStatus(workflow.id)];

                if (workflow.status !== 'DRAFT' && workflow.status !== 'SPEC_GENERATING') {
                    promises.push(API.workflow.spec.get(workflow.id).catch(() => null));
                }
                if (['TECH_DESIGN_GENERATED', 'TECH_DESIGN_APPROVED', 'TASK_LIST_GENERATING', 'TASK_LIST_GENERATED', 'CODE_GENERATING', 'COMPLETED'].includes(workflow.status)) {
                    promises.push(API.workflow.techDesign.get(workflow.id).catch(() => null));
                }
                if (['TASK_LIST_GENERATED', 'CODE_GENERATING', 'COMPLETED'].includes(workflow.status)) {
                    promises.push(API.workflow.taskList.get(workflow.id).catch(() => null));
                }

                const results = await Promise.all(promises);

                this.currentWorkflow = {
                    ...results[0].data,
                    specification: results[1]?.data || null,
                    technicalDesign: results[2]?.data || null,
                    taskList: results[3]?.data || null
                };

                this.showDetailDialog = true;

                if (!['COMPLETED', 'FAILED', 'CANCELLED'].includes(workflow.status)) {
                    this.startPolling(workflow.id);
                }
            } catch (error) {
                this.$message.error('获取工作流详情失败');
            }
        },

        async cancel(workflow) {
            try {
                await this.$confirm('确定要取消该工作流吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                });

                await API.workflow.cancel(workflow.id, '用户手动取消');
                this.$message.success('工作流已取消');
                await this.loadWorkflows();
            } catch (error) {
                if (error !== 'cancel') {
                    this.$message.error('取消工作流失败');
                }
            }
        },

        handleCreated(workflowId) {
            this.loadWorkflows();
            this.startPolling(workflowId);
        },

        startPolling(workflowId) {
            if (this.pollingTimer) {
                clearInterval(this.pollingTimer);
            }

            this.pollingTimer = setInterval(async () => {
                try {
                    const res = await API.workflow.getStatus(workflowId);
                    const status = res.data.status;

                    await this.loadWorkflows();

                    if (this.showDetailDialog && this.currentWorkflow.id === workflowId) {
                        await this.viewDetail({ id: workflowId, status });
                    }

                    if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(status)) {
                        clearInterval(this.pollingTimer);
                        this.pollingTimer = null;
                    }
                } catch (error) {
                    console.error('轮询工作流状态失败', error);
                }
            }, 3000);
        },

        formatDate(dateStr) {
            return Utils.formatDate(dateStr);
        },

        getStatusType(status) {
            const typeMap = {
                'DRAFT': 'info', 'SPEC_GENERATING': 'warning', 'SPEC_GENERATED': 'success',
                'TECH_DESIGN_GENERATING': 'warning', 'TECH_DESIGN_GENERATED': 'success', 'TECH_DESIGN_APPROVED': 'success',
                'TASK_LIST_GENERATING': 'warning', 'TASK_LIST_GENERATED': 'success',
                'CODE_GENERATING': 'warning', 'COMPLETED': 'success', 'FAILED': 'danger', 'CANCELLED': 'info'
            };
            return typeMap[status] || 'info';
        },

        getStatusText(status) {
            const textMap = {
                'DRAFT': '草稿', 'SPEC_GENERATING': '生成规格文档中', 'SPEC_GENERATED': '规格文档已生成',
                'TECH_DESIGN_GENERATING': '生成技术方案中', 'TECH_DESIGN_GENERATED': '技术方案已生成', 'TECH_DESIGN_APPROVED': '技术方案已批准',
                'TASK_LIST_GENERATING': '生成任务列表中', 'TASK_LIST_GENERATED': '任务列表已生成',
                'CODE_GENERATING': '代码生成中', 'COMPLETED': '已完成', 'FAILED': '失败', 'CANCELLED': '已取消'
            };
            return textMap[status] || status;
        },

        getProgressColor(percentage) {
            if (percentage < 30) return '#f56c6c';
            if (percentage < 70) return '#e6a23c';
            return '#67c23a';
        },

        canContinue(workflow) {
            return !['COMPLETED', 'FAILED', 'CANCELLED'].includes(workflow.status);
        },

        canCancel(workflow) {
            return !['COMPLETED', 'FAILED', 'CANCELLED'].includes(workflow.status);
        }
    }
});
