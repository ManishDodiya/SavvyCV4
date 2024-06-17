//-----------------------------------------------------------------------------
//
//	GaugeBase
//
//	Author:		Mike Smits
//	Date:		30 Dec 20
//	Revision:	201230.1316
//
//-----------------------------------------------------------------------------

package com.savvytech.savvylevelfour.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GaugeBase extends View {

//	private final static String TAG = "Gauge";

	//Parameters --------------------------------------------------------------

	protected boolean isSquareDimension = true;
	protected int widthSize;
	protected int heightSize;
	
	//Primitives --------------------------------------------------------------
	
	private Bitmap bitmap;
	private Paint paint;
	private Canvas canvas;
	
	//Constructors ------------------------------------------------------------

	public GaugeBase(Context context, AttributeSet attrs) {
		super(context, attrs);
        initialize();
	}
	
	//Override Methods --------------------------------------------------------

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int width = MeasureSpec.getSize(widthMeasureSpec);
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		
		int chosenWidth = chooseDimension(widthMode, width);
		int chosenHeight = chooseDimension(heightMode, height);
		
		int chosenDimension = Math.min(chosenWidth, chosenHeight);
		
		if (isSquareDimension)
			setMeasuredDimension(chosenDimension, chosenDimension);
		else
			setMeasuredDimension(chosenWidth, chosenHeight);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// free the old bitmap
		if (bitmap != null) {
			bitmap.recycle();
		}
		if ((w > 0) && (h > 0)) {
			bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			canvas = new Canvas(bitmap);

			widthSize = w;

			Log.e("WIDTH_SIZE", widthSize + "   *****   ");

			heightSize = h;
			redrawCanvas();
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (bitmap != null) {
			canvas.drawBitmap(bitmap, 0, 0, paint);
		}
	}
    
	//Protected Methods -------------------------------------------------------

	protected void initialize() {
		// Note that this method executes before controls height and width are known
		
		//for android version 3.0 (honeycomb) and later we must software render
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//		    setLayerType(LAYER_TYPE_SOFTWARE, null);
	}
	
	// Override this method to redraw the bitmap
	protected void redrawCanvas() {
		// this is executed whenever views size changes
	}

	protected final Canvas getCanvas() {
		return canvas;
	}
	
	protected int mapX(float x) {
		// transform 0.0 to 1.0 into canvas X px dimension
		return (int) (x * widthSize);
	}
	
	protected int mapY(float y) {
		// transform 0.0 to 1.0 into canvas Y px dimension
		return (int) (y * heightSize);
	}
	
	protected void recycleBitmap() {
		// must be called when disposing of descendant control
		// this disposes of bitmap
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
			canvas = null;
		}
	}

	
	//Private Methods ---------------------------------------------------------

	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return 100;	// return this in case there is no size specified
		} 
	}
	
	

}
