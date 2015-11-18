/**
 * @author mengsun
 * @date 2015-11-17 17:28:05
 */

package com.gerherg.android.os;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

public abstract class BaseApp extends Application {

    private static BaseApp mMe;

    public BaseApp() {
        mMe = this;
    }

    public static BaseApp getInstance() {
        return mMe;
    }

    public abstract SQLiteDatabase getCacheDatabase();
}
