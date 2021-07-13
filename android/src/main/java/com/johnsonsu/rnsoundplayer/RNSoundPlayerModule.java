package com.johnsonsu.rnsoundplayer;


import android.net.Uri;

import java.io.File;

import java.io.IOException;

import javax.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;

public class RNSoundPlayerModule extends ReactContextBaseJavaModule {

  public final static String EVENT_FINISHED_PLAYING = "FinishedPlaying";
  public final static String EVENT_FINISHED_LOADING = "FinishedLoading";
  public final static String EVENT_FINISHED_LOADING_FILE = "FinishedLoadingFile";
  public final static String EVENT_FINISHED_LOADING_URL = "FinishedLoadingURL";

  private final ReactApplicationContext reactContext;
  private IRNMediaPlayer mediaPlayer;
  private float volume;

  private boolean useExoPlayer = true;

  public RNSoundPlayerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.volume = 1.0f;
  }

  @Override
  public String getName() {
    return "RNSoundPlayer";
  }

  @ReactMethod
  public void playSoundFile(String name, String type) throws IOException {
    mountSoundFile(name, type);
    this.resume();
  }

  @ReactMethod
  public void loadSoundFile(String name, String type) throws IOException {
    mountSoundFile(name, type);
  }

  @ReactMethod
  public void playUrl(String url) throws IOException {
    prepareUrl(url);
    this.resume();
  }

  @ReactMethod
  public void loadUrl(String url) throws IOException {
    prepareUrl(url);
  }

  @ReactMethod
  public void pause() throws IllegalStateException {
    if (this.mediaPlayer != null) {
      this.mediaPlayer.pause();
    }
  }

  @ReactMethod
  public void resume() throws IOException, IllegalStateException {
    if (this.mediaPlayer != null) {
      this.setVolume(this.volume);
      this.mediaPlayer.play();
    }
  }

  @ReactMethod
  public void stop() throws IllegalStateException {
    if (this.mediaPlayer != null) {
      this.mediaPlayer.stop();
    }
  }

  @ReactMethod
  public void seek(float seconds) throws IllegalStateException {
    if (this.mediaPlayer != null) {
      this.mediaPlayer.seekTo((int)seconds * 1000);
    }
  }

  @ReactMethod
  public void setVolume(float volume) throws IOException {
    this.volume = volume;
    if (this.mediaPlayer != null) {
      this.mediaPlayer.setVolume(volume);
    }
  }

  @ReactMethod
  public void getInfo(
          Promise promise) {
    if (this.mediaPlayer == null) {
      promise.resolve(null);
      return;
    }
    WritableMap map = Arguments.createMap();
    map.putDouble("currentTime", this.mediaPlayer.getCurrentPosition() / 1000.0);
    map.putDouble("duration", this.mediaPlayer.getDuration() / 1000.0);
    promise.resolve(map);
  }

  private void sendEvent(ReactApplicationContext reactContext,
          String eventName,
          @Nullable WritableMap params) {
    reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  private void mountSoundFile(String name, String type) throws IOException {
    int soundResID = getReactApplicationContext().getResources().getIdentifier(name, "raw", getReactApplicationContext().getPackageName());

    if (this.mediaPlayer == null) {
      this.mediaPlayer = createMediaPlayer();
      if (soundResID > 0) {
        this.mediaPlayer.setRawResourceId(soundResID);
      } else {
        this.mediaPlayer.setUri(this.getUriFromFile(name, type));
      }

      this.mediaPlayer.setOnCompletionListener(
              new IRNOnCompletionListener() {
                @Override
                public void onCompletion(IRNMediaPlayer arg0) {
                  WritableMap params = Arguments.createMap();
                  params.putBoolean("success", true);
                  sendEvent(getReactApplicationContext(), EVENT_FINISHED_PLAYING, params);
                }
              });
    } else {
      this.mediaPlayer.reset();
      if (soundResID > 0) {
        this.mediaPlayer.setRawResourceId(soundResID);
      } else {
        this.mediaPlayer.setUri(this.getUriFromFile(name, type));
      }
      this.mediaPlayer.prepare();
    }

    WritableMap params = Arguments.createMap();
    params.putBoolean("success", true);
    sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING, params);
    WritableMap onFinishedLoadingFileParams = Arguments.createMap();
    onFinishedLoadingFileParams.putBoolean("success", true);
    onFinishedLoadingFileParams.putString("name", name);
    onFinishedLoadingFileParams.putString("type", type);
    sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING_FILE, onFinishedLoadingFileParams);
  }

  private Uri getUriFromFile(String name, String type) {
    String folder = getReactApplicationContext().getFilesDir().getAbsolutePath();
    String file = name + "." + type;

    // http://blog.weston-fl.com/android-mediaplayer-prepare-throws-status0x1-error1-2147483648
    // this helps avoid a common error state when mounting the file
    File ref = new File(folder + "/" + file);

    if (ref.exists()) {
      ref.setReadable(true, false);
    }

    return Uri.parse("file://" + folder + "/" + file);
  }

  private void setMediaPlayerListeners(final String url) {
    if (this.mediaPlayer == null) {
      return;
    }

    mediaPlayer.setOnErrorListener(new IRNOnErrorListener() {
      @Override
      public boolean onError(IRNMediaPlayer mp, int what, int extra, String whatString, String extraString) {
        mp.reset();

        WritableMap params = Arguments.createMap();
        params.putBoolean("success", false);
        params.putString("url", url);

        WritableMap extraMap = Arguments.createMap();
        extraMap.putInt("what", extra);
        extraMap.putString("whatString", whatString);
        extraMap.putInt("extra", extra);
        extraMap.putString("extraString", extraString);
        params.putMap("extra", extraMap);

        sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING_URL, params);

        return true;
      }
    });
    this.mediaPlayer.setOnCompletionListener(
            new IRNOnCompletionListener() {
              @Override
              public void onCompletion(IRNMediaPlayer mediaPlayer) {
                WritableMap params = Arguments.createMap();
                params.putBoolean("success", true);
                params.putString("url", url);
                sendEvent(getReactApplicationContext(), EVENT_FINISHED_PLAYING, params);
              }
            });
    this.mediaPlayer.setOnPreparedListener(
            new IRNOnPreparedListener() {
              @Override
              public void onPrepared(IRNMediaPlayer mediaPlayer) {
                WritableMap onFinishedLoadingURLParams = Arguments.createMap();
                onFinishedLoadingURLParams.putBoolean("success", true);
                onFinishedLoadingURLParams.putString("url", url);
                sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING_URL, onFinishedLoadingURLParams);
              }
            }
    );
  }

  private void prepareUrl(final String url) throws IOException {
    Uri uri = Uri.parse(url);
    if (this.mediaPlayer == null) {
      this.mediaPlayer = createMediaPlayer();
      setMediaPlayerListeners(url);
      this.mediaPlayer.setUri(uri);
    } else {
      this.mediaPlayer.reset();
      this.mediaPlayer.setUri(uri);
    }
    this.mediaPlayer.prepare();
    WritableMap params = Arguments.createMap();
    params.putBoolean("success", true);
    params.putString("url", url);
    sendEvent(getReactApplicationContext(), EVENT_FINISHED_LOADING, params);
  }

  private IRNMediaPlayer createMediaPlayer() {
    if (useExoPlayer) {
      return new RNExoPlayer(getCurrentActivity());
    } else {
      return new RNMediaPlayer(getCurrentActivity(), getReactApplicationContext());
    }
  }
}
