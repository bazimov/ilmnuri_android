package com.ilmnuri.com.reciever;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ilmnuri.com.event.AudioEvent;
import com.ilmnuri.com.model.Audio;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;

import de.greenrobot.event.EventBus;


public class DownloadHelper extends Service {

    DownloadManager downloadManager;
    File dir;
    private HashMap<Long, Audio> specialFeedItem;
    Gson mGson;
    private int counter = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        specialFeedItem = new HashMap<>();
        super.onCreate();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {

        if (intent != null) {
            mGson = new Gson();
            String filePath = intent.getStringExtra("file_path");
            String fileName = intent.getStringExtra("file_name");
            String audio = intent.getStringExtra("audio");
            Type type = new TypeToken<Audio>() {
            }.getType();
            Audio mAudio = mGson.fromJson(audio, type);
            dir = new File(Environment.getExternalStorageDirectory() + "/ilmnuri");
            boolean isDirectoryCreated=dir.exists();
            if (!isDirectoryCreated) {
                isDirectoryCreated= dir.mkdirs();
            }
            if(isDirectoryCreated) {
                Log.d("mkdirs option", "Directory already exists.");
            }

            downloadManager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri download_uri = Uri.parse(filePath);
            DownloadManager.Request request = new DownloadManager.Request(download_uri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(false);
            request.setDescription(mAudio.getTrackName());
            request.setTitle("Darslik yuklanmoqda...");
            request.setDestinationInExternalPublicDir("/ilmnuri", fileName);
            final long enqueue = downloadManager.enqueue(request);

            specialFeedItem.put(enqueue, mAudio);
            Objects.requireNonNull(specialFeedItem.get(enqueue)).setDownloaded(false);
            new Thread(() -> {

                try {
                    boolean downloading = true;

                    while (downloading) {

                        DownloadManager.Query q = new DownloadManager.Query();
                        q.setFilterById(enqueue);

                        Cursor cursor = downloadManager.query(q);
                        if (cursor.moveToFirst()) {
                            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                                downloading = false;
                                if (specialFeedItem.get(enqueue) != null) {
                                    Audio audio1 = specialFeedItem.get(enqueue);
                                    Objects.requireNonNull(specialFeedItem.get(enqueue)).setDownloaded(true);
                                    EventBus.getDefault().post(AudioEvent.stop_download(audio1));
                                    counter++;
                                }

                            } else {
                                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_FAILED) {
                                    //Utils.showToast(DownloadHelper.this, "Yuklashda xatolik bo'ldi?");
                                    Log.d("DownloadHelper", "Something went wrong download failed. bilmadim yana");
                                }
                            }
                            cursor.close();
                        }
                    }
                } finally {
                    if (specialFeedItem.size() == counter) {
                        EventBus.getDefault().post(AudioEvent.update_download()
                        );
                        Log.d("Service ", " is done his job ");
                    }
                }

            }).start();

        }


        return START_STICKY;
    }
}
