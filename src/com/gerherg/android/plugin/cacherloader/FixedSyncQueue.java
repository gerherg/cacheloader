/**
 * @author mengsun
 * @date 2015-11-17 17:22:44
 */

package com.gerherg.android.plugin.cacherloader;

import java.util.ArrayList;
import java.util.Collection;

public class FixedSyncQueue extends ArrayList<FixedSyncQueue.SyncRequest> {

    private static final long serialVersionUID = 1L;

    private final int mSize;
    private final boolean mReversed;
    private SyncRequest mCurrent;

    public static abstract class SyncRequest {

        private final FixedSyncQueue mSyncQueue;

        public SyncRequest(FixedSyncQueue queue) {
            mSyncQueue = queue;
        }

        public final void recycle() {
            mSyncQueue.remove(this);
        }

        public final void loop() {
            mSyncQueue.add(this);
        }

        public abstract void onStart();

        public abstract void onRemoveFromQueue();
    }

    public FixedSyncQueue(int size, boolean reversed) {
        mSize = size;
        mReversed = reversed;
    }

    @Override
    public synchronized final boolean add(SyncRequest object) {
        if (contains(object)) {
            return false;
        }
        boolean result = super.add(object);
        checkSize();
        moveNextIfNeed();
        return result;
    }

    @Override
    public synchronized final void add(int index, SyncRequest object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized final boolean addAll(Collection<? extends SyncRequest> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean addAll(int index, Collection<? extends SyncRequest> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized final boolean remove(Object object) {
        SyncRequest request = (SyncRequest) object;
        if (!contains(request)) {
            return false;
        }
        boolean result = super.remove(request);
        request.onRemoveFromQueue();
        if (mCurrent == request) {
            mCurrent = null;
        }
        moveNextIfNeed();
        return result;
    }

    private synchronized void checkSize() {
        if (size() > mSize) {
            remove(get(mReversed ? 0 : size() - 1));
        }
    }

    private synchronized SyncRequest next() {
        if (isEmpty()) {
            return null;
        } else {
            return get(mReversed ? size() - 1 : 0);
        }
    }

    private synchronized boolean moveNextIfNeed() {
        SyncRequest request = next();
        if (request != null && mCurrent == null) {
            mCurrent = request;
            mCurrent.onStart();
            return true;
        }
        return false;
    }
}
