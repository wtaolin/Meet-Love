package com.huawei.esdk.uc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.huawei.esdk.uc.login.LoginActivity;

/**
 * Created by lWX303895 on 2016/3/15.
 */
public class PushActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(PushActivity.this, LoginActivity.class);
        startActivity(intent);
        PushActivity.this.finish();
    }
}
