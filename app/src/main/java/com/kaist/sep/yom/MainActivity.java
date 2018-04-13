package com.kaist.sep.yom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    static final int REQ_CODE_OVERLAY = 1;
    static final int REQ_CODE_REPHOTO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onButtonOverlayClicked(View v) {
        Intent intent = new Intent(this, LoadImageActivity.class);
        startActivityForResult(intent, REQ_CODE_OVERLAY);
    }

    public void onButtonRephotoClicked(View v) {
        Intent intent = new Intent(this, LoadImageActivity.class);
        startActivityForResult(intent, REQ_CODE_REPHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_OVERLAY) {
            if (resultCode != Activity.RESULT_CANCELED) {
                String image_uri = data.getData().toString();
                Intent intent = new Intent(this, CameraOverlayActivity.class);
                intent.putExtra("uri", image_uri);
                startActivityForResult(intent, 0);
            }
        }
        if (requestCode == REQ_CODE_REPHOTO) {
            if (resultCode != Activity.RESULT_CANCELED) {
                Intent intent = new Intent(this, CameraRephotoActivity.class);
                startActivityForResult(intent, 0);
            }
        }
    }
}
