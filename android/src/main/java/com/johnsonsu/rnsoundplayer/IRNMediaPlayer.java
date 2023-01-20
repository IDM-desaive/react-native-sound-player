package com.johnsonsu.rnsoundplayer;

import android.media.MediaPlayer;
import android.net.Uri;

import com.facebook.react.bridge.Promise;

import java.io.IOException;

/**
 * Media Player abstraction interface
 */
public interface IRNMediaPlayer {
    void play();
    void pause();
    void stop();
    void setUri(final Uri uri) throws IOException;
    void setRawResourceId(final int resourceId) throws IOException;
    void prepare() throws IOException;
    void reset();
    void seekTo(final int msec);
    void setVolume(final float volume);
    void getDeviceVolume(Promise promise);
    void isDeviceMuted(Promise promise);
    long getCurrentPosition();
    long getDuration();

    void setOnCompletionListener(IRNOnCompletionListener onCompletionListener);
    void setOnErrorListener(IRNOnErrorListener onErrorListener);
    void setOnPreparedListener(IRNOnPreparedListener onPreparedListener);
}
