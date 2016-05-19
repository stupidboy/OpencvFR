package com.spreadtrum.opencvtestfr;

import android.content.Context;

/**
 * Created by joe.yu on 2016/5/13.
 */
public class FaceDetection {


    public native int faceRec(long ptr,String name);

    Context mContext;
    public FaceDetection(Context context){
        mContext = context;
    }

    public static  String getSaveFolder(){
        return "/data/data/com.spreadtrum.opencvtestfr/person/";
    }
}
