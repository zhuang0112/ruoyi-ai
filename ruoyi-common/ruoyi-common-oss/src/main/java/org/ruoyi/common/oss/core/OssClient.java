package org.ruoyi.common.oss.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import org.ruoyi.common.core.utils.DateUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.oss.constant.OssConstant;
import org.ruoyi.common.oss.entity.UploadResult;
import org.ruoyi.common.oss.enumd.AccessPolicyType;
import org.ruoyi.common.oss.enumd.PolicyType;
import org.ruoyi.common.oss.exception.OssException;
import org.ruoyi.common.oss.properties.OssProperties;
import org.ruoyi.common.oss.storage.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

/**
 * S3 存储协议 所有兼容S3协议的云厂商均支持
 * 阿里云 腾讯云 七牛云 minio
 *
 * @author Lion Li
 */
public class OssClient {

    private final String configKey;

    private final OssProperties properties;

    private final AbstractStorageClient client;

    public OssClient(String configKey, OssProperties ossProperties) {
        this.configKey = configKey;
        this.properties = ossProperties;
        try {
        this.client = getStorageClient(configKey, ossProperties);
            // createBucket();
        } catch (Exception e) {
            if (e instanceof OssException) {
                throw e;
            }
            throw new OssException("配置错误! 请检查系统配置:[" + e.getMessage() + "]");
        }
    }

    public void createBucket() {
        try {
           client.createBucket(properties.getBucketName());
        } catch (Exception e) {
            throw new OssException("创建Bucket失败, 请核对配置信息:[" + e.getMessage() + "]");
        }
    }

    // public UploadResult upload(byte[] data, String path, String contentType) {
    //     return upload(new ByteArrayInputStream(data), path, contentType);
    // }
    //
    // public UploadResult upload(InputStream inputStream, String path, String contentType) {
    //
    //     if (!(inputStream instanceof ByteArrayInputStream)) {
    //         inputStream = new ByteArrayInputStream(IoUtil.readBytes(inputStream));
    //     }
    //     try {
    //         ObjectMetadata metadata = new ObjectMetadata();
    //         metadata.setContentType(contentType);
    //         metadata.setContentLength(inputStream.available());
    //         PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), path, inputStream, metadata);
    //         // 设置上传对象的 Acl 为公共读
    //         putObjectRequest.setCannedAcl(getAccessPolicy().getAcl());
    //         client.putObject(putObjectRequest);
    //     } catch (Exception e) {
    //         throw new OssException("上传文件失败，请检查配置信息:[" + e.getMessage() + "]");
    //     }
    //     return UploadResult.builder().url(getUrl() + "/" + path).filename(path).build();
    // }

    public void delete(String url) {
        String fileRelativePath = client.getFileRelativePathFromUrl(url);
        try {
            client.executeDelete(fileRelativePath);
        } catch (Exception e) {
            throw new OssException("删除文件失败，请检查配置信息:[" + e.getMessage() + "]");
        }
    }

    public UploadResult upload(MultipartFile file) {
        String url = client.uploadFile(file);
        return UploadResult.builder().url(url).filename(FileUtil.getName(url)).build();

    }

    // public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
    //     return upload(data, getPath(properties.getPrefix(), suffix), contentType);
    // }
    //
    // public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
    //     return upload(inputStream, getPath(properties.getPrefix(), suffix), contentType);
    // }

    /**
     * 获取文件元数据
     *
     * @param url 完整文件路径
     */
    public InputStream getObjectMetadata(String url) {
        String fileRelativePath = client.getFileRelativePathFromUrl(url);
        path = path.replace(getUrl() + "/", "");
        InputStream object = client.downloadFile(fileRelativePath);
        return object;
    }

    public InputStream getObjectContent(String url) {
        String fileRelativePath = client.getFileRelativePathFromUrl(url);
        InputStream object = client.downloadFile(fileRelativePath);
        return object;
    }

