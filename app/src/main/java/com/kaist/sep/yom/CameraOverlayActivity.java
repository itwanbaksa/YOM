package com.kaist.sep.yom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
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

public class CameraOverlayActivity extends Activity implements SurfaceHolder.Callback {

    private SurfaceView mCameraView;
    private SurfaceHolder mCameraHolder;
    private Camera mCamera;
    View view;
    ImageView imageView;
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    Bitmap image;
    Bitmap resizedImage;
    Bitmap shareBitmap;

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    YuvImage yuv_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_overlay);

        Intent intent = getIntent();
        String imageUri = intent.getStringExtra("uri");
        Uri uri = Uri.parse(imageUri);

        view = getWindow().getDecorView();
        imageView =  (ImageView) findViewById(R.id.imageView_overlay);
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        matrix.postScale(0.3f, 0.3f, 0, 0);
        imageView.setImageMatrix(matrix);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        if (oldDist > 10f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode = ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            matrix.set(savedMatrix);
                            matrix.postTranslate(event.getX() - start.x, event.getY()- start.y);
                        } else if (mode == ZOOM) {
                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                matrix.set(savedMatrix);
                                float scale = newDist / oldDist;
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }
                        }
                        break;
                }
                imageView.setImageMatrix(matrix);
                //resizedImage = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
                return true;
            }
        });

        try {
            // 비트맵 이미지로 가져온다
            image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // 이미지를 상황에 맞게 회전시킨다
            ExifInterface exif = new ExifInterface(getPath(getApplicationContext(), uri));
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifOrientationToDegrees(exifOrientation);
            image = rotate(image, exifDegree);

            // 변환된 이미지 사용
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
            imageView.setColorFilter(f);
            image = addWhiteBorder(image, 100);

            imageView.setImageBitmap(image);

        }
        catch(Exception e) {}

        mCameraView = (SurfaceView)findViewById(R.id.surfaceView_overlay);
        mCameraView.setWillNotDraw(false);
        init();
    }

    private Bitmap addWhiteBorder(Bitmap bmp, int borderSize) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }

    public void onImageButtonCaptureClicked(View v) {
        if (makePreviewImage() == false)
            return;

        Log.e("wan", "onImageButtonCaptureClicked -1");
        File screenShot = ScreenShot();
        Log.e("wan", "onImageButtonCaptureClicked -2");
        if(screenShot!=null){
            //갤러리에 추가
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(screenShot)));
        }
        Log.e("wan", "send OverlayResultAndShareActivity!!!");
        //OverlayResultAndShareActivity에 전달
        Uri uri = Uri.fromFile(screenShot);
        Intent intent = new Intent(this, OverlayResultAndShareActivity.class);
        intent.putExtra("uri", uri);
        startActivityForResult(intent, 0);
        Log.e("wan", "sent!!!");
        finish();

    }

    public void onImageButtonRotateClicked(View v) {
        //TODO ::
    }

    public void onImageButtonUpClicked(View v) {
        //TODO :: 화면 미세조정
    }

    public File ScreenShot(){
        imageView.setDrawingCacheEnabled(true);  //화면에 뿌릴때 캐시를 사용하게 한다
        imageView.buildDrawingCache();

        surfaceDestroyed(null); //Thread 잠시 멈춤(pause)
        Bitmap resultImage = Bitmap.createBitmap(view.getWidth(), view.getHeight(), null);
        Canvas canvas = new Canvas(resultImage);
        canvas.drawBitmap(shareBitmap, new Matrix(), null);
        Bitmap bm = imageView.getDrawingCache();
        canvas.drawBitmap(bm, new Matrix(), null);


        String filename = "yom_overlay.png";
        File file = new File(Environment.getExternalStorageDirectory()+"/Pictures", filename);  //Pictures폴더 screenshot.png 파일
        FileOutputStream os = null;
        try{
            os = new FileOutputStream(file);
            resultImage.compress(Bitmap.CompressFormat.JPEG, 80, os);   //비트맵을 JPEG파일로 변환
            os.close();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
        view.setDrawingCacheEnabled(false);
        //surfaceCreated(null); //Thread 재개(resume)
        return file;
    }

    private void init(){

        mCamera = Camera.open();
        //mCamera.setDisplayOrientation(90);

        mCameraHolder = mCameraView.getHolder();
        mCameraHolder.addCallback(this);
        mCameraHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(previewCallback);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("wan", "surfaceChanged!!!");
        if (mCameraHolder.getSurface() == null) {
            return;
        }

        if (mCamera == null)
            init();

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }

        try {
            mCamera.setPreviewDisplay(mCameraHolder);
            mCamera.setPreviewCallback(previewCallback);
            mCamera.startPreview();
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
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters params = mCamera.getParameters();
            int w = params.getPreviewSize().width;
            int h = params.getPreviewSize().height;

            int format = params.getPreviewFormat();
            yuv_image = new YuvImage(data, format, w, h, null);
        }
    };

    public boolean makePreviewImage() {
        Camera.Parameters params = mCamera.getParameters();
        int w = params.getPreviewSize().width;
        int h = params.getPreviewSize().height;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rect area = new Rect(0, 0, w, h);
        yuv_image.compressToJpeg(area, 80, out);

        shareBitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size()).copy(Bitmap.Config.ARGB_8888, true);
        return (shareBitmap != null);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("wan", "surfaceDestroyed!!!");
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = 2*imageView.getLeft() + event.getX(0) + event.getX(1);
        float y = 2*imageView.getTop() + event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
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


