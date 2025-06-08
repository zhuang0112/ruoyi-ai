package org.ruoyi.common.oss.core;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
* @Description: 文件上传策略
* @date: 2023/11/19
* @author: mrzhuang
*/

public interface StorageClient {

    /**
     * 上传文件
     *
     * @param file        文件
     * @return {@link String} 文件上传的全路径
     */
    String uploadFile(MultipartFile file);

    /**
     * 上传文件(带文件相对路径)
     *
     * @param file        文件
     * @return {@link String} 文件上传的全路径
     */
    String uploadFile(MultipartFile file, String fileRelativePath);

    /**
     * 下载文件
     *
     * @param fileRelativePath 文件相对桶的文件路径
     */
    InputStream downloadFile(String fileRelativePath);

    /**
     * 删除文件
     *
     * @param fileRelativePath 文件相对桶的文件路径
     */
    void deleteFile(String fileRelativePath);


}
