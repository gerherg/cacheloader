/**
 * @author mengsun
 * @date 2015-11-17 17:28:05
 */

package com.gerherg.android.os;

import com.gerherg.android.plugin.cacherloader.Cache;

import android.app.Application;

public abstract class BaseApp extends Application {

    private static BaseApp mMe;

    public BaseApp() {
        mMe = this;
    }

    public static BaseApp getInstance() {
        return mMe;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Cache.init(this);
    }

}
