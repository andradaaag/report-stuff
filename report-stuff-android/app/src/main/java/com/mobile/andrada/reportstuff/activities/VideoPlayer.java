package com.mobile.andrada.reportstuff.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.VideoView;

import com.mobile.andrada.reportstuff.R;

import java.util.Objects;

import static com.mobile.andrada.reportstuff.activities.ChatActivity.EXTRA_MEDIA_URI;

public class VideoPlayer extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        String mediaUri = Objects.requireNonNull(getIntent().getExtras()).getString(EXTRA_MEDIA_URI);

        VideoView videoView = findViewById(R.id.video_view);
        Uri uri = Uri.parse(mediaUri);
//        videoView.setVideoURI(uri);
//
//        MediaController mediaController = new MediaController(this);
//        videoView.setMediaController(mediaController);
//        mediaController.setAnchorView(videoView);

        videoView = (VideoView)findViewById(R.id.video_view);
        videoView.setVideoURI(uri);
        videoView.setMediaController(new MediaController(this));
        videoView.requestFocus();
        videoView.start();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, ChatActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
