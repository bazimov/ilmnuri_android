package com.ilmnuri.com;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ilmnuri.com.adapter.AlbumAdapterDemo;
import com.ilmnuri.com.event.AudioEvent;
import com.ilmnuri.com.model.AlbumModel;
import com.ilmnuri.com.model.Api;
import com.ilmnuri.com.model.Audio;
import com.ilmnuri.com.reciever.DownloadHelper;
import com.ilmnuri.com.utility.DividerItemDecoration;
import com.ilmnuri.com.utility.Utils;

import java.io.File;
import java.lang.reflect.Type;

import butterknife.BindView;
import de.greenrobot.event.EventBus;


public class AlbumActivity extends BaseActivity {


    @BindView(R2.id.recyclerview)
    RecyclerView mRecyclerView;
    @BindView(R2.id.tv_album_title)
    TextView tvTitle;
    private File dir;

    String fileName;

    private AlbumModel albumModel;
    AlbumAdapterDemo adpaterDemo;

    Gson mGson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        EventBus.getDefault().register(this);
        mGson = new Gson();
        String albumBody = getIntent().getStringExtra("album");
        Type type = new TypeToken<AlbumModel>() {
        }.getType();
        albumModel = mGson.fromJson(albumBody, type);
        initView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        assert toolbar != null;
        dir = new File(Environment.getExternalStorageDirectory(), "/ilmnuri");
        boolean isDirectoryCreated=dir.exists();
        if (!isDirectoryCreated) {
            isDirectoryCreated= dir.mkdirs();
        }
        if(isDirectoryCreated) {
            // do something
            Log.d("mkdirs option", "Directory already exists.");
        }
        tvTitle.setText(String.format("%s Domla: %s", albumModel.getCategory().replace('_', ' '), albumModel.getAlbum().replace('_', ' ')));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, null));
        adpaterDemo = new AlbumAdapterDemo(this, albumModel, mOnItemClickListener);
        mRecyclerView.setAdapter(adpaterDemo);
    }

    AlbumAdapterDemo.OnItemClickListener mOnItemClickListener = new AlbumAdapterDemo.OnItemClickListener() {
        @Override
        public void onDeleteListener(AlbumModel model, int position) {
            String title = model.getAudios().get(position).getTrackName();
            alertDelete(title, position);
        }

        @Override
        public void onDownloadListener(AlbumModel model, int position) {
            String url = model.getCategory() + "/" + model.getAlbum() + "/" + model.getAudios().get(position).getTrackName();
            Audio audio = model.getAudios().get(position);
            alertDownload(url, audio);
        }


    };


    private void alertDelete(final String title, final int position) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        Utils.deleteFile(dir.getPath() + "/" + title);
                        Utils.showToast(AlbumActivity.this, "Darslik o'chirib tashlandi!");
                        adpaterDemo.deleteItem(position);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(AlbumActivity.this);
        builder.setMessage("Bu darsni o'chirib tashlashni xohlaysizmi?")
                .setPositiveButton("Ha", dialogClickListener)
                .setNegativeButton("Yo'q", dialogClickListener).show();
    }

    private void alertDownload(final String url, final Audio audio) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        String filePath = Api.BaseUrl + url;
                        fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

                        Gson gson = new Gson();

                        Intent intent = new Intent(AlbumActivity.this, DownloadHelper.class);
                        intent.putExtra("file_path", filePath);
                        intent.putExtra("file_name", fileName);

                        intent.putExtra("audio", gson.toJson(audio));
                        startService(intent);

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Bu darsni yuklab olishni xohlaysizmi? (faqat bir marta bosing...)")
                .setPositiveButton("Albatta", dialogClickListener)
                .setNegativeButton("Yo'q", dialogClickListener).show();
    }

    public void onEvent(final AudioEvent event) {
        if (adpaterDemo != null) {
            if (event.getType() == AudioEvent.Type.STOP) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adpaterDemo.onEvent(event);
                    }
                });
            } else if (event.getType() == AudioEvent.Type.UPDATE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adpaterDemo.notifyDataSetChanged();
                    }
                });
            }
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
