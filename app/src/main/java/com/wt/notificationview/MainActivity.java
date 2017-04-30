package com.wt.notificationview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.wt.notificationview.widget.CustomNotificationView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements CustomNotificationView.ClickCallBack {

    @BindView(R.id.interal_notify_container)
    CustomNotificationView mNotificatioNView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

    }

    @OnClick(R.id.click_btn)
    public void showNotification(View view){
        mNotificatioNView.refreshByData("测试文本-标题" , this);
    }


    @Override
    public void clickCall() {
        showToast("点击了通知栏");
    }

    private void showToast(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this , msg , Toast.LENGTH_SHORT).show();
            }
        });
    }
}
