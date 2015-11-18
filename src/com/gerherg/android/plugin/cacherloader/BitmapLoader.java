/**
 * @author mengsun
 * @date 2015-11-17 17:12:08
 */

package com.gerherg.android.plugin.cacherloader;

import com.gerherg.android.os.AsyncResult;
import com.gerherg.android.util.Utils;

import android.graphics.Bitmap;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.LruCache;

public class BitmapLoader {

    private static final LongSparseArray<FixedSyncQueue> sTaskQueueMap = new LongSparseArray<FixedSyncQueue>();

    private final HandlerThread mThread;
    private final int mPoolSize;;
    private final LruCache<String, Bitmap> mMemoryCache;

    public BitmapLoader(LoaderConfiguration configuration) {
        mThread = configuration.mThread;
        mPoolSize = configuration.mThreadPoolSize;
        mMemoryCache = configuration.mMemoryCache;
    }

    public synchronized void execute(String url, LoaderOption option, Message callback) {
        String cacheKey = Utils.getBitmapCacheKey(url, option.mWidth, option.mHeight,
                option.mRoundPx);
        Bitmap cache = Utils.loadCachedBitmap(mMemoryCache, cacheKey);
        if (cache != null) {
            if (callback != null && callback.getTarget() != null) {
                AsyncResult.forMessage(callback, cache, null);
                callback.sendToTarget();
            }
            return;
        }

        FixedSyncQueue queue = sTaskQueueMap.get(mThread.getId());
        if (queue == null) {
            queue = new FixedSyncQueue(mPoolSize, true);
            sTaskQueueMap.put(mThread.getId(), queue);
        }
        synchronized (queue) {
            BitmapLoadTask task = new BitmapLoadTask(queue, url, option, mThread, mMemoryCache,
                    callback);
            task.loop();
        }
    }

}
