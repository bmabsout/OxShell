package com.OxGames.OxShell;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.OxGames.OxShell.Data.DataLocation;
import com.OxGames.OxShell.Data.DataRef;
import com.OxGames.OxShell.Data.SettingsKeeper;
import com.OxGames.OxShell.Helpers.MathHelpers;
import com.OxGames.OxShell.Helpers.MusicPlayer;
import com.OxGames.OxShell.Views.MediaPlayerView;

import java.util.concurrent.atomic.AtomicBoolean;

public class MusicPlayerActivity extends PagedActivity {
    private MediaPlayerView mpv;
    private Handler trackPositionHandler;
    private final AtomicBoolean isTrackingPosition = new AtomicBoolean(false);
    private final Runnable trackPositionListener = new Runnable() {
        @Override
        public void run() {
            if (MusicPlayer.isPlaying()) {
                isTrackingPosition.set(true);
                setMediaPlayerViewPosition();
                trackPositionHandler.postDelayed(this, MathHelpers.calculateMillisForFps(60));
            } else
                isTrackingPosition.set(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri receivedPath = getIntent().getData();
        if (receivedPath != null) {
            Log.d("MusicPlayerActivity", "Received uri, sending to MusicPlayer");
            MusicPlayer.setPlaylist(DataRef.from(receivedPath, DataLocation.resolverUri));
            MusicPlayer.play();
        }

        FrameLayout parentView = new FrameLayout(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        parentView.setLayoutParams(layoutParams);
        parentView.setFocusable(false);
        parentView.setId(R.id.parent_layout);
        setContentView(parentView);

        parentView.addView(mpv = new MediaPlayerView(this));
        mpv.addMediaBtnListener(this::onMediaButtonPressed);
        mpv.addSeekBarListener(this::onSeekBarSuk);
        mpv.setIsPlaying(MusicPlayer.isPlaying());
        mpv.setTitle(MusicPlayer.getCurrentTitle());
        MusicPlayer.addIsPlayingListener(this::onMusicPlayerIsPlaying);
        MusicPlayer.addMediaItemChangedListener(this::onMusicPlayerMediaChanged);
        MusicPlayer.addSeekEventListener(this::onSeekEvent);
        startTrackingPosition();

        Log.i("MusicPlayerActivity", "Received data: " + (getIntent() != null ? getIntent().getData() : "null") + " extras: " + (getIntent() != null ? getIntent().getExtras() : "null"));
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        int systemUi = SettingsKeeper.getSystemUiVisibility();
        setMarginsFor(SettingsKeeper.hasStatusBarVisible(systemUi), SettingsKeeper.hasNavBarVisible(systemUi), mpv);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mpv.onDestroy();
        MusicPlayer.removeIsPlayingListener(this::onMusicPlayerIsPlaying);
        MusicPlayer.removeMediaItemChangedListener(this::onMusicPlayerMediaChanged);
        MusicPlayer.removeSeekEventListener(this::onSeekEvent);
    }

    private void setMediaPlayerViewPosition() {
        //Log.d("MusicPlayerActivity", "Setting position of seekbar");
        long currentPosition = MusicPlayer.getCurrentPosition();
        long currentDuration = MusicPlayer.getCurrentDuration();
        if (currentDuration > 0 && currentPosition != PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN)
            mpv.setPosition(currentPosition / (float)currentDuration);
    }

    private void onMusicPlayerIsPlaying(boolean onOff) {
        mpv.setIsPlaying(onOff);
        if (onOff)
            startTrackingPosition();
    }
    private void startTrackingPosition() {
        if (trackPositionHandler == null)
            trackPositionHandler = new Handler();
        if (!isTrackingPosition.get())
            trackPositionHandler.post(trackPositionListener);
    }
    private void onMusicPlayerMediaChanged(int index) {
        mpv.setTitle(MusicPlayer.getCurrentTitle());
        setMediaPlayerViewPosition();
    }
    private void onSeekEvent(long position) {
        setMediaPlayerViewPosition();
    }
    private void onMediaButtonPressed(MediaPlayerView.MediaButton btn) {
        switch (btn) {
            case end:
                MusicPlayer.stop();
            case back:
                finish();
                break;
            case play:
                MusicPlayer.play();
                break;
            case pause:
                MusicPlayer.pause();
                break;
            case skipNext:
                MusicPlayer.seekToNext();
                break;
            case skipPrev:
                MusicPlayer.seekToPrev();
                break;
            case seekFwd:
                MusicPlayer.seekForward();
                break;
            case seekBck:
                MusicPlayer.seekBack();
                break;
        }
    }
    private void onSeekBarSuk(float value) {
        MusicPlayer.seekTo((long)(MusicPlayer.getCurrentDuration() * value));
    }
}
