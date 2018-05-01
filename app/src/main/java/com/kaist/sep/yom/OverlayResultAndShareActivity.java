package com.kaist.sep.yom;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

public class OverlayResultAndShareActivity extends Activity {
    Bitmap image;
    ImageView imageView;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay_result_and_share);

        Intent intent = getIntent();
        uri  = intent.getParcelableExtra("uri");

        imageView =  (ImageView) findViewById(R.id.imageView_result);
        try {
            image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            imageView.setImageBitmap(image);
        }
        catch (Exception e) {
        }
    }

    public void onButtonShareInstagram(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        Uri permissionedUri = FileProvider.getUriForFile(this, "com.kaist.sep.yom.provider", new File(uri.getPath()));
        intent.putExtra(Intent.EXTRA_STREAM, permissionedUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage("com.instagram.android");
        startActivity(intent);
        finish();
    }
}
