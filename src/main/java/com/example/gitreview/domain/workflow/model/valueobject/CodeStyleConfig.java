package com.example.gitreview.domain.workflow.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 代码风格配置值对象
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class CodeStyleConfig {

    private final String architecture;
    private final String codingStyle;
    private final String namingConvention;
    private final String commentLanguage;
    private final Integer maxMethodLines;
    private final Integer maxParameters;

    @JsonCreator
    public CodeStyleConfig(
            @JsonProperty("architecture") String architecture,
            @JsonProperty("codingStyle") String codingStyle,
            @JsonProperty("namingConvention") String namingConvention,
            @JsonProperty("commentLanguage") String commentLanguage,
            @JsonProperty("maxMethodLines") Integer maxMethodLines,
            @JsonProperty("maxParameters") Integer maxParameters) {
        this.architecture = architecture;
        this.codingStyle = codingStyle;
        this.namingConvention = namingConvention;
        this.commentLanguage = commentLanguage;
        this.maxMethodLines = maxMethodLines;
        this.maxParameters = maxParameters;
    }

    public static CodeStyleConfig createDefault() {
        return new CodeStyleConfig(
                "DDD 六边形架构（Domain/Application/Infrastructure）",
                "Alibaba-P3C 规范",
                "驼峰命名法",
                "中文",
                50,
                5
        );
    }

    public String buildPromptGuidelines() {
        StringBuilder guidelines = new StringBuilder();

        guidelines.append("架构要求：\n");
        guidelines.append("- ").append(architecture).append("\n\n");

        guidelines.append("代码风格：\n");
        guidelines.append("- ").append(codingStyle).append("\n");
        guidelines.append("- 命名规范: ").append(namingConvention).append("\n");
        guidelines.append("- 注释语言: ").append(commentLanguage).append("\n");
        guidelines.append("- 方法长度不超过 ").append(maxMethodLines).append(" 行\n");
        guidelines.append("- 参数不超过 ").append(maxParameters).append(" 个\n");

        return guidelines.toString();
    }

    public String getArchitecture() {
        return architecture;
    }

    public String getCodingStyle() {
        return codingStyle;
    }

    public String getNamingConvention() {
        return namingConvention;
    }

    public String getCommentLanguage() {
        return commentLanguage;
    }

    public Integer getMaxMethodLines() {
        return maxMethodLines;
    }

    public Integer getMaxParameters() {
        return maxParameters;
    }
}
