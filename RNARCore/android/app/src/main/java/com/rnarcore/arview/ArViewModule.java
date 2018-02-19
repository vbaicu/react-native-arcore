package com.rnarcore.arview;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.rnarcore.arview.reactcomponent.ARSessionJSProtocol;
import com.rnarcore.arview.reactcomponent.ARSessionReactCallback;

/**
 * Created by vbaicu on 2/13/18.
 */

public class ArViewModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final int PICK_IMAGE = 1;


    public ArViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener((ActivityEventListener) this);
    }

    @Override
    public String getName() {
        return "ArView";
    }


    private void emit(String name, String data){
        this.getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(name,data);
    }


    @ReactMethod
    public void setUp(String modelBase64, String textureBase64, float defualtScale){
        ARSessionInfo.getInstance().modelName = modelBase64;
        ARSessionInfo.getInstance().textureName = textureBase64;
        ARSessionInfo.getInstance().defaultScale  = defualtScale;
        ARSessionJSProtocol currentRenderer = ARSessionInfo.getInstance().currentRenderer;
        if(currentRenderer == null) {
            return;
        }
        currentRenderer.setUp(modelBase64, textureBase64, defualtScale, new ARSessionReactCallback() {
            @Override
            public void invoke(String data) {
                emit("onObjectPlaced",data);
            }
        }, new ARSessionReactCallback() {
            @Override
            public void invoke(String data) {
                emit("onObjectSelected",data);
            }
        }, new ARSessionReactCallback() {
            @Override
            public void invoke(String data) {
                emit("onPlaneStateUpdate",data);
            }
        }, new ARSessionReactCallback() {
            @Override
            public void invoke(String data) {
                emit("onError",data);
            }
        });
    }

    @ReactMethod
    public void start() {
        ARSessionJSProtocol currentRenderer = ARSessionInfo.getInstance().currentRenderer;
        if (currentRenderer == null) {
            return;
        }
        try {
            currentRenderer.start();
        } catch (Exception ex) {

        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onNewIntent(Intent intent) {

    }
}
