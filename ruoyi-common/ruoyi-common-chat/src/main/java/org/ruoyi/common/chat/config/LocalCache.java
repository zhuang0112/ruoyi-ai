package org.ruoyi.common.chat.config;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateUnit;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author https:www.unfbx.com
 * @date 2023-03-10
 */
@Slf4j
public class LocalCache {
    /**
     * 缓存时长
     */
    public static final long TIMEOUT = 30 * DateUnit.MINUTE.getMillis();
    /**
     * 清理间隔
     */
    private static final long CLEAN_TIMEOUT = 30 * DateUnit.MINUTE.getMillis();

    /**
     * 缓存对象
     */
    public static final TimedCache<String, Object> CACHE = CacheUtil.newTimedCache(TIMEOUT);


    static {
        //启动定时任务
        CACHE.schedulePrune(CLEAN_TIMEOUT);
    }

}
