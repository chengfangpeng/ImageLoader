package com.cnwir.imageloader;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by cfp on 15-9-25.
 */
public class ImageMemoryCache {

    /**
     * 使用双层缓冲
     * 常用数据使用强应用缓冲，不常用的使用软引用缓冲
     */
    private static final String TAG = ImageMemoryCache.class.getSimpleName();
    //强引用缓冲
    private LruCache<String, Bitmap> mLruCache;

    //弱引用缓冲
    private LinkedHashMap<String, SoftReference<Bitmap>> mSoftCache;

    //强引用缓冲的大小
    private static final int LRU_CACHE_SIZE = 4 * 1024 * 1024;

    //软引用的个数
    private static final int SOFT_CACHE_NUM = 20;

    //初始化强引用和软引用缓冲
    public ImageMemoryCache() {

        mLruCache = new LruCache<String, Bitmap>(LRU_CACHE_SIZE) {
            //sizeOf 返回单个hashmap value的大小
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (value != null) {

                    return value.getRowBytes() * value.getHeight();
                } else {

                    return 0;
                }

            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {

                if (oldValue != null) {
                    //强引用满的时候会根据,会根据Lru算法把最近没有使用的图片保存到该软引用缓冲
                    Log.d(TAG, "LruCache is full, move to softReferenceCache");
                    mSoftCache.put(key, new SoftReference<Bitmap>(oldValue));


                }

            }
        };

        mSoftCache = new LinkedHashMap<String, SoftReference<Bitmap>>(SOFT_CACHE_NUM, 0.75f, true) {


            @Override
            protected boolean removeEldestEntry(Entry<String, SoftReference<Bitmap>> eldest) {

                //当软引用的个数大于20时,从hashmap链表中移除
                if (size() > SOFT_CACHE_NUM) {

                    Log.d(TAG, "the oldest reference is moved");
                    return true;
                }
                return false;
            }
        };


    }

    /**
     * 从缓冲中获取图片
     *
     * @param url
     * @return
     */

    public Bitmap getBitmapFromMemory(String url) {

        Bitmap bitmap;

        //从强引用中获取
        synchronized (mLruCache) {
            bitmap = mLruCache.get(url);
            if (bitmap != null) {
                //如果bitmap不为空的话，把bitmap移到LinkedhashMap的最前面，从而保证Lru中是最后删除的
                mLruCache.remove(url);
                mLruCache.put(url, bitmap);
                Log.d(TAG, "get bitmap from LruCache url = " +  url);
                return bitmap;
            }


        }
        //如果强引用中没有的话，到软引用中查找，找到后把它移动到强引用中
        synchronized (mSoftCache){

            SoftReference<Bitmap> bitmapReference = mSoftCache.get(url);

            if(bitmapReference != null){

                bitmap = bitmapReference.get();
                if(bitmap != null){

                    mLruCache.put(url, bitmap);
                    mSoftCache.remove(url);
                    Log.d(TAG, "get bitmap from softRefrenceCache url = " + url);
                    return bitmap;
                }else{

                    mSoftCache.remove(url);
                }
            }

        }

        return null;

    }

    /**
     * 将图片添加的缓存中
     * @param url
     * @param bitmap
     */

    public void addBitmapToMemory(String url, Bitmap bitmap){

        if(bitmap != null){

            synchronized (mLruCache){

                mLruCache.put(url, bitmap);

            }

        }

    }

    /**
     * 清除缓存
     */
    public void clearMemory(){

        mSoftCache.clear();
    }


}
