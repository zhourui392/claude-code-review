package com.example.gitreview.infrastructure.git.adapter;

import com.example.gitreview.infrastructure.git.GitOperationPort;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JGit仓库操作适配器
 * 实现Git仓库相关操作，包括克隆、分支管理、差异分析等
 */
@Component
public class JGitRepositoryAdapter implements GitOperationPort {

    private static final Logger logger = LoggerFactory.getLogger(JGitRepositoryAdapter.class);

    @Override
    public File cloneRepository(String repositoryUrl, String username, String password, String branch)
            throws GitAPIException, IOException {
        Path tempDir = Files.createTempDirectory("git-review-");
        File localDir = tempDir.toFile();

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(
            username, password);

        logger.info("Cloning repository {} with branch: {}", repositoryUrl, branch);

        try (Git git = Git.cloneRepository()
            .setURI(repositoryUrl)
            .setDirectory(localDir)
            .setBranch(branch)
            .setCloneAllBranches(true)
            .setCredentialsProvider(credentials)
            .call()) {

            // 打印可用分支用于调试
            logger.debug("Available branches after clone:");
            git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
                .forEach(ref -> logger.debug("  {}", ref.getName()));

            // 检出目标分支
            if (!branch.equals("master") && !branch.equals("main")) {
                try {
                    git.checkout().setName(branch).call();
                    logger.info("Checked out branch: {}", branch);
                } catch (Exception e) {
                    logger.warn("Could not checkout branch {}: {}", branch, e.getMessage());
                }
            }
        }

        return localDir;
    }

    @Override
    public List<String> getBranches(File repositoryDir) throws GitAPIException, IOException {
        try (Git git = Git.open(repositoryDir)) {
            return git.branchList()
                .call()
                .stream()
                .map(ref -> ref.getName().replace("refs/heads/", ""))
                .collect(java.util.stream.Collectors.toList());
        }
    }

    @Override
    public List<String> getRemoteBranches(String repositoryUrl, String username, String password)
            throws GitAPIException, IOException {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(
            username, password);

        logger.info("Fetching remote branches from: {}", repositoryUrl);

        // 使用ls-remote获取分支，避免完整克隆
        try {
            java.util.Collection<org.eclipse.jgit.lib.Ref> refs = Git.lsRemoteRepository()
                .setHeads(true)
                .setTags(false)
                .setRemote(repositoryUrl)
                .setCredentialsProvider(credentials)
                .setTimeout(30) // 设置30秒超时
                .call();

            List<String> branches = refs.stream()
                .map(ref -> ref.getName())
                .map(name -> name.replace("refs/heads/", ""))
                .filter(branch -> !branch.isEmpty())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            logger.info("Successfully fetched {} remote branches", branches.size());
            logger.debug("Remote branches: {}", branches);
            return branches;

        } catch (Exception e) {
            logger.error("Failed to fetch remote branches using ls-remote: {}", e.getMessage());
            // 回退到克隆方式
            return getRemoteBranchesByClone(repositoryUrl, username, password);
        }
    }

    @Override
    public List<DiffEntry> getDiffBetweenBranches(File repositoryDir, String baseBranch, String targetBranch)
            throws IOException, GitAPIException {

        try (Git git = Git.open(repositoryDir)) {
            Repository repository = git.getRepository();

            logger.info("Getting diff between branches: {} -> {}", baseBranch, targetBranch);

            // 获取两个分支的提交
            RevCommit baseCommit = null;
            RevCommit targetCommit = null;

            try {
                baseCommit = getBranchCommit(repository, baseBranch);
                logger.info("Base branch '{}' commit: {}", baseBranch, baseCommit.getId().getName());
            } catch (IOException e) {
                logger.warn("Base branch '{}' not found: {}", baseBranch, e.getMessage());
                // 如果基础分支不存在，使用HEAD
                baseCommit = getBranchCommit(repository, "HEAD");
                logger.info("Using HEAD as base instead: {}", baseCommit.getId().getName());
            }

            try {
                targetCommit = getBranchCommit(repository, targetBranch);
                logger.info("Target branch '{}' commit: {}", targetBranch, targetCommit.getId().getName());
            } catch (IOException e) {
                throw new IOException("Target branch '" + targetBranch + "' not found: " + e.getMessage());
            }

            // 如果两个提交相同，返回空差异
            if (baseCommit.getId().equals(targetCommit.getId())) {
                logger.info("Base and target commits are identical, no changes to review");
                return new ArrayList<>();
            }

            // 准备树解析器用于差异比较
            try (ObjectReader reader = repository.newObjectReader()) {
                CanonicalTreeParser baseTree = new CanonicalTreeParser();
                baseTree.reset(reader, baseCommit.getTree());

                CanonicalTreeParser targetTree = new CanonicalTreeParser();
                targetTree.reset(reader, targetCommit.getTree());

                // 获取差异
                List<DiffEntry> diffs = git.diff()
                    .setOldTree(baseTree)
                    .setNewTree(targetTree)
                    .call();

                logger.info("Generated {} diff entries", diffs.size());
                return diffs;
            }
        }
    }

