package com.ilmnuri.com;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.ilmnuri.com.api.IlmApi;
import com.ilmnuri.com.application.IlmApplication;
import com.ilmnuri.com.utility.CacheUtils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;


public class BaseActivity extends AppCompatActivity {
    @Inject
    IlmApi mApi;

    @Inject
    CacheUtils mCacheUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getIlmApplication().inject(this);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public IlmApplication getIlmApplication() {
        return (IlmApplication) getApplication();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onEvent(){

    }

}
