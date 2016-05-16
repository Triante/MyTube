package com.example.triante.mytube;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import layout.MyTubeBrowseFragment;


/**
 * Referenced from:
 * https://www.youtube.com/watch?v=a4NT5iBFuZs
 */
public class VideoPlayerActivity extends YouTubeBaseActivity {

    private TextView titleTextView, descTextView;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer.OnInitializedListener listener;
    private String vTitle, vID, vDesc;
    Button bBack, bAdd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        bAdd = (Button) findViewById(R.id.player_add_to);
        bBack = (Button) findViewById(R.id.player_back);

        titleTextView = (TextView) findViewById(R.id.player_title_text_view);
        descTextView = (TextView) findViewById(R.id.player_description);

        vTitle = getIntent().getStringExtra(MyTubeBrowseFragment.VIDEO_TITLE);
        vID = getIntent().getStringExtra(MyTubeBrowseFragment.VIDEO_ID);
        vDesc = getIntent().getStringExtra(MyTubeBrowseFragment.VIDEO_DESCRIPTION);

        youTubePlayerView = (YouTubePlayerView) findViewById(R.id.youtube_player);
        listener = new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                youTubePlayer.loadVideo(vID);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

            }
        };


        titleTextView.setText(vTitle);
        String description = "Description:\n\n" + vDesc;
        descTextView.setText(description);
        youTubePlayerView.initialize("AIzaSyB9POKflwqbgIwOxBgY0_-fQA8kAENH6BQ", listener);

        bBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        final Toast t = Toast.makeText(this, "Not yet Implemented", Toast.LENGTH_SHORT);
        bAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                t.show();
            }
        });
    }
}