    @Override
    public String getDiffContent(File repositoryDir, DiffEntry diffEntry) throws IOException {
        try (Git git = Git.open(repositoryDir);
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             DiffFormatter formatter = new DiffFormatter(output)) {

            formatter.setRepository(git.getRepository());
            formatter.format(diffEntry);
            return output.toString();
        }
    }

    // 私有辅助方法
    private List<String> getRemoteBranchesByClone(String repositoryUrl, String username, String password)
            throws GitAPIException, IOException {
        Path tempDir = Files.createTempDirectory("git-remote-");
        File localDir = tempDir.toFile();

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(
            username, password);

        try (Git git = Git.cloneRepository()
            .setURI(repositoryUrl)
            .setDirectory(localDir)
            .setCloneAllBranches(true)
            .setCredentialsProvider(credentials)
            .call()) {

            List<String> branches = git.branchList()
                .setListMode(ListBranchCommand.ListMode.ALL)
                .call()
                .stream()
                .map(ref -> {
                    String name = ref.getName();
                    if (name.startsWith("refs/remotes/origin/")) {
                        return name.replace("refs/remotes/origin/", "");
                    } else if (name.startsWith("refs/heads/")) {
                        return name.replace("refs/heads/", "");
                    }
                    return name;
                })
                .filter(branch -> !branch.equals("HEAD") && !branch.isEmpty())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            logger.info("Found branches by clone: {}", branches);
            return branches;

        } finally {
            deleteTempDirectory(localDir);
        }
    }

    private RevCommit getBranchCommit(Repository repository, String branchName) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            // 尝试不同的分支引用模式
            String[] refPatterns = {
                "refs/heads/" + branchName,
                "refs/remotes/origin/" + branchName,
                branchName,
                "origin/" + branchName
            };

            for (String refPattern : refPatterns) {
                org.eclipse.jgit.lib.ObjectId objectId = repository.resolve(refPattern);
                if (objectId != null) {
                    logger.debug("Found branch reference: {} -> {}", refPattern, objectId.getName());
                    return walk.parseCommit(objectId);
                }
            }

