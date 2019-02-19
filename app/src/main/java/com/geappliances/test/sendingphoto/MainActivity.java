package com.geappliances.test.sendingphoto;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.geappliances.test.sendingphoto.common.Constants;
import com.geappliances.test.sendingphoto.common.Pref;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = "메인";

    private ImageButton btnSelect;
    private ImageButton btnCamera;
    public static EditText editHost;
    public static EditText editPort;

    private static final int REQUEST_SELECT_PHOTO = 1;
    private static final int REQUEST_TAKE_PHOTO = 0;

    private Uri imgUri, photoURI, albumURI;
    private String currentPhotoPath;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Pref.initPrefs(this);


//        Permission Check
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();


        editHost = (EditText) findViewById(R.id.edit_host);
        editHost.setHint(Pref.getString(Constants.HOST,"192.168.2.10"));

        editPort = (EditText) findViewById(R.id.edit_port);
        editPort.setHint(Integer.toString(Pref.getInt(Constants.PORT,60000)));

//        Button Set
        btnSelect = (ImageButton) findViewById(R.id.btn_select);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "사진선택클릭");
                selectAlbum();

            }
        });


        btnCamera = (ImageButton) findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "사진찍기클릭");
                takePhoto();
            }
        });
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
            Toast.makeText(this,"Please, retry",Toast.LENGTH_LONG).show();

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
                        albumURI = data.getData();
                        InputStream inStream = getContentResolver().openInputStream(albumURI);
                        albumFile = writeFile(inStream);
                        Uri providerURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", albumFile);
                        imgUri = providerURI;
//                        imageView.setImageURI(imgUri);
                        sendPhoto(imgUri);
                        Log.d("album",albumFile.getAbsolutePath());
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

//                    imageView.setImageURI(imgUri);
                    sendPhoto(imgUri);

                } catch (Exception e) {

                    e.printStackTrace();

                }

                break;

            }


        }

    }

    private void sendPhoto(Uri imgUri) {
   // 이미지 편집
//        resizedImage( imgUri.toString()  );

        Log.v("메인", MainActivity.editPort.getText().toString());
        String host = Pref.getString(Constants.HOST,"192.168.2.10");
        int port = Pref.getInt(Constants.PORT);
        if (!MainActivity.editPort.getText().toString().isEmpty()) {
            port = Integer.parseInt(String.valueOf(MainActivity.editPort.getText()));
        }
        if (!MainActivity.editHost.getText().toString().isEmpty()) {
            host = String.valueOf(MainActivity.editHost.getText());
        }

        Log.v( "Uri", currentPhotoPath);
        Intent intent = new Intent(getApplicationContext(), ImageCropActivity.class);
        intent.putExtra("HOST",host);
        intent.putExtra("PORT",port);
        intent.putExtra( "imgUri", currentPhotoPath);

        startActivity(intent);
    }

    private File writeFile(InputStream in) throws IOException {
        File file = null;
        file = createImageFile();

        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public void getImage(String imageView) {

    }


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
