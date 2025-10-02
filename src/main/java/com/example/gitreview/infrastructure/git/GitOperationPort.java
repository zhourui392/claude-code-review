package com.example.gitreview.infrastructure.git;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Git操作端口接口
 * 定义Git相关操作的抽象接口，支持适配器模式
 */
public interface GitOperationPort {

    /**
     * 克隆仓库到本地
     * @param repositoryUrl 仓库URL
     * @param username 用户名
     * @param password 密码
     * @param branch 分支名
     * @return 本地仓库目录
     */
    File cloneRepository(String repositoryUrl, String username, String password, String branch)
            throws GitAPIException, IOException;

    /**
     * 获取分支列表
     * @param repositoryDir 仓库目录
     * @return 分支列表
     */
    List<String> getBranches(File repositoryDir) throws GitAPIException, IOException;

    /**
     * 获取远程分支列表
     * @param repositoryUrl 仓库URL
     * @param username 用户名
     * @param password 密码
     * @return 远程分支列表
     */
    List<String> getRemoteBranches(String repositoryUrl, String username, String password)
            throws GitAPIException, IOException;

    /**
     * 获取分支间差异
     * @param repositoryDir 仓库目录
     * @param baseBranch 基础分支
     * @param targetBranch 目标分支
     * @return 差异条目列表
     */
    List<DiffEntry> getDiffBetweenBranches(File repositoryDir, String baseBranch, String targetBranch)
            throws IOException, GitAPIException;

    /**
     * 获取差异内容
     * @param repositoryDir 仓库目录
     * @param diffEntry 差异条目
     * @return 差异内容字符串
     */
    String getDiffContent(File repositoryDir, DiffEntry diffEntry) throws IOException;

    /**
     * 查找指定类的源码文件
     * @param repositoryDir 仓库目录
     * @param className 类名
     * @return 找到的文件列表
     */
    List<File> findClassFiles(File repositoryDir, String className) throws IOException;

    /**
     * 根据包名和类名查找文件
     * @param repositoryDir 仓库目录
     * @param packageName 包名
     * @param className 类名
     * @return 文件（如果找到）
     */
    File findClassFileByPackage(File repositoryDir, String packageName, String className) throws IOException;

    /**
     * 读取文件内容
     * @param file 文件
     * @return 文件内容
     */
    String readFileContent(File file) throws IOException;

    /**
     * 获取仓库中所有Java文件
     * @param repositoryDir 仓库目录
     * @return Java文件列表
     */
    List<File> getAllJavaFiles(File repositoryDir) throws IOException;

    /**
     * 根据路径模式查找文件
     * @param repositoryDir 仓库目录
     * @param pathPattern 路径模式
     * @return 匹配的文件列表
     */
    List<File> findFilesByPattern(File repositoryDir, String pathPattern) throws IOException;

    /**
     * 获取文件的相对路径
     * @param repositoryDir 仓库目录
     * @param file 文件
     * @return 相对路径
     */
    String getRelativePath(File repositoryDir, File file);
}