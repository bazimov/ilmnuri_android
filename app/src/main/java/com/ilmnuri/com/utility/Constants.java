package com.ilmnuri.com.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ilmnuri.com.R;


public class Constants {

    public interface ACTION{
        public static String MAIN_ACTION = "com.ilmnuri.com.action.main";
        public static String INIT_ACTION = "com.ilmnuri.com.action.init";
        public static String PREV_ACTION = "com.ilmnuri.com.action.prev";
        public static String PLAY_ACTION = "com.ilmnuri.com.action.play";
        public static String NEXT_ACTION = "com.ilmnuri.com.action.next";
        public static String CLOSE_ACTION = "com.ilmnuri.com.action.close";
        public static String STARTFOREGROUND_ACTION = "com.ilmnuri.com.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.ilmnuri.com.action.stopforeground";

    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }

    public static Bitmap getDefaultAlbumArt(Context context) {
        Bitmap bm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            bm = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.backimage, options);
        } catch (Error ee) {
        } catch (Exception e) {
        }
        return bm;
    }
}
