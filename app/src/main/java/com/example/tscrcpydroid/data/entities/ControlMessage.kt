package com.example.tscrcpydroid.data.entities

import android.service.controls.Control

/**
 * 控制事件，改写为kotlin中的data class
 */
data class ControlMessage(
    val type: ControlMessageType,// TYPE_*
    val text: String? = "",
    val metaState: Int? = 0,// KeyEvent.META_*
    val action: Int? = 0,// KeyEvent.ACTION_* or MotionEvent.ACTION_* or POWER_MODE_*
    val keycode: Int? = 0,// KeyEvent.KEYCODE_*
    val buttons: Int? = 0,// MotionEvent.BUTTON_*
    val pointerId: Long? = 0,
    val pressure: Float? = 0F,
    val position: Position? = null,
    val hScroll: Float? = 0F,
    val vScroll: Float? = 0F,
    val copyKey: Int? = 0,
    val paste: Boolean = false,
    val repeat: Int? = 0,
    val sequence: Long? = 0L
)

fun createInjectKeycode(
    action: Int,
    keycode: Int,
    repeat: Int,
    metaState: Int
): ControlMessage{
    return ControlMessage(
        type = ControlMessageType.TYPE_INJECT_KEYCODE,
        action = action,
        keycode = keycode,
        repeat = repeat,
        metaState = metaState
    )
}

fun createInjectText(text: String): ControlMessage{
    return ControlMessage(
        type = ControlMessageType.TYPE_INJECT_TEXT,
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
        type = ControlMessageType.TYPE_INJECT_TOUCH_EVENT,
        action = action,
        pointerId = pointerId,
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
        type = ControlMessageType.TYPE_INJECT_SCROLL_EVENT,
        position = position,
        hScroll = hScroll,
        vScroll = vScroll,
        buttons = buttons
    )
}

fun createBackOrScreenOn(action: Int): ControlMessage{
    return ControlMessage(
        type = ControlMessageType.TYPE_BACK_OR_SCREEN_ON,
        action = action
    )
}

fun createGetClipboard(copyKey: Int): ControlMessage{
    return ControlMessage(
        type = ControlMessageType.TYPE_GET_CLIPBOARD,
        copyKey = copyKey
    )
}

fun createSetClipboard(
    sequence: Long,
    text: String,
    paste: Boolean
): ControlMessage{
    return ControlMessage(
        type = ControlMessageType.TYPE_SET_CLIPBOARD,
        sequence = sequence,
        text = text
    )
}

fun createEmpty(type: ControlMessageType): ControlMessage{
    return ControlMessage(type=type)
}

enum class ControlMessageType {
    TYPE_INJECT_KEYCODE,
    TYPE_INJECT_TEXT,
    TYPE_INJECT_TOUCH_EVENT,
    TYPE_INJECT_SCROLL_EVENT,
    TYPE_BACK_OR_SCREEN_ON,
    TYPE_EXPAND_NOTIFICATION_PANEL,
    TYPE_EXPAND_SETTINGS_PANEL,
    TYPE_COLLAPSE_PANELS,
    TYPE_GET_CLIPBOARD,
    TYPE_SET_CLIPBOARD,
    TYPE_SET_SCREEN_POWER_MODE,
    TYPE_ROTATE_DEVICE
}

