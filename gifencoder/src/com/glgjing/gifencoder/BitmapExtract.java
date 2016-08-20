package com.glgjing.gifencoder;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import java.util.ArrayList;
import java.util.List;


public class BitmapExtract {

  private String filePath;
  private List<Bitmap> bitmaps = new ArrayList<>();
  private int width = 0;
  private int height = 0;
  private int begin = 0;
  private int end = 0;
  private int fps = 5;


  public List<Bitmap> createBitmaps(String path) {
    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    mmr.setDataSource(path);
    double inc = 1000 * 1000 / fps;

    for (double i = begin; i < end; i += inc) {
      Bitmap frame = mmr.getFrameAtTime((long) i, MediaMetadataRetriever.OPTION_CLOSEST);
      if (frame != null) {
        bitmaps.add(scale(frame));
      }
    }

    return bitmaps;
  }

  private void setSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  private void setScope(int begin, int end) {
    this.begin = begin;
    this.end = end;
  }

  private void setFPS(int fps) {
    this.fps = fps;
  }

  private Bitmap scale(Bitmap bitmap) {
    return Bitmap.createScaledBitmap(bitmap,
        width > 0 ? width : bitmap.getWidth(),
        height > 0 ? height : bitmap.getHeight(),
        true);
  }
}
