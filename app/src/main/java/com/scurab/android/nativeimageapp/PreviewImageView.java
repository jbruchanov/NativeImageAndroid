package com.scurab.android.nativeimageapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by JBruchanov on 17/04/2017.
 */

public class PreviewImageView extends AppCompatImageView {

    private final Point mOffset = new Point();
    private final Paint mPaint = new Paint();
    private final Point mImageSize = new Point();
    private final Point mCropBitmapSize = new Point();
    private float mScale = 1f;

    public PreviewImageView(Context context) {
        super(context);
    }

    public PreviewImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PreviewImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            final float width = getWidth();
            float realImageScale = width / mImageSize.x;
            canvas.save();
            canvas.scale(realImageScale, realImageScale);
            mPaint.setStrokeWidth(1 / realImageScale);
            canvas.drawRect(mOffset.x, mOffset.y, mOffset.x + (mCropBitmapSize.x * mScale), mOffset.y + (mCropBitmapSize.y * mScale), mPaint);
            canvas.restore();
        }
    }

    public void setData(int realWidth, int realHeight, int offsetX, int offsetY, int cropBitmapWidth, int cropBitmapHeight, float scale) {
        mImageSize.set(realWidth, realHeight);
        mOffset.set(offsetX, offsetY);
        mCropBitmapSize.set(cropBitmapWidth, cropBitmapHeight);
        mScale = scale;
        invalidate();
    }

    private Bitmap getBitmap() {
        final Drawable drawable = getDrawable();
        return drawable instanceof BitmapDrawable
                ? ((BitmapDrawable) drawable).getBitmap()
                : null;
    }
}
