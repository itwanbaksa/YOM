package com.kaist.sep.yom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RephotoResultAndShareActivity extends Activity {

    Bitmap image_old;
    Bitmap image_now;
    Bitmap resultImage;

    View view;
    ImageView imageView_old;
    ImageView imageView_now;

    File resultFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rephoto_result_and_share);

        Intent intent = getIntent();
        Uri uri_old  = intent.getParcelableExtra("uri_old");
        Uri uri_now  = intent.getParcelableExtra("uri_now");

        imageView_old =  (ImageView) findViewById(R.id.imageView_old);
        imageView_now =  (ImageView) findViewById(R.id.imageView_now);

        imageView_old.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView_now.setScaleType(ImageView.ScaleType.CENTER_CROP);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        imageView_old.setColorFilter(f);


        try {
            image_old = MediaStore.Images.Media.getBitmap(getContentResolver(), uri_old);
            image_now = MediaStore.Images.Media.getBitmap(getContentResolver(), uri_now);

            // 이미지를 상황에 맞게 회전시킨다
            ExifInterface exif = new ExifInterface(getPath(getApplicationContext(), uri_old));
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifOrientationToDegrees(exifOrientation);
            image_old = rotate(image_old, exifDegree);

            imageView_old.setImageBitmap(image_old);
            imageView_now.setImageBitmap(image_now);
        }
        catch (Exception e) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        view = getWindow().getDecorView();
        imageView_old.setLeft(0);
        imageView_old.setTop(0);

        imageView_now.setLeft(view.getWidth()/2);
        imageView_now.setTop(0);

        makeResultImage();
    }

    public void makeResultImage() {
        Bitmap resultImage = Bitmap.createBitmap(view.getWidth(), view.getHeight(), null);
        Canvas canvas = new Canvas(resultImage);

        imageView_old.setDrawingCacheEnabled(true);  //화면에 뿌릴때 캐시를 사용하게 한다
        imageView_old.buildDrawingCache();
        Bitmap bm1 = imageView_old.getDrawingCache();
        canvas.drawBitmap(bm1, new Matrix(), null);

        imageView_now.setDrawingCacheEnabled(true);  //화면에 뿌릴때 캐시를 사용하게 한다
        imageView_now.buildDrawingCache();
        Bitmap bm2 = imageView_now.getDrawingCache();
        canvas.drawBitmap(bm2, view.getWidth()/2, 0, null);

        String filename = "yom_rephoto_result.png";
        resultFile = new File(Environment.getExternalStorageDirectory()+"/Pictures", filename);  //Pictures폴더 screenshot.png 파일
        FileOutputStream os = null;
        try{
            os = new FileOutputStream(resultFile);
            resultImage.compress(Bitmap.CompressFormat.JPEG, 100, os);   //비트맵을 PNG파일로 변환
            os.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public void onButtonShareInstagram(View v) {

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        Uri permissionedUri = FileProvider.getUriForFile(this, "com.kaist.sep.yom.provider", resultFile);
        intent.putExtra(Intent.EXTRA_STREAM, permissionedUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage("com.instagram.android");
        startActivity(intent);
        finish();
    }

    public int exifOrientationToDegrees(int exifOrientation) {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    public Bitmap rotate(Bitmap bitmap, int degrees) {
        if(degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);

            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted) {
                    bitmap.recycle();
                    bitmap = converted;
                }
            }
            catch(OutOfMemoryError ex) {}
        }
        return bitmap;
    }

    public String getPath(Context cxt, Uri uri) {
        Cursor cursor = cxt.getContentResolver().query(uri, null, null, null, null );
        cursor.moveToNext();
        String path = cursor.getString( cursor.getColumnIndex( "_data" ) );
        cursor.close();

        return path;
    }
}
