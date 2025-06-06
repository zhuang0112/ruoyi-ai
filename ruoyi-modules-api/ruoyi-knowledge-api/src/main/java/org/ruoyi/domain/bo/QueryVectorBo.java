package org.ruoyi.domain.bo;


import lombok.Data;

/**
 * 查询向量所需参数
 * @author ageer
 */
@Data
public class QueryVectorBo {

    /**
     * 查询内容
     */
    private String query;

    /**
     * 知识库kid
     */
    private String kid;

    /**
     * 查询向量返回条数
     */
    private Integer maxResults;

    /**
     * 向量库模型名称
     */
    private String vectorModelName;

    /**
     * 向量化模型名称
     */
    private String embeddingModelName;

    /**
     * 请求key
     */
    private String apiKey;

    /**
     * 请求地址
     */
    private String baseUrl;

}
