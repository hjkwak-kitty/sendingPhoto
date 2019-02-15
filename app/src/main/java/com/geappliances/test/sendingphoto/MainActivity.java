package com.geappliances.test.sendingphoto;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = "메인";

    private Button btnSelect;
    private Button btnCamera;
    private Button btnResizing;
    private ImageView imageView;
    private EditText editHost;
    private EditText editPort;

    private static final int REQUEST_SELECT_PHOTO = 1;
    private static final int REQUEST_TAKE_PHOTO = 0;

    private Uri imgUri, photoURI, albumURI;
    private String currentPhotoPath;

    SimpleSocket ssocket;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Make can do socket transport in main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

//        Permission Check
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();


        editHost = (EditText) findViewById(R.id.edit_host);
        editHost.setHint(Constants.IP);

        editPort = (EditText) findViewById(R.id.edit_port);
        editPort.setHint(String.valueOf(Constants.PORT));

//        Button Set
        btnSelect = (Button) findViewById(R.id.btn_select);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "사진선택클릭");
                selectAlbum();

            }
        });


        btnCamera = (Button) findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "사진찍기클릭");
                takePhoto();
            }
        });

        imageView = (ImageView) findViewById(R.id.image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ssocket.isConnected()) {
                    File imgfile = new File(currentPhotoPath);
                    ssocket.sendString("size " + imgfile.length() + " .jpg");

                }
            }
        });

        btnResizing = (Button) findViewById(R.id.btn_resizingImage);
        btnResizing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "사진Resizing");
                makeBitmap(currentPhotoPath, 1200000);// 1.2MP
            }
        });

    }

    private Bitmap makeBitmap(String path, int IMAGE_MAX_SIZE) {

        try {
//            final int IMAGE_MAX_SIZE = 1200000;
            //resource = getResources();

            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            int scale = 1;
            while ((options.outWidth * options.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
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

            Log.d("TAG", "bitmap size - width: " +pic.getWidth() + ", height: " + pic.getHeight());
            imageView.setImageBitmap( pic );
            return pic;

        } catch (Exception e) {
            Log.e("TAG", e.getMessage(),e);
            return null;
        }

    }

    public void selectAlbum() {

        Log.d(TAG, "사진선택");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECT_PHOTO);

    }


    public void takePhoto() {

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (intent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {

                    photoFile = createImageFile();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (photoFile != null) {
                    Uri providerURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                    imgUri = providerURI;
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, providerURI);
                    startActivityForResult(intent, REQUEST_TAKE_PHOTO);

                }

            }

        } else {

            return;

        }

    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_SELECT_PHOTO: {

                //앨범에서 가져오기

                if (data.getData() != null) {

                    try {

                        File albumFile = null;

                        albumFile = createImageFile();
                        photoURI = data.getData();
                        albumURI = Uri.fromFile(albumFile);
                        imageView.setImageURI(photoURI);
                        sendPhoto(currentPhotoPath);
                        //cropImage();

                    } catch (Exception e) {

                        e.printStackTrace();

                        Log.v("알림", "앨범에서 가져오기 에러");

                    }

                }

                break;

            }

            case REQUEST_TAKE_PHOTO: {

                //촬영

                try {

                    Log.v("알림", "FROM_CAMERA 처리");

                    imageView.setImageURI(imgUri);
                    sendPhoto(currentPhotoPath);

                } catch (Exception e) {

                    e.printStackTrace();

                }

                break;

            }


        }

    }

    private void sendPhoto(String imgUri) {
        Log.v("메인", editPort.getText().toString());
        String host = Constants.IP;
        int port = Constants.PORT;
        if (!editPort.getText().toString().isEmpty()) {
            port = Integer.parseInt(String.valueOf(editPort.getText()));
        }
        if (!editHost.getText().toString().isEmpty()) {
            host = String.valueOf(editHost.getText());
        }
        ssocket = new SimpleSocket(host, port, mHandler, imgUri);
        ssocket.start();


    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            switch (inputMessage.what) {
                case Constants.SIMSOCK_CONNECTED:
                    String msg = (String) inputMessage.obj;
                    Toast.makeText(getApplicationContext(),"Socket connected",Toast.LENGTH_SHORT).show();
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
                        ssocket.sendFile(currentPhotoPath);
//                    } else {
//                        //오류
//                    }
                    // do something with UI
                    break;
                    case Constants.SIMSOCK_DATA:
                        Toast.makeText(getApplicationContext(),inputMessage.obj.toString(),Toast.LENGTH_SHORT).show();
                        Log.d("OUT", inputMessage.obj.toString());
                        break;
                case Constants.SIMSOCK_ERROR:
                    Toast.makeText(getApplicationContext(),"Error: "+ inputMessage.obj.toString(),Toast.LENGTH_SHORT).show();
                    Log.d("OUT", "error");

                    //오류
                        break;


            }
        }
    };

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
        }

    };

}
