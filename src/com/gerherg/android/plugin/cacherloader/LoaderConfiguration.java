/**
 * @author mengsun
 * @date 2015-11-18 10:36:52
 */
package com.gerherg.android.plugin.cacherloader;

import android.graphics.Bitmap;
import android.os.HandlerThread;
import android.support.v4.util.LruCache;

public class LoaderConfiguration {

    HandlerThread mThread;
    int mThreadPoolSize;;
    int mThreadOrder;
    
    LruCache<String, Bitmap> mMemoryCache;
    
    long mDiscCacheSize;
    int mDiscCacheCount;

    public LoaderConfiguration setThread(HandlerThread thread) {
        mThread = thread;
        return this;
    }

    public LoaderConfiguration setThreadPoolSize(int threadPoolSize) {
        mThreadPoolSize = threadPoolSize;
        return this;
    }

    public LoaderConfiguration setThreadOrder(int threadOrder) {
        mThreadOrder = threadOrder;
        return this;
    }

    public LoaderConfiguration setMemoryCache(LruCache<String, Bitmap> memoryCache) {
        mMemoryCache = memoryCache;
        return this;
    }

    
}
