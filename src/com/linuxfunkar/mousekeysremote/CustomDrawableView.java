/**
 * @file CustomDrawableView.java
 * @brief 
 *
 * Copyright (C)2010-2012 Magnus Uppman <magnus.uppman@gmail.com>
 * License: GPLv3+
 */

package com.linuxfunkar.mousekeysremote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.View;

import com.linuxfunkar.mousekeysremote.MouseKeysRemote.EditMode;

public class CustomDrawableView extends View {
	private static final String MARK_S = "(s)";
	private static final String MARK_C = "(C)";
	private static final String MARK_HASH = "(#)";
	private static final String MARK_STAR = "(*)";

	private ShapeDrawable mDrawable, backGround, wheel;
	private int measuredHeight = 0;
	private int measuredWidth = 0;
	private int keyWidth = 0;
	private int keyHeight = 0;
	private RectF keyRect;
	private Paint rectPaint, textPaint, textLayoutPaint, textLabelLayoutPaint;
	// Convert the dps to pixels
	final float scale = getContext().getResources().getDisplayMetrics().density;
	final float xdpi = getContext().getResources().getDisplayMetrics().xdpi;
	float tsize, t_layout_size, l_layout_size;

	private String mark;
	private EditMode mode = EditMode.None;

	public CustomDrawableView(Context context) {
		super(context);

		// private int a = (RemotePC.)

		// colorObj = new Color();
		keyRect = new RectF();
		rectPaint = new Paint();
		textPaint = new Paint();
		textLayoutPaint = new Paint();
		textLabelLayoutPaint = new Paint();

		rectPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		// textPaint.setFakeBoldText(true);
		textLabelLayoutPaint.setAntiAlias(true);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);
		// rectPaint.setColor(R.color.key_color);
		// rectPaint.setColor(0xff74AC23);
		// rectPaint.setColor(0xff333333);
		textPaint.setColor(0xffffffff);
		// textPaint.setColor(0xff333333);

		// textPaint.setStrokeWidth(1);

		// textPaint.set
		tsize = textPaint.getFontSpacing();

		textLayoutPaint.setColor(0xffffffff); // Layout number
		textLabelLayoutPaint.setColor(0xaa00ff00); // Layout label

		mDrawable = new ShapeDrawable(new RectShape()); // Mousepad
		// mDrawable.getPaint().setColor(0xff000088); // Blue
		mDrawable.getPaint().setColor(0xff2222ff); // Blue

		wheel = new ShapeDrawable(new RectShape()); // Mousepad wheel
		// wheel.getPaint().setColor(0xff555588); //
		wheel.getPaint().setColor(0xff8888ff); //

		backGround = new ShapeDrawable(new RectShape()); // Keys background
		// backGround.getPaint().setColor(0xff8888ff); // Blue
		backGround.getPaint().setColor(0xff000000); // Black

