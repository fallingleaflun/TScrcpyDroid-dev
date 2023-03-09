package com.example.tscrcpydroid.viewmodels

import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.data.entities.Resolution

sealed class SettingEvent {
    data class ipAddressChange(val value: String) : SettingEvent()
    data class timeOutChange(val value: String) : SettingEvent()
    data class portChange(val value: String) : SettingEvent()
    data class ResolutionChosen(val value: Resolution) : SettingEvent()
    object ResolutionExpandedChange : SettingEvent()
    object ResolutionExpandedDismissed : SettingEvent()
    data class BitRateChosen(val value: BitRate) : SettingEvent()
    object BitRateExpandedChange : SettingEvent()
    object BitRateExpandedDismissed : SettingEvent()
    data class noControlCheckChange(val value: Boolean) : SettingEvent()
    data class navBarCheckChange(val value: Boolean) : SettingEvent()
    object startButtonClicked: SettingEvent()
}
