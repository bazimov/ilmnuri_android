package com.ilmnuri.com.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ilmnuri.com.PlayActivity;
import com.ilmnuri.com.R;
import com.ilmnuri.com.event.AudioEvent;
import com.ilmnuri.com.model.AlbumModel;
import com.ilmnuri.com.model.Audio;
import com.ilmnuri.com.utility.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class AlbumAdapterDemo extends RecyclerView.Adapter<AlbumAdapterDemo.ViewHolder> {

    private Context mContext;
    private AlbumModel mAlbumModel;
    private OnItemClickListener mOnItemClickListener;
    private List<ViewHolder> mViewHolders = new ArrayList<>();
    Handler handler;
    File dir;

    public AlbumAdapterDemo(Context context, AlbumModel albumModel, OnItemClickListener listener) {
        mContext = context;
        mAlbumModel = albumModel;
        this.mOnItemClickListener = listener;
        dir = new File(Environment.getExternalStorageDirectory(), "/ilmnuri");
        boolean isDirectoryCreated = dir.exists();
        if (!isDirectoryCreated) {
            isDirectoryCreated = dir.mkdirs();
        }
        if (isDirectoryCreated) {
            Log.d("mkdirs option", "Directory already exists.");
        }
    }

    public void deleteItem(int position) {
        mAlbumModel.getAudios().remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_album, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        mViewHolders.add(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Audio audio = getItem(position);

        if (holder.tvAlbumTitle != null) {
            assert audio != null;
            holder.tvAlbumTitle.setText(audio.getTrackName().replace(".mp3", "").replace("_", " "));
        }
        if (holder.audioSize != null) {
            assert audio != null;
            holder.audioSize.setText(audio.getTrackSize());
        }

        if (Utils.checkFileExist(dir.getPath() + "/" + mAlbumModel.getAudios().get(position).getTrackName())) {
            if (holder.btnDownload != null) {
                holder.btnDownload.setVisibility(View.INVISIBLE);
            }
            if (holder.mLinearLayout != null) {
                holder.mLinearLayout.setEnabled(true);
                holder.mLinearLayout.setClickable(true);
            }
            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.INVISIBLE);
            }
            if (holder.mLinearLayout != null) {
                holder.mLinearLayout.setEnabled(false);
                holder.mLinearLayout.setClickable(false);
            }
            if (holder.btnDownload != null) {
                holder.btnDownload.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mAlbumModel.getAudios().size();
    }

    private Audio getItem(int position) {
        if (position >= 0 && position < mAlbumModel.getAudios().size()) {
            return mAlbumModel.getAudios().get(position);
        }
        return null;
    }

    public void onEvent(AudioEvent event) {

        switch (event.getType()) {

            case STOP:
                for (ViewHolder vh : mViewHolders) {
                    if (vh != null) {
                        Audio audio = getItem(vh.getAdapterPosition());
                        if (audio != null && audio.getTrackId() == (event.getAudio().getTrackId())
                                && audio.getTrackName().equals(event.getAudio().getTrackName())) {
                            vh.closeSeekBar(true);
                        }
                    }
                }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable
        @BindView(R.id.rl_item_album)
        LinearLayout mLinearLayout;

        @Nullable
        @BindView(R.id.tv_item_album)
        TextView tvAlbumTitle;

        @Nullable
        @BindView(R.id.btn_delete)
        ImageButton btnDelete;

        @Nullable
        @BindView(R.id.btn_download)
        ImageButton btnDownload;

        @Nullable
        @BindView(R.id.audioSize)
        TextView audioSize;

        @Nullable
        @BindView(R.id.progressBar)
        SeekBar mProgressBar;

        Gson mGson;

        public ViewHolder(View itemView) {
            super(itemView);
            try {
                ButterKnife.bind(this, itemView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            handler = new Handler();
            mGson = new Gson();
        }

        @OnClick(R.id.rl_item_album)
        void clickItem() {
            Intent intent = new Intent(mContext, PlayActivity.class);
            intent.putExtra("album_body", mGson.toJson(mAlbumModel));
            intent.putExtra("category", mAlbumModel.getCategory());
            intent.putExtra("url", mAlbumModel.getCategory() + "/" + mAlbumModel.getAlbum() + "/" + mAlbumModel.getAudios().get(getAdapterPosition()).getTrackName());
            mContext.startActivity(intent);
        }

        @OnClick({R.id.btn_download, R.id.btn_delete})
        void options(View view) {
            switch (view.getId()) {
                case R.id.btn_delete:
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onDeleteListener(mAlbumModel, getAdapterPosition());
                    }
                    break;
                case R.id.btn_download:
                    mOnItemClickListener.onDownloadListener(mAlbumModel, getAdapterPosition());
                    break;
            }
        }


        public void closeSeekBar(boolean isSeekBar) {
            if (isSeekBar) {

                if (btnDownload != null) {
                    btnDownload.setVisibility(View.GONE);
                }
                if (btnDelete != null) {
                    btnDelete.setVisibility(View.VISIBLE);
                }
                if (mLinearLayout != null) {
                    mLinearLayout.setEnabled(true);
                    mLinearLayout.setClickable(true);
                }

            }
        }

    }

    public interface OnItemClickListener {
        void onDeleteListener(AlbumModel model, int position);

        void onDownloadListener(AlbumModel model, int position);
    }
}