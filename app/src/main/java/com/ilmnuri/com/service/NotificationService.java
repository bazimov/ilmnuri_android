package com.ilmnuri.com.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ilmnuri.com.PlayActivity;
import com.ilmnuri.com.R;
import com.ilmnuri.com.event.AudioEvent;
import com.ilmnuri.com.model.AlbumModel;
import com.ilmnuri.com.model.Global;
import com.ilmnuri.com.utility.Constants;
import com.ilmnuri.com.utility.MediaCenter;

import de.greenrobot.event.EventBus;


public class NotificationService extends Service {

    Notification status;
    String gsonBody;
    String url;
    private AlbumModel mAlbumModel;
    Gson mGson;
    NotificationManager mNotificationManager;
    private int Notification_ID = 101;
    MediaCenter mc;
    @Override
    public void onCreate() {
        super.onCreate();
        mGson = new Gson();
        mc = MediaCenter.getInstance();
        EventBus.getDefault().register(this);
      mNotificationManager  = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        gsonBody = intent.getStringExtra("album_body");
        mAlbumModel = mGson.fromJson(gsonBody, new TypeToken<AlbumModel>() {
        }.getType());
        url = intent.getStringExtra("url");

        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            showNotification(true);
        } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {
            Toast.makeText(this, "Clicked Previous", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
            Toast.makeText(this, "Clicked Play", Toast.LENGTH_SHORT).show();
            EventBus.getDefault().post(AudioEvent.play());
        } else if (intent.getAction().equals(Constants.ACTION.CLOSE_ACTION)) {
            stopForeground(true);
            stopSelf();
            EventBus.getDefault().post(AudioEvent.closePlayer());
//            mNotificationManager.cancel(Notification_ID);
        } else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Toast.makeText(this, "Service Stoped", Toast.LENGTH_SHORT).show();
            stopForeground(true);
            stopSelf();
        }


        return START_STICKY;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification(boolean isShown) {


//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notIntent = new Intent(this, PlayActivity.class);
        notIntent.putExtra("album_body", mGson.toJson(mAlbumModel));
        notIntent.putExtra("category", mAlbumModel.getCategory());
        notIntent.putExtra("url", url);

        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        Intent playIntent = new Intent(this, NotificationService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);

        Intent closeIntent = new Intent(this, NotificationService.class);
        closeIntent.setAction(Constants.ACTION.CLOSE_ACTION);
        PendingIntent pCloseIntent = PendingIntent.getService(this, 0, closeIntent, 0);



        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.status_bar);


        remoteViews.setTextViewText(R.id.track_name, Global.getInstance().getCurrentPlayingSong());
        remoteViews.setOnClickPendingIntent(R.id.notification_play, pPlayIntent);
        remoteViews.setOnClickPendingIntent(R.id.close_status_bar, pCloseIntent);
//        remoteViews.
        builder.setContentIntent(pendInt)
                .setContent(remoteViews)
                .setSmallIcon(R.drawable.ic_attachment)
                .setOngoing(false)
                .setAutoCancel(false)
                .setVisibility(isShown ? View.VISIBLE : View.GONE);

        Notification notification = builder.build();


        startForeground(10111, notification);
//        views.setViewVisibility(R.id.status_bar_icon, View.VISIBLE);
    }


    public void onEvent(AudioEvent event) {
        if (event.getType() == AudioEvent.Type.SHOW_NOTIFICATION) {
            showNotification(true);
        }
//        else if (event.getType() == AudioEvent.Type.CLOSE_PLAYER) {
//            showNotification(false);
//        }
    }
}
