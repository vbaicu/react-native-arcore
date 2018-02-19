package com.rnarcore.arview.reactcomponent;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

/**
 * Created by vbaicu on 2/18/18.
 */

public class ARSurfaceView extends GLSurfaceView {

    private ARRenderer mRenderer;

    public ARSurfaceView(Context context) {
        super(context);

        mRenderer = new ARRenderer(this.getContext());

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mRenderer.mScaleDetector.onTouchEvent(event);
                mRenderer.rotationGestureDetector.onTouchEvent(event);
                return mRenderer.mGestureDetector.onTouchEvent(event);
            }
        });

        Exception exception = null;
        String message = null;
        try {
            mRenderer.mSession = new Session(/* context= */ this.getContext());

            //start session
//            mRenderer.mSession.resume();
//            mRenderer.mDisplayRotationHelper.onResume();
        } catch (UnavailableArcoreNotInstalledException e) {
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            exception = e;
        } catch (Exception e) {
            message = "This device does not support AR";
            exception = e;
        }
        if(exception != null){
            Toast.makeText(this.getContext(),"Ar cannot be started", Toast.LENGTH_SHORT).show();
            //TODO: notify react about ar error;
        }

    }



}
