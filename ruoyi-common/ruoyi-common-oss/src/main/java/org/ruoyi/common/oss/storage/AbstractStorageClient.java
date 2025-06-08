package org.ruoyi.common.oss.storage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.oss.constant.OssConstant;
import org.ruoyi.common.oss.core.StorageClient;
import org.ruoyi.common.oss.entity.UploadResult;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


/**
 * @Description: 上传策略抽象模板类
 * @date: 2023/11/19
 * @author: mrzhuang
 */

@Getter
@Setter
@Slf4j
public abstract class AbstractStorageClient implements StorageClient {

    @Override
    public String uploadFile(MultipartFile file) {
       return uploadFile(file, null);
    }

    @Override
    public String uploadFile(MultipartFile file, String fileRelativePath) {
        try {

            fileRelativePath = StringUtils.isEmpty(fileRelativePath) ? getFileRelativePath(file) : fileRelativePath;
            // region 检测文件是否已经存在，不存在则进行上传操作
            if (!checkFileIsExisted(fileRelativePath)) {
                executeUpload(file, fileRelativePath);
            } else {
                log.warn("文件已存在!, fileRelativePath: {}", fileRelativePath);
            }

            // endregion
            return getPublicNetworkAccessUrl(fileRelativePath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BaseException("文件上传失败");
        }
    }

    /**
     * 文件下载
     *
     * @param fileRelativePath 文件相对桶的文件路径
     */
    @Override
    public InputStream downloadFile(String fileRelativePath) {
        try {
            return executeDownload(fileRelativePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFile(String fileRelativePath) {
        executeDelete(fileRelativePath);
    }

    /**
     * 初始化客户端
     */
    public abstract void initClient();

    /**
     * 检查文件是否已经存在（文件MD5值唯一）
     *
     * @param fileRelativePath 文件相对路径
     * @return true 已经存在  false 不存在
     */
    public abstract boolean checkFileIsExisted(String fileRelativePath);

    /**
     * 执行上传操作
     *
     * @param file             文件
     * @param fileRelativePath 文件相对路径
     * @throws IOException io异常信息
     */
    public abstract void executeUpload(MultipartFile file, String fileRelativePath);

    /**
     * 获取公网访问路径
     *
     * @param fileRelativePath 文件相对路径
     * @return 公网访问绝对路径
     */
    public abstract String getPublicNetworkAccessUrl(String fileRelativePath);

    public abstract InputStream executeDownload(String fileRelativePath);

    public abstract void executeDelete(String fileRelativePath);

    public abstract String getFileRelativePathFromUrl(String url);

    public String getFileRelativePath(MultipartFile file) {
        //  获取文件md5值 -> 获取文件后缀名 -> 生成相对路径
        String fileRelativePath = "";
        String md5 = null;
        try {
            md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
            String fileName = FileUtil.getName(file.getOriginalFilename());
            fileRelativePath = DateUtil.format(new Date(), "yyyyMMdd") + OssConstant.SLASH + md5 + OssConstant.SLASH + md5 + "." + FileUtil.getSuffix(fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileRelativePath;
    }

    public abstract void createBucket(String bucketName);
}
