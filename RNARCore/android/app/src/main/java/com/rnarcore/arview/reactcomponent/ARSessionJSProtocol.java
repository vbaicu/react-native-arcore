package com.rnarcore.arview.reactcomponent;

/**
 * Created by vbaicu on 2/19/18.
 */

public interface ARSessionJSProtocol {
    void setUp(String modelBase64, String textureBase64, float scale, ARSessionReactCallback onObjectPlaced, ARSessionReactCallback onObjectSelected, ARSessionReactCallback onPlaneStateUpdate, ARSessionReactCallback onError);
    void start() throws Exception;
    void stop();
}
