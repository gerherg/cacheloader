
package com.gerherg.android.plugin.cacherloader;

import java.io.File;

import com.gerherg.android.os.BaseApp;
import com.gerherg.android.util.IOUtils;
import com.gerherg.android.util.Log;
import com.gerherg.android.util.Utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

public class Cache {

    private static final String TAG = "CACHE";

    public static final String TABLE_CACHE = "cache";

    public static final String TABLE_ICON = "icon";

    public static final String COLUMN_CACHE_URL = "url";
    public static final String COLUMN_CACHE_FILE = "file";
    public static final String COLUMN_CACHE_FILE_CRC = "crc";

    public static final String COLUMN_ICON_URL = "url";
    public static final String COLUMN_ICON_DATA = "data";
    public static final String COLUMN_ICON_WIDTH = "width";
    public static final String COLUMN_ICON_HEIGHT = "height";

    static boolean cache(String url, byte[] data) {
        Utils.enforceNonUIThread();

        if (data == null) {
            Log.d(TAG, "data is null, failed to cahce");
            return false;
        }
        String filePath = Utils.getHttpCacheDir(BaseApp.getInstance())
                + java.util.UUID.randomUUID();
        if (!IOUtils.write(data, filePath)) {
            Log.d(TAG, "failed cache, write to file: " + filePath);
            return false;
        }
        long crc = IOUtils.getCRC(data);
        if (crc <= 0) {
            Log.d(TAG, "failed cache, get crc: " + crc);
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_CACHE_URL, url);
        values.put(COLUMN_CACHE_FILE, filePath);
        values.put(COLUMN_CACHE_FILE_CRC, crc);

        String selection = COLUMN_CACHE_URL + "=?";
        String[] selectionArgs = new String[] {
                url
        };

        boolean result = true;
        if (BaseApp.getInstance().getCacheDatabase().update(TABLE_CACHE, values, selection,
                selectionArgs) == 0) {
            result = BaseApp.getInstance().getCacheDatabase().insert(TABLE_CACHE, null,
                    values) != -1;
        }

        if (result) {
            removeIconCache(url);
        }

        return result;
    }

    static byte[] loadCache(String url) {
        Utils.enforceNonUIThread();

        String selection = COLUMN_CACHE_URL + "=?";
        String[] selectionArgs = new String[] {
                url
        };
        String filePath = null;
        long crc = 0;
        Cursor c = null;
        try {
            c = BaseApp.getInstance().getCacheDatabase().query(TABLE_CACHE, null, selection,
                    selectionArgs, null, null, null);
            if (c != null && c.moveToNext()) {
                filePath = c.getString(c.getColumnIndex(COLUMN_CACHE_FILE));
                crc = c.getLong(c.getColumnIndex(COLUMN_CACHE_FILE_CRC));
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.d(TAG, "failed to load cache, remove it: " + url, e);
            removeCache(url);
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (TextUtils.isEmpty(filePath) || crc <= 0) {
            Log.d(TAG, String.format("params[%s, %s] error!", filePath, crc));
            removeCache(url);
            return null;
        }
        byte[] data = IOUtils.readWithCRC(filePath, crc);
        if (data == null) {
            Log.d(TAG, "file not exist or crc check error!");
            removeCache(url);
        }
        return data;
    }

    private static void removeCache(String url) {
        Utils.enforceNonUIThread();

        // delete cache file
        String selection = COLUMN_CACHE_URL + "=?";
        String[] selectionArgs = new String[] {
                url
        };

        Cursor c = null;
        try {
            c = BaseApp.getInstance().getCacheDatabase().query(TABLE_CACHE, null, selection,
                    selectionArgs, null, null, null);
            if (c != null && c.moveToNext()) {
                String filePath = c.getString(c.getColumnIndex(COLUMN_CACHE_FILE));
                IOUtils.delete(filePath);
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        BaseApp.getInstance().getCacheDatabase().delete(TABLE_CACHE, selection, selectionArgs);
    }

    static boolean cacheIcon(String url, byte[] data, float width, float height) {
        Utils.enforceNonUIThread();

        ContentValues values = new ContentValues();
        values.put(COLUMN_ICON_DATA, data);
        values.put(COLUMN_ICON_URL, url);
        values.put(COLUMN_ICON_WIDTH, width);
        values.put(COLUMN_ICON_HEIGHT, height);

        String selection = COLUMN_ICON_URL + "=? and " + COLUMN_ICON_WIDTH + "=? and "
                + COLUMN_ICON_HEIGHT + "=?";
        String[] selectionArgs = new String[] {
                url, String.valueOf(width), String.valueOf(height)
        };
        if (BaseApp.getInstance().getCacheDatabase().update(TABLE_ICON, values, selection,
                selectionArgs) == 0) {
            return BaseApp.getInstance().getCacheDatabase().insert(TABLE_ICON, null, values) != -1;
        } else {
            return true;
        }
    }

    private static void removeIconCache(String url) {
        Utils.enforceNonUIThread();
        String selection = COLUMN_ICON_URL + "=?";
        String[] selectionArgs = new String[] {
                url
        };
        BaseApp.getInstance().getCacheDatabase().delete(TABLE_ICON, selection, selectionArgs);
    }

    public static void clear() {
        Utils.enforceNonUIThread();
        BaseApp.getInstance().getCacheDatabase().delete(TABLE_CACHE, null, null);
        BaseApp.getInstance().getCacheDatabase().delete(TABLE_ICON, null, null);
        IOUtils.delete(Utils.getHttpCacheDir(BaseApp.getInstance()));
    }

    public static long getCacheSize() {
        File root = new File(Utils.getHttpCacheDir(BaseApp.getInstance()));
        return IOUtils.getLength(root);
    }

    static byte[] loadIconCache(String url, float width, float height) {
        Utils.enforceNonUIThread();
        Cursor c = null;
        String selection = COLUMN_ICON_URL + "=? and " + COLUMN_ICON_WIDTH + "=? and "
                + COLUMN_ICON_HEIGHT + "=?";
        String[] selectionArgs = new String[] {
                url, String.valueOf(width), String.valueOf(height)
        };
        try {
            c = BaseApp.getInstance().getCacheDatabase().query(TABLE_ICON, null, selection,
                    selectionArgs, null, null, null);
            if (c != null && c.moveToNext()) {
                return c.getBlob(c.getColumnIndex(COLUMN_ICON_DATA));
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

}
