package com.kaist.sep.yom;

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
        intent.putExtra("reqid", REQ_CODE_OVERLAY);
        startActivity(intent);
    }

    public void onButtonRephotoClicked(View v) {
        Intent intent = new Intent(this, LoadImageActivity.class);
        intent.putExtra("reqid", REQ_CODE_REPHOTO);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RESULT_CANCELED) {
        //TODO ::
        }
    }
}
