package com.ilmnuri.com.utility;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.widget.SeekBar;

import com.ilmnuri.com.event.AudioEvent;

import java.io.IOException;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;


public class MediaCenter {

    private static MediaCenter instance;
    private Context mContext;
    private String audioPost;

    private MediaPlayer mediaPlayer;
    private int mPlaybackPosition;
    private int progress;
    public boolean shouldDownload;
    public boolean isPlaying;
    private boolean isOffline;
    private ArrayList<String> listPath;


    private AudioManager mAudioManager;
    private Handler handler = new Handler();
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds


    private MediaCenter(Context context) {
        this.mContext = context;

//        IShowApplication.getInstanse();
//        if (databaseHelper != null) {
//            checkRecords();
//        }
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);


    }


    public static void initInstance(Context context) {
        instance = new MediaCenter(context);
    }


    // TODO get media center from application, NPE
    public static MediaCenter getInstance() {
        return instance;
    }


    public String getAudioPost() {
        return audioPost;
    }

    public boolean isOnlinePlaying() {
        return !isOffline;
    }

    public boolean isPlaying(String post) {
        return isPlaying && audioPost != null && audioPost.equals(post);
    }


    public void forwardAudio() {
        int currentTime = mediaPlayer.getCurrentPosition();

        if (currentTime + seekForwardTime <= mediaPlayer.getDuration()) {
            mediaPlayer.seekTo(currentTime + seekForwardTime);
            mPlaybackPosition = mediaPlayer.getCurrentPosition();

        } else {
            mediaPlayer.seekTo(mediaPlayer.getDuration());
        }
        EventBus.getDefault().post(AudioEvent.forward(audioPost, currentTime + seekBackwardTime));
    }

    public void backwardAudio() {
        int currentTime = mediaPlayer.getCurrentPosition();
        if (currentTime - seekBackwardTime >= 0) {
            mediaPlayer.seekTo(currentTime - seekBackwardTime);
            mPlaybackPosition = mediaPlayer.getCurrentPosition();

        } else {
            mediaPlayer.seekTo(0);
        }
        EventBus.getDefault().post(AudioEvent.backward(audioPost, currentTime - seekBackwardTime));

    }

    public void playAudio(String post) {
        isOffline = false;
        if (audioPost != null && audioPost.equals(post)) {
            resumeAudio();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(null);
        }

        requestAudioFocus();

        killMediaPlayer();
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        this.isPlaying = true;
        this.audioPost = post;
        try {
            mediaPlayer.setDataSource(audioPost);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            if (what != -38) {
                                pauseAudio();
                            }

                            return true;
                        }
                    });
                    mediaPlayer.setOnCompletionListener(onCompletionListener);
                    handler.post(progressUpdateRunnable);
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        EventBus.getDefault().post(AudioEvent.start(audioPost));
    }

    private void requestAudioFocus() {
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }


    public void resumeAudio() {
        if (!isPlaying && audioPost != null && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            requestAudioFocus();
            isPlaying = true;
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.seekTo(mPlaybackPosition);
            mediaPlayer.start();
            handler.post(progressUpdateRunnable);
            EventBus.getDefault().post(AudioEvent.resume(audioPost, progress));
        }
    }

    public void pauseAudio() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mPlaybackPosition = mediaPlayer.getCurrentPosition();
                mediaPlayer.pause();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        isPlaying = false;
        if (audioPost != null) {
            EventBus.getDefault().post(AudioEvent.pause(audioPost));
        }
    }


    public void killMediaPlayer() {
        mPlaybackPosition = 0;
        if (audioPost != null) {
            EventBus.getDefault().post(AudioEvent.stop(audioPost));
        }

        isPlaying = false;
        audioPost = null;
        handler.removeCallbacks(progressUpdateRunnable);
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void closePlayer() {
        killMediaPlayer();
        EventBus.getDefault().post(AudioEvent.closePlayer());
    }

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if (isOffline) {

                closePlayer();

            } else {


                closePlayer();

            }
        }
    };


    private Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            try {
                if (mediaPlayer.isPlaying()) {
                    handler.postDelayed(this, 500);
                }
            } catch (Exception e) {
                //
            }

        }
    };

    private void updateProgress() {
        try {
            int dur = mediaPlayer.getDuration();
            int cur = mediaPlayer.getCurrentPosition();
            if (dur > 0) {
                progress = cur * 100 / dur;
                EventBus.getDefault().post(AudioEvent.progress(audioPost, progress));
            }
        } catch (Exception e) {
            //
        }
    }

    public void playCurrentPosition(SeekBar seekBar) {
        try {

            int dur = mediaPlayer.getDuration();
            int currentPosition = progressToTimer(seekBar.getProgress(), dur);
            mediaPlayer.seekTo(currentPosition);
            mPlaybackPosition = mediaPlayer.getCurrentPosition();

//            updateProgress();


        } catch (Exception e) {

        }
    }


    public int totalDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentTimePosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int progressToTimer(int progress, int totalDuration) {
        int currentDuration;
        totalDuration = (totalDuration / 1000);
        currentDuration = (int) ((((double) progress) / 100) * totalDuration);

        return currentDuration * 1000;
    }

    AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {

                case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
                    // Lower the volume while ducking.
                    mediaPlayer.setVolume(0.2f, 0.2f);
                    break;
                case (AudioManager.AUDIOFOCUS_GAIN_TRANSIENT):
                    mediaPlayer.setVolume(0.2f, 0.2f);
                    break;
                case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
                    pauseAudio();
                    break;

                case (AudioManager.AUDIOFOCUS_LOSS):
                    pauseAudio();
                    break;

                default:
                    break;
            }
        }
    };



    public boolean isShouldDownload() {
        return shouldDownload;
    }

    public void setShouldDownload(boolean shouldDownload) {
        this.shouldDownload = shouldDownload;
    }
}
