package com.geappliances.test.sendingphoto;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
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

import com.gun0912.tedpermission.TedPermission;

import java.io.File;

public class ImageCropActivity extends AppCompatActivity {
    int IMAGE_SIZE_1M = 1000000;
    int IMAGE_SIZE_500K = 500000;

    File imageFile;

    String imagePath;

    ImageView imageView_original;
    ImageView imageView_resized_1M;
    ImageView imageView_resized_500K;

    TextView textView_original_value;
    TextView textView_resized_1M_value;
    TextView textView_resized_500K_value;

    SimpleSocket ssocket;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_image_crop );

//        Make can do socket transport in main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

//        imageResizing( "path" );
        Intent intent = getIntent();
        imagePath = intent.getStringExtra( "imgUri" );
//        Log.v( "Uri", imagePath );
//        Uri urivalue = Uri.parse( imagePath );
        imageFile = new File(imagePath);
        Bitmap originalBitmap = BitmapFactory.decodeFile( imageFile.getAbsolutePath() );

        imageView_original = (ImageView) findViewById( R.id.imageView_original );
        imageView_resized_1M = (ImageView) findViewById( R.id.imageView_resized_1M );
        imageView_resized_500K = (ImageView) findViewById( R.id.imageView_resized_500K );

        textView_original_value = (TextView) findViewById( R.id.textView_original_value );
        textView_resized_1M_value = (TextView) findViewById( R.id.textView_resized_1M_value );
        textView_resized_500K_value = (TextView) findViewById( R.id.textView_resized_500K_value );

        imageView_original.setImageBitmap(originalBitmap);
        Log.d("file size", String.valueOf(imageFile.length()));
        connectServer(imagePath);
        imageView_original.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ssocket.isConnected()) {
                    connectServer(imagePath);
                }
                Log.d("file size", String.valueOf(imageFile.length()));
                ssocket.sendString("size " + imageFile.length() + " .jpg");
            }
        });

        Bitmap BitmpaImageSize1M = resizedImage(imagePath, IMAGE_SIZE_1M);
        imageView_resized_1M.setImageBitmap(BitmpaImageSize1M);
        Log.d("file size", String.valueOf(imageFile.length()));
        connectServer(imagePath);

        imageView_resized_1M.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ssocket.isConnected()) {
                    connectServer(imagePath);
                }
                Log.d("file size", String.valueOf(imageFile.length()));
                ssocket.sendString("size " + imageFile.length() + " .jpg");
            }
        });
        Bitmap BitmpaImageSize500K = resizedImage(imagePath, IMAGE_SIZE_500K);
        imageView_resized_500K.setImageBitmap(BitmpaImageSize500K);
        connectServer(imagePath);
        imageView_resized_500K.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ssocket.isConnected()) {
                    connectServer(imagePath);
                }
                Log.d("file size", String.valueOf(imageFile.length()));
                ssocket.sendString("size " + imageFile.length() + " .jpg");
            }
        });
        textView_original_value.setText( String.valueOf( imageFile.length() ) + " KB");
    }

    private void connectServer(String imgUri) {
        Log.v("메인", MainActivity.editPort.getText().toString());
        String host = Constants.IP;
        int port = Constants.PORT;
        if (!MainActivity.editPort.getText().toString().isEmpty()) {
            port = Integer.parseInt(String.valueOf(MainActivity.editPort.getText()));
        }
        if (!MainActivity.editHost.getText().toString().isEmpty()) {
            host = String.valueOf(MainActivity.editHost.getText());
        }
        ssocket = new SimpleSocket(host, port, mHandler, imgUri);
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
                    if (ssocket.isConnected()) {
                        ssocket.sendString("ok");
                    }
                    Toast.makeText(getApplicationContext(), inputMessage.obj.toString(), Toast.LENGTH_SHORT).show();
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
}
