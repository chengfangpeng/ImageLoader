package com.cnwir.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by cfp on 15-9-25.
 */
public class ImageManager {

    private static final String TAG = ImageManager.class.getSimpleName();

    private ImageMemoryCache imageMemoryCache;

    private ImageFileCache imageFileCache;


    //正在下载的image列表
    private static HashMap<String, Handler> ongoingTaskMap = new HashMap<String, Handler>();

    //等待下载的image列表

    private static HashMap<String, Handler> waitTaskMap = new HashMap<String, Handler>();

    //同时下载图片的线程数

    private static final int MAX_DOWNLOAD_IMAGE_THREAD = 4;

    private final Handler downloadStatusHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {

            startDownloadNest();
        }
    };


    public ImageManager() {

        imageMemoryCache = new ImageMemoryCache();

        imageFileCache = new ImageFileCache();


    }

    /**
     * 获取图片多线程的入口
     *
     * @param url
     * @param handler
     */

    public void loadBitmap(String url, Handler handler) {


        //先从内存中获取图片，获取后直接加载
        Bitmap bitmap = getBitmapFromNative(url);

        if (bitmap != null) {

            Log.d(TAG, "load bitmap from native");

            Message message = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            message.obj = bitmap;
            message.setData(bundle);
            handler.sendMessage(message);
        } else {
            //新开一个线程中下载图片
            Log.d(TAG, "load bitmap by network");
            loadBmpOnNewThread(url, handler);

        }


    }


    private void loadBmpOnNewThread(final String url, final Handler handler) {

        Log.d(TAG, "ongoingTaskMap size = " + ongoingTaskMap.size());

        if (ongoingTaskMap.size() >= MAX_DOWNLOAD_IMAGE_THREAD) {

            synchronized (waitTaskMap) {

                waitTaskMap.put(url, handler);
            }
        } else {

            synchronized (ongoingTaskMap) {

                ongoingTaskMap.put(url, handler);
            }
        }
        new Thread() {

            @Override
            public void run() {

                Bitmap bitmap;
                bitmap = getBitmapFromHttp(url);

                //无论是否成功都从下载队列中移除,再由业务逻辑判断是否重新下载
                //下载所用的httpClientRequest,本身带有重连机制
                synchronized (ongoingTaskMap) {

                    ongoingTaskMap.remove(url);
                }
                if (downloadStatusHandler != null) {

                    downloadStatusHandler.sendEmptyMessage(0);
                }

                Message msg = Message.obtain();
                msg.obj = bitmap;
                Bundle bundle = new Bundle();
                bundle.putString("url", url);
                msg.setData(bundle);

                if (handler != null) {
                    handler.sendMessage(msg);
                }

            }
        }.start();


    }

    /**
     * 从内存或缓冲文件中获取图片
     *
     * @param url
     * @return
     */

    private Bitmap getBitmapFromNative(String url) {

        Bitmap bitmap;
        bitmap = imageMemoryCache.getBitmapFromMemory(url);
        if (bitmap == null) {

            bitmap = imageFileCache.getBitmapFromFile(url);

            if (bitmap != null) {

                imageMemoryCache.addBitmapToMemory(url, bitmap);

            }


        }


        return bitmap;
    }

    /**
     * 取出等待队列第一个任务，开始下载
     */
    private void startDownloadNest() {

        synchronized (waitTaskMap){

            Log.d(TAG, "begin start next");

            Iterator iterator = waitTaskMap.entrySet().iterator();
            while(iterator.hasNext()){

                Map.Entry entry = (Map.Entry) iterator.next();

                if(entry != null){

                    waitTaskMap.remove(entry.getKey());
                    loadBmpOnNewThread((String)entry.getKey(), (Handler)entry.getValue());
                }
                break;

            }

        }

    }


    private Bitmap getBitmapFromHttp(String url) {

        Bitmap bitmap = null;


        try {
            byte[] temByte = getBitmapByteFromHttp(url);

            if (temByte != null) {

                bitmap = BitmapFactory.decodeByteArray(temByte, 0, temByte.length);

            }
            temByte = null;
            if (bitmap != null) {

                imageFileCache.saveBitmapToFile(url, bitmap);

                imageMemoryCache.addBitmapToMemory(url, bitmap);
            }
        } catch (Exception e) {

            e.printStackTrace();
        }


        return bitmap;
    }

    /**
     * 下载图片资源
     *
     * @param url
     * @return
     */

    private byte[] getBitmapByteFromHttp(String url) {


        byte[] pic;
        InputStream is = null;
        ByteArrayOutputStream os = null;

        int byteSize = 1024;
        byte[] temByte = new byte[byteSize];
        HttpURLConnection conn = null;
        try {
            URL imgUrl = new URL(url);
            conn = (HttpURLConnection) imgUrl.openConnection();

            is = conn.getInputStream();
            os = new ByteArrayOutputStream();
            for (; ; ) {
                int count = is.read(temByte, 0, byteSize);

                if (count == -1) {

                    break;
                }

                os.write(temByte, 0, count);

            }

            pic = os.toByteArray();

            Log.d(TAG, pic.length + "");
            return pic;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {
                if (os != null) {
                    os.close();
                }
                if (is != null) {

                    is.close();

                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return temByte;


    }


}
