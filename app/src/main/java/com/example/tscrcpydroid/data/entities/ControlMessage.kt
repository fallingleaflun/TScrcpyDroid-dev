package com.example.tscrcpydroid.data.entities

/**
 * 因为原来的项目是pc端对android，所以需要搞一套控制事件的协议转换
 * 现在是android对android，两个方法
 *      1. motionEvent->ControlMessage->过去
 *      2. 改写服务器代码，直接用MotionEvent
 * 用方法1，因为服务器代码还有其他事件的定义，这些都统一在一起了
 * 比如如果外界键盘或手柄还能处理KeyEvent
 * 控制事件，改写为kotlin中的data class
 */
data class ControlMessage(
    private val type: Byte,// TYPE_*
    private val text: String? = "",
    private val metaState: Int? = 0,// KeyEvent.META_*
    private val action: Int? = 0,// KeyEvent.ACTION_* or MotionEvent.ACTION_* or POWER_MODE_*
    private val keycode: Int? = 0,// KeyEvent.KEYCODE_*
    private val buttons: Int? = 0,// MotionEvent.BUTTON_*
    private val pointerId: Long? = 0,
    private val pressure: Float? = 0F,
    private val position: Position? = null,
    private val hScroll: Float? = 0F,
    private val vScroll: Float? = 0F,
    private val copyKey: Int? = 0,
    private val paste: Boolean = false,
    private val repeat: Int? = 0,
    private val sequence: Long? = 0L
)

fun createInjectKeycode(
    action: Int,
    keycode: Int,
    repeat: Int,
    metaState: Int
): ControlMessage{
    return ControlMessage(
        type = TYPE_INJECT_KEYCODE,
        action = action,
        keycode = keycode,
        repeat = repeat,
        metaState = metaState
    )
}

fun createInjectText(text: String): ControlMessage{
    return ControlMessage(
        type = TYPE_INJECT_TEXT,
        text = text
    )
}

fun createInjectTouchEvent(
    action: Int,
    pointerId: Long,
    position: Position,
    pressure: Float,
    buttons: Int
): ControlMessage{
    return ControlMessage(
        type = TYPE_INJECT_TOUCH_EVENT,
        action = action,
        pointerId = pointerId,
        position = position,
        pressure = pressure,
        buttons = buttons
    )
}

fun createInjectScrollEvent(
    position: Position,
    hScroll: Float,
    vScroll: Float,
    buttons: Int
): ControlMessage{
    return ControlMessage(
        type = TYPE_INJECT_SCROLL_EVENT,
        position = position,
        hScroll = hScroll,
        vScroll = vScroll,
        buttons = buttons
    )
}

fun createBackOrScreenOn(action: Int): ControlMessage{
    return ControlMessage(
        type = TYPE_BACK_OR_SCREEN_ON,
        action = action
    )
}

fun createGetClipboard(copyKey: Int): ControlMessage{
    return ControlMessage(
        type = TYPE_GET_CLIPBOARD,
        copyKey = copyKey
    )
}

fun createSetClipboard(
    sequence: Long,
    text: String,
    paste: Boolean
): ControlMessage{
    return ControlMessage(
        type = TYPE_SET_CLIPBOARD,
        sequence = sequence,
        text = text
    )
}

fun createEmpty(type: Byte): ControlMessage{
    return ControlMessage(type=type)
}

//类型只要1个byte
val TYPE_INJECT_KEYCODE: Byte = 0
val TYPE_INJECT_TEXT: Byte = 1
val TYPE_INJECT_TOUCH_EVENT: Byte = 2
val TYPE_INJECT_SCROLL_EVENT: Byte = 3
val TYPE_BACK_OR_SCREEN_ON: Byte = 4
val TYPE_EXPAND_NOTIFICATION_PANEL: Byte = 5
val TYPE_EXPAND_SETTINGS_PANEL: Byte = 6
val TYPE_COLLAPSE_PANELS: Byte = 7
val TYPE_GET_CLIPBOARD: Byte = 8
val TYPE_SET_CLIPBOARD: Byte = 9
val TYPE_SET_SCREEN_POWER_MODE: Byte = 10
val TYPE_ROTATE_DEVICE: Byte = 11

