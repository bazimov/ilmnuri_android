package com.ilmnuri.com.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ilmnuri.com.PlayActivity;
import com.ilmnuri.com.R;
import com.ilmnuri.com.event.AudioEvent;
import com.ilmnuri.com.model.AlbumModel;
import com.ilmnuri.com.model.Global;
import com.ilmnuri.com.utility.Constants;
import com.ilmnuri.com.utility.MediaCenter;

import java.io.File;

import de.greenrobot.event.EventBus;

public class NotificationService extends Service {

    Notification status;
    RemoteViews views;
    String gsonBody;
    private AlbumModel mAlbumModel;
    private final String LOG_TAG = "NotificationService";
    Gson gson;
    String category, url, fileName;
    File dir;
    MediaCenter mediaCenter;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        gson = new Gson();
        mediaCenter = MediaCenter.getInstance();
        dir = new File(Environment.getExternalStorageDirectory(), "/ilmnuri");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            gsonBody = intent.getStringExtra("album_body");
            category = intent.getStringExtra("category");
            fileName = intent.getStringExtra("song_title");
            url = intent.getStringExtra("url");
            mAlbumModel = gson.fromJson(gsonBody, new TypeToken<AlbumModel>() {
            }.getType());

            showNotification();

            Toast.makeText(this, "Darslik boshlandi", Toast.LENGTH_SHORT).show();

        } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {

            Toast.makeText(this, "Clicked Previous", Toast.LENGTH_SHORT).show();

            Log.i(LOG_TAG, "Clicked Previous");

        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {

            if (mediaCenter.isPlaying(String.valueOf(Uri.parse(dir.getPath() + "/" + fileName)))) {
                EventBus.getDefault().post(AudioEvent.pause(String.valueOf(Uri.parse(dir.getPath() + "/" + fileName))));
                Toast.makeText(this, "Pause", Toast.LENGTH_SHORT).show();
            } else {
                EventBus.getDefault().post(AudioEvent.resume(String.valueOf(Uri.parse(dir.getPath() + "/" + fileName)), 1));
                Toast.makeText(this, "Resume", Toast.LENGTH_SHORT).show();
            }


            Log.i(LOG_TAG, "Clicked Play");

        } else if (intent.getAction().equals(Constants.ACTION.NEXT_ACTION)) {

            Toast.makeText(this, "Clicked Next", Toast.LENGTH_SHORT).show();

            Log.i(LOG_TAG, "Clicked Next");
        } else if (intent.getAction().equals(

                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            EventBus.getDefault().post(AudioEvent.closePlayer());

            Log.i(LOG_TAG, "Received Stop Foreground Intent");

            Toast.makeText(this, "Darslik to'htatildi", Toast.LENGTH_SHORT).show();

            stopForeground(true);

            stopSelf();
            EventBus.getDefault().unregister(this);
        }
        return START_STICKY;

    }


    private void showNotification() {
        views = new RemoteViews(getPackageName(),
                R.layout.status_bar);

        views.setViewVisibility(R.id.status_bar_icon, View.VISIBLE);
        views.setViewVisibility(R.id.status_bar_album_art, View.GONE);

        Intent notificationIntent = new Intent(this, PlayActivity.class);
        notificationIntent.putExtra("url", url);
        notificationIntent.putExtra("album_body", gson.toJson(mAlbumModel));
        notificationIntent.putExtra("category", category);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);


        Intent playIntent = new Intent(this, NotificationService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);


        Intent closeIntent = new Intent(this, NotificationService.class);
        closeIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent pcloseIntent = PendingIntent.getService(this, 0,
                closeIntent, 0);

        views.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent);

        views.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent);
        views.setTextViewText(R.id.status_bar_track_name, Global.getInstance().getCurrentPlayingSong());
        views.setTextViewText(R.id.status_bar_artist_name, mAlbumModel.getAlbum());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            status = new Notification.Builder(this).build();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            views.setImageViewResource(R.id.status_bar_play, R.drawable.pause_jelly);
            views.setImageViewResource(R.id.status_bar_collapse, R.drawable.ic_clear_black_24dp);
            views.setTextColor(R.id.status_bar_track_name, R.color.black);
            views.setTextColor(R.id.status_bar_artist_name, R.color.black);
            status.contentView = views;
//        status.bigContentView = bigViews;
            status.flags = Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.logo;
            status.contentIntent = pendingIntent;
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
        } else {
            views.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_pause);
            views.setImageViewResource(R.id.status_bar_collapse, R.drawable.ic_clear_white_24dp);
            views.setTextColor(R.id.status_bar_track_name, R.color.white);
            views.setTextColor(R.id.status_bar_artist_name, R.color.white);
            status.contentView = views;
//        status.bigContentView = bigViews;
            status.flags = Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.logo;
            status.contentIntent = pendingIntent;
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);

        }

    }

    @Subscribe
    public void onEventMainThread(AudioEvent event) {
        if (event.getType() == AudioEvent.Type.PAUSE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                views.setImageViewResource(R.id.status_bar_play, R.drawable.play_jelly);
                views.setImageViewResource(R.id.status_bar_collapse, R.drawable.ic_clear_black_24dp);
                views.setTextColor(R.id.status_bar_track_name, R.color.black);
                views.setTextColor(R.id.status_bar_artist_name, R.color.black);
                status.contentView = views;
                status.flags = Notification.FLAG_ONGOING_EVENT;
                status.icon = R.drawable.logo;
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
            } else {
                views.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_play);
                views.setImageViewResource(R.id.status_bar_collapse, R.drawable.ic_clear_white_24dp);
                views.setTextColor(R.id.status_bar_track_name, R.color.white);
                views.setTextColor(R.id.status_bar_artist_name, R.color.white);
                status.contentView = views;
                status.flags = Notification.FLAG_ONGOING_EVENT;
                status.icon = R.drawable.logo;
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);

            }

        } else if (event.getType() == AudioEvent.Type.RESUME) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                views.setImageViewResource(R.id.status_bar_play, R.drawable.pause_jelly);
                views.setTextColor(R.id.status_bar_track_name, R.color.black);
                status.contentView = views;
                status.flags = Notification.FLAG_ONGOING_EVENT;
                status.icon = R.drawable.logo;
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
            } else {
                views.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_pause);
                views.setTextColor(R.id.status_bar_track_name, R.color.white);
                status.contentView = views;
                status.flags = Notification.FLAG_ONGOING_EVENT;
                status.icon = R.drawable.logo;
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);

            }
        }
    }
}