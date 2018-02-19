package com.rnarcore.arview.reactcomponent;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;


import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.rnarcore.arview.ARSessionInfo;
import com.rnarcore.arview.DisplayRotationHelper;
import com.rnarcore.arview.RotationGestureDetector;
import com.rnarcore.arview.rendering.BackgroundRenderer;
import com.rnarcore.arview.rendering.ObjectRenderer;
import com.rnarcore.arview.rendering.PlaneRenderer;
import com.rnarcore.arview.rendering.PointCloudRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by vbaicu on 2/18/18.
 */

public class ARRenderer implements GLSurfaceView.Renderer, ARSessionJSProtocol {

    public Session mSession;
    public ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    public Context context;

    public GestureDetector mGestureDetector;
    private float mScaleFactor= ARSessionInfo.getInstance().defaultScale;
    private Snackbar mMessageSnackbar;
    public DisplayRotationHelper mDisplayRotationHelper;
    public RotationGestureDetector rotationGestureDetector;
    public ScaleGestureDetector mScaleDetector;

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer mVirtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private final PointCloudRenderer mPointCloud = new PointCloudRenderer();


    private final float cubeHitAreaRadius = 0.8f;
    private final float[] centerVertexOfCube = {0f, 0f, 0f, 1};
    private final float[] vertexResult = new float[4];

    private final float[] mAnchorMatrix = new float[16];

    private ArrayList<ObjectRenderer> objects = new ArrayList<>();

    private final ArrayList<Anchor> mAnchors = new ArrayList<>();
    private final ArrayList<Anchor> mShadowAnchors = new ArrayList<>();


    private float[] mProjmtx = null;
    private float[] mViewmtx = null;
    private float rotationAngle = 0f;
    private int viewWidth;
    private int viewHeight;
    private int mSelectedObjectIndex =-1;
    private String TAG = "ARRenderer";
    private String modelBase64;
    private String textureBase64;
    public ARSessionReactCallback onObjectPlaced;
    public ARSessionReactCallback onObjectSelected;
    public ARSessionReactCallback onPlaneStateUpdate;
    private boolean isSetUp = false;
    private ARSessionReactCallback onError;


