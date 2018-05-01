package com.kaist.sep.yom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraRephotoActivity extends Activity implements SurfaceHolder.Callback {
    View view;
    private Camera mCamera;
    private SurfaceView mCameraView;
    private SurfaceHolder mCameraHolder;

    Bitmap image;
    Bitmap shareBitmap;
    ImageView imageView;
    Uri uri_old;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_rephoto);

        Intent intent = getIntent();
        String imageUri = intent.getStringExtra("uri");
        uri_old = Uri.parse(imageUri);
        view = getWindow().getDecorView();
        imageView =  (ImageView) findViewById(R.id.imageView_rephoto);
        mCameraView = (SurfaceView)findViewById(R.id.surfaceView_rephoto);
        mCameraView.setWillNotDraw(false);

        try {
            // 비트맵 이미지로 가져온다
            image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri_old);

            // 이미지를 상황에 맞게 회전시킨다
            ExifInterface exif = new ExifInterface(getPath(getApplicationContext(), uri_old));
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifOrientationToDegrees(exifOrientation);
            image = rotate(image, exifDegree);

            // 변환된 이미지 사용
            imageView.setImageBitmap(image);
            imageView.setImageAlpha(120);
        }
        catch(Exception e) {}

        init();
    }

    private void init(){

        mCamera = Camera.open();
        //mCamera.setDisplayOrientation(90);

        mCameraHolder = mCameraView.getHolder();
        mCameraHolder.addCallback(this);
        mCameraHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void onImageButtonCaptureClicked(View v) {
        File screenShot = ScreenShot();
        if(screenShot!=null){
            //갤러리에 추가
            //sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(screenShot)));
        }

        //RephotoResultAndShareActivity에 전달
        Uri uri_now = Uri.fromFile(screenShot);

        Intent intent = new Intent(this, RephotoResultAndShareActivity.class);
        intent.putExtra("uri_old", uri_old);
        intent.putExtra("uri_now", uri_now);
        startActivityForResult(intent, 0);
        finish();
    }

    public void onImageButtonRotateClicked(View v) {
        //TODO ::
    }

    public void onImageButtonUpClicked(View v) {
        //TODO ::
    }

    public File ScreenShot(){
        imageView.setDrawingCacheEnabled(true);  //화면에 뿌릴때 캐시를 사용하게 한다
        imageView.buildDrawingCache();

        surfaceDestroyed(null); //Thread 잠시 멈춤(pause)

        Bitmap resultImage = Bitmap.createBitmap(view.getWidth(), view.getHeight(), null);
        Canvas canvas = new Canvas(resultImage);
        canvas.drawBitmap(shareBitmap, new Matrix(), null);

        String filename = "yom_rephoto_now.png";
        File file = new File(Environment.getExternalStorageDirectory()+"/Pictures", filename);  //Pictures폴더 screenshot.png 파일
        FileOutputStream os = null;
        try{
            os = new FileOutputStream(file);
            resultImage.compress(Bitmap.CompressFormat.PNG, 100, os);   //비트맵을 PNG파일로 변환
            os.close();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }

        view.setDrawingCacheEnabled(false);
        //surfaceCreated(null); //Thread 재개(resume)
        return file;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCameraHolder.getSurface() == null) {
            return;
        }

        if (mCamera == null)
            init();

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(view.getWidth(), view.getHeight());
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(parameters);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result  = (90 - degrees + 360) % 360;
        mCamera.setDisplayOrientation(result);

        try {
            mCamera.setPreviewDisplay(mCameraHolder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Camera.Parameters params = mCamera.getParameters();
                    int w = params.getPreviewSize().width;
                    int h = params.getPreviewSize().height;

                    int format = params.getPreviewFormat();
                    YuvImage image = new YuvImage(data, format, w, h, null);

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Rect area = new Rect(0, 0, w, h);
                    image.compressToJpeg(area, 100, out);
                    shareBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size()).copy(Bitmap.Config.ARGB_8888, true);
                }
            });
            mCamera.startPreview();
        } catch (Exception e) {
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
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
