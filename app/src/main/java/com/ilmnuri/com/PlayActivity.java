package com.ilmnuri.com;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ilmnuri.com.event.AudioEvent;
import com.ilmnuri.com.model.Api;
import com.ilmnuri.com.model.Category;
import com.ilmnuri.com.model.Global;
import com.ilmnuri.com.service.NotificationService;
import com.ilmnuri.com.utility.Constants;
import com.ilmnuri.com.utility.MediaCenter;
import com.ilmnuri.com.utility.Utils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;


public class PlayActivity extends BaseActivity {

    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;

    private ImageView imageView;

    private TextView tvTitle;
    private int currentCategory;
    private String trackPath;
    private String url;
    private String fileName;
    private File dir;
    boolean readExternalStoragePermission;
    MediaCenter mc;
    public TextView duration;
    private double timeElapsed = 0, finalTime = 0;
    private Handler durationHandler = new Handler();
    private SeekBar seekbar;
    private String albumModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);


        initVariables();
        checkReadStoragePermission();
        initUI();

        dir = new File(Environment.getExternalStorageDirectory(), "/ilmnuri");
        boolean isDirectoryCreated = dir.exists();
        if (!isDirectoryCreated) {
            isDirectoryCreated = dir.mkdirs();
        }
        if (isDirectoryCreated) {
            Log.d("mkdirs option", "Directory already exists.");
        }

        if (Utils.checkFileExist(dir.getPath() + "/" + fileName)) {
            if (readExternalStoragePermission) {
                initMediaPlayer();
                startService();
            }

        } else {
            if (!isNetworkAvailable()) {
                Utils.showToast(PlayActivity.this, "INTERNET YO'Q! Yuklay olmaysiz!");
                finish();
            }
        }
    }

    public void startService() {
        Intent serviceIntent = new Intent(PlayActivity.this, NotificationService.class);
        serviceIntent.putExtra("song_title", fileName);
        serviceIntent.putExtra("url", trackPath);
        serviceIntent.putExtra("album_body", albumModel);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(serviceIntent);
    }


    public void onEvent(AudioEvent event) {
        if (event.getType() == AudioEvent.Type.SHOW_NOTIFICATION) {
            if (mc.isPlaying(String.valueOf(Uri.parse(dir.getPath() + "/" + Global.getInstance().getCurrentPlayingSong())))) {
                mc.pauseAudio();
            }  else {
                mc.resumeAudio();
            }

        }else if (event.getType() == AudioEvent.Type.CLOSE_PLAYER) {
            mc.killMediaPlayer();
        }
    }

    private void checkReadStoragePermission() {
        int permissinCheck = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            permissinCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permissinCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                try {
                    readExternalStoragePermission = true;
                } catch (Exception e) {
                    Utils.showToast(PlayActivity.this, "Diskdan joy berilmaganga o'hshaydi!");
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            200);
                }
            }

        } else {
            readExternalStoragePermission = true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 200:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readExternalStoragePermission = true;
                } else {
                    Utils.showToast(this, "Diskga yozishga ruxsat bermabsiz!");
                    finish();
                }
        }
    }

    private void initVariables() {
        mc = MediaCenter.getInstance();
        dir = new File(Environment.getExternalStorageDirectory(), "/ilmnuri");
        boolean isDirectoryCreated = dir.exists();
        if (!isDirectoryCreated) {
            isDirectoryCreated = dir.mkdirs();
        }
        if (isDirectoryCreated) {
            Log.d("mkdirs option", "Directory already exists.");
        }

        readExternalStoragePermission = false;
        trackPath = getIntent().getStringExtra("url");
        albumModel = getIntent().getStringExtra("album_body");
        String url = Api.BaseUrl + trackPath;
        fileName = url.substring(url.lastIndexOf('/') + 1);
        String catetory = getIntent().getStringExtra("category");

        if (catetory.equals(Category.category1)) {
            currentCategory = 0;
        } else if (catetory.equals(Category.category2)) {
            currentCategory = 1;
        } else {
            currentCategory = 2;
        }

        Global.getInstance().setCurrentPlayingSong(fileName);
    }

    private void initUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (toolbar != null) {
            tvTitle = (TextView) toolbar.findViewById(R.id.tv_play_title);
        }
        tvTitle.setText(trackPath.replace(".mp3", "").replace("_", " "));

        imageView = (ImageView) findViewById(R.id.iv_play);

        ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Downloading file..");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        loadImage();

    }

    private void loadImage() {

//        String imageUrl = "";
        switch (currentCategory) {
            case 0:
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ilm1));
                break;
            case 1:
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ilm2));
                break;
            case 2:
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ilm3));
                break;

        }


    }

    ////play music===============start

    @Bind(R.id.media_play)
    View btnStart;

    public void initMediaPlayer() {
        duration = (TextView) findViewById(R.id.songDuration);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        play();

        finalTime = mc.totalDuration();

        if (seekbar != null) {
            seekbar.setMax((int) finalTime);
        }
        if (seekbar != null) {
            seekbar.setClickable(true);
        }
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekBar.setProgress(progress);
//                    mc.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaCenter.getInstance().playCurrentPosition(seekBar);
//                updateProgress();
            }
        });

    }

    @OnClick(R.id.media_play)
    void playAudio() {
        if (mc.isPlaying(String.valueOf(Uri.parse(dir.getPath() + "/" + fileName)))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));
            } else {
                btnStart.setBackgroundResource(R.drawable.play_jelly);
            }
            mc.pauseAudio();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            } else {
                btnStart.setBackgroundResource(R.drawable.pause_jelly);
            }
            mc.resumeAudio();
        }
    }

    // play mp3 song
    public void play() {
        mc.playAudio(String.valueOf(Uri.parse(dir.getPath() + "/" + fileName)));
        timeElapsed = mc.getCurrentTimePosition();
        seekbar.setProgress((int) timeElapsed);
        durationHandler.postDelayed(updateSeekBarTime, 100);
    }

    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
//            killMediaPlayer();
        }
    };
    //handler to change seekBarTime
    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {
            //get current position
            timeElapsed = mc.getCurrentTimePosition();

            //set seekbar progress
            seekbar.setProgress((int) timeElapsed);
            //set time remaing
            double timeRemaining = finalTime - timeElapsed;
            duration.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining), TimeUnit.MILLISECONDS.toSeconds((long) timeRemaining) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining))));

            //repeat yourself that again in 100 miliseconds
            durationHandler.postDelayed(this, 100);
        }
    };


    // pause mp3 song
    public void pause(View view) {
        mc.pauseAudio();
    }

    // go forward at forwardTime seconds
    public void forward(View view) {
        mc.forwardAudio();
    }

    // go backwards at backwardTime seconds
    public void rewind(View view) {
        mc.backwardAudio();
    }

    /////////////////end


    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        if (readExternalStoragePermission) {
//            if (mc != null)
//                mc.killMediaPlayer();
//        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_play, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
