package com.kaist.sep.yom;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class LoadImageActivity extends AppCompatActivity {
    private GridView gv_nearby;
    private Context mContext;

    static final int REQ_CODE_OVERLAY = 1;
    static final int REQ_CODE_REPHOTO = 2;

    int reqId;
    ImageRecommandManager mRecommandManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_image);
        mRecommandManager = new ImageRecommandManager(this.getApplicationContext());

        Intent intent = getIntent();
        reqId = intent.getIntExtra("reqid", 000);

        gv_nearby = (GridView)findViewById(R.id.gridview_image_nearby);
        final ImageAdapter ia = new ImageAdapter(this);
        gv_nearby.setAdapter(ia);

        gv_nearby.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                ia.selectImage(position);
            }
        });
    }

    public void onButtonLoadGallery(View v) {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
    }

    //갤러리로부터 복귀할때 호출됨
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("[wan]", "reqId : " + reqId);
        if(resultCode== Activity.RESULT_OK)
        {
            Log.e("[wan]", "resultCode== Activity.RESULT_OK");
            if (reqId == REQ_CODE_OVERLAY) {
                Log.e("[wan]", "reqId == REQ_CODE_OVERLAY");
                Intent intent = new Intent(this, CameraOverlayActivity.class);
                Uri uri  = data.getData();
                intent.putExtra("uri", uri.toString());
                intent.setType("image/*");
                Log.e("[wan]", "uri.toString() : " + uri.toString());
                startActivity(intent);
            }
            if (reqId == REQ_CODE_REPHOTO) {
                Log.e("[wan]", "reqId == REQ_CODE_REPHOTO");
                Intent intent = new Intent(this, CameraRephotoActivity.class);
                Uri uri  = data.getData();
                intent.putExtra("uri", uri.toString());
                Log.e("[wan]", "uri.toString() : " + uri.toString());
                intent.setType("image/*");
                startActivity(intent);
            }
        }
        else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getBaseContext(), "선택하지 않음", Toast.LENGTH_SHORT).show();
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED);
        }
    }

    public class ImageAdapter extends BaseAdapter {
        private String imgData;
        private String geoData;
        private ArrayList<String> thumbsDataList;
        private ArrayList<String> thumbsIDList;

        ImageAdapter(Context c){
            mContext = c;
            thumbsDataList = new ArrayList<String>();
            thumbsIDList = new ArrayList<String>();
            getThumbInfo(thumbsIDList, thumbsDataList);
        }

        public final void selectImage(int selectedIndex){
            String imgPath = thumbsDataList.get(selectedIndex);

            Cursor c = getContentResolver().query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, "_data = '" + imgPath + "'", null, null );
            c.moveToNext();
            int id = c.getInt( c.getColumnIndex( "_id" ) );
            Uri uri = ContentUris.withAppendedId( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id );

            //Log.e("[wan]", "imgPath : " + imgPath);
            //Log.e("[wan]", "uri : " + uri.toString());


            Intent intent;
            if (reqId == REQ_CODE_OVERLAY) {
                intent = new Intent(mContext, CameraOverlayActivity.class);
                intent.putExtra("uri", uri.toString());
                startActivity(intent);
            } else if (reqId == REQ_CODE_REPHOTO) {
                intent = new Intent(mContext, CameraRephotoActivity.class);
                intent.putExtra("uri", uri.toString());
                startActivity(intent);
            }
        }

        public boolean deleteSelected(int sIndex){
            return true;
        }

        public int getCount() {
            return thumbsIDList.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null){
                imageView = new ImageView(mContext);
                //imageView.setLayoutParams(new GridView.LayoutParams(GridLayout.LayoutParams.MATCH_PARENT, GridLayout.LayoutParams.MATCH_PARENT));
                imageView.setLayoutParams(new GridView.LayoutParams(500, 500));
                imageView.setAdjustViewBounds(false);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(2, 2, 2, 2);
            }else{
                imageView = (ImageView) convertView;
            }
            BitmapFactory.Options bo = new BitmapFactory.Options();
            bo.inSampleSize = 16;
            Bitmap bmp = BitmapFactory.decodeFile(thumbsDataList.get(position), bo);

            Bitmap resized = Bitmap.createScaledBitmap(bmp, 150, 150, true);

            try {
                ExifInterface exif = new ExifInterface(thumbsDataList.get(position));
                int exifOrientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int exifDegree = exifOrientationToDegrees(exifOrientation);
                resized = rotate(resized, exifDegree);

                imageView.setImageBitmap(resized);
            } catch (Exception e) {}

            return imageView;
        }

        private void getThumbInfo(ArrayList<String> thumbsIDs, ArrayList<String> thumbsDatas){
            ExifInterface exif;

            String[] proj = {
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    };

            String selection = MediaStore.Audio.Media.DATA + " like ? " ;
            String[] whereVal = new String[]{"%/storage/emulated/%"};

            Cursor imageCursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    proj, selection, whereVal, MediaStore.Images.Media.DEFAULT_SORT_ORDER);

            if (imageCursor != null && imageCursor.moveToFirst()) {
                String thumbsData;
                String thumbsID;
                String thumbsImageID;


                int thumbsDataCol = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int thumbsIDCol = imageCursor.getColumnIndex(MediaStore.Images.Media._ID);
                int thumbsImageIDCol = imageCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

                do {
                    thumbsData = imageCursor.getString(thumbsDataCol);
                    thumbsID = imageCursor.getString(thumbsIDCol);
                    thumbsImageID = imageCursor.getString(thumbsImageIDCol);

                    //Log.e("[wan]", "thumbsData : " + thumbsData + ", thumbsID :" + thumbsID + ", thumbsImageID : " + thumbsImageID);

                    //TODO :: 근처 사진만 가져오기!!!
                    if (mRecommandManager.isNearby(thumbsData)) {
                        if (thumbsImageID != null){
                            thumbsIDs.add(thumbsID);
                            thumbsDatas.add(thumbsData);
                            //Log.e("[wan]", "add!!!");
                        }
                    }
                }while (imageCursor.moveToNext());
            }
            //imageCursor.close();
            return;
        }

        private String getImageInfo(String ImageData, String Location, String thumbID){
            String imageDataPath = null;
            String[] proj = {MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE};
            Cursor imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    proj, "_ID='"+ thumbID +"'", null, null);

            if (imageCursor != null && imageCursor.moveToFirst()){
                if (imageCursor.getCount() > 0){
                    int imgData = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    imageDataPath = imageCursor.getString(imgData);
                }
            }
            //imageCursor.close();
            return imageDataPath;
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

