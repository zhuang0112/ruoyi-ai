package org.ruoyi.common.oss.storage;

import cn.hutool.core.io.FileUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.oss.constant.OssConstant;
import org.ruoyi.common.oss.exception.OssException;
import org.ruoyi.common.oss.properties.OssProperties;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @Description: 本地上传策略实现类
 * @date: 2023/11/19
 * @author: mrzhuang
 */

@Slf4j
@Getter
@Setter
// @RequiredArgsConstructor
// @Service("localServiceImpl")
public class LocalStorageClient extends AbstractStorageClient {

    /**
     * 本地项目端口
     */
    private String port;

    /**
     * 前置路径 ip/域名
     */
    private String prefixUrl;

    /**
     * 构造器注入bean
     */
    private OssProperties properties;

    public LocalStorageClient(OssProperties properties, String port) {
        this.properties = properties;
        this.port = port;
        initClient();
    }

    @Override
    public void initClient() {
        try {
            if (StringUtils.isNoneEmpty(properties.getBucketName())) {
                prefixUrl = FileUtil.getAbsolutePath(properties.getBucketName());
            } else {
                prefixUrl = ResourceUtils.getURL("classpath:").getPath() + "static/";

            }
            // windows 系统去掉路径前面的"/"
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                prefixUrl = prefixUrl.startsWith("/") ? prefixUrl.replaceFirst("/", "") : prefixUrl;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new OssException("文件不存在");
        }
        log.info("Local Init Success...");
    }

    @Override
    public boolean checkFileIsExisted(String fileRelativePath) {
        return new File(prefixUrl + fileRelativePath).exists();
    }

    @Override
    public void executeUpload(MultipartFile file, String fileRelativePath) {
        File dest = checkFolderIsExisted(fileRelativePath);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OssException("文件上传失败");
        }
    }

    @Override
    public String getPublicNetworkAccessUrl(String fileRelativePath) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            if (StringUtils.isEmpty(properties.getDomain())) {
                return String.format("http://%s:%s/%s", host, port, fileRelativePath);
            }
            return properties.getDomain() + OssConstant.SLASH + fileRelativePath;
        } catch (UnknownHostException e) {
            throw new OssException("未知异常");
        }
    }

    @Override
    public InputStream executeDownload(String fileRelativePath) {
        try {
            return Files.newInputStream(Paths.get(prefixUrl + fileRelativePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeDelete(String fileRelativePath) {
        // FileUtil.del(Paths.get(prefixUrl + fileRelativePath));
        // 对于本地文件，上级目录是md5文件夹，也需删除--因此只需删除父文件夹即可
        FileUtil.del(new File(prefixUrl + fileRelativePath).getParentFile());
    }

    @Override
    public String getFileRelativePathFromUrl(String url) {
        return url.replace(prefixUrl, "");
    }


    /**
     * 检查文件夹是否存在，若不存在则创建文件夹，最终返回上传文件
     *
     * @param fileRelativePath 文件相对路径
     * @return {@link  File} 文件
     */
    private File checkFolderIsExisted(String fileRelativePath) {
        File rootPath = new File(prefixUrl + fileRelativePath);
        if (!rootPath.exists()) {
            if (!rootPath.mkdirs()) {
                throw new OssException("文件夹创建失败");
            }
        }
        return rootPath;
    }

    @Override
    public void createBucket(String bucketName) {
        if (!FileUtil.exist(prefixUrl + bucketName)) {
            FileUtil.mkdir(prefixUrl + bucketName);
        }
    }

}
