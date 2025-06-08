package org.ruoyi.common.oss.storage;

import cn.hutool.core.util.IdUtil;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.oss.properties.OssProperties;
import org.ruoyi.common.oss.util.FilePathUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
// @Service("s3ServiceImpl")

public class S3StorageClient extends AbstractStorageClient {

    private OssProperties properties;
    /**
     * aws s3 client
     */
    private AmazonS3 s3 = null;

    /**
     * 构造器注入
     */
    public S3StorageClient(OssProperties properties) {
        this.properties = properties;
        initClient();
    }

    @Override
    public void initClient() {
        if (Objects.isNull(properties) || Objects.isNull(properties.getEndpoint())) {
            return;
        }
        ClientConfiguration config = new ClientConfiguration();
        config.setMaxConnections(200);
        AwsClientBuilder.EndpointConfiguration endpointConfig =
                new AwsClientBuilder.EndpointConfiguration(properties.getEndpoint(), Regions.CN_NORTH_1.getName());

        AWSCredentials awsCredentials = new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey());
        AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);

        s3 = AmazonS3Client.builder()
                .withEndpointConfiguration(endpointConfig)
                .withClientConfiguration(config)
                .withCredentials(awsCredentialsProvider)
                .disableChunkedEncoding()
                .withPathStyleAccessEnabled(true)
                .build();

        log.info("AmazonS3Client Init Success...");
    }

    @Override
    public boolean checkFileIsExisted(String fileRelativePath) {
        boolean exist;
        try {
            exist = s3.doesObjectExist(properties.getBucketName(), fileRelativePath);
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
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setContentType(file.getContentType());
            // metadata.setContentLength(file.getSize());
            // PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), fileRelativePath, file.getInputStream(), metadata);
            File file1 = convertMultiPartToFile(file);
            PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), fileRelativePath, file1);
            // 文件名称相同会覆盖
            s3.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPublicNetworkAccessUrl(String fileRelativePath) {
        return FilePathUtil.fixFilePathSlash(properties.getDomain()) + FilePathUtil.fixFilePathSlash(properties.getBucketName()) + fileRelativePath;
    }

    @Override
    public InputStream executeDownload(String fileRelativePath) {
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(properties.getBucketName(), fileRelativePath);
            return s3.getObject(getObjectRequest).getObjectContent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeDelete(String fileRelativePath) {
        try {
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(properties.getBucketName(), fileRelativePath);
            s3.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFileRelativePathFromUrl(String url) {
        return url.replace(FilePathUtil.fixFilePathSlash(properties.getDomain()), "");
    }

    /**
     * 查看存储bucket是否存在
     *
     * @return boolean
     */
    public Boolean bucketExists(String bucketName) {
        boolean found;
        try {
            found = s3.doesBucketExistV2(properties.getBucketName());
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
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(properties.getBucketName());
            s3.createBucket(createBucketRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 创建临时文件
     *
     * @param multipartFile
     * @return
     * @throws IOException
     */
    private File convertMultiPartToFile(MultipartFile multipartFile) throws IOException {
        // 获取文件名
        String fileName = multipartFile.getOriginalFilename();
        // 获取文件后缀
        assert fileName != null;
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        // 用uuid作为文件名，防止生成的临时文件重复
        File excelFile = File.createTempFile(IdUtil.randomUUID(), prefix);
        // MultipartFile to File
        multipartFile.transferTo(excelFile);
        return excelFile;
    }

    @Override
    public void createBucket(String bucketName) {
        if (!s3.doesBucketExistV2(bucketName)) {
            s3.createBucket(bucketName);
        }
    }

}
