/**
 * @file WrapMotionEvent.java
 * @brief 
 *
 * Copyright (C)2010-2012 Magnus Uppman <magnus.uppman@gmail.com>
 * License: GPLv3+
 */

package com.linuxfunkar.mousekeysremote;

import android.view.MotionEvent;

public class WrapMotionEvent {
	
	protected MotionEvent event;

	protected WrapMotionEvent(MotionEvent event)
	{
		this.event = event;
	}
	
	static public WrapMotionEvent wrap(MotionEvent event)
	{
		try
		{
			//System.out.println("EclairMotionEvent");
			return new EclairMotionEvent(event);
		} catch (VerifyError e)
		{
			//System.out.println("WrapMotionEvent");
			return new WrapMotionEvent(event);
		}
	}
	
	public final long getDownTime() {
		return event.getDownTime();
	}

	public final long getEventTime() {
		return event.getEventTime();
	}

	public int findPointerIndex(int pointerId) {
		return 0;
	}

	public final int getAction() {
		return event.getAction();
	}

	public int getPointerCount() {
		return 1;
	}

	public int getPointerId(int pointerIndex) {
		return 0;
	}

	public final float getX() {
		return event.getX();
	}

	public float getX(int pointerIndex) {
		return event.getX();
	}

	public final float getY() {
		return event.getY();
	}

	public float getY(int pointerIndex) {
		return event.getY();
	}
	
	public final float getPressure() {
		return event.getPressure();
	}
	
	public final float getPressure(int pointerIndex) {
		return event.getPressure();
	}
	
}
