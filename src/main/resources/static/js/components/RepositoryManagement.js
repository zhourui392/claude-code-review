Vue.component('repository-management', {
    template: `
        <div class="section">
            <h3>Git仓库配置</h3>
            <el-button type="primary" @click="showAddDialog = true">添加仓库</el-button>

            <el-table :data="repositories" style="width: 100%; margin-top: 20px;">
                <el-table-column prop="name" label="仓库名称"></el-table-column>
                <el-table-column prop="repositoryUrl" label="仓库地址"></el-table-column>
                <el-table-column prop="description" label="描述"></el-table-column>
                <el-table-column label="操作" width="200">
                    <template slot-scope="scope">
                        <el-button size="mini" @click="testConnection(scope.row)">测试连接</el-button>
                        <el-button size="mini" type="danger" @click="deleteRepository(scope.row.id)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-dialog title="添加Git仓库" :visible.sync="showAddDialog" width="500px">
                <el-form :model="newRepository" label-width="100px">
                    <el-form-item label="仓库名称">
                        <el-input v-model="newRepository.name"></el-input>
                    </el-form-item>
                    <el-form-item label="仓库地址">
                        <el-input v-model="newRepository.url"></el-input>
                    </el-form-item>
                    <el-form-item label="用户名">
                        <el-input v-model="newRepository.username"></el-input>
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input v-model="newRepository.password" type="password"></el-input>
                    </el-form-item>
                    <el-form-item label="描述">
                        <el-input v-model="newRepository.description" type="textarea"></el-input>
                    </el-form-item>
                </el-form>
                <span slot="footer" class="dialog-footer">
                    <el-button @click="showAddDialog = false">取消</el-button>
                    <el-button type="primary" @click="addRepository">确定</el-button>
                </span>
            </el-dialog>
        </div>
    `,
    props: ['repositories'],
    data() {
        return {
            showAddDialog: false,
            newRepository: {
                name: '',
                url: '',
                username: '',
                password: '',
                description: ''
            }
        };
    },
    methods: {
        async addRepository() {
            try {
                await API.repositories.create(this.newRepository);
                this.showAddDialog = false;
                this.newRepository = { name: '', url: '', username: '', password: '', description: '' };
                this.$emit('refresh');
                this.$message.success('仓库添加成功');
            } catch (error) {
                this.$message.error('添加仓库失败: ' + (error.response?.data?.message || error.message));
            }
        },

        async testConnection(repository) {
            try {
                await API.repositories.testConnection(repository.id);
                this.$message.success('连接测试成功');
            } catch (error) {
                this.$message.error('连接测试失败');
            }
        },

        async deleteRepository(id) {
            try {
                await API.repositories.delete(id);
                this.$emit('refresh');
                this.$message.success('删除成功');
            } catch (error) {
                this.$message.error('删除失败');
            }
        }
    }
});
