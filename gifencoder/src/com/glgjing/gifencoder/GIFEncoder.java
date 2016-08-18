package com.glgjing.gifencoder;

import android.graphics.Bitmap;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class GIFEncoder {

  protected int width;
  protected int height;
  protected int delay = 0; // 单位百分之一秒
  protected OutputStream out;
  protected byte[] pixels;
  protected byte[] indexedPixels;
  protected byte[] colorTab;
  protected boolean[] usedEntry = new boolean[256];


  public void init(Bitmap firstBitmap) {
    width  = firstBitmap.getWidth();
    height = firstBitmap.getHeight();
    getImagePixels(firstBitmap);
    analyzePixels();
  }

  public boolean start(OutputStream os) {
    if (os == null) {
      return false;
    }
    out = os;
    try {
      writeHeader();
      writeLSD();
      writePalette();

      writeGraphicCtrlExt();
      writeImageDesc();
      writePalette();
      writePixels();
      return true;
    } catch (IOException e) {
    }
    return false;
  }

  public boolean start(String file) {
    try {
      out = new BufferedOutputStream(new FileOutputStream(file));
      return start(out);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean addFrame(Bitmap bitmap) {
    try {
      getImagePixels(bitmap);
      analyzePixels();

      writeGraphicCtrlExt();
      writeImageDesc();
      writePalette();
      writePixels();
      return true;
    } catch (IOException e) {
    }
    return false;
  }

  // 完成 GIF 文件生成
  public boolean finish() {
    try {
      out.write(0x3b); // gif trailer
      out.flush();
      out.close();
      out = null;
      pixels = null;
      indexedPixels = null;
      colorTab = null;
      return true;
    } catch (IOException e) {
    }
    return false;
  }

  // 设置帧率
  public void setFrameRate(float fps) {
    if (fps != 0f) {
      delay = Math.round(100f / fps);
    }
  }

  // 生成颜色表
  protected void analyzePixels() {
    int len = pixels.length;
    int nPix = len / 3;
    indexedPixels = new byte[nPix];
    NeuQuant nq = new NeuQuant(pixels, len, 10);
    colorTab = nq.process();
    // convert map from BGR to RGB
    for (int i = 0; i < colorTab.length; i += 3) {
      byte temp = colorTab[i];
      colorTab[i] = colorTab[i + 2];
      colorTab[i + 2] = temp;
      usedEntry[i / 3] = false;
    }
    // map image pixels to new palette
    int k = 0;
    for (int i = 0; i < nPix; i++) {
      int index = nq.map(pixels[k++] & 0xff, pixels[k++] & 0xff,  pixels[k++] & 0xff);
      usedEntry[index] = true;
      indexedPixels[i] = (byte) index;
    }
    pixels = null;
  }

  // 提取 Bitmap 的像素值
  protected void getImagePixels(Bitmap bitmap) {
    int w = bitmap.getWidth();
    int h = bitmap.getHeight();
    pixels = new byte[w * h * 3];
    for (int i = 0; i < h; i++) {
      int stride = w * 3 * i;
      for (int j = 0; j < w; j++) {
        int p = bitmap.getPixel(j, i);
        int step = j * 3;
        int offset = stride + step;
        pixels[offset + 0] = (byte) ((p & 0x000000FF) >> 0); // blue
        pixels[offset + 1] = (byte) ((p & 0x0000FF00) >> 8); // green
        pixels[offset + 2] = (byte) ((p & 0x00FF0000) >> 16); // red
      }
    }
  }

  // 写入文件头
  protected void writeHeader() throws IOException {
    writeString("GIF89a");
  }

  // 写入图形控制扩展
  protected void writeGraphicCtrlExt() throws IOException {
    out.write(0x21); // 扩展块标识，固定值 0x21
    out.write(0xf9); // 图形控制扩展标签，固定值 0xf9
    out.write(4);    // 块大小，固定值 4
    out.write(
        0 |    // 1:3 保留位
        0 |    // 4:6 不使用处置方法
        0 |    // 7 用户输入标志置 0
        0);    // 8 透明色标志置 0

    writeShort(delay); // 延迟时间
    out.write(0);      // 透明色索引值
    out.write(0);      // 块终结器，固定值 0
  }

  // 写入图象标识符
  protected void writeImageDesc() throws IOException {
    out.write(0x2c);    // 图象标识符开始，固定值为 0x2c
    writeShort(0);      // x 方向偏移
    writeShort(0);      // y 方向偏移
    writeShort(width);  // 图像宽度
    writeShort(height); // 图像高度
    out.write((
        0x80 |  // 局部颜色列表标志置 1
        0x00 |
        0x00 |
        0x07)); // 局部颜色列表的索引数（2的7+1次方）
  }

  // 写入逻辑屏幕标识符
  protected void writeLSD() throws IOException {
    writeShort(width);   // 写入图像宽度
    writeShort(height);  // 写入图像高度

    out.write((
        0x80 |  // 全局颜色列表标志置 1
        0x70 |  // 确定图象的颜色深度（7+1=8）
        0x00 |  // 全局颜色列表分类排列置为 0
        0x07)); // 颜色列表的索引数（2的7+1次方）

    out.write(0); // 背景颜色(在全局颜色列表中的索引)
    out.write(0); // 像素宽高比默认 1:1
  }

  // 写入调色板
  protected void writePalette() throws IOException {
    out.write(colorTab, 0, colorTab.length);
    int n = (3 * 256) - colorTab.length;
    for (int i = 0; i < n; i++) {
      out.write(0);
    }
  }

  // 对 pixel 进行 LZW 编码, 并写入文件
  protected void writePixels() throws IOException {
    LZWEncoder encoder =
        new LZWEncoder(width, height, indexedPixels, 8);
    encoder.encode(out);
  }

  protected void writeShort(int value) throws IOException {
    out.write(value & 0xff);
    out.write((value >> 8) & 0xff);
  }

  protected void writeString(String s) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      out.write((byte) s.charAt(i));
    }
  }
}
