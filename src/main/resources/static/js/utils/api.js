const API = {
    repositories: {
        list: () => axios.get('/api/repositories'),
        create: (data) => axios.post('/api/repositories', data),
        delete: (id) => axios.delete(`/api/repositories/${id}`),
        testConnection: (id) => axios.post(`/api/repositories/${id}/test-connection`),
        getRemoteBranches: (id) => axios.get(`/api/repositories/${id}/remote-branches`)
    },

    git: {
        getBranches: (repoId) => axios.get(`/api/git/${repoId}/branches`)
    },

    review: {
        start: (repoId, params) => {
            const formData = new URLSearchParams();
            formData.append('baseBranch', params.baseBranch);
            formData.append('targetBranch', params.targetBranch);
            formData.append('mode', params.mode);
            return axios.post(`/api/review/${repoId}/claude`, formData, {
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });
        }
    },

    testGeneration: {
        generate: (data) => axios.post('/api/test-generation/generate', data),
        getStatus: (taskId) => axios.get(`/api/test-generation/status/${taskId}`),
        getResult: (taskId) => axios.get(`/api/test-generation/result/${taskId}`)
    },

    workflow: {
        list: () => axios.get('/api/workflow'),
        create: (data) => axios.post('/api/workflow', data),
        getStatus: (id) => axios.get(`/api/workflow/${id}/status`),
        cancel: (id, reason) => axios.post(`/api/workflow/${id}/cancel`, { reason }),

        spec: {
            get: (id) => axios.get(`/api/workflow/${id}/spec`),
            generate: (id, data) => axios.post(`/api/workflow/${id}/spec/generate`, data)
        },

        techDesign: {
            get: (id) => axios.get(`/api/workflow/${id}/tech-design`),
            generate: (id) => axios.post(`/api/workflow/${id}/tech-design/generate`),
            update: (id, content) => axios.put(`/api/workflow/${id}/tech-design`, { content }),
            approve: (id) => axios.post(`/api/workflow/${id}/tech-design/approve`)
        },

        taskList: {
            get: (id) => axios.get(`/api/workflow/${id}/tasklist`),
            generate: (id) => axios.post(`/api/workflow/${id}/tasklist/generate`)
        },

        codeGeneration: {
            start: (id) => axios.post(`/api/workflow/${id}/code-generation/start`)
        }
    }
};
