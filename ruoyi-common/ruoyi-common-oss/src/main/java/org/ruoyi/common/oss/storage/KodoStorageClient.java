package org.ruoyi.common.oss.storage;

import cn.hutool.http.HttpRequest;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.oss.properties.OssProperties;
import org.ruoyi.common.oss.util.FilePathUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;

/**
 * @Description: 七牛云Kodo上传策略实现类
 * @date: 2023/11/19
 * @author: mrzhuang
 */

@Slf4j
@Getter
@Setter
// @RequiredArgsConstructor
// @Service("kodoServiceImpl")
public class KodoStorageClient extends AbstractStorageClient {

    /**
     * 构造器注入Bean
     */
    private OssProperties properties;

    public KodoStorageClient(OssProperties properties) {
            this.properties = properties;
            initClient();
    }

    /**
     * upToken
     */
    private String upToken;

    /**
     * 上传Manger
     */
    private UploadManager uploadManager;

    /**
     * 存储桶Manger
     */
    private BucketManager bucketManager;

    Auth auth;


    @Override
    public void initClient() {
        if (Objects.isNull(properties) || Objects.isNull(properties.getEndpoint())){
            return;
        }
        auth = Auth.create(properties.getAccessKey(), properties.getSecretKey());
        upToken = auth.uploadToken(properties.getBucketName());
        Configuration cfg = new Configuration(Region.region0());
        cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
        uploadManager = new UploadManager(cfg);
        bucketManager = new BucketManager(auth, cfg);
        log.info("KodoClient Init Success...");
    }

    @Override
    public boolean checkFileIsExisted(String fileRelativePath) {
        try {
            if (null == bucketManager.stat(properties.getBucketName(), fileRelativePath)) {
                return false;
            }
        } catch (QiniuException e) {
            return false;
        }
        return true;
    }

    @Override
    public void executeUpload(MultipartFile file, String fileRelativePath) {
        try {
            uploadManager.put(file.getInputStream(), fileRelativePath, upToken, null, null);
        } catch (Exception e) {
            log.error("文件上传失败");
            throw new BaseException("文件上传失败");
        }
    }

    @Override
    public String getPublicNetworkAccessUrl(String fileRelativePath) {
        return FilePathUtil.fixFilePathSlash(properties.getDomain()) + fileRelativePath;
    }

    @Override
    public InputStream executeDownload(String fileRelativePath) {
        // 对文件名进行 URL 安全的 Base64 编码
        String publicUrl = auth.privateDownloadUrl(getPublicNetworkAccessUrl(fileRelativePath));
        // 使用hutool工具
        return HttpRequest.post(publicUrl).execute().bodyStream();
        // 使用okhttp请求
        //return Objects.requireNonNull(new OkHttpClient().newCall(new Request.Builder().url(publicUrl).build()).execute().body()).byteStream();
    }

    @Override
    public void executeDelete(String fileRelativePath) {
        try {
            bucketManager.delete(properties.getBucketName(), fileRelativePath);
        } catch (QiniuException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            bucketManager.createBucket(bucketName, Region.region0().toString());
        } catch (QiniuException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFileRelativePathFromUrl(String url) {
        return url.replace(FilePathUtil.fixFilePathSlash(properties.getDomain()) , "");
    }
}
