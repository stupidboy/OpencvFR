package com.spreadtrum.opencvtestfr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by joe.yu on 2016/5/11.
 */
public class FaceRecognitionActivity extends Activity implements CvCameraViewListener2 {


    private static final String TAG = "joe";
    private static final Scalar FACE_RECT_COLOR = new Scalar(255, 0, 0, 255); // red
    private static final Scalar EYE_RECT_COLOR_LEFT = new Scalar(0, 255, 0, 255); // green
    private static final Scalar EYE_RECT_COLOR_RIGHT = new Scalar(0, 0, 255,
            255); // blue
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mEyesDetector;
    //  private DetectionBasedTracker mNativeDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private LinearLayout mFaceDetectedResult;

    private DetectedFaceView[] mFaceViews = new DetectedFaceView[Config.MAX_FACE_NEEDED_REC];
    private Button mSaveButton;

    static {
        System.loadLibrary("face_detected_jni");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //  System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(
                                R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir,
                                "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(
                                mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from "
                                    + mCascadeFile.getAbsolutePath());
                        // add begin
                        InputStream isEye = getResources().openRawResource(
                                R.raw.haarcascade_eye);
                        File eyeCascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File eyeCascadeFile = new File(eyeCascadeDir,
                                "haarcascade_eye.xml");
                        FileOutputStream oseye = new FileOutputStream(
                                eyeCascadeFile);