            // 如果没有找到引用，抛出描述性异常
            throw new IOException("Branch '" + branchName + "' not found. Available references: " +
                getAllRefs(repository));
        }
    }

    private String getAllRefs(Repository repository) {
        try {
            List<org.eclipse.jgit.lib.Ref> refs = repository.getRefDatabase().getRefs();
            List<String> refNames = new ArrayList<>();
            for (org.eclipse.jgit.lib.Ref ref : refs) {
                refNames.add(ref.getName());
            }
            return refNames.toString();
        } catch (IOException e) {
            return "Unable to list refs: " + e.getMessage();
        }
    }

    private void deleteTempDirectory(File directory) {
        if (directory != null && directory.exists()) {
            deleteRecursively(directory);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    // 以下方法将在后续的适配器中实现，这里提供基本实现
    @Override
    public List<File> findClassFiles(File repositoryDir, String className) throws IOException {
        logger.info("Searching for class files: {} in repository: {}", className, repositoryDir.getAbsolutePath());

        List<File> foundFiles = new ArrayList<>();
        String simpleClassName = extractSimpleClassName(className);

        // 搜索 .java 文件
        findJavaFiles(repositoryDir, simpleClassName, foundFiles);

        logger.info("Found {} files for class: {}", foundFiles.size(), className);
        return foundFiles;
    }

    @Override
    public File findClassFileByPackage(File repositoryDir, String packageName, String className) throws IOException {
        logger.info("Searching for class: {}.{} in repository: {}", packageName, className, repositoryDir.getAbsolutePath());

        // 构建期望的文件路径
        String expectedPath = packageName.replace(".", File.separator) + File.separator + className + ".java";

        // 在常见的源码目录中查找
        String[] sourceDirs = {"src/main/java", "src", "java", ""};

        for (String sourceDir : sourceDirs) {
            File searchDir = sourceDir.isEmpty() ? repositoryDir : new File(repositoryDir, sourceDir);
            if (searchDir.exists() && searchDir.isDirectory()) {
                File classFile = new File(searchDir, expectedPath);
                if (classFile.exists() && classFile.isFile()) {
                    logger.info("Found class file: {}", classFile.getAbsolutePath());
                    return classFile;
                }
            }
        }

        logger.warn("Class file not found for: {}.{}", packageName, className);
        return null;
    }

    @Override
    public String readFileContent(File file) throws IOException {
        logger.info("Reading file content: {}", file.getAbsolutePath());

        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            logger.debug("Successfully read file content. Length: {} characters", content.length());
            return content;
        } catch (Exception e) {
            logger.error("Error reading file: {}", file.getAbsolutePath(), e);
            throw new IOException("Failed to read file: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public List<File> getAllJavaFiles(File repositoryDir) throws IOException {
        logger.info("Getting all Java files from repository: {}", repositoryDir.getAbsolutePath());

        List<File> javaFiles = new ArrayList<>();
        findAllJavaFiles(repositoryDir, javaFiles);

        logger.info("Found {} Java files in repository", javaFiles.size());
        return javaFiles;
    }

    @Override
    public List<File> findFilesByPattern(File repositoryDir, String pathPattern) throws IOException {
        logger.info("Searching for files with pattern: {} in repository: {}", pathPattern, repositoryDir.getAbsolutePath());

        List<File> matchingFiles = new ArrayList<>();

        try {
            Path repoPath = repositoryDir.toPath();
            Files.walk(repoPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String relativePath = repoPath.relativize(path).toString();
                    return matchesPattern(relativePath, pathPattern);
                })
                .forEach(path -> matchingFiles.add(path.toFile()));
        } catch (IOException e) {
            logger.error("Error searching for files with pattern: {}", pathPattern, e);
            throw e;
        }

        logger.info("Found {} files matching pattern: {}", matchingFiles.size(), pathPattern);
        return matchingFiles;
    }

    @Override
    public String getRelativePath(File repositoryDir, File file) {
        Path repoPath = repositoryDir.toPath().toAbsolutePath();
        Path filePath = file.toPath().toAbsolutePath();

        try {
            return repoPath.relativize(filePath).toString();
        } catch (Exception e) {
            logger.warn("Could not get relative path for file: {}", file.getAbsolutePath(), e);
            return file.getName();
        }
    }

    // 私有辅助方法
    private String extractSimpleClassName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }

    private void findJavaFiles(File directory, String className, List<File> foundFiles) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 跳过常见的非源码目录
                String dirName = file.getName();
                if (!dirName.equals(".git") && !dirName.equals("target") &&
                    !dirName.equals("build") && !dirName.equals("node_modules")) {
                    findJavaFiles(file, className, foundFiles);
                }
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                // 检查文件名是否匹配
                String fileName = file.getName();
                String fileClassName = fileName.substring(0, fileName.length() - 5); // 移除.java后缀

                if (fileClassName.equals(className) ||
                    fileName.toLowerCase().contains(className.toLowerCase())) {
                    foundFiles.add(file);
                }
            }
        }
    }

    private void findAllJavaFiles(File directory, List<File> javaFiles) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                if (!dirName.equals(".git") && !dirName.equals("target") &&
                    !dirName.equals("build") && !dirName.equals("node_modules")) {
                    findAllJavaFiles(file, javaFiles);
                }
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }

    private boolean matchesPattern(String path, String pattern) {
        // 简单的通配符匹配实现
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*").replace("?", ".");
            return path.matches(regex);
        } else {
            return path.contains(pattern);
        }
    }
}