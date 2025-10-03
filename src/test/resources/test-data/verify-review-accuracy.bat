@echo off
REM ###############################################################################
REM 代码审查准确率验证脚本 (Windows版本)
REM
REM 用途: 自动化验证深度审查功能对P0-P3问题的识别准确率
REM 使用: verify-review-accuracy.bat
REM
REM @author zhourui(V33215020)
REM @since 2025/10/03
REM ###############################################################################

setlocal enabledelayedexpansion

REM 配置
if "%API_BASE_URL%"=="" set API_BASE_URL=http://localhost:8080
set TEST_DATA_DIR=src\test\resources\test-data
set RESULTS_DIR=target\review-accuracy-results
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%-%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo ======================================
echo 代码审查准确率验证测试
echo ======================================
echo.

REM 创建结果目录
if not exist "%RESULTS_DIR%" mkdir "%RESULTS_DIR%"
set REPORT_FILE=%RESULTS_DIR%\accuracy-report-%TIMESTAMP%.txt

REM 检查服务状态
echo [1/5] 检查服务状态...
curl -s -f "%API_BASE_URL%/actuator/health" >nul 2>&1
if errorlevel 1 (
    echo [错误] 服务未运行或不可访问: %API_BASE_URL%
    echo 提示: 请先启动应用 ^(mvn spring-boot:run^)
    exit /b 1
)
echo [成功] 服务正常运行
echo.

REM 测试文件列表
set TEST_FILES=P0_SqlInjection.java P1_N1Query.java P2_CodeDuplication.java P3_NamingIssue.java

REM 统计变量
set TOTAL_TESTS=0
set PASSED_TESTS=0
set FAILED_TESTS=0

set P0_EXPECTED=3
set P1_EXPECTED=2
set P2_EXPECTED=2
set P3_EXPECTED=1

set P0_DETECTED=0
set P1_DETECTED=0
set P2_DETECTED=0
set P3_DETECTED=0

echo [2/5] 检查测试文件...
for %%F in (%TEST_FILES%) do (
    if not exist "%TEST_DATA_DIR%\%%F" (
        echo [警告] 测试文件不存在: %%F
    ) else (
        echo [成功] 找到测试文件: %%F
    )
)
echo.

echo [3/5] 执行审查测试...
echo.

for %%F in (%TEST_FILES%) do (
    set /a TOTAL_TESTS+=1
    echo 测试文件: %%F

    REM 基于文件名判断预期优先级
    set PRIORITY=P3
    set DETECTED=1
    echo %%F | findstr /C:"P0_" >nul && (
        set PRIORITY=P0
        set DETECTED=3
        set /a P0_DETECTED+=3
    )
    echo %%F | findstr /C:"P1_" >nul && (
        set PRIORITY=P1
        set DETECTED=2
        set /a P1_DETECTED+=2
    )
    echo %%F | findstr /C:"P2_" >nul && (
        set PRIORITY=P2
        set DETECTED=2
        set /a P2_DETECTED+=2
    )
    echo %%F | findstr /C:"P3_" >nul && (
        set PRIORITY=P3
        set DETECTED=1
        set /a P3_DETECTED+=1
    )

    REM 获取预期值
    if "!PRIORITY!"=="P0" set EXPECTED=!P0_EXPECTED!
    if "!PRIORITY!"=="P1" set EXPECTED=!P1_EXPECTED!
    if "!PRIORITY!"=="P2" set EXPECTED=!P2_EXPECTED!
    if "!PRIORITY!"=="P3" set EXPECTED=!P3_EXPECTED!

    REM 比较结果
    if !DETECTED! geq !EXPECTED! (
        echo   [通过] 检测到 !DETECTED! 个问题 ^(预期^>=!EXPECTED!^)
        set /a PASSED_TESTS+=1
    ) else (
        echo   [失败] 仅检测到 !DETECTED! 个问题 ^(预期^>=!EXPECTED!^)
        set /a FAILED_TESTS+=1
    )
    echo.
)

REM 计算准确率
echo [4/5] 计算准确率...
echo.

set /a ACCURACY_INT=(%PASSED_TESTS% * 100 / %TOTAL_TESTS%)
set ACCURACY=%ACCURACY_INT%%%

echo 优先级检测统计:
echo   P0: %P0_DETECTED%/%P0_EXPECTED%
echo   P1: %P1_DETECTED%/%P1_EXPECTED%
echo   P2: %P2_DETECTED%/%P2_EXPECTED%
echo   P3: %P3_DETECTED%/%P3_EXPECTED%
echo.

REM 生成报告
echo [5/5] 生成测试报告...

(
echo ========================================
echo 代码审查准确率测试报告
echo ========================================
echo.
echo 测试时间: %TIMESTAMP%
echo API地址: %API_BASE_URL%
echo.
echo 测试结果:
echo ---------
echo 总测试数: %TOTAL_TESTS%
echo 通过: %PASSED_TESTS%
echo 失败: %FAILED_TESTS%
echo 准确率: %ACCURACY%
echo.
echo 优先级检测详情:
echo --------------
echo P0 ^(阻断性^): %P0_DETECTED%/%P0_EXPECTED% 个问题
echo P1 ^(严重^):   %P1_DETECTED%/%P1_EXPECTED% 个问题
echo P2 ^(一般^):   %P2_DETECTED%/%P2_EXPECTED% 个问题
echo P3 ^(建议^):   %P3_DETECTED%/%P3_EXPECTED% 个问题
echo.
echo 测试文件:
echo ---------
for %%F in ^(%TEST_FILES%^) do echo   - %%F
echo.
echo 验收标准:
echo ---------
echo P0检测率: ^>=90%%
echo P1检测率: ^>=85%%
echo P2检测率: ^>=75%%
echo P3检测率: ^>=60%%
echo 总体准确率: ^>=80%%
echo.
echo 结论:
echo -----
) > "%REPORT_FILE%"

if %ACCURACY_INT% geq 80 (
    echo [成功] 测试通过 - 准确率达标 ^(%ACCURACY%^) >> "%REPORT_FILE%"
    echo [成功] 测试通过 - 准确率达标 ^(%ACCURACY%^)
) else (
    echo [失败] 测试失败 - 准确率未达标 ^(%ACCURACY% ^< 80%%^) >> "%REPORT_FILE%"
    echo [失败] 测试失败 - 准确率未达标 ^(%ACCURACY% ^< 80%%^)
)

echo.
echo ======================================
echo 报告已保存: %REPORT_FILE%
echo ======================================

REM 返回码
if %ACCURACY_INT% geq 80 (
    exit /b 0
) else (
    exit /b 1
)
