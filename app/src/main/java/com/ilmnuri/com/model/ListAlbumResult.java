package com.ilmnuri.com.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;


public class ListAlbumResult  {

    @SerializedName("albums")
    ArrayList<AlbumModel> mAlbumModels;





    public ArrayList<AlbumModel> getAlbumModels() {
        return mAlbumModels;
    }

    public void setAlbumModels(ArrayList<AlbumModel> albumModels) {
        mAlbumModels = albumModels;
    }


}
