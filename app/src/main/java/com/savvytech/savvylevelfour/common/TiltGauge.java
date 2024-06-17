//-----------------------------------------------------------------------------
//
//	TiltGauge
//
//	Author:		Mike Smits
//	Date:		07 Dec 20
//	Revision:	201207.1402
//
//-----------------------------------------------------------------------------

package com.savvytech.savvylevelfour.common;

import static com.savvytech.savvylevelfour.common.ConstantsKt.EXTRA_LYNX;
import static com.savvytech.savvylevelfour.common.ConstantsKt.EXTRA_MM;
import static com.savvytech.savvylevelfour.common.ConstantsKt.EXTRA_OZI;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.savvytech.savvylevelfour.R;

import java.text.DecimalFormat;

public class TiltGauge extends FrameLayout {

    private final String TAG = "Gauge";

    // Public Constants -------------------------------------------------------

    public static final boolean RIGHT = true;        // used with dial type
    public static final boolean LEFT = false;        // used with dial type

    public static final char NORMAL = 0;                // +/- 45 degrees
    public static final char ZOOM = 1;                    // +/- 4.5 degrees

    public static final char MILLIMETERS = 0;
    public static final char INCHS = 1;

    public static final char NO_OVERFLOW = 0;
    public static final char POSITIVE_OVERFLOW = 1;
    public static final char NEGATIVE_OVERFLOW = 2;

    // Private Constants ------------------------------------------------------

    private static final int WARNING_COLOR = 0xFFFF0000;    // red color

    private static final float MIDPOINT = 0.5f;
    private static final float RADIUS = 0.38f;

    private static final boolean DEFAULTGAUGETYPE = RIGHT;
    private static final boolean DEFAULTSECONDARYTEXTVISIBILITY = false;
    private static final int DEFAULTGAUGEMODE = NORMAL;
    private static final char DEFAULTGUAGEUNITS = MILLIMETERS;

    private boolean atLeastMarshmallow = (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP);

    // Objects ----------------------------------------------------------------

    private Dial dial;
    private Content content;
    private AngleText angleText;
    private HeightText heightText;
    private AppCompatTextView heightTextNew;
    private AppCompatTextView blockValue;
    private BlinkyUp blinkyDown;
    private BlinkyUp blinkyUp;

    // Globals ----------------------------------------------------------------

    private Context context;
    private AttributeSet attrs;

    // event handling
    private CustomEvents customEvents;
    private CustomEvents.MyEvent eventOverflow;
    private char overflowState = NO_OVERFLOW;

    private boolean gaugeType;
    private Drawable drawableImage = null;
    private Drawable drawableImageArow = null;
    //    private int styleColor = Color.BLACK;
    private int styleColor = Color.BLACK;
    private int styleColorText = Color.BLACK;

    private int mHeight;                // gauge height
    private int mWidth;                    // gauge width

    private boolean reJustIndicatorFlag = true;    // triggers a re-justify of IndText object
    private char zoom;                    // dial zoom
    private boolean secondaryTextVisibility = false;
    private char secondaryTextUnits;    // secondary indicator secondaryTextUnits (MILLIMETERS or INCHES)
    private int secondaryTextSpan;        // tilt secondaryTextSpan (hypotheses)

    private int maxPhysicalAngle;        // maximum physical angle
    private float maxIndicatedAngle;    // maximum indication angle
    private int majorTickAngle;            // in physical degrees must be a factor of maxPhysicalAngle
    private int minorTickAngle;            // in physical degrees must be a factor of majorTickAngle
    private float minorIndAngle;        // must be a factor of maxIndicatorAngle
    private float zoomFactor;            // level of roll magnification

    private float hitchAngle = 0f;
    private boolean hitchEnabled = false;
    Preference preference;
    private String spanType = "";
    private Bitmap bitmapTopArrow;

// Constructors -----------------------------------------------------------

    public TiltGauge(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        this.attrs = attrs;

        styleColor = ContextCompat.getColor(context, R.color.hitch_color);

        setWillNotDraw(false);        // cause onDraw to be executed when required
        // set custom attributes
        setCustomAttributes(context, attrs);

        // event handling
        customEvents = new CustomEvents();
        eventOverflow = customEvents.new MyEvent();

        setupZoom(zoom);

        dial = new Dial(context, attrs);
        content = new Content(context, attrs);
        angleText = new AngleText(context, attrs);
        heightText = new HeightText(context, attrs);
        if (atLeastMarshmallow) {
            blinkyUp = new BlinkyUp(context, attrs);
            blinkyDown = new BlinkyDown(context, attrs);
        }
        setSecondaryTextVisibility(secondaryTextVisibility, "", new AppCompatTextView(context),
                new AppCompatTextView(context), context);
        setSecondaryTextUnits(secondaryTextUnits);
        setSecondaryTextSpan(secondaryTextSpan);

        addViews();
    }

