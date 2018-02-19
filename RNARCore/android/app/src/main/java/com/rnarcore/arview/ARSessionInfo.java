package com.rnarcore.arview;

import com.rnarcore.arview.reactcomponent.ARSessionJSProtocol;

/**
 * Created by vbaicu on 2/14/18.
 */

public class ARSessionInfo {
    private static final ARSessionInfo ourInstance = new ARSessionInfo();

    public String modelName;
    public String textureName;
    public float defaultScale = 0.01f;
    public ARSessionJSProtocol currentRenderer;

    public static ARSessionInfo getInstance() {
        return ourInstance;
    }

    private ARSessionInfo() {
    }

}
