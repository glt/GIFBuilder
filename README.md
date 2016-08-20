# Android 开发之 MP4 文件转 GIF 文件详解
## 引言
之前写过一篇关于 Android 录屏的文章：[Android Lollipop (5.0) 屏幕录制实现](http://www.jianshu.com/p/e9a9771eb7b8)，现在又有个需求就是将录下来的视频选取一小段转为 GIF 文件，不仅时间段可以手动选取，而且还需要支持截取视频的局部区域转为 GIF，网上调研了一下技术方案，觉得还是有必要把实现过程拿出来分享下，有需要的可以直接拿过去用。

## 一 基本实现原理
在介绍具体实现过程之前，先简单说下基本原理和实现步骤，在解决相对比较复杂的问题，我习惯先理清主要原理步骤，不要一开始就被繁琐细节绊住，待具体实现时再逐个攻破。下面是主要步骤：

1. 视频文件的读取：包括录制和本地文件读取
2. 将需要转换的视频部分解析为 Bitmap 序列
3. 将解析好的 Bitmap 序列编码生成 GIF 文件

## 二 视频文件的读取
视频文件的读取比较简单，没什么特别需要说的地方，这里简单贴出视频读取的核心部分代码，详细实现可以看文章底部的源码地址，或者 Google 一下就行了。

```
private View.OnClickListener clickListener = new View.OnClickListener() {
  @Override
  public void onClick(View v) {
    Intent intent = new Intent();
    intent.setType("video/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Select Video"), SELECT_VIDEO);
  }
};

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (requestCode == REQUEST_SELECT_VIDEO) {
    if (resultCode == RESULT_OK) {
      Uri videoUri = data.getData();
      filePath = getRealFilePath(videoUri);
    }
  }
}
```

## 三 视频文件的解析
视频文件读取成功后，接下来要做的就是解析视频文件，选取需要转换的视频片段，提取 Bitmap 序列。下面来看下具体实现，提取 Bitmap 序列就是根据给定的起始时间和结束时间以及帧率从视频文件中获取相应的 Bitmap，本文主要是利用 `MediaMetadataRetriever` 提供的 API 来实现的，在看代码前可以先看下 `MediaMetadataRetriever` 的 [API 文档](https://developer.android.com/reference/android/media/MediaMetadataRetriever.html)，该类的核心功能就是获取视频的帧和元数据，下面是核心实现代码：
```
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

private Bitmap scale(Bitmap bitmap) {
  return Bitmap.createScaledBitmap(bitmap,
    width > 0 ? width : bitmap.getWidth(),
    height > 0 ? height : bitmap.getHeight(),
    true);
}
```

## 四 生成 GIF 文件
拿到要生成 GIF 的 Bitmap 序列，接下来需要做的就是将 Bitmap 序列中的数据按照 GIF 的文件格式编码，生成最终的 GIF 文件。目标很明确，接下来就看具体实现过程了。

### 1. GIF 格式简介
生成 GIF 文件之前有必要介绍下 GIF 的存储格式，GIF 格式的相关文章比较多，这里也没必要太详细的介绍，只是简单说下后面程序中会用到的方面。详细内容可以参考：[GIF图形文件格式文档](http://www.cnblogs.com/thinhunan/archive/2006/04/12/372942.html)
>GIF 图象是基于颜色列表的（存储的数据是该点的颜色对应于颜色列表的索引值），最多只支持 8 位（256 色）。GIF 文件内部分成许多存储块，用来存储多幅图象或者是决定图象表现行为的控制块，用以实现动画和交互式应用。GIF 文件还通过 LZW 压缩算法压缩图象数据来减少图象尺寸。
GIF 文件内部是按块划分的，包括控制块和数据块两种。控制块是控制数据块行为的，根据不同的控制块包含一些不同的控制参数；数据块只包含一些 8-bit 的字符流，由它前面的控制块来决定它的功能，每个数据块 0 到 255 个字节，数据块的第一个字节指出这个数据块大小（字节数），计算数据块的大小时不包括这个字节，所以一个空的数据块有一个字节，那就是数据块的大小0x00。

### 2. GIF 文件写入
刚开始接触 GIF 文件会觉得比较复杂，存储格式、编码格式等都比 Bitmap 要复杂的多，但其实可以把问题简单化理解，生成 GIF 和生成 Bitmap 原理类似，就是按照规定的格式写文件就行了，不用太纠结内部细节，否则就会陷入繁琐的细节（俗称钻牛角尖）而忽略了最终目的只是为了生成 GIF 文件。下面就来看下有哪些文件部分需要写入的：

#### 提取 Bitmap 的像素值
首先需要将上面得到的 Bitmap 的像素值提取出来，方便后面把像素值写入到 GIF 文件中，在提取像素值的同时，生成 GIF 文件所需要的颜色表，生成颜色表过程比较复杂，这里就不贴出源码，感兴趣的可以 Google 一下颜色量化算法，不感兴趣的直接用现成的就好，下面是提取像素值的具体实现：
```
protected void getImagePixels() {
  int w = image.getWidth();
  int h = image.getHeight();
  pixels = new byte[w*h*3];
  for (int i = 0; i < h; i++) {
    int stride = w * 3 * i;
    for (int j = 0; j < w; j++) {
      int p = image.getPixel(j, i);
      int step = j * 3;
      int offset = stride + step;
      // blue
      pixels[offset+0] = (byte) ((p & 0x0000FF) >> 0);
      // green
      pixels[offset+1] = (byte) ((p & 0x00FF00) >> 8);
      // red
      pixels[offset+2] = (byte) ((p & 0xFF0000) >> 16); 
    }
  }
}
```

#### GIF 文件头(Header)
文件头部分总共 6 个字节，包括：GIF 署名和版本号，GIF 署名由 3 个字符"GIF"组成，共 3 个字节，版本号也是由 3 个字节组成，可以为"87a"或"89a"（分别为 1987 年和 1989 年版本），实现代码如下：

```
// 写入文件头
protected void writeHeader() throws IOException {
  writeString("GIF89a");
}

protected void writeString(String s) throws IOException {
  for (int i = 0; i < s.length(); i++) {
    out.write((byte) s.charAt(i));
  }
}
```

#### 逻辑屏幕标识符(Logical Screen Descriptor)
文件头的后面是逻辑屏幕标识符(Logical Screen Descriptor)，这一部分由 7 个字节组成，定义了 GIF 图象的大小、颜色深度、背景色以及有无全局颜色列表和颜色列表的索引数。实现代码如下：

```
// 写入逻辑屏幕标识符
protected void writeLSD() throws IOException {
  writeShort(width);   // 写入图像宽度
  writeShort(height);  // 写入图像高度
  
  out.write((0x80 |  // 全局颜色列表标志置 1
             0x70 |  // 确定图象的颜色深度（7+1=8）
             0x00 |  // 全局颜色列表分类排列置为 0
             0x07)); // 颜色列表的索引数（2的7+1次方）
             
  out.write(0); // 背景颜色(在全局颜色列表中的索引)
  out.write(0); // 像素宽高比默认 1:1
}

protected void writeShort(int value) throws IOException {
  out.write(value & 0xff);
  out.write((value >> 8) & 0xff);
}
```
逻辑屏幕标识符部分结构稍微复杂些，如果不知道每一位代表什么意思可以参考：[GIF图形文件格式文档](http://www.cnblogs.com/thinhunan/archive/2006/04/12/372942.html) 中的逻辑屏幕标识符部分。

#### 全局颜色列表(Global Color Table)
全局颜色列表必须紧跟在逻辑屏幕标识符后面，每个颜色列表索引条目由三个字节组成，按R、G、B的顺序排列，具体生成颜色表的实现可以看源码部分，由于生成过程比较复杂，这里就不贴颜色表生成的代码了，下面是写入颜色表的代码：

```
// 写入颜色表
protected void writePalette() throws IOException {
  out.write(colorTab, 0, colorTab.length);
  int n = (3 * 256) - colorTab.length;
  for (int i = 0; i < n; i++) {
    out.write(0);
  }
}
```

#### 图形控制扩展(Graphic Control Extension)
这一部分是可选的，89a 版本才支持，可以放在一个图象块(包括图象标识符、局部颜色列表和图象数据)或文本扩展块的前面，用来控制跟在它后面的第一个图象（或文本）的渲染( Render )形式，下面实现代码：
```
protected void writeGraphicCtrlExt() throws IOException {
  out.write(0x21); // 扩展块标识，固定值 0x21
  out.write(0xf9); // 图形控制扩展标签，固定值 0xf9
  out.write(4);    // 块大小，固定值 4
  out.write(0 |    // 1:3 保留位
            0 |    // 4:6 不使用处置方法
            0 |    // 7 用户输入标志置 0
            0);    // 8 透明色标志置 0
  
  writeShort(delay); // 延迟时间
  out.write(0);      // 透明色索引值
  out.write(0);      // 块终结器，固定值 0
}
```

#### 图象标识符(Image Descriptor)
一个 GIF 文件内可以包含多幅图象，一幅图象结束之后紧接着下是一幅图象的标识符，图象标识符以 `0x2C`('`,`')字符开始，定义紧接着它的图象的性质，包括图象相对于逻辑屏幕边界的偏移量、图象大小以及有无局部颜色列表和颜色列表大小，由10个字节组成，下面是实现代码：
```
protected void writeImageDesc() throws IOException {
  out.write(0x2c);    // 图象标识符开始，固定值为 0x2c
  writeShort(0);      // x 方向偏移
  writeShort(0);      // y 方向偏移
  writeShort(width);  // 图像宽度
  writeShort(height); // 图像高度
  out.write((
        0x80 |        // 局部颜色列表标志置 1
        0x00 |
        0x00 |
        0x07));       // 局部颜色列表的索引数（2的7+1次方）
}
```

#### 图象数据(Image Data)
GIF 图象数据使用了 LZW 压缩算法,大大减小了图象数据的大小，具体的 LZW 压缩算法可以 Google 一下，程序实现部分可以参考文章底部的源码链接。下面是图像数据的写入实现：
```
protected void writePixels() throws IOException {
  LZWEncoder encoder = new LZWEncoder(
      width, height, indexedPixels, colorDepth);
  encoder.encode(out);
}
```

#### 文件终结器(Trailer)
这一部分只有一个字节，标识一个GIF文件结束，固定值为 `0x3B`，实现代码：

```
public void finish() throws IOException {
  out.write(0x3b);
  out.flush();
  out.close();
}
```

## 总结
到目前为止，将 MP4 文件转换为 GIF 文件的实现过程基本完成，如果需要对 GIF 文件进行裁剪、添加水印等处理的话，可以在 Bitmap 序列写入 GIF 之前，对 Bitmap 进行相应的处理即可，如果有什么问题欢迎交流学习。

[源码下载地址](https://github.com/GLGJing/GIFBuilder)
