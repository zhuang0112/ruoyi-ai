package org.ruoyi.common.oss.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;
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
 * @Description: 腾讯云Cos文件上传策略实现类
 * @date: 2023/11/19
 * @author: mrzhuang
 */

@Slf4j
@Getter
@Setter
// @RequiredArgsConstructor
// @Service("cosServiceImpl")
public class CosStorageClient extends AbstractStorageClient {

    /**
     * 构造器注入
     */
    private OssProperties properties;

    /**
     * 属性
     */
    private COSClient cosClient;

    public CosStorageClient(OssProperties properties) {
        this.properties = properties;
        initClient();
    }


    @Override
    public void initClient() {
        if (Objects.isNull(properties) || Objects.isNull(properties.getEndpoint())) {
            return;
        }
        COSCredentials cred = new BasicCOSCredentials(properties.getAccessKey(), properties.getSecretKey());
        // region ClientConfig 中包含了后续请求 COS 的客户端设置：
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(new Region(properties.getEndpoint()));
        clientConfig.setHttpProtocol(HttpProtocol.http);
        clientConfig.setSocketTimeout(30 * 1000);
        clientConfig.setConnectionTimeout(30 * 1000);
        // endregion

        // 生成 cos 客户端
        cosClient = new COSClient(cred, clientConfig);
        log.info("CosClient Init Success...");
    }

    @Override
    public boolean checkFileIsExisted(String fileRelativePath) {
        return cosClient.doesObjectExist(properties.getBucketName(), fileRelativePath);
    }

    @Override
    public void executeUpload(MultipartFile file, String fileRelativePath) {
        log.info("File Upload Starts...");
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            cosClient.putObject(properties.getBucketName(), fileRelativePath, file.getInputStream(), metadata);
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
        COSObject object = cosClient.getObject(properties.getBucketName(), fileRelativePath);
        return object.getObjectContent();
    }

    @Override
    public void executeDelete(String fileRelativePath) {
        cosClient.deleteObject(properties.getBucketName(), fileRelativePath);
    }

    @Override
    public void createBucket(String bucketName) {
        if (!cosClient.doesBucketExist(bucketName)){
            cosClient.createBucket(bucketName);
        }
    }

    @Override
    public String getFileRelativePathFromUrl(String url) {
        return url.replace(FilePathUtil.fixFilePathSlash(properties.getDomain()), "");
    }
}
