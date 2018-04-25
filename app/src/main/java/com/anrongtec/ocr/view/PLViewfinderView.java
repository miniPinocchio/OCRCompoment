package com.anrongtec.ocr.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.anrongtec.ocr.R;

/**
 * @author huiliu
 */
public class PLViewfinderView extends View {

    private static final long ANIMATION_DELAY = 10L;
    private Paint paint;
    private Paint paintLine;
    private int maskColor;
    private int frameColor;
    private Paint mTextPaint;
    private String mText;
    private Rect frame;
    private int w, h;
    private boolean boo = false;
    private float textSize;
    private float textWidth;

    public PLViewfinderView(Context context, int w, int h) {
        super(context);
        this.w = w;
        this.h = h;
        paint = new Paint();
        paintLine = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        // 绿色
        frameColor = resources.getColor(R.color.viewfinder_frame);
        textSize = resources.getDimension(R.dimen.finder_textpaint_size);
        textWidth = resources.getDimension(R.dimen.finder_textpaint_width);
    }

    public PLViewfinderView(Context context, int w, int h, boolean boo) {
        super(context);
        this.w = w;
        this.h = h;
        this.boo = boo;
        paint = new Paint();
        paintLine = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        frameColor = resources.getColor(R.color.viewfinder_frame);// 绿色
        textSize = resources.getDimension(R.dimen.finder_textpaint_size);
        textWidth = resources.getDimension(R.dimen.finder_textpaint_width);
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int t,b,l,r;
        l = 4;
        r = width - 4;
        t = height / 5;
        b = height * 3 / 5;
        frame = new Rect(l, t, r, b);

        // 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
        // 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
                paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);


        if (width > height && !boo) {

            paintLine.setColor(frameColor);
            paintLine.setStrokeWidth(10);
            paintLine.setAntiAlias(true);
            int num = 40;
            canvas.drawLine(l - 4, t, l + num, t, paintLine);
            canvas.drawLine(l, t, l, t + num, paintLine);

            canvas.drawLine(r, t, r - num, t, paintLine);
            canvas.drawLine(r, t - 4, r, t + num, paintLine);

            canvas.drawLine(l - 4, b, l + num, b, paintLine);
            canvas.drawLine(l, b, l, b - num, paintLine);

            canvas.drawLine(r, b, r - num, b, paintLine);
            canvas.drawLine(r, b + 4, r, b - num, paintLine);

        } else {
            paintLine.setColor(frameColor);
            paintLine.setStrokeWidth(10);
            paintLine.setAntiAlias(true);
            canvas.drawLine(l, t, l + 100, t, paintLine);
            canvas.drawLine(l, t, l, t + 100, paintLine);
            canvas.drawLine(r, t, r - 100, t, paintLine);
            canvas.drawLine(r, t, r, t + 100, paintLine);
            canvas.drawLine(l, b, l + 100, b, paintLine);
            canvas.drawLine(l, b, l, b - 100, paintLine);
            canvas.drawLine(r, b, r - 100, b, paintLine);
            canvas.drawLine(r, b, r, b - 100, paintLine);
        }
        mText = "车牌";
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStrokeWidth(textWidth);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(frameColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(mText, w / 2, h / 2, mTextPaint);

        if (frame == null) {
            return;
        }
        postInvalidateDelayed(ANIMATION_DELAY);
    }
}