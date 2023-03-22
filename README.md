# TScrcpyDroid-dev
This is a client app for scrcpy written in Kotlin.
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
