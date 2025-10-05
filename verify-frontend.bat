@echo off
echo ========================================
echo 前端重构验证脚本
echo ========================================
echo.

echo [1/4] 检查HTTP服务器...
curl -s -o nul -w "HTTP状态码: %%{http_code}\n" http://localhost:9090/index.html
if %errorlevel% neq 0 (
    echo ❌ HTTP服务器未启动
    exit /b 1
)

echo.
echo [2/4] 检查静态资源文件...
curl -s -o nul -w "css/styles.css: %%{http_code}\n" http://localhost:9090/css/styles.css
curl -s -o nul -w "js/utils/api.js: %%{http_code}\n" http://localhost:9090/js/utils/api.js
curl -s -o nul -w "js/utils/common.js: %%{http_code}\n" http://localhost:9090/js/utils/common.js
curl -s -o nul -w "js/components/RepositoryManagement.js: %%{http_code}\n" http://localhost:9090/js/components/RepositoryManagement.js
curl -s -o nul -w "js/components/CodeReview.js: %%{http_code}\n" http://localhost:9090/js/components/CodeReview.js
curl -s -o nul -w "js/components/TestGeneration.js: %%{http_code}\n" http://localhost:9090/js/components/TestGeneration.js
curl -s -o nul -w "js/components/WorkflowManagement.js: %%{http_code}\n" http://localhost:9090/js/components/WorkflowManagement.js
curl -s -o nul -w "js/components/WorkflowCreateDialog.js: %%{http_code}\n" http://localhost:9090/js/components/WorkflowCreateDialog.js
curl -s -o nul -w "js/components/WorkflowDetailDialog.js: %%{http_code}\n" http://localhost:9090/js/components/WorkflowDetailDialog.js

echo.
echo [3/4] 检查文件大小...
dir /s "D:\agent_workspace\claude-code-review\src\main\resources\static\js\components" | find "个文件"

echo.
echo [4/4] 对比文件行数...
echo 旧版本行数:
find /c /v "" "D:\agent_workspace\claude-code-review\src\main\resources\static\index-old.html"
echo 新版本行数:
find /c /v "" "D:\agent_workspace\claude-code-review\src\main\resources\static\index.html"

echo.
echo ========================================
echo ✅ 验证完成！请打开浏览器访问:
echo    http://localhost:9090/index.html
echo ========================================
pause
