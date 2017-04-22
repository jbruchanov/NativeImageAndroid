# NativeImageAndroid

This library has been created just for fun and to learn how to work with [CrystaX NDK](https://www.crystax.net/)
and kinda get more familiar with all native Android development.

NativeImage works purely outside java heap so the memory limitation is more less just by device's memory.
That means if your device has 2GiB memory you can load image and fill almost all the memory (see notes below for better understanding).

## Installation
```
dependencies {
    ...
    compile 'com.scurab:nativeimage:0.3'
}

```

Currently supported formats are only JPG and PNG.
Both are tested with some basic samples, it can handle basically whatever are [libpng](http://www.libpng.org/pub/png/libpng.html) and [libjpeg](http://libjpeg.sourceforge.net/) capable of.
Specially PNG is tested only with standard RGB/RGBA images, anything else like different color scheme etc won't work.


As far as I saw, there is no public API to draw pixels into Canvas from native code, so there has to be always some bitmap allocated in
java heap, of course only in case when you need to draw it.

## Example
```java
import com.scurab.android.nativeimage.NativeImage;

/*
create an object, pass argument if you have RGB or RGBA image, having RGB saves 25% of memory!
this is first benefit of NativeImage, as android Bitmap doesn't have only RGB_888 (24bits/pixel)
so you are always wasting 25% of memory if you don't care about alpha channel
*/
NativeImage image = new NativeImage(RGB);
/*
load Image from file, currently you can't pass directly byte[], because it could allocate again new array with copied data.
So save it first locally and then load it. Slower, but more memory efficient
*/
image.load("wherever/myhugeimage.jpg");
//some stuff

//If you need a preview, get a bitmap with preview
Bitmap bitmap = image.asScaledBitmap(500, 0);
myImageView.setBitmap(bitmap);

/*
And always be sure at the end to release the memory
We are in native code, there is no garbage collector =>
if your are done call dispose(), otherwise kittens will DIE!
*/
image.dispose();
```

Library has few basic [effects/operations](https://github.com/jbruchanov/NativeImageAndroid/blob/master/nativeimage/src/main/java/com/scurab/android/nativeimage/NativeImage.java#L503-L568) what can be done.
##### Basic ops
- Rotation
- Crop
- Flips
- Edit Brightness/Contrast
- Downscale
- [...](https://github.com/jbruchanov/NativeImageAndroid/blob/master/nativeimage/src/main/java/com/scurab/android/nativeimage/NativeImage.java#L503-L568)

Most of those are just simple pixel operations.
However there are few exceptions.

##### Rotation(int angle, boolean fast):
supported angles are only 0/90/180/270 any other angle is NOT simple pixel operation.
Fast argument is only for case of 90/270. Because the data structure is just simple single sized array.
There are 2 ways how to rotate the image.
- Quickly -> `O(m*n)` additional memory is needed (6 - 10x faster than slow version)
- Slowly -> `O(1)` constant memory complexity, but it's slow.

##### Scaling
Do always scaling/rotation during the rendering (canvas transformations). 
In case you really need to do it with image, the library will more less let you only downscale it.
[Algorithm](https://github.com/jbruchanov/NativeImage/blob/master/src/Effect.cpp#L141-L156) is very primitive to keep it fast with `O(1)` memory usage, so it's definately NOT best quality what you can get.
See `setScaledPixels`, `asScaledBitmap`

### Memory management:
Basic math here. Simple 8Mpix photo (no alpha channel/transparency) has resolution 3264x2448.
That means to create a bitmap in android you need (3264 * 2448 * 4  (Config RGBA_8888 - 1byte/color => 4bytes/pixel)) =~ **32MiB**.
Having couple of those images you will immediately hit `OutOfMemoryException`, it's better than used to be, but still not amazing.
For example 16MPix image, you need 64MiB and if you need to rotate it, you need another same size bitmap => **128MB***.
And because of memory fragmentation, you can be easily in situation of having `OutOfMemoryException` even if you have enough memory.

##### So how NativeImage handles that ?
Firstly it lives outside the java heap, so you are not strictly limited by app java heap size limit.
Secondly it can allocate 3 or 4 bytes/pixel (3 in case of RGB) so you are saving 25% of memory basically for free.
(3264 * 2448 * 3  (3bytes/pixel)) =~ **24MB**.
And then use downscaled preview which allocates smallest possible bitmap on java heap to satisfy UX.

##### There is unfortunately one significant issue.
Android OS is still continuously tracking memory footprint of all apps and if your process is just greedy and allocating all the memory, 
your process **will be unmercifully killed** even if your app is foreground. I put there simple check,
but I didn't find any usable documentation how this behaviour works, so for example
on my `Samsung S3 (I9300)` which seems to have 1GB of memory (/proc/meminfo says ~800MB),
I allocated cca 600MB of memory and process has been killed in like 3seconds.
So **you just can't really allocate all the memory**. Firstly device is getting extremely slow when
you are allocating more and more memory and getting closer to allocating all free memory and secondly
when you hit some critical limit **android will kill it** with no hesitation :).
So if you really need to do this on device, **use it wisely**.

License
-------

    Copyright 2017 Jiri Bruchanov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
