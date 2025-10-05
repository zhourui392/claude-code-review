Vue.component('test-generation', {
    template: `
        <div class="section">
            <h3>智能单元测试生成</h3>

            <el-form :model="testGenForm" label-width="120px">
                <el-form-item label="选择仓库">
                    <el-select v-model="testGenForm.repositoryId" placeholder="请选择仓库" @change="loadTestBranches">
                        <el-option v-for="repo in repositories"
                                   :key="repo.id"
                                   :label="repo.name"
                                   :value="repo.id">
                        </el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="分支">
                    <el-select v-model="testGenForm.branch" placeholder="请选择分支" filterable allow-create>
                        <el-option v-for="branch in testBranches"
                                   :key="'test-' + branch"
                                   :label="branch"
                                   :value="branch">
                        </el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="Java类名">
                    <el-input
                        type="textarea"
                        v-model="testGenForm.className"
                        :rows="3"
                        placeholder="例如: UserService 或 UserService,OrderService,ProductService (逗号分隔多个类)">
                    </el-input>
                    <div style="color: #909399; font-size: 12px; margin-top: 5px;">
                        支持输入多个类名，使用英文逗号分隔
                    </div>
                </el-form-item>

                <el-form-item label="准入ID">
                    <el-input v-model="testGenForm.gateId" placeholder="可选，未填写则从Git历史提取">
                        <template slot="prepend">Gate ID</template>
                    </el-input>
                    <div style="color: #909399; font-size: 12px; margin-top: 5px;">
                        如不填写，系统将自动从最近的Git提交历史中提取准入ID
                    </div>
                </el-form-item>

                <el-form-item label="测试要求">
                    <el-input
                        type="textarea"
                        v-model="testGenForm.requirement"
                        :rows="4"
                        placeholder="可选，输入对测试代码的特殊要求（如：需要测试边界条件、需要覆盖异常场景等）">
                    </el-input>
                    <div style="color: #909399; font-size: 12px; margin-top: 5px;">
                        可选填写，留空则使用默认的Mock测试标准（完美质量）
                    </div>
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="startTestGeneration" :loading="testGenLoading">
                        生成测试代码
                    </el-button>
                    <el-button @click="resetTestForm">重置</el-button>
                </el-form-item>
            </el-form>

            <div v-if="testGenProgress.show" class="test-progress">
                <h4>生成进度</h4>
                <el-steps :active="testGenProgress.currentStep" finish-status="success">
                    <el-step title="分析源码" description="解析Java类结构"></el-step>
                    <el-step title="生成测试" description="Claude AI生成测试代码"></el-step>
                    <el-step title="验证编译" description="检查测试代码语法"></el-step>
                    <el-step title="执行测试" description="运行并验证测试"></el-step>
                </el-steps>
                <el-progress :percentage="testGenProgress.percentage"
                            :status="testGenProgress.status"
                            style="margin-top: 20px;">
                </el-progress>
                <div v-if="testGenProgress.message" style="margin-top: 10px; color: #666;">
                    {{ testGenProgress.message }}
                </div>
            </div>

            <div v-if="testGenResult.show" class="test-result">
                <h4>生成结果</h4>
                <el-tabs type="card">
                    <el-tab-pane label="测试代码">
                        <div class="code-container">
                            <div class="code-header">
                                <span>{{ testGenResult.testFileName }}</span>
                                <div>
                                    <el-button size="mini" @click="copyTestCode">复制代码</el-button>
                                    <el-button size="mini" type="primary" @click="downloadTestCode">下载文件</el-button>
                                </div>
                            </div>
                            <pre class="code-content"><code>{{ testGenResult.testCode }}</code></pre>
                        </div>
                    </el-tab-pane>
                    <el-tab-pane label="执行结果">
                        <el-descriptions border>
                            <el-descriptions-item label="编译状态">
                                <el-tag :type="testGenResult.compilationSuccess ? 'success' : 'danger'">
                                    {{ testGenResult.compilationSuccess ? '编译成功' : '编译失败' }}
                                </el-tag>
                            </el-descriptions-item>
                            <el-descriptions-item label="测试通过">
                                <el-tag :type="testGenResult.testsPass ? 'success' : 'warning'">
                                    {{ testGenResult.testsPass ? '全部通过' : '部分失败' }}
                                </el-tag>
                            </el-descriptions-item>
                            <el-descriptions-item label="覆盖率">
                                <span>{{ testGenResult.coveragePercentage }}%</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="生成时间">
                                <span>{{ testGenResult.generationTime }}秒</span>
                            </el-descriptions-item>
                        </el-descriptions>
                        <div v-if="testGenResult.output" class="test-output">
                            <h5>执行输出：</h5>
                            <pre>{{ testGenResult.output }}</pre>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </div>
        </div>
    `,
    props: ['repositories'],
    data() {
        return {
            testGenForm: {
                repositoryId: '',
                branch: 'master',
                className: '',
                gateId: '',
                requirement: '',
                testType: 'mock',
                qualityLevel: 5
            },
            testBranches: [],
            testGenLoading: false,
            testGenProgress: {
                show: false,
                currentStep: 0,
                percentage: 0,
                status: '',
                message: ''
            },
            testGenResult: {
                show: false,
                taskId: '',
                testCode: '',
                testFileName: '',
                compilationSuccess: false,
                testsPass: false,
                coveragePercentage: 0,
                generationTime: 0,
                output: ''
            }
        };
    },
    methods: {
        async loadTestBranches() {
            if (!this.testGenForm.repositoryId) return;

            try {
                const response = await API.git.getBranches(this.testGenForm.repositoryId);
                this.testBranches = response.data.branches || [];
            } catch (error) {
                this.$message.error('加载分支列表失败');
            }
        },

        resetTestForm() {
            this.testGenForm = {
                repositoryId: '',
                branch: 'master',
                className: '',
                gateId: '',
                requirement: '',
                testType: 'mock',
                qualityLevel: 5
            };
            this.testGenProgress.show = false;
            this.testGenResult.show = false;
        },

        async startTestGeneration() {
            if (!this.testGenForm.repositoryId || !this.testGenForm.branch || !this.testGenForm.className) {
                this.$message.warning('请填写所有必填字段');
                return;
            }

            this.testGenLoading = true;
            this.testGenProgress.show = true;
            this.testGenProgress.currentStep = 0;
            this.testGenProgress.percentage = 0;
            this.testGenProgress.status = 'active';
            this.testGenProgress.message = '正在分析源码...';
            this.testGenResult.show = false;

            try {
                const response = await API.testGeneration.generate({
                    repositoryId: this.testGenForm.repositoryId,
                    branch: this.testGenForm.branch,
                    className: this.testGenForm.className,
                    gateId: this.testGenForm.gateId,
                    requirement: this.testGenForm.requirement,
                    testType: this.testGenForm.testType,
                    qualityLevel: this.testGenForm.qualityLevel
                });

                const taskId = response.data.taskId;
                await this.pollTaskStatus(taskId);
            } catch (error) {
                this.testGenProgress.status = 'exception';
                this.testGenProgress.message = '生成失败: ' + (error.response?.data?.message || error.message);
                this.$message.error('测试生成失败');
            } finally {
                this.testGenLoading = false;
            }
        },

        async pollTaskStatus(taskId) {
            const pollInterval = 2000;
            const maxPolls = 150;
            let pollCount = 0;

            const poll = async () => {
                if (pollCount >= maxPolls) {
                    this.testGenProgress.status = 'exception';
                    this.testGenProgress.message = '生成超时';
                    return;
                }

                try {
                    const response = await API.testGeneration.getStatus(taskId);
                    const status = response.data;

                    this.updateProgress(status);

                    if (status.status === 'COMPLETED') {
                        await this.loadFinalResult(taskId);
                        return;
                    } else if (status.status === 'FAILED') {
                        this.testGenProgress.status = 'exception';
                        this.testGenProgress.message = '生成失败: ' + status.message;
                        return;
                    }

                    pollCount++;
                    setTimeout(poll, pollInterval);
                } catch (error) {
                    this.testGenProgress.status = 'exception';
                    this.testGenProgress.message = '状态查询失败';
                }
            };

            await poll();
        },

        updateProgress(status) {
            const stepMap = {
                'ANALYZING': { step: 0, message: '正在分析Java类结构...', percentage: 25 },
                'GENERATING': { step: 1, message: '正在生成测试代码...', percentage: 50 },
                'COMPILING': { step: 2, message: '正在验证编译...', percentage: 75 },
                'TESTING': { step: 3, message: '正在执行测试...', percentage: 90 },
                'COMPLETED': { step: 4, message: '生成完成', percentage: 100 }
            };

            const progress = stepMap[status.status] || { step: 0, message: status.message, percentage: 0 };

            this.testGenProgress.currentStep = progress.step;
            this.testGenProgress.percentage = progress.percentage;
            this.testGenProgress.message = progress.message;

            if (status.status === 'COMPLETED') {
                this.testGenProgress.status = 'success';
            }
        },

        async loadFinalResult(taskId) {
            try {
                const response = await API.testGeneration.getResult(taskId);
                const result = response.data;

                this.testGenResult = {
                    show: true,
                    taskId: taskId,
                    testCode: result.testCode,
                    testFileName: result.testFileName,
                    compilationSuccess: result.compilationSuccess,
                    testsPass: result.testsPass,
                    coveragePercentage: result.coveragePercentage || 0,
                    generationTime: result.generationTime || 0,
                    output: result.output || ''
                };

                this.$message.success('测试代码生成完成！');
            } catch (error) {
                this.$message.error('获取结果失败');
            }
        },

        copyTestCode() {
            if (!this.testGenResult.testCode) return;
            Utils.copyToClipboard(this.testGenResult.testCode);
            this.$message.success('代码已复制到剪贴板');
        },

        downloadTestCode() {
            if (!this.testGenResult.testCode) return;
            Utils.downloadFile(this.testGenResult.testCode, this.testGenResult.testFileName || 'TestClass.java');
            this.$message.success('文件下载完成');
        }
    }
});
