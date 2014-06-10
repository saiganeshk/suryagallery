package com.saiganeshk.suryagallery.utils;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

public class MemoryCache {
	private LruCache<String, Bitmap> memoryCache;
	
	public MemoryCache() {
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        
		memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap bitmap) {
            	int size;
            	if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            		size = bitmap.getByteCount();
            	}
            	else {
            		size = bitmap.getRowBytes() * bitmap.getHeight();
            	}
            	
            	return size;
            }
        };
	}
	
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }
 
    public Bitmap getBitmapFromMemCache(String key) {
        return (Bitmap) memoryCache.get(key);
    }
}
