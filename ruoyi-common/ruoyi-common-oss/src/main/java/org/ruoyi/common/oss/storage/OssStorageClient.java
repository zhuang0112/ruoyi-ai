package org.ruoyi.common.oss.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.oss.properties.OssProperties;
import org.ruoyi.common.oss.util.FilePathUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @Description: Oss上传策略实现类
 * @date: 2023/11/19
 * @author: mrzhuang
 */

@Slf4j
@Getter
@Setter
// @RequiredArgsConstructor
// @Service("ossServiceImpl")
public class OssStorageClient extends AbstractStorageClient {

    /**
     * 构造器注入bean
     */
    private OssProperties properties;

    public OssStorageClient(OssProperties properties) {
        this.properties = properties;
        initClient();
    }

    /**
     * 当前类的属性
     */
    private OSS ossClient;


    @Override
    public void initClient() {
        if (Objects.isNull(properties) || Objects.isNull(properties.getEndpoint())){
            return;
        }
        ossClient = new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKey(), properties.getSecretKey());
        log.info("OssClient Init Success...");
    }

    @Override
    public boolean checkFileIsExisted(String fileRelativePath) {
        return ossClient.doesObjectExist(properties.getBucketName(), fileRelativePath);
    }

    @Override
    public void executeUpload(MultipartFile file, String fileRelativePath) {
        log.info("File Upload Starts...");
        try {
            ossClient.putObject(properties.getBucketName(), fileRelativePath, file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("File Upload Finish...");
    }

    @Override
    public String getPublicNetworkAccessUrl(String fileRelativePath) {
        return FilePathUtil.fixFilePathSlash(properties.getDomain()) + fileRelativePath;
    }

    @Override
    public InputStream executeDownload(String fileRelativePath) {
        OSSObject object = ossClient.getObject(properties.getBucketName(), fileRelativePath);
        return object.getObjectContent();
    }

    @Override
    public void executeDelete(String fileRelativePath) {
        ossClient.deleteObject(properties.getBucketName(), fileRelativePath);
    }

    @Override
    public String getFileRelativePathFromUrl(String url) {
        return url.replace(FilePathUtil.fixFilePathSlash(properties.getDomain()), "");
    }

    @Override
    public void createBucket(String bucketName) {
        if (!ossClient.doesBucketExist(bucketName)) {
            ossClient.createBucket(bucketName);
        }
    }
}
