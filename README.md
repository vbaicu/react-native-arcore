## react-native-arcore
This project showcases simple bindings of Android ARCore into react-native. It's not a full arcore wrapper in rect-native, just reveals the path for integration in bigger awesome projects.

I'll update it as I improve the interface and controls in my project.

My project is based on Google's Java ARCore starter example.

### Features
* `GLSurfaceView` with ARSession as a react UI Component, easy to integrate

* Callbacks from native code to JS

* Load modules and textures as base64
* Object scaling
* Object rotation
* Object selection
* Object moving

### Usage
I'll skip the "integrate into your project" part cause it's pretty boring  and you probably know that already. You can also run this project out of the box(droid only).
**Note: You need an ARCore compatible device and the preview sdk installed on your phone(https://developers.google.com/ar/develop/java/quickstart)** 

#### Add the view
```js const ARSurfaceViewDroid = require('./ARSurfaceViewDroid')
const Dimensions = require('Dimensions');
...
return  <ARSurfaceViewDroid style={{width:Dimensions.get('window').width, height:Dimensions.get('window').height}} />

```

Yes, you need to make it full screen, otherwise the touch listener will go crazy and no longer recognize your input. 
Feel free to make a pull request with a fix :) 

#### Add the listeners
```js
 DeviceEventEmitter.addListener('onObjectPlaced',(data) => {})
    DeviceEventEmitter.addListener('onObjectSelectd',(data) => console.log(data))
    DeviceEventEmitter.addListener('onPlaneStateUpdate',(data) => console.log(data))
    DeviceEventEmitter.addListener('onError',(error) => console.log(error))

```

#### Load the models(base64 strings) and start the ARSession

```js
  componentDidMount() {
    setTimeout(() => {
    NativeModules.ArView.setUp(models.raptor,models.cat,0.01)
    NativeModules.ArView.start()
    },1000)
}
```

**Note** that I did the loading in `componentDidMount` with 1 second delay, so my native object will be instantiated by the time this code is executed.

Now you should be able to load **Raptors** with shadows, move, rotate and scale them.

