//
// Created by joe.yu on 2016/5/13.
//
#include "jni.h"

#define LOG_NDEBUG 0
#define LOG_TAG "joe"
#include "android/log.h"
#include "com_spreadtrum_opencvtestfr_FaceDetection.h"
#define LOGD(...) ((void)__android_log_write(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

JNIEXPORT jint JNICALL Java_com_spreadtrum_opencvtestfr_FaceDetection_faceRec
  (JNIEnv *env, jobject obj, jlong ptr, jstring name){

   LOGD("JNI: faceRec");
    return 0;
  }
