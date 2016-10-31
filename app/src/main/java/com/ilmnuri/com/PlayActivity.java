package com.ilmnuri.com;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
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

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

import static com.ilmnuri.com.R.id.seekBar;


public class PlayActivity extends BaseActivity {

    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;

    @Bind(R.id.iv_play)
    ImageView imageView;

    @Bind(R.id.tv_play_title)
    TextView tvTitle;
    private int currentCategory;
    private String trackPath;
    private String url;
    private String fileName;
    private File dir;
    boolean readExternalStoragePermission;
    @Bind(R.id.songDuration)
    TextView duration;
    @Bind(R.id.media_play)
    View btnStart;
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds
    private Handler durationHandler = new Handler();
    @Bind(seekBar)
    SeekBar seekbar;
    private String albumModel;
    private MediaCenter mediaCenter;
    private AudioManager mAudioManager;
    private double timeElapsed = 0, finalTime = 0;
    private boolean isReaply;
    String catetory;
    private int progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        initVariables();
        initUI();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initMediaPlayer();
        startService();
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

    }

    public void startService() {
        Intent serviceIntent = new Intent(PlayActivity.this, NotificationService.class);
        serviceIntent.putExtra("song_title", fileName);
        serviceIntent.putExtra("url", trackPath);
        serviceIntent.putExtra("album_body", albumModel);
        serviceIntent.putExtra("category", catetory);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(serviceIntent);
    }


    public void onEvent(AudioEvent event) {
        if (event.getType() == AudioEvent.Type.PAUSE) {

            removeCallback();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));
            } else {
                btnStart.setBackgroundResource(R.drawable.play_jelly);
            }
            mediaCenter.pauseAudio();
        } else if (event.getType() == AudioEvent.Type.RESUME) {
            play();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            } else {
                btnStart.setBackgroundResource(R.drawable.pause_jelly);
            }
            mediaCenter.resumeAudio();
        } else if (event.getType() == AudioEvent.Type.CLOSE_PLAYER) {
            if (mediaCenter != null) {
                mediaCenter.killMediaPlayer();
            }
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
        dir = new File(Environment.getExternalStorageDirectory(), "/ilmnuri");
        readExternalStoragePermission = false;
        trackPath = getIntent().getStringExtra("url");
        albumModel = getIntent().getStringExtra("album_body");
        String url = Api.BaseUrl + trackPath;
        fileName = url.substring(url.lastIndexOf('/') + 1);
        catetory = getIntent().getStringExtra("category");
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
        tvTitle.setText(trackPath.replace(".mp3", "").replace("_", " "));
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


    public void initMediaPlayer() {
        isReaply = false;
        mediaCenter = MediaCenter.getInstance();
        mediaCenter.playAudio(String.valueOf(Uri.parse(dir.getPath() + "/" + fileName)));
        finalTime = mediaCenter.totalDuration();
        seekbar.setMax((int) finalTime);
        play();

    }

    @OnClick(R.id.media_play)
    void playAudio() {
        if (isReaply) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            } else {
                btnStart.setBackgroundResource(R.drawable.pause_jelly);
            }
            initMediaPlayer();
            startService();
        } else {
            if (mediaCenter.isPlaying(String.valueOf(Uri.parse(dir.getPath() + "/" + fileName)))) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));
                } else {
                    btnStart.setBackgroundResource(R.drawable.play_jelly);
                }
                mediaCenter.pauseAudio();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
                } else {
                    btnStart.setBackgroundResource(R.drawable.pause_jelly);
                }
                mediaCenter.resumeAudio();
            }
        }
    }

    public void play() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            btnStart.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
        } else {
            btnStart.setBackgroundResource(R.drawable.pause_jelly);
        }
        int position = MediaCenter.getInstance().getCurrentTimePosition();
        int total = MediaCenter.getInstance().totalDuration();
        if (position > 0) {
            progress = position * 100 / total;
        }
        seekbar.setMax(mediaCenter.totalDuration());
        updateProgress();
    }


    public void removeCallback() {
        durationHandler.removeCallbacks(mUpdateTimeTask);
    }

    public void updateProgress() {
        durationHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = MediaCenter.getInstance().totalDuration();
            long currentDuration = MediaCenter.getInstance().getCurrentTimePosition();

            duration.setText("" + milliSecondsToTimer(currentDuration));
            int progress = getProgressPercentage(currentDuration, totalDuration);
            int mCurrentPosition = mediaCenter.getCurrentTimePosition() / 1000;
            seekbar.setProgress(mCurrentPosition);

            durationHandler.postDelayed(this, 100);
        }
    };

    public int getProgressPercentage(long currentDuration, long totalDuration) {
        Double percentage = (double) 0;

        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);

        percentage = (((double) currentSeconds) / totalSeconds) * 100;

        return percentage.intValue();
    }

    public String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        return finalTimerString;
    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//            updateProgressPosition(progress);
//            try {
//                if (mediaCenter.isPlaying(fileName) || mediaCenter != null) {
//                    if (fromUser)
//                        mediaCenter.playCurrentPosition(seekBar);
//                } else if (mediaCenter == null) {
//                    Toast.makeText(getApplicationContext(), "Media is not running",
//                            Toast.LENGTH_SHORT).show();
//                    seekBar.setProgress(0);
//                }
//            } catch (Exception e) {
//                Log.e("seek bar", "" + e);
//                seekBar.setEnabled(false);
//
//            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            MediaCenter.getInstance().playCurrentPosition(seekBar);
            updateProgress();
        }
    };

    @OnClick({R.id.media_backward, R.id.media_forward})
    void click(View view) {
        switch (view.getId()) {
            case R.id.media_backward: {
                rewind(view);
                break;
            }
            case R.id.media_forward: {
                forward(view);
                break;
            }
        }
    }

    public void forward(View view) {
        mediaCenter.forwardAudio();
    }

    public void rewind(View view) {
        mediaCenter.backwardAudio();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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


}