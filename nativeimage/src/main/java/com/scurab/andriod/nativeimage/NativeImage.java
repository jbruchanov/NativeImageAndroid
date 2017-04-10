package com.scurab.andriod.nativeimage;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

/**
 * Created by JBruchanov on 03/04/2017.
 */

@SuppressWarnings({"unused", "JniMissingFunction"})
public class NativeImage {

    @IntDef(value = {ColorSpace.RGB, ColorSpace.RGBA})
    public @interface ColorSpace {
        int RGB = 3;
        int RGBA = 4;
    }

    public static final int NO_ERR = 0;
    public static final int CANT_OPEN_FILE = -1;
    public static final int OUT_OF_MEMORY = -2;
    public static final int NO_DATA = -3;
    public static final int NOT_SAME_RESOLUTION = -201;
    public static final int INVALID_BITMAP_FORMAT = -202;
    public static final int INVALID_RESOLUTION = -203;
    public static final int INVALID_JSON = -301;
    public static final int INVALID_PNG = -401;
    public static final int NOT_SUPPORTED_PNG_CONFIGURATION = -402;
    public static final int ERR_EFFECT_NOT_DEFINED = -800;
    public static final int ERR_UNKNOWN = -999;

    static final String IMAGE_WIDTH = "imageWidth";
    static final String IMAGE_HEIGHT = "imageHeight";
    private static final float LIMIT = 0.85f;//10%, don't allocate if we hit this limit
    private static final long GB = 1024L * 1024 * 1024;

    public enum Format {
        JPEG_RGB(1), PNG_RGB(2), PNG_RGBA(3);

        int processor;

        Format(int processor) {
            this.processor = processor;
        }
    }

    private static final HashSet<Long> POINTER_TRACKER = new HashSet<>();
    private static long sAllocatedMemory = 0;

    private long mNativeRef;
    private int mBytesPerPixel;

    static {
        System.loadLibrary("crystax");
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("nimage");
    }

    public NativeImage() {
        this(ColorSpace.RGB);
    }

    public NativeImage(@ColorSpace int bytesPerPixel) {
        mBytesPerPixel = bytesPerPixel;
        if (!(bytesPerPixel == ColorSpace.RGB || bytesPerPixel == ColorSpace.RGBA)) {
            throw new IllegalStateException(String.format("Invalid bytesPerPixel:%s valid is only 3 or 4", bytesPerPixel));
        }
        _init(mBytesPerPixel);
        if (mNativeRef == 0) {
            throw new IllegalStateException("Unable to init native code!");
        }
    }

    /**
     * Initialize C++ class
     * @param componentsPerPixel
     */
    private native void _init(int componentsPerPixel);

    /**
     * Load image, format is detected based on path (extension file .png .jpg or .jpeg)
     * @param path
     * @return
     * @throws OutOfMemoryError, IllegalStateException if format can't be detected
     */
    public int loadImage(@NonNull String path) throws OutOfMemoryError {
        return loadImage(path, getFormatBasedExt(path));
    }

    /**
     * Load Image
     * @param path
     * @param format jpeg or PNG, if PNG doesn't matter if RGB or RGBA
     * @return
     * @throws OutOfMemoryError
     */
    public int loadImage(@NonNull String path, @NonNull Format format) throws OutOfMemoryError {
        checkFreMemory(path);
        int result = _loadImage(path, format.processor);
        sAllocatedMemory += getAllocatedBytes();
        return result;
    }

    private native int _loadImage(String path, int processor);

    /**
     * Save image
     * @param path target location
     * @param format
     * @return
     */
    public int saveImage(@NonNull String path, @NonNull Format format) {
        return _saveImage(path, format.processor, null);
    }

    /**
     *
     * @param path
     * @param format
     * @param params use {@link SaveParamsBuilder}
     * @return
     */
    public int saveImage(@NonNull String path, @NonNull Format format, @Nullable String params) {
        return _saveImage(path, format.processor, params);
    }

    private native int _saveImage(String path, int processor, String params);