		MouseKeysRemote.mouseWheelWidth = (int) (xdpi / 2);
	}

	public void setMark(EditMode mode) {
		this.mode = mode;

		if (mode == EditMode.Binding) {
			mark = MARK_STAR;
		} else if (mode == EditMode.Name) {
			mark = MARK_HASH;
		} else if (mode == EditMode.Color) {
			mark = MARK_C;
		} else if (mode == EditMode.Sticky) {
			mark = MARK_S;
			this.mode = EditMode.None;
		} else {
			mark = "";
		}
	}

	@Override
	protected void onMeasure(int wMeasureSpec, int hMeasureSpec) {
		measuredHeight = MeasureSpec.getSize(hMeasureSpec);
		measuredWidth = MeasureSpec.getSize(wMeasureSpec);

		/*
		 * if (RemotePC2.enableMouseWheel) // Fill entire right hand side. Maybe
		 * later.. { RemotePC2.xOffsetRight = (int)(xdpi/2); } else
		 * RemotePC2.xOffsetRight = 0;
		 */
		if (MouseKeysRemote.enableMousePad) {
			float calc = (float) measuredHeight
					* (((float) MouseKeysRemote.mousepadRelativeSize / 100));
			// RemotePC2.debug ((int)calc);
			MouseKeysRemote.yOffsetBottom = (int) calc; // 200; //measuredHeight
														// -
														// measuredHeight/(RemotePC2.mousepadRelativeSize/10);
														// // Size of
														// keypad/mousepad
		} else
			MouseKeysRemote.yOffsetBottom = 0;
		try {
			keyWidth = ((measuredWidth - MouseKeysRemote.xOffsetRight) - MouseKeysRemote.xOffsetLeft)
					/ MouseKeysRemote.numberOfKeyCols;
			keyHeight = ((measuredHeight - MouseKeysRemote.yOffsetBottom) - MouseKeysRemote.yOffsetTop)
					/ MouseKeysRemote.numberOfKeyRows;
		} catch (Exception e) {
			MouseKeysRemote.debug(e.toString());
		}
		// RemotePC2.debug("measuredHeight: " + measuredHeight +
		// " measuredWidth: " + measuredWidth);
		// RemotePC2.debug("keyWidth: " + keyWidth + "keyHeight: " + keyHeight);
		// textLayoutPaint.setTextSize(measuredHeight);
		textLabelLayoutPaint.setTextSize(measuredHeight / 16);
		// t_layout_size = textLayoutPaint.getFontSpacing();
		l_layout_size = textLabelLayoutPaint.getFontSpacing();
		// RemotePC2.debug("t_layout_size: " + t_layout_size);
		// textPaint.setTextSize(keyHeight/6);
		MouseKeysRemote.keyWidth = keyWidth;
		MouseKeysRemote.keyHeight = keyHeight;

		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	protected void onDraw(Canvas canvas) {

		// RemotePC.debug("onDraw: h:" + getMeasuredHeight() +" w: " +
		// getMeasuredWidth());
		backGround.getPaint().setColor(MouseKeysRemote.backCol);
		backGround.setBounds(0, 0, measuredWidth, measuredHeight);
		backGround.draw(canvas);

		textPaint.setTextScaleX(MouseKeysRemote.textSize);
		if (MouseKeysRemote.calibrate == true) {
			canvas.drawText("Calibrate sensors", 0, measuredHeight / 4,
					textLabelLayoutPaint);
			canvas.drawText("Touch when ready!", 0, measuredHeight / 2,
					textLabelLayoutPaint);
		} else {
			drawMousepad(canvas);

			drawKeys(canvas);

			drawLayoutlabel(canvas);
		}
	}

	private void drawLayoutlabel(Canvas canvas) {
		// Draw layout label
		String layoutLabel = MouseKeysRemote.keys_layout_name;
		float layoutLabelWidth = textLabelLayoutPaint.measureText(layoutLabel);
		canvas.drawText(layoutLabel,
				(measuredWidth / 2) - layoutLabelWidth / 2, measuredHeight
						- (l_layout_size / 2), textLabelLayoutPaint);
	}

	private void drawKeys(Canvas canvas) {
		int keyCnt = 0;
		int keyCol;
		for (int i = 0; i < MouseKeysRemote.numberOfKeyRows; i++) {
			// yCnt = yCnt + (keyHeight * i); // Row
			for (int j = 0; j < MouseKeysRemote.numberOfKeyCols; j++) {
				MouseKeysRemote.x1Pos[keyCnt] = MouseKeysRemote.xOffsetLeft
						+ (keyWidth * j);
				MouseKeysRemote.y1Pos[keyCnt] = MouseKeysRemote.yOffsetTop
						+ (keyHeight * i);
				MouseKeysRemote.x2Pos[keyCnt] = MouseKeysRemote.xOffsetLeft
						+ (keyWidth * j) + keyWidth;
				MouseKeysRemote.y2Pos[keyCnt] = MouseKeysRemote.yOffsetTop
						+ (keyHeight * i) + keyHeight;
				// RemotePC2.debug("keyRect.toString: " +
				// keyRect.toString());
				int sticky = MouseKeysRemote.getKeyValueSticky(keyCnt + 1);

				keyCol = MouseKeysRemote.mySharedPreferences.getInt("KeyColor"
						+ "layout" + MouseKeysRemote.keys_layout + "key"
						+ (keyCnt + 1), 0xff2222bb);
				//
				// int r = Color.red(keyCol);
				// int g = Color.green(keyCol);
				// int b = Color.blue(keyCol);
				// newCol = Color.argb(255, (255 -r)*2 & 0xFF, (255 -g)*2 &
				// 0xFF ,(255 -b)*2 & 0xFF);
				//
				if ((MouseKeysRemote.keyState[keyCnt] == MouseKeysRemote.KEY_STATE_UP || (MouseKeysRemote
						.getKeyValue(keyCnt + 1) == MouseKeysRemote.DUMMY && mode == EditMode.None))
						&& sticky == MouseKeysRemote.UNSTUCK_KEY) // Button
																	// up
				{

					rectPaint.setStyle(Paint.Style.FILL_AND_STROKE);

					rectPaint.setColor(keyCol);
					keyRect.set(
							MouseKeysRemote.x1Pos[keyCnt] + (keyWidth / 10),
							MouseKeysRemote.y1Pos[keyCnt] + (keyHeight / 10),
							MouseKeysRemote.x2Pos[keyCnt] - (keyWidth / 10),
							MouseKeysRemote.y2Pos[keyCnt] - (keyHeight / 10));
					canvas.drawRoundRect(keyRect, (float) keyWidth / 4,
							(float) keyHeight / 4, rectPaint);

					rectPaint.setStyle(Paint.Style.STROKE);

					// rectPaint.setColor(newCol);
					rectPaint.setColor(0xffffffff); // Outer

					keyRect.set(
							MouseKeysRemote.x1Pos[keyCnt] + (keyWidth / 10),
							MouseKeysRemote.y1Pos[keyCnt] + (keyHeight / 10),
							MouseKeysRemote.x2Pos[keyCnt] - (keyWidth / 10),
							MouseKeysRemote.y2Pos[keyCnt] - (keyHeight / 10));
					canvas.drawRoundRect(keyRect, (float) keyWidth / 4,
							(float) keyHeight / 4, rectPaint);

				} else // Down
				{
					rectPaint.setStyle(Paint.Style.FILL_AND_STROKE);

					rectPaint.setColor(0xff999999); // Key down
					keyRect.set(
							MouseKeysRemote.x1Pos[keyCnt] + (keyWidth / 10),
							MouseKeysRemote.y1Pos[keyCnt] + (keyHeight / 10),
							MouseKeysRemote.x2Pos[keyCnt] - (keyWidth / 10)
									- (keyWidth / 10) / 2,
							MouseKeysRemote.y2Pos[keyCnt] - (keyHeight / 10)
									- (keyHeight / 10) / 2);
					canvas.drawRoundRect(keyRect, (float) keyWidth / 4,
							(float) keyHeight / 4, rectPaint);
				}

				if (MouseKeysRemote.getKeyValue(keyCnt + 1) == MouseKeysRemote.DUMMY
						&& mode == EditMode.None) {
				} else {
					String keyName;
					if (mode == EditMode.Binding) {
						keyName = mark
								+ MouseKeysRemote.getActionName(MouseKeysRemote
										.getKeyValue(keyCnt + 1));
					} else {
						keyName = mark
								+ MouseKeysRemote.mySharedPreferences
										.getString(
												"nameOfKey"
														+ "layout"
														+ MouseKeysRemote.keys_layout
														+ "key" + (keyCnt + 1),
												MouseKeysRemote
														.getActionName(MouseKeysRemote
																.getKeyValue(keyCnt + 1)));
					}
					// MouseKeysRemote.debug("nameOfKey" + "layout" +
					// MouseKeysRemote.keys_layout + "key" + (keyCnt + 1));
					// float keyNameWidth =
					// backGround.getPaint().measureText(keyName);
					// RemotePC2.debug("tsize: " + tsize);
					// canvas.drawText (keyName,
					// MouseKeysRemote.x1Pos[keyCnt] + (keyWidth/2) -
					// (keyNameWidth/2), MouseKeysRemote.y1Pos[keyCnt]+
					// keyHeight/2 + (int)tsize/2, textPaint);
					// canvas.drawText (keyName,
					// MouseKeysRemote.x1Pos[keyCnt] + (keyWidth/10) + 2,
					// MouseKeysRemote.y1Pos[keyCnt]+ keyHeight/2 +
					// (int)tsize/2, textPaint);
					textPaint.setColor(MouseKeysRemote.textCol);
					canvas.drawText(keyName, MouseKeysRemote.x1Pos[keyCnt]
							+ (keyWidth / 2), MouseKeysRemote.y1Pos[keyCnt]
							+ keyHeight / 2 + (int) tsize / 2, textPaint);

				}
				keyCnt++;
			}
		}
	}

	private void drawMousepad(Canvas canvas) {
		// Draw mouse pad
		if (MouseKeysRemote.enableMousePad) {
			// Mousepad background
			mDrawable.getPaint().setColor(MouseKeysRemote.mousepadCol);
			mDrawable.setBounds(0,
					(measuredHeight - MouseKeysRemote.yOffsetBottom),
					measuredWidth, measuredHeight);
			mDrawable.draw(canvas);

			// RemotePC2.debug("scale: " + scale+ "xdpi: " + xdpi);
			// Mousewheel background
			if (MouseKeysRemote.enableMouseWheel) {
				// wheel.setBounds(measuredWidth -
				// (int)(xdpi/2),(RemotePC2.yOffsetTop), measuredWidth ,
				// measuredHeight);
				wheel.getPaint().setColor(MouseKeysRemote.mousewheelCol);
				wheel.setBounds(
						measuredWidth - MouseKeysRemote.mouseWheelWidth,
						(measuredHeight - MouseKeysRemote.yOffsetBottom),
						measuredWidth, measuredHeight);
				wheel.draw(canvas);
			}

			// canvas.save();
		}
	}

}
