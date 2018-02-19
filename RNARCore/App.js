/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  Platform,
  StyleSheet,
  Text,
  View, DeviceEventEmitter, NativeModules
} from 'react-native';

const instructions = Platform.select({
  ios: 'Press Cmd+R to reload,\n' +
    'Cmd+D or shake for dev menu',
  android: 'Double tap R on your keyboard to reload,\n' +
    'Shake or press menu button for dev menu',
});


const models = require('./models')
const ARSurfaceViewDroid = require('./ARSurfaceViewDroid')
const Dimensions = require('Dimensions');

type Props = {};
export default class App extends Component<Props> {

  componentDidMount() {
    DeviceEventEmitter.addListener('onObjectPlaced',(data) => {})
    DeviceEventEmitter.addListener('onObjectSelectd',(data) => console.log(data))
    DeviceEventEmitter.addListener('onPlaneStateUpdate',(data) => console.log(data))
    DeviceEventEmitter.addListener('onError',(error) => console.log(error))
    setTimeout(() => {
    NativeModules.ArView.setUp(models.raptor,models.cat,0.01)
    NativeModules.ArView.start()
    },1000)
}

  render() {
    return  <ARSurfaceViewDroid style={{width:Dimensions.get('window').width, height:Dimensions.get('window').height}} />
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
