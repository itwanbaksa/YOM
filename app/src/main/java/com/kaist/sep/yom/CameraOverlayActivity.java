package com.kaist.sep.yom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

public class CameraOverlayActivity extends Activity implements SurfaceHolder.Callback {

    private SurfaceView mCameraView;
    private SurfaceHolder mCameraHolder;
    private Camera mCamera;
    ImageView imageView;
    boolean zoom = false;
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_overlay);

        imageView =  (ImageView) findViewById(R.id.imageView_overlay);
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        Intent intent = getIntent();
        String imageUri = intent.getStringExtra("uri");
        Uri uri = Uri.parse(imageUri);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        Log.e("[wan]", "MotionEvent.ACTION_DOWN!!!");
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        Log.e("[wan]", "MotionEvent.ACTION_POINTER_DOWN!!!");
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
                        Log.e("[wan]", "MotionEvent.ACTION_MOVE!!!");
                        if (mode == DRAG) {
                            Log.e("[wan]", "MotionEvent.ACTION_MOVE - DRAG");
                            matrix.set(savedMatrix);
                            matrix.postTranslate(event.getX() - start.x, event.getY()- start.y);

                        } else if (mode == ZOOM) {
                            Log.e("[wan]", "MotionEvent.ACTION_MOVE - ZOOM");
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
                return true;
            }
        });

        try {
            // 비트맵 이미지로 가져온다
            Bitmap image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // 이미지를 상황에 맞게 회전시킨다
            ExifInterface exif = new ExifInterface(getPath(getApplicationContext(), uri));
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifOrientationToDegrees(exifOrientation);
            image = rotate(image, exifDegree);

            // 변환된 이미지 사용
            imageView.setImageBitmap(image);
        }
        catch(Exception e) {}



        mCameraView = (SurfaceView)findViewById(R.id.surfaceView_overlay);
        init();
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

    public void onImageButtonRotateClicked(View v) {
        Log.e("[YOM]", "onImageViewClicked!!!");
    }

    public void onImageButtonCaptureClicked(View v) {
        //TODO::
    }

    private void init(){

        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);

        mCameraHolder = mCameraView.getHolder();
        mCameraHolder.addCallback(this);
        mCameraHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
            mCamera.startPreview();
        } catch (Exception e) {
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
