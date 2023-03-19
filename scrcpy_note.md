## Server.java(入口)
1. 构建一个`Device`
2. `startInitThread`启动一个设置选项的线程
3. 打开一个`DesktopConnection`
4. 调用`DesktopConnection`发送一些metadata
5. 构建一个`ScreenEncoder`
6. 构建一个`Controller`
7. `startController`启动一个线程执行`Controller.control()`
8. `startDeviceMessageSender`启动一个线程执行`Controller.sender.loop()`
9. 在`device`注册一个监听器`setClipboardListener`，如果剪切板改变就用`Controller.sender`通知controller
10. `screenEncoder.streamScreen`显示到`device`上，没开线程所以会阻塞`Server`这个主线程

## Device.java
- 设备的抽象
- 包括参数以及一些调用XXXManager所暴露的方法的方法
- TODO

## Controller.java
- TODO

## ScreenEncoder.java

## 跨进程通信
### LocalSocket以及LocalServerSocket
这是安卓用来跨进程的socketAPI
`DesktopConnection`用到了一个`LocalServerSocket`以及两个`LocalSocket`(`videoSocket`和`controlSocket`)

## 其他
- 使用了`System.arraycopy`进行数组复制,这是个native方法，据说效率比较高，但是是浅拷贝
- try-with-resources语句: Java7开始允许在try关键字后跟一对圆括号，圆括号可以声明，初始化一个或多个资源，此处的资源指那些必须在程序结束时必须关闭的资源（比如数据库连接，网络连接等），try语句在该语句结束时自动关闭这些资源。
- Gradle执行流程
  - `Gradle.buildStarted()`
  - 执行setting.gradle
  - `Gradle.settingsEvaluated()`
  - `Gradle.projectsLoaded()`
  - `Gradle.beforeProject()`
  - `Project.beforeEvaluate()`
  - 执行build.gradle，确定任务子集和配置task
  - `Gradle.afterProject()`
  - `Project.afterEvaluate()`
  - `Gradle.projectsEvaluated()`
  - `Gradle.taskGraph.whenReady()`
  - `Gradle.taskGraph.beforeTask()`
  - 执行Task中的Actions
  - `Gradle.taskGraph.afterTask()`
  - `Gradle.buildFinish()`

## 各种Size
- DisplayInfo.size: 屏幕的大小
- Device.deviceSize: 同上，是屏幕的大小
- maxSize: 客户端传的值
- clientVideoSize
- screnInfo: 包括很多信息
  - contentRect: 设备大小(有可能被crop过)
  - unlockedVideoSize: 视频大小(小于等于设备大小)，已考虑方向(对于固定方向时不适用)
  - deviceRotation: 与受控设备的方向相关
  - lockedVideoOrientation: (-1: 不固定, 0: 正常, 1: 逆时针90°, 2: 180°, 3: 顺时针90°)

## TODO
1. server添加参数ip以及port的参数解析 (完成
2. server的connection连接到localsocket -> 连接到ip:port的socket （完成
3. app的connection tool的命令添加localip:port参数 (完成
4. app启动一个service(完成
5. service开启socket连接到targetip:port(完成
6. 读懂server端的视频流的字节流协议格式(完成
7. service通过socket的输入流接受视频流(完成
8. service通过解码器把视频流解码并渲染到surfaceView上(完成
9.  读懂server段的控制事件的字节流协议格式(完成
10. service通过socket的输出流发送控制事件(完成
11. 出现bug`[server] WARN: Ignore touch event, it was generated for a different device size`，需要搞清楚各种size的关系


