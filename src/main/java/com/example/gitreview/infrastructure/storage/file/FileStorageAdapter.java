package com.example.gitreview.infrastructure.storage.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文件存储适配器
 * 提供文件读写操作的统一接口
 */
@Component
public class FileStorageAdapter {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageAdapter.class);

    /**
     * 写入文件内容
     * @param filePath 文件路径
     * @param content 文件内容
     * @throws IOException IO异常
     */
    public void writeFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);

        // 确保父目录存在
        Files.createDirectories(path.getParent());

        // 写入文件
        Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        logger.debug("Written file: {}, size: {} bytes", filePath, content.length());
    }

    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException IO异常
     */
    public String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        String content = Files.readString(path, StandardCharsets.UTF_8);
        logger.debug("Read file: {}, size: {} bytes", filePath, content.length());

        return content;
    }

    /**
     * 追加内容到文件
     * @param filePath 文件路径
     * @param content 追加内容
     * @throws IOException IO异常
     */
    public void appendFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);

        // 确保父目录存在
        Files.createDirectories(path.getParent());

        // 追加内容
        Files.write(path, content.getBytes(StandardCharsets.UTF_8),
                   StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        logger.debug("Appended to file: {}, size: {} bytes", filePath, content.length());
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                logger.debug("Deleted file: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 是否存在
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 获取文件大小
     * @param filePath 文件路径
     * @return 文件大小（字节）
     * @throws IOException IO异常
     */
    public long getFileSize(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.size(path);
    }

    /**
     * 读取文件所有行
     * @param filePath 文件路径
     * @return 行列表
     * @throws IOException IO异常
     */
    public List<String> readLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        logger.debug("Read {} lines from file: {}", lines.size(), filePath);

        return lines;
    }

    /**
     * 写入文件行
     * @param filePath 文件路径
     * @param lines 行列表
     * @throws IOException IO异常
     */
    public void writeLines(String filePath, List<String> lines) throws IOException {
        Path path = Paths.get(filePath);

        // 确保父目录存在
        Files.createDirectories(path.getParent());

        // 写入行
        Files.write(path, lines, StandardCharsets.UTF_8,
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        logger.debug("Written {} lines to file: {}", lines.size(), filePath);
    }

    /**
     * 创建临时文件
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件路径
     * @throws IOException IO异常
     */
    public String createTempFile(String prefix, String suffix) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        String filePath = tempFile.toString();
        logger.debug("Created temp file: {}", filePath);
        return filePath;
    }

    /**
     * 创建带时间戳的文件
     * @param directory 目录
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 文件路径
     */
    public String createTimestampedFile(String directory, String prefix, String suffix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = prefix + "_" + timestamp + suffix;
        return Paths.get(directory, fileName).toString();
    }

    /**
     * 确保目录存在
     * @param directoryPath 目录路径
     * @throws IOException IO异常
     */
    public void ensureDirectoryExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        Files.createDirectories(path);
        logger.debug("Ensured directory exists: {}", directoryPath);
    }

    /**
     * 复制文件
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @throws IOException IO异常
     */
    public void copyFile(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);

        // 确保目标目录存在
        Files.createDirectories(target.getParent());

        // 复制文件
        Files.copy(source, target);
        logger.debug("Copied file from {} to {}", sourcePath, targetPath);
    }
}