    public String getUrl() {
        String domain = properties.getDomain();
        String endpoint = properties.getEndpoint();
        String header = OssConstant.IS_HTTPS.equals(properties.getIsHttps()) ? "https://" : "http://";
        // 云服务商直接返回
        if (StringUtils.containsAny(endpoint, OssConstant.CLOUD_SERVICE)) {
            if (StringUtils.isNotBlank(domain)) {
                return header + domain;
            }
            return header + properties.getBucketName() + "." + endpoint;
        }
        // minio 单独处理
        if (StringUtils.isNotBlank(domain)) {
            return header + domain + "/" + properties.getBucketName();
        }
        return header + endpoint + "/" + properties.getBucketName();
    }

    public String getPath(String prefix, String suffix) {
        // 生成uuid
        String uuid = IdUtil.fastSimpleUUID();
        // 文件路径
        String path = DateUtils.datePath() + "/" + uuid;
        if (StringUtils.isNotBlank(prefix)) {
            path = prefix + "/" + path;
        }
        return path + suffix;
    }


    public String getConfigKey() {
        return configKey;
    }

    /**
     * 获取私有URL链接
     *
     * @param objectKey 对象KEY
     * @param second    授权时间
     */
    public String getPrivateUrl(String objectKey, Integer second) {
        // GeneratePresignedUrlRequest generatePresignedUrlRequest =
        //         new GeneratePresignedUrlRequest(properties.getBucketName(), objectKey)
        //                 .withMethod(HttpMethod.GET)
        //                 .withExpiration(new Date(System.currentTimeMillis() + 1000L * second));
        // URL url = client.generatePresignedUrl(generatePresignedUrlRequest);
        // return url.toString();
        return "";
    }

    /**
     * 检查配置是否相同
     */
    public boolean checkPropertiesSame(OssProperties properties) {
        return this.properties.equals(properties);
    }

    /**
     * 获取当前桶权限类型
     *
     * @return 当前桶权限类型code
     */
    public AccessPolicyType getAccessPolicy() {
        return AccessPolicyType.getByType(properties.getAccessPolicy());
    }

    private static String getPolicy(String bucketName, PolicyType policyType) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\"Statement\": [\n{\n\"Action\": [\n");
        builder.append(switch (policyType) {
            case WRITE -> "\"s3:GetBucketLocation\",\n\"s3:ListBucketMultipartUploads\"\n";
            case READ_WRITE -> "\"s3:GetBucketLocation\",\n\"s3:ListBucket\",\n\"s3:ListBucketMultipartUploads\"\n";
            default -> "\"s3:GetBucketLocation\"\n";
        });
        builder.append("],\n\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("\"\n},\n");
        if (policyType == PolicyType.READ) {
            builder.append("{\n\"Action\": [\n\"s3:ListBucket\"\n],\n\"Effect\": \"Deny\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
            builder.append(bucketName);
            builder.append("\"\n},\n");
        }
        builder.append("{\n\"Action\": ");
        builder.append(switch (policyType) {
            case WRITE ->
                    "[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n";
            case READ_WRITE ->
                    "[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:GetObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n";
            default -> "\"s3:GetObject\",\n";
        });
        builder.append("\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("/*\"\n}\n],\n\"Version\": \"2012-10-17\"\n}\n");
        return builder.toString();
    }

    private AbstractStorageClient getStorageClient(String configKey, OssProperties properties) {

        switch (configKey) {
            case OssConstant.MINIO -> {
                return new MinioStorageClient(properties);
            }
            case OssConstant.ALIYUN -> {
                return new OssStorageClient(properties);
            }
            case OssConstant.QCLOUD -> {
                return new CosStorageClient(properties);
            }
            case OssConstant.QINIU -> {
                return new KodoStorageClient(properties);
            }
            default -> {
                throw new OssException("存储配置有误！");
            }

        }

    }

}
