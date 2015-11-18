
package com.gerherg.android.plugin.cacherloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gerherg.android.os.AsyncResult;
import com.gerherg.android.util.IOUtils;
import com.gerherg.android.util.Log;

import android.os.Message;
import android.text.TextUtils;

public class HttpLoader {

    private static String TAG = HttpLoader.class.getSimpleName();

    private static final int POOL_SIZE = 10;
    private static final ExecutorService sExecutorService = Executors.newFixedThreadPool(POOL_SIZE);
    private static final Map<String, List<Message>> sCallbacks = new HashMap<String, List<Message>>();

    private static class LoadThread extends Thread {

        private String mUrl;

        private LoadThread(String url) {
            mUrl = url;
        }

        @Override
        public void run() {
            Throwable ex = null;
            byte[] data = Cache.loadCache(mUrl);
            if (data == null) {
                try {
                    data = IOUtils.readUrl(mUrl);
                } catch (Exception e) {
                    ex = e;
                }
                if (data != null) {
                    Cache.cache(mUrl, data);
                }
            }
            HttpLoader.notifyLoaded(mUrl, data, ex);
        }

    }

    synchronized static void load(String url, Message callback) {
        if (TextUtils.isEmpty(url)) {
            if (callback != null && callback.getTarget() != null) {
                AsyncResult.forMessage(callback, null, new NullPointerException("url is null"));
                callback.sendToTarget();
            }
        } else if (!registerLoaded(url, callback)) {
            Log.d(TAG, "the task exists, just loop the callback: " + url);
        } else {
            sExecutorService.execute(new LoadThread(url));
        }
    }

    private synchronized static void notifyLoaded(String url, byte[] data, Throwable ex) {
        List<Message> list = sCallbacks.get(url);
        if (list != null) {
            synchronized (list) {
                for (Message message : list) {
                    if (message != null && message.getTarget() != null) {
                        AsyncResult.forMessage(message, data, ex);
                        message.sendToTarget();
                    }
                }
                list.clear();
            }
        }
        sCallbacks.remove(url);
    }

    private synchronized static boolean registerLoaded(String url, Message callback) {
        boolean newTask = false;
        List<Message> list = sCallbacks.get(url);
        if (list == null) {
            newTask = true;
            list = new ArrayList<Message>();
            sCallbacks.put(url, list);
        }
        synchronized (list) {
            list.add(callback);
        }
        return newTask;
    }
}
