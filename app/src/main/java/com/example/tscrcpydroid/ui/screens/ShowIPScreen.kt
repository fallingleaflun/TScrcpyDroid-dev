package com.example.tscrcpydroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.tscrcpydroid.R
import com.example.tscrcpydroid.viewmodels.ShowIPScreenViewModel

@Composable
fun ShowIPScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ShowIPScreenViewModel = hiltViewModel()
) {
    var currentIP:String? by remember {
        mutableStateOf("")
    }
    LaunchedEffect(key1 = Unit){
        viewModel.currentIpAddress.collect {
            currentIP = it
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(
            id = R.string.showip_screen_hint),
            style = MaterialTheme.typography.titleMedium
        )
        if(currentIP.isNullOrBlank()) {
            Text(
                text = stringResource(id = R.string.showip_screen_empty_hint),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.LightGray
            )
        }
        else{
            Text(
                text = currentIP!!,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}