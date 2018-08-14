package com.ljr.com.multi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerClickEvent(findViewById(R.id.regular_bt), 3);
        registerClickEvent(findViewById(R.id.irregular_bt), 0);
    }

    private void registerClickEvent(View view, final int column) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), GridPagerTestActivity.class);
                intent.putExtra("column", column);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        });
    }
}
