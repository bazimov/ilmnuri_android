package com.ilmnuri.com.event;

import com.ilmnuri.com.model.Audio;


public class AudioEvent {
    private Type type;
    private Audio mAudio;
    private int progress;
    private String url;

    public static AudioEvent update_download() {
        AudioEvent e = new AudioEvent();
        e.type = Type.UPDATE;
        return e;
    }

    public static AudioEvent play() {
        AudioEvent e = new AudioEvent();
        e.type = Type.SHOW_NOTIFICATION;
        return e;
    }

    public static AudioEvent closePlayer() {
        AudioEvent e = new AudioEvent();
        e.type = Type.CLOSE_PLAYER;
        return e;
    }

    public static AudioEvent start(String url){
        AudioEvent e = new AudioEvent();
        e.url =url;
        e.type = Type.START;
        return e;
    }

    public static AudioEvent stop_download(Audio id) {
        AudioEvent e = new AudioEvent();
        e.type = Type.STOP;
        e.mAudio = id;
        return e;
    }


    public static AudioEvent stop(String url) {
        AudioEvent e = new AudioEvent();
        e.type = Type.STOP;
        e.url = url;
        return e;
    }
    public static AudioEvent pause(){
        AudioEvent e = new AudioEvent();
        e.type = Type.PAUSE_NOTIFICATION;
        return e;
    }

    public static AudioEvent pause(String url){
        AudioEvent e = new AudioEvent();
        e.type = Type.PAUSE;
        return e;
    }

    public static AudioEvent forward(String post, int progress) {
        AudioEvent e = new AudioEvent();
        e.type = Type.FORWARD;
        e.progress = progress;
        return e;
    }
    public static AudioEvent backward(String post, int progress) {
        AudioEvent e = new AudioEvent();
        e.type = Type.BACKWARD;
        e.progress = progress;
        return e;
    }

    public static AudioEvent resume(String url , int progress){
        AudioEvent e = new AudioEvent();
        e.type = Type.RESUME;
        return e;
    }

    public static AudioEvent progress(String url , int progress){
        AudioEvent e = new AudioEvent();
        e.type = Type.PROGRESS;
        e.url = url;
        e.progress = progress;
        return e;

    }


    public enum Type {
        UPDATE,
        STOP,
        SHOW_NOTIFICATION,
        START,
        PAUSE,
        FORWARD,
        BACKWARD,
        RESUME,
        PAUSE_NOTIFICATION,
        CLOSE_PLAYER,
        PROGRESS
    }

    public Audio getAudio() {
        return mAudio;
    }

    public int getProgress() {
        return progress;
    }

    public Type getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }
}
