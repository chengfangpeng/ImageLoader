package com.cnwir.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by cfp on 15-9-26.
 */
public class ImageFileCache {

    private static final String TAG = ImageFileCache.class.getSimpleName();


    /**
     * 图片缓存目录
     */
    private static final String IMGCACHDIR = "/sdcard/ImgCach";


    //保存文件cache扩展名
    private static final String CACHTAIL = ".cach";

    private static final int MB = 1024 * 1024;

    private static final int CACHE_SIZE = 1;

    //当sd卡的内存少于10M时，会清理缓存

    private static final int FREE_SD_SPACE_NEEDED_TO_CACHE = 10;

    public ImageFileCache() {


        //清除部分文件缓存
        removeCache(IMGCACHDIR);
    }


    /**
     * 从文件中获取图片
     *
     * @param url
     * @return
     */

    public Bitmap getBitmapFromFile(String url) {

        String path = IMGCACHDIR + "/" + convertUrlToFilename(url);

        File file = new File(path);

        if (file != null && file.exists()) {

            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap == null) {

                file.delete();
            } else {

                updateFileTime(path);

                Log.d(TAG, "get bitmap from file url = " + url);
                return bitmap;
            }

        }


        return null;
    }


    /**
     *更新文件的修改时间
     * @param path
     */
    private void updateFileTime(String path) {
        File file = new File(path);
        long newModifiedTime = System.currentTimeMillis();
        file.setLastModified(newModifiedTime);
    }


    /**
     * 将图片存入文件
     *
     * @param url
     * @param bitmap
     */
    public void saveBitmapToFile(String url, Bitmap bitmap) {

        if (bitmap == null) {

            return;
        }
        if (FREE_SD_SPACE_NEEDED_TO_CACHE > sdCardFreeSpace()) {
            //如果sd卡的剩余空间不足
            return;
        }
        String filename = convertUrlToFilename(url);

        File dirFile = new File(IMGCACHDIR);
        if (!dirFile.exists())
            dirFile.mkdirs();
        File file = new File(IMGCACHDIR + "/" + filename);
        try {
            file.createNewFile();
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {

            Log.d(TAG, "IOException");
        }
    }

    /**
     * 将url转化成文件名
     *
     * @param url
     * @return
     */
    private String convertUrlToFilename(String url) {

        return url.hashCode() + CACHTAIL;
    }


    /**
     * 计算存储目录下的文件大小，
     * 当文件总大小大于规定的CACHE_SIZE或者sdcard剩余空间小于FREE_SD_SPACE_NEEDED_TO_CACHE的规定
     * 那么删除40%最近没有被使用的文件
     */
    private boolean removeCache(String imgcachdir) {

        File dir = new File(imgcachdir);
        File[] files = dir.listFiles();

        if (files == null) {
            return true;
        }
        if (!android.os.Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

            return false;
        }
        int dirSize = 0;

        for (int i = 0; i < files.length; i++) {

            if (files[i].getName().contains(CACHTAIL)) {

                dirSize += files[i].length();
            }
        }
        if (dirSize > CACHE_SIZE * MB || FREE_SD_SPACE_NEEDED_TO_CACHE < sdCardFreeSpace()) {

            int removeFactor = (int) (0.4 * files.length);

            Arrays.sort(files, new FileLastModifSort());
            for (int i = 0; i < removeFactor; i++) {

                if (files[i].getName().contains(CACHTAIL)) {

                    files[i].delete();
                }


            }


        }
        if (sdCardFreeSpace() < CACHE_SIZE) {

            return false;
        }
        return true;


    }


    /**
     * sd卡剩余的空间大小
     *
     * @return
     */
    private int sdCardFreeSpace() {

        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdFreeMB = ((double) statFs.getAvailableBlocks() * (double) statFs.getBlockSize()) / MB;
        return (int) sdFreeMB;
    }

    private class FileLastModifSort implements Comparator<File> {


        @Override
        public int compare(File lhs, File rhs) {

            if (lhs.lastModified() > rhs.lastModified()) {

                return 1;

            }
            if (lhs.lastModified() == rhs.lastModified()) {

                return 0;
            } else {
                return -1;
            }

        }
    }
}
