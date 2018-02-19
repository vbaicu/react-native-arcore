package com.rnarcore.arview;

import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.rnarcore.arview.reactcomponent.ARSurfaceView;

/**
 * Created by vbaicu on 2/17/18.
 */

public class ReactImageManager extends SimpleViewManager<ARSurfaceView> {
    public static final String REACT_CLASS = "ARSurfaceViewDroid";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ARSurfaceView createViewInstance(ThemedReactContext reactContext) {
            return new ARSurfaceView(reactContext);
    }

}