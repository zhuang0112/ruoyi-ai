package org.ruoyi.common.oss.storage;

import cn.hutool.core.map.MapUtil;
import io.minio.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.oss.properties.OssProperties;
import org.ruoyi.common.oss.util.FilePathUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: minio上传策略实现类
 * @date: 2023/11/19
 * @author: mrzhuang
 */

@Slf4j
@Getter
@Setter
// @RequiredArgsConstructor
// @Service("minioServiceImpl")
public class MinioStorageClient extends AbstractStorageClient {

    private OssProperties properties;

    /**
     * 构造器注入
     */
    public MinioStorageClient(OssProperties properties) {
        this.properties = properties;
        initClient();
    }

    /**
     * minio客户端
     */
    private MinioClient minioClient;

    @Override
    public void initClient() {
        if (Objects.isNull(properties) || Objects.isNull(properties.getEndpoint())) {
            return;
        }
        minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        log.info("MinioClient Init Success...");
    }

    @Override
    public boolean checkFileIsExisted(String fileRelativePath) {
        boolean exist = true;
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(properties.getBucketName()).object(fileRelativePath).build());
        } catch (Exception e) {
            exist = false;
        }
        return exist;
    }

    @Override
    public void executeUpload(MultipartFile file, String fileRelativePath) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new RuntimeException();
        }
        try {
            // 支持读取zip中的文件
            Map<String, String> extraHeader = MapUtil.builder("x-minio-extract", "true").build();
            PutObjectArgs objectArgs = PutObjectArgs.builder().bucket(properties.getBucketName()).object(fileRelativePath)
                    .stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).extraHeaders(extraHeader).build();
            // 文件名称相同会覆盖
            minioClient.putObject(objectArgs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPublicNetworkAccessUrl(String fileRelativePath) {
        return FilePathUtil.fixFilePathSlash(properties.getDomain()) + FilePathUtil.fixFilePathSlash(properties.getBucketName())+ fileRelativePath;
    }

    @Override
    public InputStream executeDownload(String fileRelativePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(properties.getBucketName()).object(fileRelativePath).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeDelete(String fileRelativePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(properties.getBucketName()).object(fileRelativePath).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFileRelativePathFromUrl(String url) {
        return url.replace(FilePathUtil.fixFilePathSlash(properties.getDomain()) + FilePathUtil.fixFilePathSlash(properties.getBucketName()) , "");
    }

    /**
     * 查看存储bucket是否存在
     *
     * @return boolean
     */
    public Boolean bucketExists(String bucketName) {
        boolean found;
        try {
            found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return found;
    }

    /**
     * 创建存储bucket
     *
     * @return Boolean
     */
    public Boolean makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            makeBucket(bucketName);
        }
    }
}
