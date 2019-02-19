package com.geappliances.test.sendingphoto;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.geappliances.test.sendingphoto.common.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageCropActivity extends AppCompatActivity {
    private static String getDirectoryPath;

    int IMAGE_SIZE_1M = 2000000;
    int IMAGE_SIZE_500K = 1000000;

    File imageFile_original;
    File imageFile_large;
    File imageFile_small;

    String imagePath;
    String resizeImage_large;
    String resizeImage_small;

    ImageView imageView_original;
    ImageView imageView_large;
    ImageView imageView_small;

    TextView textView_original_value;
    TextView textView_large_value;
    TextView textView_small_value;

    SimpleSocket ssocket;
    String host;
    int port;

    protected void onCreate(Bundle savedInstanceState) {
        resizeImage_large = "resizeImage_large";
        resizeImage_small = "resizeImage_small";

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_image_crop );

//        Make can do socket transport in main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

//        get Intent data and connect socket
        Intent intent = getIntent();
        imagePath = intent.getStringExtra( "imgUri" );
         host = intent.getStringExtra("HOST");
         port = intent.getIntExtra("port",60000);
        connectServer(host, port);

//        Log.v( "Uri", imagePath );
//        Uri urivalue = Uri.parse( imagePath );
        imageFile_original = new File(imagePath);
        Bitmap originalBitmap = BitmapFactory.decodeFile( imageFile_original.getAbsolutePath() );

        imageView_original = (ImageView) findViewById( R.id.imageView_original );
        imageView_large = (ImageView) findViewById( R.id.imageView_resized_1M );
        imageView_small = (ImageView) findViewById( R.id.imageView_resized_500K );

        textView_original_value = (TextView) findViewById( R.id.textView_original_value );
        textView_large_value = (TextView) findViewById( R.id.textView_resized_1M_value );
        textView_small_value = (TextView) findViewById( R.id.textView_resized_500K_value );

        imageView_original.setImageBitmap(originalBitmap);
        Log.d("file size", String.valueOf(imageFile_original.length()));
        imageView_original.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ssocket.isConnected()) {
                    connectServer(host,port);
                }
                imageFile_large = new File(imagePath);
                Log.d("file size", String.valueOf(imageFile_original.length()));
                ssocket.sendString("size " + imageFile_original.length() + " .jpg");
            }
        });

        Bitmap BitmpaImageSize1M = resizedImage(imagePath, IMAGE_SIZE_1M);
        SaveBitmapToFileCache(BitmpaImageSize1M, imagePath, resizeImage_large);
        final String resizeImage_large_path = getDirectoryPath + "/" + resizeImage_large + ".jpg";
        imageFile_large = new File(resizeImage_large_path);
        Log.d("resizeImage_large_path", String.valueOf(resizeImage_large_path));
        Log.d("1M file size", String.valueOf(imageFile_large.length()));
        imageView_large.setImageBitmap(BitmpaImageSize1M);
        imageView_large.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ssocket.isConnected()) {
                    connectServer(host,port);
                }
                imagePath = resizeImage_large_path;
                Log.d("file size", String.valueOf(imageFile_large.length()));
                ssocket.sendString("size " + imageFile_large.length() + " .jpg");
            }
        });

        Bitmap BitmpaImageSize500K = resizedImage(imagePath, IMAGE_SIZE_500K);
        SaveBitmapToFileCache(BitmpaImageSize500K, imagePath, resizeImage_small);
        final String resizeImage_small_path = getDirectoryPath + "/" + resizeImage_small + ".jpg";
        imageFile_small = new File(resizeImage_small_path);
        Log.d("500K file size", String.valueOf(imageFile_small.length()));
        imageView_small.setImageBitmap(BitmpaImageSize500K);
        imageView_small.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ssocket.isConnected()) {
                    connectServer(host,port);
                }
                imagePath = resizeImage_small_path;
                Log.d("file size", String.valueOf(imageFile_small.length()));
                ssocket.sendString("size " + imageFile_small.length() + " .jpg");
            }
        });
        textView_original_value.setText( String.valueOf( imageFile_original.length() ));
        textView_large_value.setText( String.valueOf( imageFile_large.length() ));
        textView_small_value.setText( String.valueOf(imageFile_small.length() ));
    }

    private void connectServer(String host, int port) {
        Log.d("socket", "host: " + host +" port" + port);
        ssocket = new SimpleSocket(host, port, mHandler);
        ssocket.start();
    }

    Handler mHandler = new Handler( Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            switch (inputMessage.what) {
                case Constants.SIMSOCK_CONNECTED:
                    String msg = (String) inputMessage.obj;
                    Toast.makeText(getApplicationContext(), "Socket connected", Toast.LENGTH_SHORT).show();
                    Log.d("OUT", msg);
                    // do something with UI
                    break;
                case Constants.SIMSOCK_DISCONNECTED:
                    Log.d("OUT", inputMessage.obj.toString());
//                    Toast.makeText(getApplicationContext(),"Socket disconnected",Toast.LENGTH_SHORT).show();
                    // do something with UI
                    break;
                case Constants.SIMSOCK_REQIMAGE:
                    Log.d("OUT", inputMessage.obj.toString());
//                    if(ssocket.isConnected()){
                    ssocket.sendFile(imagePath);
//                    } else {
//                        //오류
//                    }
                    // do something with UI
                    break;
                case Constants.SIMSOCK_DATA:
                    if(inputMessage.obj.toString().equals("ok")){
                        break;
                    }
                    if (ssocket.isConnected()) {
                        ssocket.sendString("ok");
                    }
                    showResult(inputMessage.obj.toString());
//                    Toast.makeText(getApplicationContext(), inputMessage.obj.toString(), Toast.LENGTH_SHORT).show();
                    Log.d("OUT", inputMessage.obj.toString());

                    break;
                case Constants.SIMSOCK_ERROR:
                    Toast.makeText(getApplicationContext(), "Error: " + inputMessage.obj.toString(), Toast.LENGTH_SHORT).show();
                    Log.d("OUT", "error");
                    //오류
                    break;
            }
        }
    };

    public Bitmap resizedImage(String path, int IMAGE_MAX_SIZE) {
        try {
//            final int IMAGE_MAX_SIZE = 1200000;
            //resource = getResources();

            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
//            Bitmap orgImage =BitmapFactory.decodeResource(getResources(), R.id.textView_original_value);

            int scale = 1;
            while ((options.outWidth * options.outHeight) * (1 / Math.pow(scale, 2)) > IMAGE_MAX_SIZE) {
                scale*=2;
            }
            Log.d("TAG", "scale = " + scale + ", orig-width: " + options.outWidth + ", orig-height: " + options.outHeight);
            Bitmap pic = null;
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                options = new BitmapFactory.Options();
                options.inSampleSize = scale;
                pic = BitmapFactory.decodeFile(path, options);

                // resize to desired dimensions

                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                Log.d("get Size ", size.toString());
                int width = size.y;
                int height = size.x;

                //int height = imageView.getHeight();
                //int width = imageView.getWidth();
                Log.d("TAG", "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(pic, (int) x, (int) y, true);
                pic.recycle();
                pic = scaledBitmap;

                System.gc();
            } else {
                pic = BitmapFactory.decodeFile(path);

            }

            Log.d("TAG", "bitmap size - width: " + pic.getWidth() + ", height: " + pic.getHeight());
            return pic;
        } catch (Exception e) {
            Log.e("TAG", e.getMessage(), e);
            return null;
        }
    }

    public static void SaveBitmapToFileCache(Bitmap bitmap, String strFilePath, String name) {
        File file = new File(strFilePath);
        if (!file.exists())
            file.mkdirs();
        File oldPath = new File(strFilePath);
        getDirectoryPath = oldPath.getParent();

        File fileCacheItem = new File(getDirectoryPath + "/" + name + ".jpg");
        OutputStream out = null;
        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showResult(String result){
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(this);
        alert_confirm.setMessage(result);
        alert_confirm.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        // 다이얼로그 생성
        AlertDialog alert = alert_confirm.create();

        // 아이콘
        alert.setTitle("Result");
        alert.show();

    }
}
