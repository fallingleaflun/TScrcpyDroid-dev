# TScrcpyDroid-dev
This is a client app for scrcpy written in Kotlin.Both Compose and SurfaceView are used.
## Usage
1. Enable Developer options on the server device(the Android device to be controlled).
2. Enbale USB debug.
3. Connect the server device to your PC via USB cable and activate the 5555 port:
```shell
adb tcpip 5555
```
4. Install the app on the client device(the Android device).
5. Input the server device's IP and Port.
6. Configure options such as resolution and BitRate.
7. start!

## Preview
<img src="https://user-images.githubusercontent.com/32243069/226901022-1a01f76b-dff9-4d9c-8698-d6ac01558c85.jpg" width="300">
