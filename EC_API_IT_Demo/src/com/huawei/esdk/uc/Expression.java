
package com.huawei.esdk.uc;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Created by yaoyuan on 16/10/16.
 */
public class Expression {

    private Face targetFace;

    private FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("8ea54153bfca452290ecaec326e2b420");

    public interface MyCallBack {
        void onFaceDetected();
    }

    private void detectAndFrame(final Bitmap imageBitmap, final MyCallBack myCallBack)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            Log.e("expression", "Detecting");
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    null           // returnFaceAttributes: a string like "age, gender"
                            );
                            targetFace = result[0];
                            if (result == null)
                            {
                                Log.e("expression", "Detection Finished. Nothing detected");
                                publishProgress("Detection Finished. Nothing detected");
                                return null;
                            }
                            Log.e("expression", String.format("Detection Finished. %d face(s) detected",
                                    result.length));
                            publishProgress(
                                    String.format("Detection Finished. %d face(s) detected",
                                            result.length));
                            myCallBack.onFaceDetected();
                            return result;
                        } catch (Exception e) {
                            Log.e("expression", "Detecting failed | " + e.getMessage());
                            publishProgress("Detection failed");
                            return null;
                        }
                    }
                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog

                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                    }
                };
        detectTask.execute(inputStream);
    }

    public Face detectFace(Bitmap bitmap, MyCallBack myCallBack){
        detectAndFrame(bitmap, myCallBack);
        return targetFace;
    }

    public double[] getNose(){

        double[] noseTip = new double[2];
        noseTip[0] = targetFace.faceLandmarks.noseTip.x;
        noseTip[1] = targetFace.faceLandmarks.noseTip.y;
        return noseTip;
    }

    public double[] getLeftEye(){
        double[] leftEye = new double[2];
        leftEye[0] = targetFace.faceRectangle.left + targetFace.faceRectangle.width / 3;
        leftEye[1] = targetFace.faceRectangle.top + targetFace.faceRectangle.height *3/ 5;
        return leftEye;
    }

    public double getEyeDis() {
        return targetFace.faceLandmarks.eyebrowRightInner.x - targetFace.faceLandmarks.eyebrowLeftInner.x;
    }

    public double getWidth() {
        return targetFace.faceRectangle.width;
    }

    public void setExpression(double[] src, double[] dst, Bitmap bitmap){// src 1 :(352,560)px ;dst 1

    }



}
