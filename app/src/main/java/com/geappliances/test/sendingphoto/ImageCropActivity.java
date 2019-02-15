package com.geappliances.test.sendingphoto;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ImageCropActivity extends AppCompatActivity {
    String imgUri;
    ImageView imageView_original;
    ImageView imageView_resized_1G;
    ImageView imageView_resized_500K;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_image_crop );
//            imageResizing( "path" );
        Intent intent = getIntent();
        imgUri = intent.getStringExtra( "imgUri" );
        Log.v( "Uri", imgUri );
        Uri urivalue = Uri.parse( imgUri );

        imageView_original = (ImageView) findViewById( R.id.imageView_original );
        imageView_resized_1G = (ImageView) findViewById( R.id.imageView_resized_1G );
        imageView_resized_500K = (ImageView) findViewById( R.id.imageView_resized_500K );

        imageView_original.setImageURI(urivalue);
        resizedImage( imgUri, 1000000 );
        resizedImage( imgUri, 500000 );
    }

    private Bitmap resizedImage(String path, int IMAGE_MAX_SIZE) {
        try {
            //resource = getResources();

            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile( path, options );

            int scale = 1;
            while ((options.outWidth * options.outHeight) * (1 / Math.pow( scale, 2 )) > IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d( "TAG", "scale = " + scale + ", orig-width: " + options.outWidth + ", orig-height: " + options.outHeight );

            Bitmap pic = null;
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                options = new BitmapFactory.Options();
                options.inSampleSize = scale;
                pic = BitmapFactory.decodeFile( path, options );

                // resize to desired dimensions

                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize( size );
                int width = size.y;
                int height = size.x;

                //int height = imageView.getHeight();
                //int width = imageView.getWidth();
                Log.d( "TAG", "1th scale operation dimenions - width: " + width + ", height: " + height );

                double y = Math.sqrt( IMAGE_MAX_SIZE
                        / (((double) width) / height) );
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap( pic, (int) x, (int) y, true );
                pic.recycle();
                pic = scaledBitmap;

                System.gc();
            } else {
                pic = BitmapFactory.decodeFile( path );
            }

            Log.d( "TAG", "bitmap size - width: " + pic.getWidth() + ", height: " + pic.getHeight() );
            if (IMAGE_MAX_SIZE == 1000000) {
                imageView_resized_1G.setImageBitmap( pic );

            } else {
                imageView_resized_500K.setImageBitmap( pic );
            }
            return pic;

        } catch (Exception e) {
            Log.e( "TAG", e.getMessage(), e );
            return null;
        }
    }
}
