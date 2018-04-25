package com.etop.plate;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * @author huiliu
 */
public class PlateAPI {
	static {
		System.loadLibrary("AndroidPlate");
	}
	
	public native int ETInitPlateKernal(String szSysPath, String FilePath, String CommpanyName, int nProductType, int nAultType, TelephonyManager telephonyManager, Context context);
	
	public native void ETUnInitPlateKernal();
	
	
	
	public native void ETSetPlateROI(int[] borders, int imgWidth, int imgHeight);
	
	
	public native int RecognizePlateNV21(byte[] ImageStreamNV21, int nType,int Width, int Height, char[] Buffer, int BufferLen,int []pLine);

	 public native String GetRecogResult(int nIndex);
	 public native int SavePlateImg(String imgPath, int nImageType );
}
