//
// Created by joe.yu on 2016/5/13.
//
#include "jni.h"
#include <opencv2/core/core.hpp>
#include "opencv2/contrib/contrib.hpp"
#include "opencv2/highgui/highgui.hpp"
#include <vector>
#include <string>
#define LOG_NDEBUG 0
#define LOG_TAG "joe"
#include "android/log.h"
#include "com_spreadtrum_opencvtestfr_FaceDetection.h"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
#define SAVE_PATH  "/data/data/com.spreadtrum.opencvtestfr/files/person/"
#define FILE_PREFIX ".png"
#define FILE_COUNT 4
using namespace std;
using namespace cv;


int lbpRecognize(vector<Mat> images,vector<int> labels, Mat sample){

    if(images.size() == 0){
       LOGD("JNI: lbp recognize images = null");
        return -1;
    }
    Ptr<FaceRecognizer>model = createLBPHFaceRecognizer();
    model->train(images,labels);
    int predictLabel = -1;
    double confidence = 0.0;
    model->predict(sample,predictLabel,confidence);
    LOGD("JNI: lbp recognize predictLabel = %d,confidence = %d",predictLabel,confidence);
    return predictLabel;

}

JNIEXPORT jint JNICALL Java_com_spreadtrum_opencvtestfr_FaceDetection_faceRec
  (JNIEnv *env, jobject obj, jlong ptr, jstring name){

   LOGD("JNI: Java_com_spreadtrum_opencvtestfr_FaceDetection_faceRec enter");
   vector<Mat> images;
   vector<int> labels;
   jboolean isCopy;
   const char * person = env->GetStringUTFChars(name,&isCopy);
   for (int i =0 ;i<FILE_COUNT; i++){
   std::ostringstream stringstream;
   stringstream <<SAVE_PATH;
   stringstream <<person;
   stringstream << "/";
   stringstream <<i;
   stringstream << FILE_PREFIX;
   Mat greymat, colormat;
   colormat = imread(stringstream.str().c_str());
   LOGD("JNI:reading  :%s",stringstream.str().c_str());
   if(colormat.data) {
   LOGD("JNI:cvt color to grey :%s",stringstream.str().c_str());
       cv::cvtColor(colormat, greymat,CV_BGR2GRAY);
       images.push_back(greymat);
       labels.push_back(i);
   }
   stringstream.clear();
   }
   lbpRecognize(images,labels,*(Mat*)ptr);
    return 0;
  }
