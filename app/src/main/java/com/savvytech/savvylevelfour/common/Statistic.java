//-----------------------------------------------------------------------------
//
//	Statistic
//
//	Author:		Mike Ghost
//	Date:		02 Aug 16
//	Revision:	1.0.160802.1909
//
//-----------------------------------------------------------------------------

package com.savvytech.savvylevelfour.common;


public class Statistic {
	
	private int capacity = 5;
	private Float[] fArray;
	private int idx = 0;
	
	private float sum() {
		float f = 0;
		for (int i = 0; i < capacity; i++) {
			if (fArray[i] == null)
				f += 0;
			else
				f += fArray[i];
		}
		return f;
	}
	
	private float average() {
		return sum() / capacity;
	}
	
	private float averageAngle() {
		float x = 0;
		float y = 0;
		for (int i = 0; i < capacity; i++) {
			if (fArray[i] == null) {
				x += 0;
				y += 0;
			} else {
				x += Math.cos(fArray[i]);
				y += Math.sin(fArray[i]);
			}
		}
		return (float) Math.atan2(y, x);
	}
	
	public Statistic() {
		fArray = new Float[capacity];
	}
	
	public Statistic(int capacity) {
		this.capacity = capacity;
		fArray = new Float[capacity];
	}
	
	public float getAverage(float f) {
		fArray[idx++] = f;
		if (idx == capacity) idx = 0;
		return average();
	}
	
	public float getAverageAngle(float angle) {
		fArray[idx++] = (float) (angle * Math.PI / 180);
		if (idx == capacity) idx = 0;
		float ret = (float) (averageAngle() * 180 / Math.PI);
		return ret;
	}
	


}
