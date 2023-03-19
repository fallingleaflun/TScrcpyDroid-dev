package com.example.tscrcpydroid.viewmodels

import android.content.Context
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.tscrcpydroid.R
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.data.entities.Resolution
import com.example.tscrcpydroid.util.ConnectionTools
import com.example.tscrcpydroid.util.getMyWifiIPAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.Integer.max
import javax.inject.Inject

/**
 * 无意义的leak，ApplicationContext本来就会贯穿整个程序
 * 但我不确定能不能在ViewModel中用Context
 */
@HiltViewModel
class SettingScreenViewModel@Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    savedStateHandle: SavedStateHandle
): ViewModel(){
    private val _state = MutableStateFlow(SettingScreenState())
    val state: StateFlow<SettingScreenState>
        get() = _state

    init {
        val prefs = applicationContext.getSharedPreferences("setting_data", Context.MODE_PRIVATE)
        val ip_address = prefs.getString("ip_address", "")
        val timeout = prefs.getInt("timeout", 100000)
        val resolution_width = prefs.getInt("resolution_width", 1920)
        val resolution_height = prefs.getInt("resolution_height", 1080)
        val bitrate = prefs.getInt("bitrate", 1*1024*1024)
        val no_control_checked = prefs.getBoolean("no_control_checked", false)
        val navbar_checked = prefs.getBoolean("navbar_checked", false)
        _state.value = _state.value.copy(
            currentIpAddress = ip_address,
            currentTimeOut = timeout,
            selectedResolution = Resolution(resolution_width, resolution_height),
            selectedBitRate = BitRate(bitrate),
            noControlChecked = no_control_checked,
            navBarChecked = navbar_checked
        )
    }

    fun saveSettingToSharedPreferences(){
        val editor = applicationContext.getSharedPreferences("setting_data", Context.MODE_PRIVATE).edit()
        editor.putString("ip_address", _state.value.currentIpAddress?:"")
        editor.putInt("timeout", _state.value.currentTimeOut)
        editor.putInt("resolution_width", _state.value.selectedResolution.width)
        editor.putInt("resolution_height", _state.value.selectedResolution.height)
        editor.putInt("bitrate", _state.value.selectedBitRate.value)
        editor.putBoolean("no_control_checked", _state.value.noControlChecked)
        editor.putBoolean("navbar_checked", _state.value.navBarChecked)
        editor.apply()
    }

    fun onEvent(event: SettingEvent){
        Log.i("TZL", "Event:${event}")
        when(event){
            is SettingEvent.ResolutionChosen -> {
                _state.value = _state.value.copy(
                    resolutionMenuExpanded = false,
                    selectedResolution = event.value
                )
            }
            is SettingEvent.ResolutionExpandedChange -> {
                _state.value = _state.value.copy(
                    resolutionMenuExpanded = !_state.value.resolutionMenuExpanded
                )
            }
            is SettingEvent.ResolutionExpandedDismissed -> {
                _state.value = _state.value.copy(
                    resolutionMenuExpanded = false
                )
            }

            is SettingEvent.BitRateChosen -> {
                _state.value = _state.value.copy(
                    selectedBitRate = event.value,
                    bitrateMenuExpanded = false
                )
            }
            is SettingEvent.BitRateExpandedChange -> {
                _state.value = _state.value.copy(
                    bitrateMenuExpanded = !_state.value.bitrateMenuExpanded
                )
            }
            is SettingEvent.BitRateExpandedDismissed -> {
                _state.value = _state.value.copy(
                    bitrateMenuExpanded = false
                )
            }
            is SettingEvent.navBarCheckChange -> {
                _state.value = _state.value.copy(
                    navBarChecked = event.value
                )
            }
            is SettingEvent.noControlCheckChange -> {
                _state.value = _state.value.copy(
                    noControlChecked = event.value
                )
            }

            is SettingEvent.ipAddressChange -> {
                _state.value = _state.value.copy(
                    currentIpAddress = event.value
                )
            }
            is SettingEvent.startButtonClicked -> {
                saveSettingToSharedPreferences()
                startControl()
            }
            is SettingEvent.timeOutChange -> {
                try{
                    val tmp = event.value.let{
                        if(it.isBlank())
                            10000
                        else
                            it.toInt()
                    }
                    _state.value = _state.value.copy(
                        currentTimeOut = tmp
                    )
                }
                catch (nfe: NumberFormatException){
                    nfe.printStackTrace()
                }
            }
            is SettingEvent.portChange -> {
                try{
                    val tmp = event.value.let{
                        if(it.isBlank())
                            13432
                        else
                            it.toInt()
                    }
                    _state.value = _state.value.copy(
                        currentPort = tmp
                    )
                }
                catch (nfe: NumberFormatException){
                    nfe.printStackTrace()
                }
            }
        }
    }

    fun startControl(): Boolean{
        val localIP = getMyWifiIPAddress(applicationContext)
        if(!Patterns.IP_ADDRESS.matcher(localIP).matches()){
            Toast.makeText(applicationContext, applicationContext.getString(R.string.local_ipaddress_error_hint), Toast.LENGTH_SHORT).show()
            return false
        }
        if(!Patterns.IP_ADDRESS.matcher(_state.value.currentIpAddress).matches()){
            Toast.makeText(applicationContext, applicationContext.getString(R.string.ipaddress_error_hint), Toast.LENGTH_SHORT).show()
            return false
        }
        if(!(_state.value.currentPort in 5000..20000)){
            Toast.makeText(applicationContext, applicationContext.getString(R.string.port_error_hint), Toast.LENGTH_SHORT).show()
            return false
        }

        if(!(_state.value.currentTimeOut in 0..100000)){
            Toast.makeText(applicationContext, applicationContext.getString(R.string.timeout_error_hint), Toast.LENGTH_SHORT).show()
            return false
        }
        Toast.makeText(applicationContext, applicationContext.getString(R.string.starting_adb_hint), Toast.LENGTH_SHORT).show()
        val result = ConnectionTools.startRemoteServer(
            applicationContext,
            localIP = localIP!!,
            targetIP = _state.value.currentIpAddress!!,
            targetPort = _state.value.currentPort,
            fileBase64 = ConnectionTools.getFileBase64(applicationContext),
            bitRate = _state.value.selectedBitRate,
            size = max(_state.value.selectedResolution.height, _state.value.selectedResolution.width),
            timeOut = _state.value.currentTimeOut
        )
        if(result==false){
            Toast.makeText(applicationContext, applicationContext.getString(R.string.start_adb_error_hint), Toast.LENGTH_SHORT).show()
            return false
        }
        Toast.makeText(applicationContext, applicationContext.getString(R.string.start_adb_success_hint), Toast.LENGTH_SHORT).show()
        return true
    }


    override fun onCleared() {
        super.onCleared()
        saveSettingToSharedPreferences()
    }
}

data class SettingScreenState(
    val currentIpAddress: String? = null,
    val currentPort: Int = 13432,
    val currentTimeOut: Int = 1000000,
    val resolutionMenuExpanded: Boolean = false,
    val selectedResolution: Resolution = Resolution(1080, 1920),
    val bitrateMenuExpanded: Boolean = false,
    val selectedBitRate: BitRate = BitRate(1024*1024),
    val noControlChecked: Boolean = false,
    val navBarChecked: Boolean = false
)