    // Override Methods -------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (reJustIndicatorFlag) {
            // executed only once when gauge is first shown on screen
            // is required to correct text position in Jelly Bean and later due to Bluetooth Adapter issue
            this.removeAllViews();
            addViews();
            angleText.resizeIndicator();
            heightText.resizeIndicator();
            reJustIndicatorFlag = false;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
        angleText.resizeIndicator();
        heightText.resizeIndicator();
    }

    // Public Parameters ------------------------------------------------------

    public void setValue(float angle) {
        content.setValue(angle);
    }

    public void setHitchAngle(float hitchAngle) {
        this.hitchAngle = hitchAngle;
        // redraw dial
        if (dial != null) {
            dial.clearCanvas();
            dial.redrawCanvas();
            dial.invalidate();
        }
    }

    public void setHitchEnabled(boolean hitchEnabled) {
        this.hitchEnabled = hitchEnabled;
        // redraw dial
        if (dial != null) {
            dial.clearCanvas();
            dial.redrawCanvas();
            dial.invalidate();
        }
    }

    public void setAnimationSpeed(int speed) {
        content.setAnimationSpeed(speed);
    }

    public void setImage(int resId, Context context) {
        // e.g. id = R.drawable.rear_bk
        drawableImage = ContextCompat.getDrawable(context, resId);
        content.clearCanvas();
        content.redrawCanvas();
    }

    public void setStyleColor(int color) {
        styleColor = color;
        dial.clearCanvas();
        dial.redrawCanvas();
        content.clearCanvas();
        content.redrawCanvas();
        angleText.setStyleColor(styleColor);
        heightText.setStyleColor(styleColor);
    }

    public char getZoom() {
        return zoom;
    }

    public void setZoom(char zoom) {
        setupZoom(zoom);
        if (dial != null) {
            // redraw dial
            dial.clearCanvas();
            dial.redrawCanvas();
            dial.invalidate();
        }
        if (content != null) {
            content.clearCanvas();
            content.redrawCanvas();
        }
    }

    public boolean getSecondaryTextVisibility() {
        return secondaryTextVisibility;
    }

    public void setSecondaryTextVisibility(boolean visibility, String spanType, AppCompatTextView heightTextNew,
                                           AppCompatTextView blockValue, Context context) {
        this.secondaryTextVisibility = visibility;
        this.heightTextNew = heightTextNew;
        this.blockValue = blockValue;
        this.context = context;
        this.spanType = spanType;

        bitmapTopArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.top_arrow);

        preference = new Preference(this.context);

        if (dial != null) {
            dial.clearCanvas();
            dial.redrawCanvas();
            dial.invalidate();
        }

        if (visibility) {
            heightText.setVisibility(TextView.INVISIBLE);
            this.heightTextNew.setVisibility(TextView.VISIBLE);
        } else {
            heightText.setVisibility(TextView.INVISIBLE);
            this.heightTextNew.setVisibility(TextView.INVISIBLE);
        }
    }

    public char getSecondaryTextUnits() {
        return secondaryTextUnits;
    }

    public void setSecondaryTextUnits(char units) {
        this.secondaryTextUnits = units;
        if (units == MILLIMETERS) {
            heightText.setUnitStr(" " + String.valueOf((char) 0x339C));
            this.heightTextNew.setText(" " + (char) 0x339C);
        } else if (units == INCHS) {
            heightText.setUnitStr(String.valueOf((char) 0x2033));
            this.heightTextNew.setText(String.valueOf((char) 0x2033));
        }
    }

    public int getSecondaryTextSpan() {
        return secondaryTextSpan;
    }

    public void setSecondaryTextSpan(int span) {
        this.secondaryTextSpan = span;
    }

    public void addEventListener(CustomEvents.MyEventListener listener) {
        eventOverflow.addEventListener(listener);
    }

    public void removeEventListener(CustomEvents.MyEventListener listener) {
        eventOverflow.removeEventListener(listener);
    }

    public void setIndicatorTypeface(Typeface tf) {
        if (angleText != null)
            angleText.setTypeface(tf, Typeface.BOLD);
        if (heightText != null)
            heightText.setTypeface(tf, Typeface.BOLD);

        if (this.heightTextNew != null)
            this.heightTextNew.setTypeface(tf, Typeface.BOLD);
    }

    public void recycleBitmaps() {
        dial.recycleBitmap();
        content.recycleBitmap();
        if (atLeastMarshmallow) {
            blinkyDown.recycleBitmap();
            blinkyUp.recycleBitmap();
        }
    }

    // Private Methods --------------------------------------------------------

    private void setCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.CustomControl, 0, 0);
        try {
            gaugeType = a.getBoolean(R.styleable.CustomControl_gauge_type, DEFAULTGAUGETYPE);
            zoom = (char) a.getInt(R.styleable.CustomControl_zoom, DEFAULTGAUGEMODE);
            secondaryTextVisibility = a.getBoolean(R.styleable.CustomControl_secondary_visible,
                    DEFAULTSECONDARYTEXTVISIBILITY);
            secondaryTextUnits = (char) a.getInt(R.styleable.CustomControl_secondary_units, DEFAULTGUAGEUNITS);
            secondaryTextSpan = a.getInt(R.styleable.CustomControl_secondary_span, 1);
            drawableImage = a.getDrawable(R.styleable.CustomControl_drawable_image);

        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }
    }

    private float getDensityAdjust(float val) {
        // factor in screen density

        // this function is used for scaling TextView objects. We want text size to be constant
        // regardless of the screen density. But TextView automatically scales according to
        // the scaleDensity (sp) so we must reverse this scaling effect.

        return val / getResources().getDisplayMetrics().scaledDensity;
    }

    private void setupZoom(char zoom) {
        this.zoom = zoom;
        switch (this.zoom) {
            case NORMAL:
                maxPhysicalAngle = 45;
                maxIndicatedAngle = 45f;
                majorTickAngle = 10;
                minorTickAngle = 5;
                minorIndAngle = 5f;
                zoomFactor = 1f;
                break;
            case ZOOM:
                maxPhysicalAngle = 45;
                maxIndicatedAngle = 4.5f;
                majorTickAngle = 10;
                minorTickAngle = 5;
                minorIndAngle = 0.5f;
                zoomFactor = 10f;
                break;
        }
    }

    private void addViews() {
        addView(dial);
        addView(content);
//        addView(angleText);
        addView(heightText);
        if (atLeastMarshmallow) {
            addView(blinkyUp);
            addView(blinkyDown);
        }
    }

    // BlinkyDown class -------------------------------------------------------

    // down flashing Arrow
    private class BlinkyDown extends BlinkyUp {

        private static final float ARROWSIZE = 0.8f;

        public BlinkyDown(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void redrawCanvas() {

            Canvas canvas = getCanvas();
            Path path = new Path();
//            if (canvas != null) {
//                // draw blinky
//                if (gaugeType == RIGHT) {
//                    path.moveTo(mapX(MIDPOINT + 0.25f), mapY(MIDPOINT + 0.35f));
//                    path.lineTo(mapX(MIDPOINT + 0.31f), mapY(MIDPOINT + 0.35f));
//                    path.lineTo(mapX(MIDPOINT + 0.28f), mapY(MIDPOINT + 0.40f));
//                    path.lineTo(mapX(MIDPOINT + 0.25f), mapY(MIDPOINT + 0.35f));
//                } else {
//                    path.moveTo(mapX(MIDPOINT - 0.25f), mapY(MIDPOINT - 0.35f));
//                    path.lineTo(mapX(MIDPOINT - 0.31f), mapY(MIDPOINT - 0.35f));
//                    path.lineTo(mapX(MIDPOINT - 0.28f), mapY(MIDPOINT - 0.40f));
//                    path.lineTo(mapX(MIDPOINT - 0.25f), mapY(MIDPOINT - 0.35f));
//                }
//                canvas.drawPath(path, blinkyPaint);
//            }

            if (canvas != null) {
                // create arrow shape
                int x;
                int y;
                if (gaugeType == RIGHT) {
                    x = mapX(MIDPOINT + 0.27f);
                    y = mapY(MIDPOINT + 0.32f);
                    path.moveTo(x, y);
                    path.lineTo(x - mapX(0.05f * ARROWSIZE), y + mapY(0.085f * ARROWSIZE));
                    path.lineTo(x, y + mapY(0.06f * ARROWSIZE));
                    path.lineTo(x + mapX(0.05f * ARROWSIZE), y + mapY(0.085f * ARROWSIZE));
                    path.lineTo(x, y);
                } else {
                    x = mapX(MIDPOINT - 0.27f);
                    y = mapY(MIDPOINT + 0.42f);
                    path.moveTo(x, y);
                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y + mapY(0.05f * ARROWSIZE));
                    path.lineTo(x + mapX(0.075f * ARROWSIZE), y);
                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y - mapY(0.05f * ARROWSIZE));
                    path.lineTo(x, y);
                }
                canvas.drawPath(path, blinkyPaint);
            }

//            if (canvas != null) {
//                // create arrow shape
//                int x;
//                int y;
//                if (gaugeType == RIGHT) {
//                    x = mapX(MIDPOINT + 0.27f);
//                    y = mapY(MIDPOINT + 0.42f);
//                    path.moveTo(x, y);
//                    path.lineTo(x - mapX(0.05f * ARROWSIZE), y - mapY(0.1f * ARROWSIZE));
//                    path.lineTo(x, y - mapY(0.05f * ARROWSIZE));
//                    path.lineTo(x + mapX(0.05f * ARROWSIZE), y - mapY(0.1f * ARROWSIZE));
//                    path.lineTo(x, y);
//                } else {
//                    x = mapX(MIDPOINT - 0.27f);
//                    y = mapY(MIDPOINT + 0.42f);
//                    path.moveTo(x, y);
//                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y + mapY(0.05f * ARROWSIZE));
//                    path.lineTo(x + mapX(0.075f * ARROWSIZE), y);
//                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y - mapY(0.05f * ARROWSIZE));
//                    path.lineTo(x, y);
//                }
//                canvas.drawPath(path, blinkyPaint);
//            }
        }
    }

    // BlinkyUp class ---------------------------------------------------------

    // up flashing Arrow
    private class BlinkyUp extends GaugeBase {

        protected Paint blinkyPaint;
        private static final float ARROWSIZE = 0.8f;

        public BlinkyUp(Context context, AttributeSet attrs) {
            super(context, attrs);

            // set draw primitives
            blinkyPaint = new Paint();
            blinkyPaint.setAntiAlias(true);
            blinkyPaint.setStyle(Paint.Style.FILL);
            blinkyPaint.setColor(0x00000000);
        }

        protected void clearCanvas() {
            Canvas canvas = getCanvas();
            if (canvas != null)
                canvas.drawColor(0, Mode.CLEAR);
        }

        protected void on() {
            clearCanvas();
            blinkyPaint.setColor(WARNING_COLOR);
            invalidate();
        }

        protected void off() {
            clearCanvas();
            blinkyPaint.setColor(0x00000000);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            redrawCanvas();
        }

        @Override
        protected void initialize() {
            super.initialize();
            isSquareDimension = true;

            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            this.setLayoutParams(params);
        }

        @Override
        protected void redrawCanvas() {
            Canvas canvas = getCanvas();
            Path path = new Path();
//            if (canvas != null) {
////                // draw blinky
//                if (gaugeType == RIGHT) {
//                    // 483    96
//                    path.moveTo(mapX(MIDPOINT + 0.25f), mapY(MIDPOINT - 0.35f));
//                    // 521    96
//                    path.lineTo(mapX(MIDPOINT + 0.31f), mapY(MIDPOINT - 0.35f));
//                    // 502    64
//                    path.lineTo(mapX(MIDPOINT + 0.28f), mapY(MIDPOINT - 0.40f));
//                    // 483    96
//                    path.lineTo(mapX(MIDPOINT + 0.25f), mapY(MIDPOINT - 0.35f));
//                } else {
//                    path.moveTo(mapX(MIDPOINT - 0.25f), mapY(MIDPOINT + 0.35f));
//                    path.lineTo(mapX(MIDPOINT - 0.31f), mapY(MIDPOINT + 0.35f));
//                    path.lineTo(mapX(MIDPOINT - 0.28f), mapY(MIDPOINT + 0.40f));
//                    path.lineTo(mapX(MIDPOINT - 0.25f), mapY(MIDPOINT + 0.35f));
//                }
////                canvas.drawBitmap(bitmapTopArrow, MIDPOINT + 0.25f, MIDPOINT + 0.35f, blinkyPaint);
//                canvas.drawPath(path, blinkyPaint);
//            }

            if (canvas != null) {
                // create arrow shape
                int x;
                int y;
                if (gaugeType == RIGHT) {
                    x = mapX(MIDPOINT + 0.27f);
                    y = mapY(MIDPOINT - 0.32f);
                    path.moveTo(x, y);
                    path.lineTo(x - mapX(0.05f * ARROWSIZE), y - mapY(0.085f * ARROWSIZE));
                    path.lineTo(x, y - mapY(0.06f * ARROWSIZE));
                    path.lineTo(x + mapX(0.05f * ARROWSIZE), y - mapY(0.085f * ARROWSIZE));
                    path.lineTo(x, y);
                } else {
                    x = mapX(MIDPOINT - 0.27f);
                    y = mapY(MIDPOINT + 0.42f);
                    path.moveTo(x, y);
                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y + mapY(0.05f * ARROWSIZE));
                    path.lineTo(x + mapX(0.075f * ARROWSIZE), y);
                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y - mapY(0.05f * ARROWSIZE));
                    path.lineTo(x, y);
                }
                canvas.drawPath(path, blinkyPaint);
            }

