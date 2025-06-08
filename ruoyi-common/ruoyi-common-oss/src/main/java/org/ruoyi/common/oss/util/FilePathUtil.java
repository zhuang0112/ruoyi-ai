package org.ruoyi.common.oss.util;

import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.oss.constant.OssConstant;

public class FilePathUtil {

    public static String fixFilePathSlash(String path) {
        if (StringUtils.isNotEmpty(path)) {
            if (!path.endsWith(OssConstant.SLASH)) {
                path = path + OssConstant.SLASH;
            }
        }
        return path;
    }
}
