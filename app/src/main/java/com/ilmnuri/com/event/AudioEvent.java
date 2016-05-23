package com.ilmnuri.com.event;

import com.ilmnuri.com.model.Audio;


public class AudioEvent {
    private Type type;
    private Audio mAudio;

    public static AudioEvent update() {
        AudioEvent e = new AudioEvent();
        e.type = Type.UPDATE;
        return e;
    }



    public static AudioEvent stop(Audio id) {
        AudioEvent e = new AudioEvent();
        e.type = Type.STOP;
        e.mAudio = id;
        return e;
    }

    public enum Type {
        UPDATE,
        STOP
    }

    public Audio getAudio() {
        return mAudio;
    }


    public Type getType() {
        return type;
    }

}
