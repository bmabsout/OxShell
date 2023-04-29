package com.OxGames.OxShell.Views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.OxGames.OxShell.Data.SettingsKeeper;
import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.Helpers.MathHelpers;
import com.OxGames.OxShell.OxShellApp;
import com.OxGames.OxShell.R;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MediaPlayerView extends FrameLayout {
    public enum MediaButton { back, end, play, pause, seekFwd, seekBck, skipNext, skipPrev, fullscreen }
    private final Context context;
    private FrameLayout imageView;
    private FrameLayout customActionBar;
    private FrameLayout controlsBar;
    private BetterTextView titleLabel;
    private Button backBtn;
    private Button endBtn;
    private Button playBtn;
    private Button seekFwd;
    private Button skipFwd;
    private Button seekBck;
    private Button skipPrv;
    private Slider seekBar;

    private boolean isPlaying;
    private boolean isSeeking;
    private boolean isFullscreen;

    private final List<Consumer<MediaButton>> mediaBtnListeners;
    private final List<Consumer<Float>> seekBarListeners;

    public MediaPlayerView(@NonNull Context context) {
        super(context);
        this.context = context;
        mediaBtnListeners = new ArrayList<>();
        seekBarListeners = new ArrayList<>();
        init();
    }
    public MediaPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        mediaBtnListeners = new ArrayList<>();
        seekBarListeners = new ArrayList<>();
        init();
    }
    public MediaPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        mediaBtnListeners = new ArrayList<>();
        seekBarListeners = new ArrayList<>();
        init();
    }
    public MediaPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        mediaBtnListeners = new ArrayList<>();
        seekBarListeners = new ArrayList<>();
        init();
    }

    public void setIsPlaying(boolean onOff) {
        isPlaying = onOff;
        playBtn.setBackground(ContextCompat.getDrawable(context, isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24));
    }
    public void setTitle(String value) {
        titleLabel.setText(value);
    }
    public void setPosition(float value) {
        if (!isSeeking)
            seekBar.setValue(MathHelpers.clamp(value, seekBar.getValueFrom(), seekBar.getValueTo()));
        else
            Log.w("MediaPlayerView", "Failed to set seek bar value since it is being manipulated");
    }
    @SuppressLint("ClickableViewAccessibility")
    public void onDestroy() {
        backBtn.setOnClickListener(null);
        endBtn.setOnClickListener(null);
        playBtn.setOnClickListener(null);
        seekFwd.setOnClickListener(null);
        skipFwd.setOnClickListener(null);
        seekBck.setOnClickListener(null);
        skipPrv.setOnClickListener(null);
        seekBar.clearOnSliderTouchListeners();
        setOnTouchListener(null);
        customActionBar.setOnTouchListener(null);
        controlsBar.setOnTouchListener(null);
        clearMediaBtnListeners();
        clearSeekBarListeners();
    }

    public void addMediaBtnListener(Consumer<MediaButton> mediaBtnListener) {
        mediaBtnListeners.add(mediaBtnListener);
    }
    public void removeMediaBtnListener(Consumer<MediaButton> mediaBtnListener) {
        mediaBtnListeners.remove(mediaBtnListener);
    }
    public void clearMediaBtnListeners() {
        mediaBtnListeners.clear();
    }
    private void fireMediaBtnEvent(MediaButton btn) {
        for (Consumer<MediaButton> mediaBtnListener : mediaBtnListeners)
            mediaBtnListener.accept(btn);
    }
    public void addSeekBarListener(Consumer<Float> seekBarListener) {
        seekBarListeners.add(seekBarListener);
    }
    public void removeSeekBarListener(Consumer<Float> seekBarListener) {
        seekBarListeners.remove(seekBarListener);
    }
    public void clearSeekBarListeners() {
        seekBarListeners.clear();
    }
    private void fireSeekBarEvent(float value) {
        for (Consumer<Float> seekBarListener : seekBarListeners)
            seekBarListener.accept(value);
    }

    public void setImage(Drawable drawable) {
        if (drawable == null) {
            imageView.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_baseline_headphones_24));
            imageView.setBackgroundTintList(ColorStateList.valueOf(Color.DKGRAY));
        } else {
            imageView.setBackground(drawable);
            imageView.setBackgroundTintList(null);
        }
    }
    public void setFullscreen(boolean onOff) {
        boolean fullscreenChanged = isFullscreen != onOff;
        isFullscreen = onOff;
        customActionBar.setVisibility(isFullscreen ? GONE : VISIBLE);
        controlsBar.setVisibility(isFullscreen ? GONE : VISIBLE);
        if (fullscreenChanged)
            fireMediaBtnEvent(MediaButton.fullscreen);
    }
    public boolean isFullscreen() {
        return isFullscreen;
    }
    public void refreshSize() {
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        setLayoutParams(layoutParams);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        LayoutParams layoutParams;

        int actionBarHeight = Math.round(AndroidHelpers.getScaledDpToPixels(context, 64));
        int textOutlineSize = Math.round(AndroidHelpers.getScaledDpToPixels(context, 3));
        int titleTextSize = Math.round(AndroidHelpers.getScaledSpToPixels(context, 16));
        int smallCushion = Math.round(AndroidHelpers.getScaledDpToPixels(context, 16));
        int btnSize = Math.round(AndroidHelpers.getScaledDpToPixels(context, 32));
        int seekBarThumbSize = Math.round(AndroidHelpers.getScaledDpToPixels(context, 5));
        int btnEdgeMargin = (actionBarHeight - btnSize) / 2;
        int controlsSeparationMargin = btnSize + smallCushion / 2;
        int imageSize = Math.round(Math.min(OxShellApp.getDisplayWidth(), OxShellApp.getDisplayHeight()) * 0.8f);

        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        setLayoutParams(layoutParams);
        setBackgroundColor(Color.DKGRAY);
        setOnTouchListener((view, touchEvent) -> {
            //Log.d("MediaPlayerView", touchEvent.toString());
            if (touchEvent.getAction() == MotionEvent.ACTION_UP)
                setFullscreen(!isFullscreen);
            return true;
        });
        setFocusable(false);

        FrameLayout imageBackdrop = new FrameLayout(context);
        layoutParams = new LayoutParams(imageSize, imageSize);
        layoutParams.gravity = Gravity.CENTER;
        imageBackdrop.setLayoutParams(layoutParams);
        imageBackdrop.setBackgroundColor(Color.GRAY);
        imageBackdrop.setFocusable(false);
        addView(imageBackdrop);

        imageView = new FrameLayout(context);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        imageView.setLayoutParams(layoutParams);
        imageView.setFocusable(false);
        imageBackdrop.addView(imageView);
        setImage(null);

        customActionBar = new FrameLayout(context);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, actionBarHeight);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        customActionBar.setLayoutParams(layoutParams);
        customActionBar.setBackgroundColor(Color.parseColor("#BB323232"));
        customActionBar.setFocusable(false);
        customActionBar.setOnTouchListener((view, touchEvent) -> true);
        addView(customActionBar);

        titleLabel = new BetterTextView(context);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        layoutParams.setMarginStart(btnSize + btnEdgeMargin + smallCushion);
        layoutParams.setMarginEnd(btnSize + btnEdgeMargin + smallCushion);
        titleLabel.setLayoutParams(layoutParams);
        titleLabel.setIgnoreTouchInput(true);
        titleLabel.setOverScrollMode(SCROLL_AXIS_VERTICAL);
        titleLabel.setMovementMethod(new ScrollingMovementMethod());
        titleLabel.setSingleLine(true);
        titleLabel.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        titleLabel.setMarqueeRepeatLimit(-1);
        titleLabel.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        titleLabel.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        titleLabel.setTextColor(context.getColor(R.color.text));
        titleLabel.setOutlineColor(Color.BLACK);
        titleLabel.setOutlineSize(textOutlineSize);
        titleLabel.setTextSize(titleTextSize);
        titleLabel.setText("Title");
        titleLabel.setFocusable(false);
        Typeface font = SettingsKeeper.getFont();
        titleLabel.setTypeface(font);
        titleLabel.setSelected(true);
        customActionBar.addView(titleLabel);

        backBtn = new Button(context);
        layoutParams = new LayoutParams(btnSize, btnSize);
        layoutParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        layoutParams.setMarginStart(btnEdgeMargin);
        backBtn.setLayoutParams(layoutParams);
        backBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.baseline_arrow_back_24));
        backBtn.setOnClickListener((btn) -> fireMediaBtnEvent(MediaButton.back));
        customActionBar.addView(backBtn);

        endBtn = new Button(context);
        layoutParams = new LayoutParams(btnSize, btnSize);
        layoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        layoutParams.setMarginEnd(btnEdgeMargin);
        endBtn.setLayoutParams(layoutParams);
        endBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.baseline_close_24));
        endBtn.setOnClickListener((btn) -> fireMediaBtnEvent(MediaButton.end));
        customActionBar.addView(endBtn);

        controlsBar = new FrameLayout(context);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, actionBarHeight * 2 + smallCushion);
        layoutParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        controlsBar.setLayoutParams(layoutParams);
        controlsBar.setBackgroundColor(Color.parseColor("#BB323232"));
        controlsBar.setFocusable(false);
        controlsBar.setOnTouchListener((view, touchEvent) -> true);
        addView(controlsBar);

        playBtn = new Button(context);
        layoutParams = new LayoutParams(btnSize, btnSize);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(0, controlsSeparationMargin, 0, btnEdgeMargin);
        playBtn.setLayoutParams(layoutParams);
        playBtn.setBackground(ContextCompat.getDrawable(context, isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24));
        playBtn.setOnClickListener((btn) -> fireMediaBtnEvent(isPlaying ? MediaButton.pause : MediaButton.play));
        controlsBar.addView(playBtn);

        seekFwd = new Button(context);
        layoutParams = new LayoutParams(btnSize, btnSize);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(btnSize + smallCushion, controlsSeparationMargin, 0,btnEdgeMargin);
        seekFwd.setLayoutParams(layoutParams);
        seekFwd.setBackground(ContextCompat.getDrawable(context, R.drawable.baseline_fast_forward_24));
        seekFwd.setOnClickListener((btn) -> fireMediaBtnEvent(MediaButton.seekFwd));
        controlsBar.addView(seekFwd);

        skipFwd = new Button(context);
        layoutParams = new LayoutParams(btnSize, btnSize);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins((btnSize + smallCushion) * 2, controlsSeparationMargin, 0,btnEdgeMargin);
        skipFwd.setLayoutParams(layoutParams);
        skipFwd.setBackground(ContextCompat.getDrawable(context, R.drawable.baseline_skip_next_24));
        skipFwd.setOnClickListener((btn) -> fireMediaBtnEvent(MediaButton.skipNext));
        controlsBar.addView(skipFwd);

        seekBck = new Button(context);
        layoutParams = new LayoutParams(btnSize, btnSize);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(0, controlsSeparationMargin, btnSize + smallCushion,btnEdgeMargin);
        seekBck.setLayoutParams(layoutParams);
        seekBck.setBackground(ContextCompat.getDrawable(context, R.drawable.baseline_fast_rewind_24));
        seekBck.setOnClickListener((btn) -> fireMediaBtnEvent(MediaButton.seekBck));
        controlsBar.addView(seekBck);

        skipPrv = new Button(context);
        layoutParams = new LayoutParams(btnSize, btnSize);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(0, controlsSeparationMargin, (btnSize + smallCushion) * 2,btnEdgeMargin);
        skipPrv.setLayoutParams(layoutParams);
        skipPrv.setBackground(ContextCompat.getDrawable(context, R.drawable.baseline_skip_previous_24));
        skipPrv.setOnClickListener((btn) -> fireMediaBtnEvent(MediaButton.skipPrev));
        controlsBar.addView(skipPrv);

        seekBar = new Slider(context);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, btnSize);
        params.gravity = Gravity.CENTER;
        params.setMargins(btnEdgeMargin, btnEdgeMargin, btnEdgeMargin, controlsSeparationMargin);
        seekBar.setLayoutParams(params);
        seekBar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                isSeeking = false;
                fireSeekBarEvent(slider.getValue());
            }
        });
        seekBar.setLabelBehavior(LabelFormatter.LABEL_GONE);
        seekBar.setTrackActiveTintList(ColorStateList.valueOf(Color.WHITE));
        seekBar.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        seekBar.setThumbRadius(seekBarThumbSize);
        seekBar.setHaloRadius(seekBarThumbSize * 2);
        controlsBar.addView(seekBar);
    }
}
