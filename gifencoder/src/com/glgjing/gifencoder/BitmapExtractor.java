package com.glgjing.gifencoder;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import java.util.ArrayList;
import java.util.List;


public class BitmapExtractor {

  private static final int US_OF_S = 1000 * 1000;

  private List<Bitmap> bitmaps = new ArrayList<>();
  private int width = 0;
  private int height = 0;
  private int begin = 0;
  private int end = 0;
  private int fps = 5;


  public List<Bitmap> createBitmaps(String path) {
    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    mmr.setDataSource(path);
    double inc = US_OF_S / fps;

    for (double i = begin * US_OF_S; i < end * US_OF_S; i += inc) {
      Bitmap frame = mmr.getFrameAtTime((long) i, MediaMetadataRetriever.OPTION_CLOSEST);
      if (frame != null) {
        bitmaps.add(scale(frame));
      }
    }

    return bitmaps;
  }

  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public void setScope(int begin, int end) {
    this.begin = begin;
    this.end = end;
  }

  public void setFPS(int fps) {
    this.fps = fps;
  }

  private Bitmap scale(Bitmap bitmap) {
    return Bitmap.createScaledBitmap(bitmap,
        width > 0 ? width : bitmap.getWidth(),
        height > 0 ? height : bitmap.getHeight(),
        true);
  }
}
