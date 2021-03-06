package com.scurab.android.nativeimage;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

/**
 * Created by JBruchanov on 03/04/2017.
 */
@SuppressWarnings({"unused", "JniMissingFunction", "WeakerAccess"})
@Keep
public class NativeImage {

    @IntDef(value = {0, 90, 180, 270})
    public @interface Angle {
    }

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

    private static final String IMAGE_WIDTH = "imageWidth";
    private static final String IMAGE_HEIGHT = "imageHeight";
    private static final long GiB = 1024L * 1024 * 1024;
    private static final float MiB = 1024f * 1024f;
    private static Double sDeviceMemory;

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
    @Keep
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
        sAllocatedMemory -= getAllocatedBytes();
        int result = throwExceptionIfError(_loadImage(path, format.processor));
        sAllocatedMemory += getAllocatedBytes();
        return result;
    }

    @Keep
    private native int _loadImage(String path, int processor);

    /**
     * Save image
     * @param path target location
     * @param format
     * @return
     */
    public int saveImage(@NonNull String path, @NonNull Format format) {
        return throwExceptionIfError(_saveImage(path, format.processor, null));
    }

    /**
     *
     * @param path
     * @param format
     * @param params use {@link SaveParamsBuilder}
     * @return
     */
    public int saveImage(@NonNull String path, @NonNull Format format, @Nullable String params) {
        return throwExceptionIfError(_saveImage(path, format.processor, params));
    }

    @Keep
    private native int _saveImage(String path, int processor, String params);

    /**
     * Get image metadata JSON
     * @return
     */
    @NonNull
    public MetaData getMetaData() {
        return new MetaData(_getMetaData(), mBytesPerPixel);
    }

    @Keep
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

    @Keep
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
        return throwExceptionIfError(_setPixels(outBitmap, 0, 0, metaData.width, metaData.height));
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
        return throwExceptionIfError(_setPixels(outBitmap, offsetX, offsetY, width, height));
    }

    private void assertRGBABitmap(Bitmap bitmap) {
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            throw new IllegalArgumentException(String.format("Bitmap has invalid Config:%s, only ARGB_8888 is supported", bitmap.getConfig().name()));
        }
    }

    @Keep
    private native int _setPixels(Bitmap bitmap, int offsetX, int offsetY, int width, int height);

    /**
     * Set pixels into bitmap with scaling.
     * @param bitmap
     * @return
     */
    public int setScaledPixels(@NonNull Bitmap bitmap) {
        MetaData metaData = getMetaData();
        return setScaledPixels(bitmap, 0, 0, metaData.width, metaData.height);
    }

    /**
     * Fill bitmap by specifying particular image area as source
     * @param bitmap
     * @param offsetX
     * @param offsetY
     * @param width
     * @param height
     * @return
     */
    public int setScaledPixels(@NonNull Bitmap bitmap, int offsetX, int offsetY, int width, int height) {
        assertRGBABitmap(bitmap);
        return throwExceptionIfError(_setScaledPixels(bitmap, offsetX, offsetY, width, height));
    }

    @Keep
    private native int _setScaledPixels(Bitmap bitmap, int offsetX, int offsetY, int width, int height);

    /**
     * Rotate image about 90,180,270
     * @param angle
     */
    public void rotate(int angle) {
        rotate(angle, false);
    }
    /**
     * Rotate image about 90, 180, 270
     * @param angle
     * @param fast (valid only for 90 or 270) true is 6 - 10x faster, allocates memory of another image size though!
     */
    public int rotate(@Angle int angle, boolean fast) {
        //noinspection WrongConstant
        angle = angle % 360;
        if (angle < 0 || angle % 90 != 0) {
            throw new IllegalArgumentException(String.format("Invalid angle:%s, must be non-negative number divisible by 90!", angle));
        }
        if (angle != 0) {
            if (fast) {
                MetaData m = getMetaData();
                checkFreeMemory(m.width, m.height, mBytesPerPixel);
            }
            return throwExceptionIfError(_rotate(angle, fast));
        }
        return NO_ERR;
    }

    @Keep
    private native int _rotate(int angle, boolean fast);

    //region Called from native
    @Keep
    void onSetNativeRef(long ref) {
        mNativeRef = ref;
        POINTER_TRACKER.add(ref);
    }

    @Keep
    long getNativeRef() {
        return mNativeRef;
    }
    //endregion

    /**
     * Apply effect use {@link EffectBuilder}
     * @param json
     */
    public int applyEffect(String json) {
        long allocated = getAllocatedBytes();
        final int result = throwExceptionIfError(_applyEffect(json));
        long newAllocated = getAllocatedBytes();
        if (allocated != newAllocated) {
            sAllocatedMemory += (-allocated + newAllocated);
        }
        return result;
    }

    @Keep
    private native int _applyEffect(String json);

    /**
     * Convert to bitmap (bitmap is created)
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
    public Bitmap asBitmap(@Nullable Bitmap bitmap) {
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
     * Get Image as scaled bitmap, width and height must not be higher than actual values
     * 1 value of width or height can be 0 to keep aspect ratio.
     * @param width
     * @param height
     * @return
     * @throws IllegalArgumentException if (width <= 0 && height <= 0)
     */
    public Bitmap asScaledBitmap(int width, int height) {
        if (width <= 0 && height <= 0) {
            throw new IllegalArgumentException(String.format("Invalid size provided width:%s height:%s, at least 1 value must be positive", width, height));
        }
        final MetaData metaData = getMetaData();
        if (width == 0) {
            width = (int) (height * (metaData.width / (float) metaData.height));
        }
        if (height == 0) {
            height = (int) (width * (metaData.height / (float) metaData.width));
        }
        return asScaledBitmap(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
    }

    /**
     * Get Image as scaled bitmap with specific scale
     * @return
     */
    public Bitmap asScaledBitmap(@FloatRange(from = 0, to = 1, fromInclusive = false) float scale) {
        final MetaData metaData = getMetaData();
        return asScaledBitmap(Bitmap.createBitmap(Math.round(scale * metaData.width), Math.round(scale * metaData.height), Bitmap.Config.ARGB_8888));
    }

    /**
     * Fill Bitmap, be sure width/height matches!
     *
     * @param bitmap null to create, not null to reuse
     * @return
     */
    public Bitmap asScaledBitmap(@NonNull Bitmap bitmap) {
        int result = setScaledPixels(bitmap);
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

    private void checkFreMemory(@NonNull String file) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        try {
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file, opts);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        checkFreeMemory(opts.outWidth, opts.outHeight, mBytesPerPixel);
    }

    private int throwExceptionIfError(int resultCode) {
        switch (resultCode){
            case NO_ERR:
                return resultCode;
            case CANT_OPEN_FILE:
                throw new IllegalStateException("Unable to open file");
            case OUT_OF_MEMORY:
                throw new OutOfMemoryError();
            case NO_DATA:
                throw new IllegalStateException("No data to process?!");
            case NOT_SAME_RESOLUTION:
            case INVALID_RESOLUTION:
                throw new IllegalStateException("Invalid resolution");
            case INVALID_BITMAP_FORMAT:
                throw new IllegalStateException("Invalid bitmap format, check Config.ARGB");
            case INVALID_JSON:
                throw new IllegalStateException("Invalid json");
            case INVALID_PNG:
                throw new IllegalStateException("Invalid PNG");
            case NOT_SUPPORTED_PNG_CONFIGURATION:
                throw new UnsupportedOperationException("PNG with unsupported configuration");
            case ERR_EFFECT_NOT_DEFINED:
                throw new IllegalArgumentException("Effect not found");
            case ERR_UNKNOWN:
            default:
                throw new IllegalStateException(String.format("Specific inner/unknown exception errorCode:%s", resultCode));
        }
    }

    @SuppressLint("DefaultLocale")
    private static void checkFreeMemory(int w, int h, int bytesPerPixel) {
        long neededMemory = (long) w * h * bytesPerPixel;
        if (sDeviceMemory == null) {
            final Pair<Long, Long> deviceMemory = ShellHelper.getDeviceMemory();
            sDeviceMemory = Double.valueOf(deviceMemory.first);
            if (sDeviceMemory == 0) {//just in case, 1GiB should be safe
                sDeviceMemory = (double) GiB;
            }
        }
        double willBeUsedMemoryCoef = (neededMemory + sAllocatedMemory) / sDeviceMemory;
        double ratio;
        if (sDeviceMemory < GiB) {
            ratio = 0.5;
        } else if (sDeviceMemory < 2 * GiB) {
            ratio = 0.7;
        } else {
            ratio = 0.85;
        }

        if (willBeUsedMemoryCoef > ratio) {
            throw new OutOfMemoryError(String.format("Allocating needs %.2f MB, device has:%.2f MB, getting close to total device memory means that OS will most likely kill our process!", neededMemory / MiB, sDeviceMemory / MiB));
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
            add(mParams, "jpegQuality", quality);
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
            add(mParams, EFFECT, "grayScale");
            return this;
        }

        public EffectBuilder crop(int offsetX, int offsetY, int width, int height) {
            add(mParams, EFFECT, "crop");
            add(mParams, "offsetX", offsetX);
            add(mParams, "offsetY", offsetY);
            add(mParams, "width", width);
            add(mParams, "height", height);
            return this;
        }

        public EffectBuilder brightness(@IntRange(from = -255, to = 255) int diff) {
            add(mParams, EFFECT, "brightness");
            add(mParams, "brightness", diff);
            return this;
        }

        public EffectBuilder contrast(@IntRange(from = -255, to = 255) int diff) {
            add(mParams, EFFECT, "contrast");
            add(mParams, "contrast", diff);
            return this;
        }

        public EffectBuilder gamma(@FloatRange(from = 0, fromInclusive = false) float diff) {
            add(mParams, EFFECT, "gamma");
            add(mParams, "gamma", diff);
            return this;
        }

        public EffectBuilder inverse() {
            add(mParams, EFFECT, "inverse");
            return this;
        }

        public EffectBuilder flipVertical() {
            add(mParams, EFFECT, "flipv");
            return this;
        }

        public EffectBuilder flipHorizontal() {
            add(mParams, EFFECT, "fliph");
            return this;
        }

        public EffectBuilder naiveDownscale(MetaData metaData, @FloatRange(from = 0, to = 1, fromInclusive = false, toInclusive = false) float scale) {
            return naiveDownscale(Math.round(scale * metaData.width), Math.round(scale * metaData.height));
        }

        public EffectBuilder naiveDownscale(int width, int height) {
            add(mParams, EFFECT, "naiveResize");
            add(mParams, "width", width);
            add(mParams, "height", height);
            return this;
        }

        public String build() {
            return mParams.toString();
        }
    }

    private static void add(@NonNull JSONObject obj, @NonNull String key, Object value) {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }
}