    /**
     * Get image metadata JSON
     * @return
     */
    @NonNull
    public MetaData getMetaData() {
        return new MetaData(_getMetaData(), mBytesPerPixel);
    }

    private native String _getMetaData();

    /**
     * Relase image from memory, be sure you always call this!
     */
    public void dispose() {
        sAllocatedMemory -= getAllocatedBytes();
        POINTER_TRACKER.remove(mNativeRef);
        _dispose();
        mNativeRef = 0;
    }

    private native void _dispose();

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mNativeRef != 0) {
            dispose();
        }
    }

    /**
     * Set pixels into outBitmap, be sure outBitmap is matching size!
     * @param outBitmap
     * @return
     */
    public int setPixels(@NonNull Bitmap outBitmap) {
        assertRGBABitmap(outBitmap);
        final MetaData metaData = getMetaData();
        return _setPixels(outBitmap, 0, 0, metaData.width, metaData.height);
    }

    /**
     * Set pixels into bitmap. This is basically crop operation
     * @param outBitmap
     * @param offsetX
     * @param offsetY
     * @param width
     * @param height
     * @return
     */
    public int setPixels(Bitmap outBitmap, int offsetX, int offsetY, int width, int height) {
        assertRGBABitmap(outBitmap);
        return _setPixels(outBitmap, offsetX, offsetY, width, height);
    }

    private void assertRGBABitmap(Bitmap bitmap) {
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            throw new IllegalArgumentException(String.format("Bitmap has invalid Config:%s, can be onloy ARGB_8888", bitmap.getConfig().name()));
        }
    }

    private native int _setPixels(Bitmap bitmap, int offsetX, int offsetY, int width, int height);

    /**
     * Rotate image about 90,180,270
     * @param angle
     */
    public void rotate(int angle) {
        rotate(angle, false);
    }
    /**
     * Rotate image about 90,180,270
     * @param angle
     * @param fast (valid only for 90 or 270) true is 6 - 10x faster, allocates memory of another image size though!
     */
    public void rotate(int angle, boolean fast) {
        angle = angle % 360;
        if (angle < 0 || angle % 90 != 0) {
            throw new IllegalArgumentException(String.format("Invalid angle:%s, must be non-negative number divisible by 90!", angle));
        }
        if (angle != 0) {
            if (fast) {
                //TODO:memory check
            }
            _rotate(angle, fast);
        }
    }

    private native void _rotate(int angle, boolean fast);

    void onSetNativeRef(long ref) {
        mNativeRef = ref;
        POINTER_TRACKER.add(ref);
    }

    long getNativeRef() {
        return mNativeRef;
    }

    /**
     * Apply effect use {@link EffectBuilder}
     * @param json
     */
    public int applyEffect(String json) {
        return _applyEffect(json);
    }

    native int _applyEffect(String json);

    /**
     * Convert to bitmap (bitmap is acreated)
     * @return
     */
    public Bitmap asBitmap() {
        return asBitmap(null);
    }

    /**
     * Fill Bitmap, be sure width/height matches!
     * @param bitmap null to create, not null to reuse
     * @return
     */
    public Bitmap asBitmap(Bitmap bitmap) {
        boolean passedBitmap = bitmap != null;
        boolean createBitmap = bitmap == null;
        final MetaData metaData = getMetaData();
        if (createBitmap) {
            bitmap = Bitmap.createBitmap(metaData.width, metaData.height, Bitmap.Config.ARGB_8888);
        } else {
            if (metaData.width != bitmap.getWidth() || metaData.height != bitmap.getHeight()) {
                throw new IllegalArgumentException(String.format("Invalid Bitmap, has %sx%s, native image has %sx%s", bitmap.getWidth(), bitmap.getHeight(), metaData.width, metaData.height));
            }
        }
        int result = setPixels(bitmap);
        if (result != NO_ERR && !passedBitmap) {
            bitmap.recycle();
            bitmap = null;
        }
        return bitmap;
    }

    /**
     * Get allocated bytes in native heap
     * @return
     */
    public long getAllocatedBytes() {
        return getMetaData().getAllocatedBytes();
    }

    /**
     * Try to get Format based on file extension
     * @param path
     * @return
     * @throws IllegalArgumentException if can't be detected
     */
    private Format getFormatBasedExt(@NonNull String path) {
        path = path.toLowerCase();
        if (path.endsWith(".png")) {
            return Format.PNG_RGBA;
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return Format.JPEG_RGB;
        }
        throw new IllegalArgumentException(String.format("Unable to detect format based on file:'%s'", path));
    }

    private void checkFreMemory(String file) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        try {
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file, opts);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        checkFreeMemory(file, opts.outWidth, opts.outHeight);
    }

    @SuppressLint("DefaultLocale")
    private void checkFreeMemory(String file, int w, int h) {
        long neededMemory = (long) w * h * mBytesPerPixel;
        final Pair<Long, Long> deviceMemory = ShellHelper.getDeviceMemory();
        long totalDevMemory = deviceMemory.first;
        long freeMemory = deviceMemory.second;

        double allocatedMemory = sAllocatedMemory / (double)totalDevMemory;
        long memAfterAllocation = freeMemory - neededMemory;
        long minFreeMemory = (long) (LIMIT * totalDevMemory);

        double ratio;
        if (totalDevMemory < GB) {
            ratio = 0.5f;
        } else if (totalDevMemory < 2 * GB) {
            ratio = 0.7;
        } else {
            ratio = 0.8;
        }

        if (allocatedMemory > ratio && memAfterAllocation < minFreeMemory) {
            throw new OutOfMemoryError(String.format("Allocating image '%s' needs %.2f MB, getting below 10%% MB of free device memory means that OS will start killing processes!", file, neededMemory / 1024f / 1024f));
        }
    }

    public static class MetaData {
        public final int width;
        public final int height;
        public final int bytesPerPixel;

        MetaData(String json, int bytesPerPixel) {
            try {
                JSONObject obj = new JSONObject(json);
                width = obj.getInt(IMAGE_WIDTH);
                height = obj.getInt(IMAGE_HEIGHT);
                this.bytesPerPixel = bytesPerPixel;
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        }

        public long getAllocatedBytes() {
            return (long) width * height * bytesPerPixel;
        }
    }

    public static class SaveParamsBuilder {
        private JSONObject mParams = new JSONObject();

        public SaveParamsBuilder setJpegQuality(int quality) {
            try {
                mParams.put("jpegQuality", quality);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public String build() {
            return mParams.toString();
        }
    }

    public static class EffectBuilder {
        private static final String EFFECT = "effect";
        private JSONObject mParams = new JSONObject();

        public EffectBuilder grayScale() {
            add(EFFECT, "grayScale");
            return this;
        }

        public EffectBuilder crop(int offsetX, int offsetY, int width, int height) {
            add(EFFECT, "grayScale");
            add("offsetX", offsetX);
            add("offsetY", offsetY);
            add("width", width);
            add("height", height);
            return this;
        }

        public EffectBuilder brightness(@IntRange(from = -255, to = 255) int diff) {
            add(EFFECT, "brightness");
            add("brightness", diff);
            return this;
        }

        public EffectBuilder contrast(@IntRange(from = -255, to = 255) int diff) {
            add(EFFECT, "contrast");
            add("contrast", diff);
            return this;
        }

        public EffectBuilder gamma(@FloatRange(from = 0, fromInclusive = false) float diff) {
            add(EFFECT, "gamma");
            add("gamma", diff);
            return this;
        }

        public EffectBuilder inverse() {
            add(EFFECT, "inverse");
            return this;
        }

        public EffectBuilder flipVertical() {
            add(EFFECT, "flipv");
            return this;
        }

        public EffectBuilder flipHorizontal() {
            add(EFFECT, "fliph");
            return this;
        }

        private void add(String key, Object value) {
            try {
                mParams.put(key, value);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        }

        public String build() {
            return mParams.toString();
        }
    }
}
