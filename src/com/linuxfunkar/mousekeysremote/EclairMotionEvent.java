/**
 * @file EclairMotionEvent.java
 * @brief 
 *
 * Copyright (C)2010-2012 Magnus Uppman <magnus.uppman@gmail.com>
 * License: GPLv3+
 */

package com.linuxfunkar.mousekeysremote;

import android.view.MotionEvent;

public class EclairMotionEvent extends WrapMotionEvent {
	
	protected EclairMotionEvent(MotionEvent event)
	{
		super(event);
	}
	public final int findPointerIndex(int pointerId) {
		return event.findPointerIndex(pointerId);
	}

	public final int getPointerCount() {
		return event.getPointerCount();
	}

	public final int getPointerId(int pointerIndex) {
		return event.getPointerId(pointerIndex);
	}

	public final float getX(int pointerIndex) {
		return event.getX(pointerIndex);
	}

	public final float getY(int pointerIndex) {
		return event.getY(pointerIndex);
	}
}
