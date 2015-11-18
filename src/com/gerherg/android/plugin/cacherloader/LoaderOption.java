/**
 * @author mengsun
 * @date 2015-11-18 10:36:52
 */

package com.gerherg.android.plugin.cacherloader;

public class LoaderOption {

    float mWidth;
    float mHeight;
    float mRoundPx;

    public LoaderOption setWidth(float width) {
        mWidth = width;
        return this;
    }

    public LoaderOption setHeight(float height) {
        mHeight = height;
        return this;
    }

    public LoaderOption setRoundPx(float roundPx) {
        mRoundPx = roundPx;
        return this;
    }

}