    public ARRenderer(Context ctx){
        this.context = ctx;
        ARSessionInfo.getInstance().currentRenderer = this;
        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ context);
        mGestureDetector = new GestureDetector(this.context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
        mScaleDetector = new ScaleGestureDetector(this.context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                mScaleFactor*= scaleGestureDetector.getScaleFactor();

                if(mScaleFactor >0.3f){
                    mScaleFactor =0.3f;
                }
                if(mScaleFactor < 0.005f) {
                    mScaleFactor = 0.005f;
                }
                if(mSelectedObjectIndex != -1 ){
                    objects.get(mSelectedObjectIndex).scaleFactor = mScaleFactor;
                }

                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

            }
        });

        rotationGestureDetector = new RotationGestureDetector(new RotationGestureDetector.OnRotationGestureListener() {
            @Override
            public void OnRotation(float angle) {
                if(mSelectedObjectIndex != -1 ){
                    objects.get(mSelectedObjectIndex).rotationAngle = angle;
                }
                Log.d("ROTATION","angle: "+angle);
            }
        });


    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/ context);
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
        }

        try {
            mVirtualObjectShadow.createOnGlThread(/*context=*/context,
                    "cercmic.obj", "cat.png");
            mVirtualObjectShadow.setBlendMode( ObjectRenderer.BlendMode.Grid);
            mVirtualObjectShadow.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            mPlaneRenderer.createOnGlThread(/*context=*/context, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        mPointCloud.createOnGlThread(/*context=*/context);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        viewWidth = width;
        viewHeight = height;
    }
    public ObjectRenderer initObject () {
        ObjectRenderer obj = new ObjectRenderer();
        try {
            obj.createOnGlThreadWithBase64Object(/*context=*/context, ARSessionInfo.getInstance().modelName, ARSessionInfo.getInstance().textureName);
            obj.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
            obj.scaleFactor = ARSessionInfo.getInstance().defaultScale;
            return obj;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            Frame frame = mSession.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && camera.getTrackingState() == Trackable.TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();
                    int selectedObjectIndex = getSelectedObjectIndex(tap);
                    if(selectedObjectIndex != -1 ) {
                        if((mSelectedObjectIndex!= -1 && mSelectedObjectIndex == selectedObjectIndex) || mSelectedObjectIndex == -1) {
                            mAnchors.remove(selectedObjectIndex);
                            mAnchors.add(selectedObjectIndex, hit.getTrackable().createAnchor(hit.getHitPose().compose(Pose.makeTranslation(0, 0.2f, 0))));
                            objects.get(selectedObjectIndex).isSelected = true;
                            mSelectedObjectIndex = selectedObjectIndex;
                            Log.d(TAG,"Object selected at index: "+mSelectedObjectIndex);
                            onObjectSelected.invoke("Obj selected "+ selectedObjectIndex);
                        }
                        break;
                    }
                    if (trackable instanceof Plane
                            && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                        if (mAnchors.size() >= 20) {
                            mAnchors.get(0).detach();
                            mAnchors.remove(0);
                        }
                        Log.d(TAG,"Selected object: "+mSelectedObjectIndex);
                        if(mSelectedObjectIndex == -1) {
                            objects.add(initObject());
                            mAnchors.add(hit.createAnchor());
                            mShadowAnchors.add(hit.createAnchor());
                            onObjectPlaced.invoke("Obj placed");
                        } else {
                            mAnchors.remove(mSelectedObjectIndex);
                            mShadowAnchors.remove(mSelectedObjectIndex);
                            mAnchors.add(hit.createAnchor());
                            mShadowAnchors.add(hit.createAnchor());
                            ObjectRenderer obj = objects.get(mSelectedObjectIndex);
                            obj.isSelected = false;
                            objects.remove(mSelectedObjectIndex);
                            objects.add(obj);
                            mSelectedObjectIndex = -1;
                            onObjectPlaced.invoke("Obj placed");
                        }

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break;
                    }
                }
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == Trackable.TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            mPointCloud.update(pointCloud);
            mPointCloud.draw(viewmtx, projmtx);

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mMessageSnackbar != null) {
                for (Plane plane : mSession.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == Trackable.TrackingState.TRACKING) {
//                        hideLoadingMessage();
                        onPlaneStateUpdate.invoke("Plane found");
                        //TODO:notify react about plane found
                        break;
                    }
                }
            }

            // Visualize planes.
            mPlaneRenderer.drawPlanes(
                    mSession.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            // Visualize anchors created by touch.
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();
            int action = MotionEvent.ACTION_DOWN;
            int x = viewWidth/2;
            int y = viewHeight/2;
            int metaState = 0;

            float scaleFactor = mScaleFactor;
            int index = 0;
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != Trackable.TrackingState.TRACKING) {
                    continue;
                }
                anchor.getPose().toMatrix(mAnchorMatrix, 0);

                if(mSelectedObjectIndex == index) {

                    MotionEvent e = MotionEvent.obtain(downTime, eventTime, action, x, y, metaState);
                    boolean canDraw = false;
                    for (HitResult hit : frame.hitTest(e)) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon
                        Trackable trackable = hit.getTrackable();
                        if (trackable instanceof Plane
                                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                            canDraw = true;
                            Pose.makeTranslation(0, 0.2f, 0).compose(hit.getHitPose()).toMatrix(mAnchorMatrix,0);
                            break;
                        }
                    }
                    Log.d(TAG,"Can draw: "+ canDraw +" at index: "+index);
                    if(!canDraw){
                        index++;
                        continue;
                    }

                }
                updateObjectScaleAndRotation(index,projmtx,viewmtx,lightIntensity,scaleFactor,rotationAngle,anchor.getPose().tx(),anchor.getPose().tz());
                index++;
            }
            index = 0;
            for (Anchor anchor : mShadowAnchors) {
                if (anchor.getTrackingState() != Trackable.TrackingState.TRACKING) {
                    continue;
                }
                anchor.getPose().toMatrix(mAnchorMatrix, 0);
                if(mSelectedObjectIndex == index) {
                    MotionEvent e = MotionEvent.obtain(downTime, eventTime, action, x, y, metaState);
                    boolean canDraw = false;
                    for (HitResult hit : frame.hitTest(e)) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon
                        Trackable trackable = hit.getTrackable();
                        if (trackable instanceof Plane
                                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                            canDraw = true;
                            Pose.makeTranslation(0, 0.0f, 0).compose(hit.getHitPose()).toMatrix(mAnchorMatrix,0);
                            break;
                        }
                    }
                    if(!canDraw){
                        index++;
                        continue;
                    }
                }

                updateShadow(projmtx,viewmtx,lightIntensity);
                index++;
            }

        } catch (Throwable t) {
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private int getSelectedObjectIndex(MotionEvent tap) {
        for(int i=0;i<objects.size();i++){
            if(isMVPMatrixHitMotionEvent(objects.get(i).getmModelViewProjectionMatrix(),tap)){
                return i;
            }
        }
        return -1;
    }

    private boolean isMVPMatrixHitMotionEvent(float[] ModelViewProjectionMatrix, MotionEvent event){
        if(event == null){
            return false;
        }
        Matrix.multiplyMV(vertexResult, 0, ModelViewProjectionMatrix, 0, centerVertexOfCube, 0);
        /**
         * vertexResult = [x, y, z, w]
         *
         * coordinates in View
         * ┌─────────────────────────────────────────┐╮
         * │[0, 0]                     [viewWidth, 0]│
         * │       [viewWidth/2, viewHeight/2]       │view height
         * │[0, viewHeight]   [viewWidth, viewHeight]│
         * └─────────────────────────────────────────┘╯
         * ╰                view width               ╯
         *
         * coordinates in GLSurfaceView frame
         * ┌─────────────────────────────────────────┐╮
         * │[-1.0,  1.0]                  [1.0,  1.0]│
         * │                 [0, 0]                  │view height
         * │[-1.0, -1.0]                  [1.0, -1.0]│
         * └─────────────────────────────────────────┘╯
         * ╰                view width               ╯
         */
        // circle hit test
        float radius = (viewWidth / 2) * (cubeHitAreaRadius/vertexResult[3]);
        float dx = event.getX() - (viewWidth / 2) * (1 + vertexResult[0]/vertexResult[3]);
        float dy = event.getY() - (viewHeight / 2) * (1 - vertexResult[1]/vertexResult[3]);
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < radius;
    }

    private void updateObjectScaleAndRotation(int index, float[] projmtx, float[] viewmtx, float lightIntensity, float scaleFactor, float rotationY, float transaltionX, float transationZ ) {
        // Update and draw the model and its shadow.
        ObjectRenderer obj =  objects.get(index);
        obj.updateModelMatrix(mAnchorMatrix,obj.scaleFactor,obj.rotationAngle,transaltionX,transationZ);
        obj.draw(viewmtx, projmtx, lightIntensity);
    }

    private void updateShadow(float[] projmtx, float[] viewmtx, float lightIntensity) {
        mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, 0.6f);
        mVirtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);
    }


    private void onSingleTap(MotionEvent e) {
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void setUp(String modelBase64, String textureBase64, float scale, ARSessionReactCallback onObjectPlaced, ARSessionReactCallback onObjectSelected, ARSessionReactCallback onPlaneStateUpdate, ARSessionReactCallback onError) {
        this.modelBase64 = modelBase64;
        this.textureBase64 = textureBase64;
        mScaleFactor = scale;
        this.onObjectPlaced = onObjectPlaced;
        this.onObjectSelected = onObjectSelected;
        this.onPlaneStateUpdate = onPlaneStateUpdate;
        this.onError = onError;
        this.isSetUp = true;
        mSession.resume();
        mDisplayRotationHelper.onResume();

    }

    @Override
    public void start() throws Exception {
        if(!isSetUp) {
            throw new Exception("Renderer not initialised!");
        }
        mSession.resume();
        mDisplayRotationHelper.onResume();
    }

    @Override
    public void stop() {

    }
}
