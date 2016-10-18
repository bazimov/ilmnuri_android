package com.ilmnuri.com.application;

import android.app.Application;

import com.ilmnuri.com.api.IlmApi;
import com.ilmnuri.com.module.MainModule;
import com.ilmnuri.com.utility.MediaCenter;

import javax.inject.Inject;

import dagger.ObjectGraph;


public class IlmApplication extends Application {


    private ObjectGraph mObjectGraph;
    @Inject
    IlmApi mApi;
    public boolean isViewed;
    public static IlmApplication sIlmApplication;


    @Override
    public void onCreate() {
        super.onCreate();
        mObjectGraph = ObjectGraph.create(new MainModule(this));
        inject(this);
        MediaCenter.initInstance(this);

    }

    public static IlmApplication getInstanse() {
        return sIlmApplication;
    }

    public void inject(Object object) {
        try {
            mObjectGraph.inject(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean isViewed() {
        return isViewed;
    }

    public void setViewed(boolean viewed) {
        isViewed = viewed;
    }
}
