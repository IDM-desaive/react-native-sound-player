package com.johnsonsu.rnsoundplayer;

public interface IRNOnErrorListener {
    boolean onError(IRNMediaPlayer mediaPlayer, int what, int extra, String whatString, String extraString);
}