                        byte[] buffer1 = new byte[4096];
                        int bytesRead1;
                        while ((bytesRead1 = isEye.read(buffer1)) != -1) {
                            oseye.write(buffer1, 0, bytesRead1);
                        }
                        isEye.close();
                        oseye.close();
                        mEyesDetector = new CascadeClassifier(
                                eyeCascadeFile.getAbsolutePath());
                        if (mEyesDetector.empty()) {
                            Log.e(TAG, "failed to load eye cascade classifier");
                        }
                        // add end
                        //  mNativeDetector = new DetectionBasedTracker(
                        //        mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public FaceRecognitionActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_rec_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.rec_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1);
        mFaceDetectedResult = (LinearLayout) this.findViewById(R.id.detected_face_imgs_rec);
        addFaceDetectedViews();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        FaceDetection fd  = new FaceDetection();
        //fd.faceRec(null,"123");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG,
                    "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
                    mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    Mat mRgbaF;
    Mat mRgbaT;
    Mat mGrayF;
    Mat mGrayT;

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        mGrayF = new Mat(height, width, CvType.CV_8UC4);
        mGrayT = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mRgbaF.release();
        mRgbaT.release();
        mGrayF.release();
        mGrayT.release();
    }

    public int foundCorretEyes(Rect face, Rect[] eyesArray, Face out) {
        ArrayList<Rect> list = new ArrayList<Rect>();
        Rect tlFace = new Rect(0, 0, face.width / 2, face.height / 2);
        Rect trFace = new Rect(face.width / 2, 0, face.width / 2,
                face.height / 2);
        Log.e(TAG, "total --->" + eyesArray.length + "face --->" + face);
        // 1. remove invalid eyes,add others to ArrayList
        for (int i = 0; i < eyesArray.length; i++) {
            Point tl = eyesArray[i].tl().clone();
            Point br = eyesArray[i].br().clone();

            // 1.1 height must < face /4
            if ((eyesArray[i].width <= face.width * 0.4)
                    && (eyesArray[i].x + eyesArray[i].width * 0.3
                    + eyesArray[i].width <= face.width)
                    && (eyesArray[i].x - eyesArray[i].width * 0.3 >= 0)) {
                // 1.2 must in the 1st or 2snd
                if (tlFace.contains(tl) && tlFace.contains(br) || trFace.contains(tl) && trFace.contains(br)) {
                    list.add(eyesArray[i]);
                }
            }

        }
        Log.e(TAG, "valid total count --->" + list.size());
        //2. get the nose order eyes,this might be right.
        if (list.size() >= 2) {
            for (int i = 0; i < list.size(); i++) {
                Rect current = list.get(i);
                Point eyeCenter = new Point(
                        current.tl().x + 0.5 * current.width, current.tl().y + 0.5
                        * current.height);
                double tonose = eyeCenter.x - 0.5 * face.width;
                Log.e(TAG, "eyes current  center --->" + eyeCenter + "-- to nose-->" + tonose);
                Log.e(TAG, "eyes current  rect --->" + current);
                for (int j = i + 1; j < list.size(); j++) {
                    Rect target = list.get(j);
                    Point eyeCenterTarget = new Point(
                            target.tl().x + 0.5 * target.width, target.tl().y + 0.5 * target.height);
                    double tonoseTarget = eyeCenterTarget.x - 0.5 * face.width;
                    Log.e(TAG, "eyes target  center --->" + eyeCenterTarget + "----to noese -->" + tonoseTarget);
                    Log.e(TAG, "eyes target  rect --->" + target);

                    // nose order
                    if (tonose * tonoseTarget < 0 &&
                            Math.abs(Math.abs(tonose) - Math.abs(tonoseTarget)) < current.width * 0.3) {
                        //found :
                        if (tlFace.contains(eyeCenter)) {
                            // current =  left
                            out.setLeftEye(current);
                            out.setRightEye(target);
                        } else {
                            // current =  right
                            out.setRightEye(current);
                            out.setLeftEye(target);
                        }
                        Log.e(TAG, "found---!");
                        return 1;

                    }
                }
            }

        }
        return 0;

    }

    private void addFaceDetectedViews() {

        for (int i = 0; i < Config.MAX_FACE_NEEDED_REC; i++) {
            mFaceViews[i] = (DetectedFaceView) LayoutInflater.from(this).inflate(R.layout.detected_face, null);
            //views[i].init();
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();


            mFaceDetectedResult.addView(mFaceViews[i], new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 70));
        }
        mSaveButton = (Button) this.findViewById(R.id.rec_person_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recPerson();
            }
        });
    }

    private void recPerson() {
        Person p = Person.fromFile(this,"joe");
        FaceDetection fd = new FaceDetection();
        if(currentFace != null) {
            fd.faceRec(currentFace.mat.getNativeObjAddr(), "joe");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "FaceRecognition Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.spreadtrum.opencvtestfr/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "FaceRecognition Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.spreadtrum.opencvtestfr/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    class updateFaceImg implements Runnable {
        DetectedFaceView mView;
        Bitmap mImg;

        public updateFaceImg(DetectedFaceView view, Bitmap img) {
            this.mImg = img;
            this.mView = view;
        }

        @Override
        public void run() {
            mView.setDetectedFaceImg(mImg);
        }
    }

    ArrayList<Face> list = new ArrayList<Face>();

    private void savePerson() {
        Person p = new Person(this, "joe", list);
        p.syncFaceToFile();
    }
   private  Face  currentFace = null;
    private void OnFaceCaptured(Face face) {
        //get an invalid pos to add
        boolean valid = true;
        for (DetectedFaceView view : mFaceViews) {
            if (!view.validFace()) {
                this.runOnUiThread(new updateFaceImg(view, face.img));
                break;
            }
        }
        for (DetectedFaceView view : mFaceViews) {
            if (!view.validFace()) {
                valid = false;
                break;
            }
        }
        if (valid) {
            //Toast.makeText(this,"face done",Toast.LENGTH_SHORT).show();
            if (mSaveButton.getVisibility() == View.GONE) {
                currentFace = face;
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSaveButton.setVisibility(View.VISIBLE);
                    }
                });

            }

        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        // rotate 90:
            /*
           Core.transpose(mRgba,mRgbaT);
            Imgproc.resize(mRgbaT,mRgbaF,mRgbaF.size(),0,0,0);
            Core.flip(mRgbaF,mRgba,-1);

            Core.transpose(mGray,mGrayT);
            Imgproc.resize(mGrayT,mGrayF,mGrayF.size(),0,0,0);
            Core.flip(mGrayF,mGray,-1);

*/

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            // mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();
        MatOfRect eyes = new MatOfRect();
       if(currentFace == null) {
           if (mDetectorType == JAVA_DETECTOR) {
               if (mJavaDetector != null)
                   mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
                           2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                           new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
                           new Size());
           } else if (mDetectorType == NATIVE_DETECTOR) {
               //if (mNativeDetector != null)
               // mNativeDetector.detect(mGray, faces);
           } else {
               Log.e(TAG, "Detection method is not selected!");
           }

           Rect[] facesArray = faces.toArray();


           for (int i = 0; i < facesArray.length; i++) {
               // Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
               // FACE_RECT_COLOR, 3);
               // add begin:
               if (!mEyesDetector.empty()) {
                   Mat faceMat = mGray.submat(facesArray[i]);
                   mEyesDetector.detectMultiScale(faceMat, eyes);
                   Rect[] eyesArray = eyes.toArray();
                   Face out = new Face();
                   out.setFace(facesArray[i]);
                   if (foundCorretEyes(facesArray[i], eyesArray, out) == 1) {

                       Imgproc.rectangle(mRgba, facesArray[i].tl(),
                               facesArray[i].br(), FACE_RECT_COLOR, 3);
                       Imgproc.rectangle(mRgba, out.geteyeABS(true, true), out.geteyeABS(true, false),
                               EYE_RECT_COLOR_LEFT, 3);
                       Imgproc.rectangle(mRgba, out.geteyeABS(false, true), out.geteyeABS(false, false), EYE_RECT_COLOR_RIGHT, 3);
                       out.img = Bitmap.createBitmap(faceMat.cols(), faceMat.rows(), Bitmap.Config.RGB_565);
                       Utils.matToBitmap(faceMat, out.img);
                       out.mat = faceMat;
                       list.add(out);
                       OnFaceCaptured(out);
                   }
                /*
                if (detected_right && detected_left) {
					// face detected...,resize to save the face..
					Imgproc.rectangle(mRgba, facesArray[i].tl(),
							facesArray[i].br(), FACE_RECT_COLOR, 3);

				}
				*/
               }
               // add end:
           }
       }
        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
            setDetectorType(tmpDetectorType);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                // mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                //mNativeDetector.stop();
            }
        }
    }
}
