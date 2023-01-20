package com.johnsonsu.rnsoundplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import com.facebook.react.bridge.Promise;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;

import java.io.IOException;
import java.util.List;

public class RNExoPlayer implements IRNMediaPlayer {
    private final Context context;
    private final SimpleExoPlayer exoPlayer;
    private IRNOnErrorListener onErrorListener;
    private IRNOnPreparedListener onPreparedListener;
    private IRNOnCompletionListener onCompletionListener;

    public RNExoPlayer(final Context context) {
        this.context = context;
        this.exoPlayer = new SimpleExoPlayer.Builder(context).build();
        this.exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onCues(List<Cue> cues) {

            }

            @Override
            public void onMetadata(Metadata metadata) {

            }

            @Override
            public void onIsLoadingChanged(boolean isLoading) {
                if (onPreparedListener != null && isLoading == false) {
                    onPreparedListener.onPrepared(RNExoPlayer.this);
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == ExoPlayer.STATE_ENDED && onCompletionListener != null) {
                    onCompletionListener.onCompletion(RNExoPlayer.this);
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                if (onErrorListener != null) {
                    String whatString = "Unknown";
                    Exception e = null;
                    switch (error.type) {
                        case ExoPlaybackException.TYPE_SOURCE:
                            whatString = "Source error";
                            e = error.getSourceException();
                            break;
                        case ExoPlaybackException.TYPE_REMOTE:
                            whatString = "Remote error";
                            break;
                        case ExoPlaybackException.TYPE_RENDERER:
                            whatString="Renderer error";
                            e = error.getRendererException();
                            break;
                        case ExoPlaybackException.TYPE_UNEXPECTED:
                            whatString = "Unexpected error";
                            e = error.getUnexpectedException();
                            break;
                    }
                    String extraString = "";
                    if (e != null) {
                        extraString = e.getMessage();
                    }
                    onErrorListener.onError(RNExoPlayer.this, error.type, error.type, whatString, extraString);
                }
            }
        });
    }

    @Override
    public void play() {
        this.exoPlayer.play();
    }

    @Override
    public void pause() {
        this.exoPlayer.pause();
    }

    @Override
    public void stop() {
        this.exoPlayer.stop();
    }

    @Override
    public void setUri(final Uri uri) throws IOException {
        final MediaItem mediaItem = MediaItem.fromUri(uri);
        this.exoPlayer.setMediaItem(mediaItem);
    }

    @Override
    public void prepare() throws IOException {
        this.exoPlayer.prepare();
    }

    @Override
    public void reset() {
        this.exoPlayer.stop(true);
    }

    @Override
    public void seekTo(int msec) {
        this.exoPlayer.seekTo(msec);
    }

    @Override
    public void setVolume(float volume) {
        this.exoPlayer.setVolume(volume);
    }

    @Override
    public long getCurrentPosition() {
        return this.exoPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return this.exoPlayer.getDuration();
    }

    @Override
    public void setOnCompletionListener(IRNOnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    @Override
    public void setOnErrorListener(IRNOnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    @Override
    public void setOnPreparedListener(IRNOnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    @Override
    public void setRawResourceId(int resourceId) throws IOException {
        final Uri uri = RawResourceDataSource.buildRawResourceUri(resourceId);
        setUri(uri);
    }

    @Override
    public void isDeviceMuted(Promise promise) {
        promise.resolve(exoPlayer.isDeviceMuted());
    }

    @Override
    public void getDeviceVolume(Promise promise) {
        int currentVolume = exoPlayer.getDeviceVolume();
        int maxVolume = exoPlayer.getDeviceInfo().maxVolume;

        float pct = (float) currentVolume / (float) maxVolume;
        promise.resolve(pct);
    }
}
