package com.example.tscrcpydroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tscrcpydroid.NavRoute
import com.example.tscrcpydroid.R
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.data.entities.Resolution
import com.example.tscrcpydroid.viewmodels.SettingEvent
import com.example.tscrcpydroid.viewmodels.SettingScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingScreenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val resolutionList = remember {
        listOf(
            Resolution(1920, 1080),
            Resolution(1280, 720),
            Resolution(854, 480),
            Resolution(800, 480),
        )
    }
    val bitrateList = remember {
        listOf(
            BitRate(8*1024*1024),
            BitRate(6*1024*1024),
            BitRate(4*1024*1024),
            BitRate(2*1024*1024),
            BitRate(1*1024*1024)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = stringResource(id = R.string.setting_screen_ip_hint),
            style = MaterialTheme.typography.titleMedium
        )
        TextField(
            value = state.currentIpAddress?:"",
            onValueChange = { viewModel.onEvent(SettingEvent.ipAddressChange(it)) }
        )

        Text(
            text = stringResource(id = R.string.setting_screen_port_hint),
            style = MaterialTheme.typography.titleMedium
        )
        TextField(
            value = state.currentPort.toString(),
            onValueChange = { viewModel.onEvent(SettingEvent.portChange(it)) }
        )

        Text(
            text = stringResource(id = R.string.setting_screen_timeout_hint),
            style = MaterialTheme.typography.titleMedium
        )
        TextField(
            value = state.currentTimeOut.toString(),
            onValueChange = { viewModel.onEvent(SettingEvent.timeOutChange(it)) }
        )

        Text(
            text = stringResource(id = R.string.setting_screen_resolution_hint),
            style = MaterialTheme.typography.titleMedium
        )
        ExposedDropdownMenuBox(
            expanded = state.resolutionMenuExpanded,
            onExpandedChange = {
                viewModel.onEvent(SettingEvent.ResolutionExpandedChange)
            }) {
            TextField(
                readOnly = true,
                value = state.selectedResolution.toString(),
                onValueChange = {},
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.resolutionMenuExpanded)
                },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = state.resolutionMenuExpanded,
                onDismissRequest = { viewModel.onEvent(SettingEvent.ResolutionExpandedDismissed) }
            ) {
                resolutionList.forEach { reso ->
                    DropdownMenuItem(
                        text = { Text(text = reso.toString()) },
                        onClick = { viewModel.onEvent(SettingEvent.ResolutionChosen(reso)) }
                    )
                }
            }
        }

        Text(
            text = stringResource(id = R.string.setting_screen_bitrate_hint),
            style = MaterialTheme.typography.titleMedium
        )
        ExposedDropdownMenuBox(
            expanded = state.bitrateMenuExpanded,
            onExpandedChange = {
                viewModel.onEvent(SettingEvent.BitRateExpandedChange)
            }) {
            TextField(
                readOnly = true,
                value = state.selectedBitRate.toString(),
                onValueChange = {},
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.bitrateMenuExpanded)
                },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = state.bitrateMenuExpanded,
                onDismissRequest = { viewModel.onEvent(SettingEvent.BitRateExpandedDismissed) }
            ) {
                bitrateList.forEach { bitrate ->
                    DropdownMenuItem(
                        text = { Text(text = bitrate.toString()) },
                        onClick = { viewModel.onEvent(SettingEvent.BitRateChosen(bitrate)) }
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.setting_screen_no_control_hint))
            Switch(
                checked = state.noControlChecked,
                onCheckedChange = {viewModel.onEvent(SettingEvent.noControlCheckChange(it))}
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.setting_screen_nav_bar_hint))
            Switch(
                checked = state.navBarChecked,
                onCheckedChange = {viewModel.onEvent(SettingEvent.navBarCheckChange(it))}
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ){
            Button(onClick = {
                if(viewModel.startControl()==true){
                    navController.navigate(
                        NavRoute.ScreenCopyScreen.route+
                                "/${state.currentIpAddress}/${state.currentPort}/${state.selectedResolution.width}/${state.selectedResolution.height}/${state.selectedBitRate.value}"
                    )
                }
            }) {
                Text(
                    text = stringResource(id = R.string.setting_screen_start),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
@Preview
fun SettingScreenPreview() {
    SettingScreen(navController = rememberNavController())
}