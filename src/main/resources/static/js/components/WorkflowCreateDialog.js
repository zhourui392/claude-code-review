Vue.component('workflow-create-dialog', {
    template: `
        <el-dialog title="创建AI辅助开发工作流" :visible.sync="dialogVisible" width="700px" @close="handleClose">
            <el-form :model="form" label-width="130px">
                <el-form-item label="工作流名称">
                    <el-input v-model="form.name" placeholder="例如：用户登录功能开发"></el-input>
                </el-form-item>
                <el-form-item label="选择仓库">
                    <el-select v-model="form.repositoryId" placeholder="请选择仓库" style="width: 100%">
                        <el-option v-for="repo in repositories"
                                   :key="repo.id"
                                   :label="repo.name"
                                   :value="repo.id">
                        </el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="PRD内容">
                    <el-input type="textarea" v-model="form.prdContent" :rows="6"
                              placeholder="请输入产品需求文档内容..."></el-input>
                </el-form-item>

                <el-divider>代码风格配置（可选）</el-divider>

                <el-form-item label="架构模式">
                    <el-select v-model="form.architecture" placeholder="默认：DDD 六边形架构" clearable style="width: 100%">
                        <el-option label="DDD 六边形架构（Domain/Application/Infrastructure）" value="DDD 六边形架构（Domain/Application/Infrastructure）"></el-option>
                        <el-option label="MVC 三层架构（Controller/Service/DAO）" value="MVC 三层架构（Controller/Service/DAO）"></el-option>
                        <el-option label="分层架构（Presentation/Business/Persistence）" value="分层架构（Presentation/Business/Persistence）"></el-option>
                        <el-option label="微服务架构（API Gateway + Service Mesh）" value="微服务架构（API Gateway + Service Mesh）"></el-option>
                        <el-option label="Clean Architecture（整洁架构）" value="Clean Architecture（整洁架构）"></el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="编码规范">
                    <el-select v-model="form.codingStyle" placeholder="默认：Alibaba-P3C" clearable style="width: 100%">
                        <el-option label="Alibaba-P3C 规范" value="Alibaba-P3C 规范"></el-option>
                        <el-option label="Google Java Style Guide" value="Google Java Style Guide"></el-option>
                        <el-option label="Spring Boot 最佳实践" value="Spring Boot 最佳实践"></el-option>
                        <el-option label="阿里巴巴微服务规范" value="阿里巴巴微服务规范"></el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="命名规范">
                    <el-select v-model="form.namingConvention" placeholder="默认：驼峰命名法" clearable style="width: 100%">
                        <el-option label="驼峰命名法（camelCase）" value="驼峰命名法"></el-option>
                        <el-option label="下划线命名法（snake_case）" value="下划线命名法"></el-option>
                    </el-select>
                </el-form-item>

                <el-form-item label="注释语言">
                    <el-select v-model="form.commentLanguage" placeholder="默认：中文" clearable style="width: 100%">
                        <el-option label="中文" value="中文"></el-option>
                        <el-option label="英文" value="英文"></el-option>
                    </el-select>
                </el-form-item>

                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="方法最大行数">
                            <el-input-number v-model="form.maxMethodLines" :min="10" :max="100" placeholder="默认：50"></el-input-number>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="参数最大个数">
                            <el-input-number v-model="form.maxParameters" :min="1" :max="10" placeholder="默认：5"></el-input-number>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <span slot="footer" class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="createWorkflow" :loading="loading">创建并生成规格文档</el-button>
            </span>
        </el-dialog>
    `,
    props: {
        visible: Boolean,
        repositories: Array
    },
    data() {
        return {
            dialogVisible: this.visible,
            loading: false,
            form: {
                name: '',
                repositoryId: '',
                prdContent: '',
                architecture: '',
                codingStyle: '',
                namingConvention: '',
                commentLanguage: '',
                maxMethodLines: null,
                maxParameters: null
            }
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
    methods: {
        async createWorkflow() {
            if (!this.form.name || !this.form.repositoryId || !this.form.prdContent) {
                this.$message.warning('请填写所有必填项');
                return;
            }

            this.loading = true;
            try {
                const createPayload = {
                    name: this.form.name,
                    repositoryId: this.form.repositoryId,
                    createdBy: 'user'
                };

                if (this.form.architecture) createPayload.architecture = this.form.architecture;
                if (this.form.codingStyle) createPayload.codingStyle = this.form.codingStyle;
                if (this.form.namingConvention) createPayload.namingConvention = this.form.namingConvention;
                if (this.form.commentLanguage) createPayload.commentLanguage = this.form.commentLanguage;
                if (this.form.maxMethodLines) createPayload.maxMethodLines = this.form.maxMethodLines;
                if (this.form.maxParameters) createPayload.maxParameters = this.form.maxParameters;

                const createRes = await API.workflow.create(createPayload);
                const workflowId = createRes.data.workflowId;

                await API.workflow.spec.generate(workflowId, {
                    prdContent: this.form.prdContent,
                    documentPaths: []
                });

                this.$message.success('工作流创建成功，正在生成规格文档...');
                this.dialogVisible = false;
                this.resetForm();
                this.$emit('created', workflowId);
            } catch (error) {
                this.$message.error('创建工作流失败: ' + (error.response?.data?.message || error.message));
            } finally {
                this.loading = false;
            }
        },

        resetForm() {
            this.form = {
                name: '',
                repositoryId: '',
                prdContent: '',
                architecture: '',
                codingStyle: '',
                namingConvention: '',
                commentLanguage: '',
                maxMethodLines: null,
                maxParameters: null
            };
        },

        handleClose() {
            this.resetForm();
        }
    }
});
