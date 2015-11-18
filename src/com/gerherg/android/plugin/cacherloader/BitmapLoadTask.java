/**
 * @author mengsun
 * @date 2015-11-17 17:12:29
 */

package com.gerherg.android.plugin.cacherloader;

import com.gerherg.android.os.AsyncResult;
import com.gerherg.android.util.BitmapUtils;
import com.gerherg.android.util.Utils;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

class BitmapLoadTask extends FixedSyncQueue.SyncRequest {

    private static final int BIGGEST_SIZE_TO_CACHE = 500 * 1024;

    private static final int CODE_DATA_LOADING = 1;
    private static final int CODE_DATA_LOADED = 2;
    private static final int CODE_TIME_OUT = 3;

    private static final long TIME_OUT_MILLIS = 30 * 1000;

    final String mUrl;
    final float mWidth;
    final float mHeight;
    final float mRoundPx;
    final Message mCallback;
    final Handler mThreadHandler;
    final LruCache<String, Bitmap> mMemoryCache;

    BitmapLoadTask(FixedSyncQueue queue, String url, LoaderOption option,
            HandlerThread thread, LruCache<String, Bitmap> cache, Message callback) {
        super(queue);
        mUrl = url;
        mWidth = option.mWidth;
        mHeight = option.mHeight;
        mRoundPx = option.mRoundPx;
        mCallback = callback;
        mMemoryCache = cache;
        mThreadHandler = new Handler(thread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                BitmapLoadTask.this.handleMessage(msg);
            }

        };
        if (mWidth <= 0 || mHeight <= 0) {
            throw new IllegalArgumentException(
                    String.format("width or height is illegal[%.0f, %.0f]", mWidth, mHeight));
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case CODE_DATA_LOADING:
                loadData();
                break;
            case CODE_DATA_LOADED:
                onDataLoaded((AsyncResult) msg.obj);
                break;
            case CODE_TIME_OUT:
                handleRound(null, false);
                break;
        }
    }

    @Override
    public void onStart() {
        mThreadHandler.sendEmptyMessageDelayed(CODE_TIME_OUT, TIME_OUT_MILLIS);
        String cacheKey = Utils.getBitmapCacheKey(mUrl, mWidth, mHeight, mRoundPx);
        Bitmap cache = Utils.loadCachedBitmap(mMemoryCache, cacheKey);
        if (cache != null) {
            notifyBitmapLoaded(cache);
        } else if (TextUtils.isEmpty(mUrl)) {
            notifyBitmapLoaded(null);
        } else {
            mThreadHandler.obtainMessage(CODE_DATA_LOADING).sendToTarget();
        }
    }

    @Override
    public void onRemoveFromQueue() {
        mThreadHandler.removeCallbacksAndMessages(null);
    }

    private void loadData() {
        Utils.enforceThread(mThreadHandler.getLooper(), true);
        Bitmap bitmap = null;
        if (TextUtils.isEmpty(mUrl)) {
            notifyBitmapLoaded(null);
        } else if ((bitmap = BitmapUtils
                .decodeBitmap(Cache.loadIconCache(mUrl, mWidth, mHeight))) != null) {
            handleRound(bitmap, false);
        } else if ((bitmap = BitmapUtils.decodeBitmap(Cache.loadCache(mUrl), mWidth,
                mHeight)) != null) {
            handleRound(bitmap, true);
        } else {
            HttpLoader.load(mUrl, mThreadHandler.obtainMessage(CODE_DATA_LOADED));
        }
    }

    private void onDataLoaded(AsyncResult async) {
        Utils.enforceThread(mThreadHandler.getLooper(), true);
        Bitmap bitmap = null;
        if (async != null && async.result != null && async.exception == null) {
            bitmap = BitmapUtils.decodeBitmap((byte[]) async.result, mWidth, mHeight);
        }
        handleRound(bitmap, true);
    }

    private void handleRound(Bitmap bitmap, boolean cache) {
        Utils.enforceThread(mThreadHandler.getLooper(), true);
        byte[] data = null;
        if (cache && (data = BitmapUtils.decodeBytes(bitmap)) != null
                && data.length < BIGGEST_SIZE_TO_CACHE) {
            Cache.cacheIcon(mUrl, data, mWidth, mHeight);
        }
        if (mRoundPx != 0) {
            Bitmap croppedBitmap = BitmapUtils.crop(bitmap, mWidth, mHeight);
            if (croppedBitmap != bitmap && bitmap != null) {
                bitmap.recycle();
            }
            Bitmap corneredBitmap = BitmapUtils.roundCorner(croppedBitmap, mRoundPx);
            if (corneredBitmap != croppedBitmap && croppedBitmap != null) {
                croppedBitmap.recycle();
            }
            bitmap = corneredBitmap;
        }

        String cacheKey = Utils.getBitmapCacheKey(mUrl, mWidth, mHeight, mRoundPx);
        Utils.cacheBitmap(mMemoryCache, cacheKey, bitmap);
        notifyBitmapLoaded(bitmap);
    }

    private void notifyBitmapLoaded(Bitmap bitmap) {
        if (mCallback != null && mCallback.getTarget() != null
                && mThreadHandler.hasMessages(CODE_TIME_OUT)) {
            AsyncResult.forMessage(mCallback, bitmap, null);
            mCallback.sendToTarget();
        }
        recycle();
    }
}
