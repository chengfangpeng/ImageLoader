
package com.cnwir.imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Button getBitmap;

    private ImageView img;
    Bitmap bitmap = null;
    ImageManager imageManage = null;


    private static final String URL = "http://img5.duitang.com/uploads/item/201405/09/20140509222156_kVexJ.thumb.jpeg";

    private static final String URL2 = "http://img5.duitang.com/uploads/item/201112/11/20111211191621_HU4Bj.thumb.jpg";

    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {

            Bitmap bitmap = (Bitmap) msg.obj;

            if(bitmap != null)
            img.setImageBitmap(bitmap);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = (ImageView) findViewById(R.id.img);
        imageManage = new ImageManager();

        imageManage.loadBitmap(URL,handler);
        getBitmap = (Button) findViewById(R.id.getBitmap_btn);
        getBitmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageManage.loadBitmap(URL2,handler);
            }
        });
    }


}