//            if (canvas != null) {
//                // create arrow shape
//                int x;
//                int y;
//                if (gaugeType == RIGHT) {
//                    x = mapX(MIDPOINT + 0.27f);
//                    y = mapY(MIDPOINT - 0.42f);
//                    path.moveTo(x, y);
//                    path.lineTo(x - mapX(0.05f * ARROWSIZE), y + mapY(0.1f * ARROWSIZE));
//                    path.lineTo(x, y + mapY(0.05f * ARROWSIZE));
//                    path.lineTo(x + mapX(0.05f * ARROWSIZE), y + mapY(0.1f * ARROWSIZE));
//                    path.lineTo(x, y);
//                } else {
//                    x = mapX(MIDPOINT - 0.27f);
//                    y = mapY(MIDPOINT + 0.42f);
//                    path.moveTo(x, y);
//                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y + mapY(0.05f * ARROWSIZE));
//                    path.lineTo(x + mapX(0.075f * ARROWSIZE), y);
//                    path.lineTo(x + mapX(0.1f * ARROWSIZE), y - mapY(0.05f * ARROWSIZE));
//                    path.lineTo(x, y);
//                }
//                canvas.drawPath(path, blinkyPaint);
//            }
        }
    }

    // AngleText class --------------------------------------------------------
    // draws angle Text in top corner
    private class AngleText extends TiltText {

        private static final float TEXTOFFSETX = 0.32f;                // 0.28f;
        private static final float TEXTOFFSETY = 0.36f;                // 0.4f;

        protected AngleText(Context context, AttributeSet attrs) {
            super(context, attrs);
            xOffset = TEXTOFFSETX;
            yOffset = TEXTOFFSETY;
            setText("0" + (char) 0x00B0);
        }

        protected void setUnSignedAngle(float angle) {
            if (overflowState == NO_OVERFLOW) {
                // no overflow
                DecimalFormat fmt;
                // we have +/-0.0 so lets get rid of sign
                angle = Math.abs(angle);

                if ((angle >= 0f) && (angle < 10f))
                    fmt = new DecimalFormat("0.0");
                else
                    fmt = new DecimalFormat("0");

                this.setText(fmt.format(angle) + String.valueOf((char) 0x00B0));
            } else {
                // we have overflow
                this.setText("...");
            }
        }
    }

    // HeightText class -------------------------------------------------------

    // draws height Text in bottom corner
    private class HeightText extends TiltText {

        private static final float TEXTOFFSETX = 0.30f;
        private static final float TEXTOFFSETY = -0.38f;
        private static final float TEXTSIZE = 0.08f;        // size of indicator text

        String unitStr = "";

        protected HeightText(Context context, AttributeSet attrs) {
            super(context, attrs);
            xOffset = TEXTOFFSETX;
            yOffset = TEXTOFFSETY;

            textSize = TEXTSIZE;

            if (secondaryTextUnits == MILLIMETERS) {
                setUnitStr(" " + String.valueOf((char) 0x339C));
                setText("0 " + (char) 0x339C);
            } else if (secondaryTextUnits == INCHS) {
                setUnitStr(String.valueOf((char) 0x2033));
                setText("0" + (char) 0x2033);
            }
        }

        public void setUnitStr(String unitStr) {
            this.unitStr = unitStr;
        }

        protected void setUnSignedAngle(float angle, float span) {
            float value = (float) (span * (Math.sin(Math.toRadians(angle))));

            if (overflowState == NO_OVERFLOW) {
                DecimalFormat fmt;
                // we have +/-0.0 so lets get rid of sign
                value = Math.abs(value);

                if (spanType.equalsIgnoreCase(EXTRA_MM)) {
                    unitStr = "mm";
                } else {
                    unitStr = "″";
                }
//                unitStr = preference.getData(Preference.Companion.getSpanType().toLowerCase());
//                if (secondaryTextUnits == MILLIMETERS) {
                if (spanType.equalsIgnoreCase(EXTRA_MM)) {
                    // millimeters
                    if (span <= 500) {
                        value = Math.round(value);
                    } else if (span <= 2800) {
                        value = Math.round(value / 5f) * 5;
                    } else {
                        value = Math.round(value / 10f) * 10;
                    }
                    fmt = new DecimalFormat("0");
                } else {
                    // inches
                    if (span <= 20) {
                        value = Math.round(value * 10) / 10f;
                    } else if (span < 110) {
                        value = Math.round(value * 5) / 5f;
                    } else {
                        value = Math.round(value * 2) / 2f;
                    }
                    fmt = new DecimalFormat("0.0");
                }

                this.setText(fmt.format(value) + unitStr);
                heightTextNew.setText(fmt.format(value) + unitStr);
                this.setTextColor(ContextCompat.getColor(context, R.color.black));
                heightTextNew.setTextColor(ContextCompat.getColor(context, R.color.black));

                if (preference.getBooleanData(Preference.Companion.getBlockType())) {
                    float first = 0f;
                    float second = 0f;

                    if (preference.getBloackTypeName(Preference.Companion.getBlockTypeName()).equals(EXTRA_OZI)) {
                        if (spanType.equalsIgnoreCase(EXTRA_MM)) {
                            first = 40.0f;
                            second = 25.0f;
                        } else {
                            first = (float) (40.0 / 25.4);
                            second = (float) (25.0 / 25.4);
                        }
                    } else if (preference.getBloackTypeName(Preference.Companion.getBlockTypeName()).equals(EXTRA_LYNX)) {
                        if (spanType.equalsIgnoreCase(EXTRA_MM)) {
                            first = 38.0f;
                            second = 25.0f;
                        } else {
                            first = (float) (38.0 / 25.4);
                            second = (float) (25.0 / 25.4);
                        }
                    } else {
                        first = Float.parseFloat(preference.getCustomBloackValue(Preference.Companion.getCustomBlockTypeValue()));
                        second = first;
                    }

//                    if (preference.getSpanType(Preference.Companion.getSpanType()).equalsIgnoreCase("INCH")) {
//                        value = value * 25.4f;
////                        value = value / 25.4f;
//                    }


//                    if (preference.getBloackTypeName(Preference.Companion.getBlockTypeName()).equals(EXTRA_OZI)) {
//                        first = spanType.equalsIgnoreCase(EXTRA_MM) ? first : (float) (40.0 / 25.4);
//                        second = spanType.equalsIgnoreCase(EXTRA_MM) ? second : (float) (25.0 / 25.4);
//                    } else if (preference.getBloackTypeName(Preference.Companion.getBlockTypeName()).equals(EXTRA_LYNX)) {
//                        first = spanType.equalsIgnoreCase(EXTRA_MM) ? 38 : (float) (38.0 / 25.4);
//                        second = spanType.equalsIgnoreCase(EXTRA_MM) ? 25 : (float) (25.0 / 25.4);
//                    } else {
//                        first = Float.parseFloat(preference.getCustomBloackValue(Preference.Companion.getCustomBlockTypeValue()));
//                        second = first;
//                    }

                    Log.e("SPAN_TYPE", spanType);
                    int blocks = calculateBlocks(value, first, second);
//                    val blocks = calculateBlocks(Math.abs(it).toFloat(), first, second)
//                int blocks = calculateBlocks(72f, 30f, 30f);

                    Log.e("Blocks", String.valueOf(blocks));

                    if (blocks > 0) {
                        blockValue.setVisibility(View.VISIBLE);
                        blockValue.setText("Blocks: " + String.valueOf(blocks));
//                        fragmentHomeBinding.txtFHBlockTitle.visibility = View.VISIBLE
//                        fragmentHomeBinding.txtFHBlockTitle.text =
//                                blocks.toString() + " " + getString(R.string.block)
                    } else {
//                        fragmentHomeBinding.txtFHBlockTitle.text =
//                                blocks.toString() + " " +  getString(R.string.block)
//                        fragmentHomeBinding.txtFHBlockTitle.visibility = View.GONE
                        blockValue.setVisibility(View.GONE);
                        blockValue.setText("Blocks: " + String.valueOf(blocks));
                    }
                }
            } else {
                // we have overflow
                this.setText("....");
                heightTextNew.setText("....");
                this.setTextColor(ContextCompat.getColor(context, R.color.red));
                heightTextNew.setTextColor(ContextCompat.getColor(context, R.color.red));
            }
        }
    }

    private int calculateBlocks(Float Hc, Float Hs, Float I) {
        int result = 0;
        float remainingHeight = Hc - Hs;
        if (remainingHeight < (-0.5 * Hs)) {
            return 0;
        } else {
            result += 1;
            if (remainingHeight > 0) {
                result += Math.round(remainingHeight / I);
            }
        }

        return result;
    }

    // TiltText class ---------------------------------------------------------

    // base class for indicator Text in corners of gauge
    private class TiltText extends androidx.appcompat.widget.AppCompatTextView {

        private static final float TEXTSIZE = 0.12f;        // size of indicator text
        protected float textSize;                    // size of indicator text

        protected float xOffset, yOffset;

        protected TiltText(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.setMaxLines(1);
//            this.setTextColor(styleColor);
            this.setTextColor(styleColorText);
            textSize = TEXTSIZE;
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            this.setLayoutParams(params);
        }

        protected void resizeIndicator() {
            LayoutParams lpIndText = (LayoutParams) this.getLayoutParams();

            if (lpIndText != null) {
                int size = Math.min(mWidth, mHeight);
                int posX, posY;
                if (gaugeType == RIGHT) {
                    posX = (int) (size * xOffset);
                    posY = (int) (size * yOffset);
                    lpIndText.setMargins(0, 0, posX, posY);    // setMarginEnd(posX);
                    this.setGravity(Gravity.CENTER);
                } else {
                    posX = (int) (size * (xOffset + 0.05f));
                    posY = (int) (size * yOffset);
                    lpIndText.setMargins(posX, 0, 0, posY);   // setMarginStart(posX);
                    this.setGravity(Gravity.CENTER);
                }
                lpIndText.topMargin = 0;
                this.setTextSize((int) (size * getDensityAdjust(textSize)));
            }
        }

        protected void setStyleColor(int color) {
            if (overflowState == NO_OVERFLOW) {
//                this.setTextColor(styleColor);
                this.setTextColor(styleColorText);
                this.invalidate();
            }
        }

        protected void on() {
            if (overflowState != NO_OVERFLOW) {
                this.setTextColor(WARNING_COLOR);
                invalidate();
            }
        }

        protected void off() {
            if (overflowState != NO_OVERFLOW) {
                this.setTextColor(0x00000000);
                invalidate();
            }
        }

        protected void stop() {
//            this.setTextColor(styleColor);
            this.setTextColor(styleColorText);
            this.invalidate();
        }

        protected void setSignedAngle(float angle) {
            if (overflowState == NO_OVERFLOW) {
                DecimalFormat fmt;
                if ((angle > -10f) && (angle < 10f))
                    fmt = new DecimalFormat("+0.0;-0.0");
                else
                    fmt = new DecimalFormat("+#;-#");
                // test if angle = 0.0
                if (Math.round(Math.abs(angle * 10)) == 0) {
                    // we have +/-0.0 so lets get rid of sign
                    angle = Math.abs(angle);
                    fmt = new DecimalFormat("0.0");
                }
                this.setText(fmt.format(angle) + String.valueOf((char) 0x00B0));
            } else {
                // we have overflow
                this.setText("...");
            }
        }
    }

    // Content class ----------------------------------------------------------

    // draws central Image and Arrow
    private class Content extends GaugeBase {

        private static final float TICKSTROKE = 0.006f;
        private static final float ARROWSIZE = 0.8f;
        private static final float IMAGESIZE = 0.4f;

        private float oldAngle = 0;
        private int animationSpeed = 50;    // animation speed (dampener)
        private ObjectAnimator animationCompass;

        private Bitmap bm;
        private Paint imagePaint;
        private Paint arrowPaint;
        private Path arrowPath;

        protected Content(Context context, AttributeSet attrs) {
            super(context, attrs);

            animationCompass = new ObjectAnimator();
            animationCompass.setPropertyName("rotation");
            animationCompass.setInterpolator(new LinearInterpolator());
            animationCompass.addUpdateListener(updateListener);
            animationCompass.setTarget(this);
        }

        protected void setValue(float angle) {
            angle *= zoomFactor;

            // limit range
            if (angle > maxPhysicalAngle) {
                // angle is too high
                angle = maxPhysicalAngle;
                overflowStateChanged(NEGATIVE_OVERFLOW);
                stop();
                start(DOWN);    // blinky down
            } else if (angle < -maxPhysicalAngle) {
                // angle is too low
                angle = -maxPhysicalAngle;
                overflowStateChanged(POSITIVE_OVERFLOW);
                stop();
                start(UP);        // blinky up
            } else {
                // angle is just right!
                overflowStateChanged(NO_OVERFLOW);
                stop();            // stop blinky
            }

            if (!(((oldAngle == maxPhysicalAngle) && (angle == maxPhysicalAngle)) ||
                    ((oldAngle == -maxPhysicalAngle) && (angle == -maxPhysicalAngle)))) {
                // old Angle and angle are in range
                animationCompass.setDuration(animationSpeed);
                animationCompass.setFloatValues(oldAngle, angle);
                animationCompass.start();
            }
        }

        protected void clearCanvas() {
            Canvas canvas = getCanvas();
            if (canvas != null)
                canvas.drawColor(0, Mode.CLEAR);
        }

        protected void setAnimationSpeed(int speed) {
            animationSpeed = speed;
        }

        @Override
        protected void initialize() {
            super.initialize();
            isSquareDimension = true;

            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(0, 0, 5, 0);
            this.setLayoutParams(params);

            // set draw primitives
            arrowPaint = new Paint();
            arrowPaint.setAntiAlias(true);
            arrowPaint.setStyle(Paint.Style.FILL);
            arrowPaint.setColor(Color.RED);

            imagePaint = new Paint();
            imagePaint.setAntiAlias(true);
            imagePaint.setFilterBitmap(true);
            imagePaint.setDither(true);
        }

        @Override
        protected void redrawCanvas() {
            Canvas canvas = getCanvas();
            if (canvas != null) {
                arrowPath = new Path();

                // create arrow shape
                int x;
                int y;
                if (gaugeType == RIGHT) {
                    x = mapX(MIDPOINT + RADIUS - TICKSTROKE / 2);
                    y = mapY(MIDPOINT);
                    arrowPath.moveTo(x, y);
                    arrowPath.lineTo(x - mapX(0.1f * ARROWSIZE), y + mapY(0.05f * ARROWSIZE));
                    arrowPath.lineTo(x - mapX(0.075f * ARROWSIZE), y);
                    arrowPath.lineTo(x - mapX(0.1f * ARROWSIZE), y - mapY(0.05f * ARROWSIZE));
                    arrowPath.lineTo(x, y);
                } else {
                    x = mapX(MIDPOINT - RADIUS + TICKSTROKE / 2);
                    y = mapY(MIDPOINT);
                    arrowPath.moveTo(x, y);
                    arrowPath.lineTo(x + mapX(0.1f * ARROWSIZE), y + mapY(0.05f * ARROWSIZE));
                    arrowPath.lineTo(x + mapX(0.075f * ARROWSIZE), y);
                    arrowPath.lineTo(x + mapX(0.1f * ARROWSIZE), y - mapY(0.05f * ARROWSIZE));
                    arrowPath.lineTo(x, y);
                }

                // draw arrow
                canvas.drawPath(arrowPath, arrowPaint);

                // get image
                if (drawableImage != null) {
//                    LightingColorFilter lcf = new LightingColorFilter( 0xFFFFFFFF, styleColor);
                    PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                    imagePaint.setColorFilter(colorFilter);

                    bm = ((BitmapDrawable) drawableImage).getBitmap();

                    // resize image
                    int h = mapY(IMAGESIZE);
                    int w = Math.round(h / ((float) bm.getHeight() / (float) bm.getWidth()));

                    Rect src = new Rect(0, 0, bm.getWidth() - 1, bm.getHeight() - 1);

                    int left = mapX(MIDPOINT) - (w / 2);
                    int right = left + w;
                    int top = mapY(MIDPOINT) - (h / 2);
                    int bottom = top + h;

                    Rect dest = new Rect(left, top, right, bottom);

                    canvas.drawBitmap(bm, src, dest, imagePaint);
                }
            }
        }

        @Override
        protected void recycleBitmap() {
            if (bm != null) {
                bm = null;
            }
            super.recycleBitmap();
        }

        private void overflowStateChanged(char state) {
            if (state != overflowState) {
                // overflow state has changed
                eventOverflow.fireEvent(state);            // trigger an event
            }
            overflowState = state;
        }

        AnimatorUpdateListener updateListener = new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                float angle = (Float) anim.getAnimatedValue();
                oldAngle = angle;
                angleText.setUnSignedAngle(angle / zoomFactor);
                heightText.setUnSignedAngle((angle / zoomFactor), secondaryTextSpan);
            }
        };

    }

    // Dial class -------------------------------------------------------------

    // draws circular Rim and Scale
    private class Dial extends GaugeBase {

        private static final int OFFSETANGLE = 0;
        private static final float MAJORTICK = 0.045f;
        private static final float MINORTICK = 0.025f;

        private static final float HITCHSIZE = 0.055f;

        private static final float TICKSTROKE = 0.006f;
        private static final float RIMSTROKE = 0.011f;

        private static final float HITCHSTROKE = 0.009f;

        private static final float TEXTRADIUS = 0.96f;
        private static final float TEXTSIZE = 0.06f;

        private float textHeight;

        private Paint linePaint;
        private Paint textPaint;
        private Paint hitchPaint;
        private Path textPath;

        protected Dial(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        protected void clearCanvas() {
            Canvas canvas = getCanvas();
            if (canvas != null)
                canvas.drawColor(0, Mode.CLEAR);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected void initialize() {
            super.initialize();
            isSquareDimension = true;

            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER;
            params.setMargins(0, 0, 5, 0);
            this.setLayoutParams(params);

            // set draw primitives
            linePaint = new Paint();
            linePaint.setAntiAlias(true);

            hitchPaint = new Paint();
            hitchPaint.setAntiAlias(true);

            textPaint = new Paint();
            textPaint.setAntiAlias(true);
//            Typeface typeface = context.getResources().getFont(R.font.poppins_light);
            Typeface typeface = ResourcesCompat.getFont(context, R.font.poppins_light);
            textPaint.setTypeface(typeface);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void redrawCanvas() {
            Canvas canvas = getCanvas();
            if (canvas != null) {
                linePaint.setColor(styleColor);
//                textPaint.setColor(styleColor);
                textPaint.setColor(styleColorText);
                hitchPaint.setColor(Color.MAGENTA);

                // perform rescaling of draw primitives here
                textHeight = mapY(TEXTSIZE);
                textPaint.setTextSize(textHeight);

                textPath = new Path();

                // draw rim
                linePaint.setStyle(Paint.Style.STROKE);
                linePaint.setStrokeWidth(mapY(RIMSTROKE));
                canvas.drawCircle(mapX(MIDPOINT), mapY(MIDPOINT), mapX(RADIUS), linePaint);

                // draw scale
                linePaint.setStrokeWidth(mapY(TICKSTROKE));
                float indicatedAngle = -maxIndicatedAngle;
                DecimalFormat fmt = new DecimalFormat("#");
                linePaint.setStyle(Paint.Style.FILL_AND_STROKE);

                if (gaugeType == RIGHT) {
                    textPath.moveTo(mapX(TEXTRADIUS), mapY(MIDPOINT) + textHeight / 2.8f);
                    textPath.lineTo(mapX(1.0f), mapY(MIDPOINT) + textHeight / 2.8f);
                    for (int angle = -maxPhysicalAngle; angle <= maxPhysicalAngle; angle += minorTickAngle) {
                        canvas.save();
                        canvas.rotate(angle + OFFSETANGLE, mapX(MIDPOINT), mapY(MIDPOINT));
                        if ((angle % majorTickAngle) == 0) {

                            if (angle >= -45 && angle <= -35) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.red));
                            } else if (angle >= -30 && angle <= -20) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.orange));
                            } else if (angle >= -15 && angle <= 15) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.dark_blue));
                            } else if (angle >= 15 && angle <= 30) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.orange));
                            } else if (angle >= 35 && angle <= 45) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.red));
                            }

                            // major tick
                            canvas.drawLine(mapX(MIDPOINT + RADIUS), mapY(MIDPOINT),
                                    mapX(MIDPOINT + RADIUS + MAJORTICK), mapY(MIDPOINT), linePaint);

                            String str = fmt.format(Math.abs(indicatedAngle));
                            canvas.drawTextOnPath(str, textPath, 0, 0, textPaint);

                        } else {

                            if (angle >= -45 && angle <= -35) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.red));
                            } else if (angle >= -30 && angle <= -20) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.orange));
                            } else if (angle >= -15 && angle <= 15) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.dark_blue));
                            } else if (angle >= 15 && angle <= 30) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.orange));
                            } else if (angle >= 35 && angle <= 45) {
                                linePaint.setColor(ContextCompat.getColor(context, R.color.red));
                            }

                            // minor tick
                            canvas.drawLine(mapX(MIDPOINT + RADIUS), mapY(MIDPOINT),
                                    mapX(MIDPOINT + RADIUS + MINORTICK), mapY(MIDPOINT), linePaint);
                        }
                        indicatedAngle += minorIndAngle;
                        canvas.restore();
                    }
                } else {
                    textPath.moveTo(0, mapY(MIDPOINT) + textHeight / 2.8f);
                    textPath.lineTo(mapX(1f - TEXTRADIUS), mapY(MIDPOINT) + textHeight / 2.8f);
                    for (int angle = -maxPhysicalAngle; angle <= maxPhysicalAngle; angle += minorTickAngle) {
                        canvas.save();
                        canvas.rotate(angle + OFFSETANGLE, mapX(MIDPOINT), mapY(MIDPOINT));
                        if ((angle % majorTickAngle) == 0) {
                            // major tick
                            canvas.drawLine(mapX(MIDPOINT - RADIUS), mapY(MIDPOINT), mapX(MIDPOINT - RADIUS - MAJORTICK), mapY(MIDPOINT), linePaint);
                            String str = fmt.format(Math.abs(indicatedAngle));
                            canvas.drawTextOnPath(str, textPath, 0, 0, textPaint);
                        } else {
                            // minor tick
                            canvas.drawLine(mapX(MIDPOINT - RADIUS), mapY(MIDPOINT), mapX(MIDPOINT - RADIUS - MINORTICK), mapY(MIDPOINT), linePaint);
                        }
                        indicatedAngle += minorIndAngle;
                        canvas.restore();
                    }
                }

                // draw hitch bug
                if (hitchEnabled) {
                    hitchPaint.setStrokeWidth(mapY(HITCHSTROKE));
                    hitchPaint.setStyle(Paint.Style.FILL);
                    Path hitchPath = new Path();
                    float phyAngle;
                    if (zoom == ZOOM)
                        phyAngle = hitchAngle * 10;
                    else phyAngle = hitchAngle;
                    if ((phyAngle > -45) && (phyAngle < 45)) {
                        if (gaugeType == RIGHT) {
                            canvas.save();
                            canvas.rotate(phyAngle + OFFSETANGLE, mapX(MIDPOINT), mapY(MIDPOINT));
                            hitchPath.moveTo(mapX(MIDPOINT + RADIUS + RIMSTROKE), mapY(MIDPOINT));
                            hitchPath.rLineTo(mapX(HITCHSIZE), mapY(-HITCHSIZE / 2));
                            hitchPath.rLineTo(mapX(0), mapY(HITCHSIZE));
                            hitchPath.rLineTo(mapX(-HITCHSIZE), mapY(-HITCHSIZE / 2));
                            canvas.drawPath(hitchPath, hitchPaint);
                            canvas.restore();
                        } else {
                            canvas.save();
                            canvas.rotate(phyAngle + OFFSETANGLE, mapX(MIDPOINT), mapY(MIDPOINT));
                            hitchPath.moveTo(mapX(MIDPOINT - RADIUS - RIMSTROKE), mapY(MIDPOINT));
                            hitchPath.rLineTo(mapX(-HITCHSIZE), mapY(-HITCHSIZE / 2));
                            hitchPath.rLineTo(mapX(0), mapY(HITCHSIZE));
                            hitchPath.rLineTo(mapX(HITCHSIZE), mapY(-HITCHSIZE / 2));
                            canvas.drawPath(hitchPath, hitchPaint);
                            canvas.restore();
                        }
                    }
                }
            }
        }

    }

    // Runnables --------------------------------------------------------------

    public static final boolean UP = true;        // blinky up
    public static final boolean DOWN = false;    // blinky down

    protected boolean isRunning = false;

    protected Handler onHandler = new Handler();
    protected Handler offHandler = new Handler();

    protected boolean blinkDirection;

    protected void start(boolean direction) {
        if (!isRunning) {
            blinkDirection = direction;
            onHandler.post(onRunnable);
            isRunning = true;
        }
    }

    protected void stop() {
        onHandler.removeCallbacks(onRunnable);
        offHandler.removeCallbacks(offRunnable);
        if (atLeastMarshmallow) {
            blinkyUp.off();
            blinkyDown.off();
        }
        angleText.stop();
        heightText.stop();
        isRunning = false;
    }

    private Runnable onRunnable = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            if (atLeastMarshmallow) {
                if (blinkDirection == UP) blinkyUp.on();
                if (blinkDirection == DOWN) blinkyDown.on();
            }
            angleText.on();
            heightText.on();
            offHandler.postDelayed(offRunnable, 100);
        }
    };

    private Runnable offRunnable = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            if (atLeastMarshmallow) {
                if (blinkDirection == UP) blinkyUp.off();
                if (blinkDirection == DOWN) blinkyDown.off();
            }
            angleText.off();
            heightText.off();
            onHandler.postDelayed(onRunnable, 200);
        }
    };
}