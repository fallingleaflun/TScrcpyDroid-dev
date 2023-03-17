package com.example.tscrcpydroid.ui.screens

import android.view.SurfaceView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.tscrcpydroid.viewmodels.ScreenCopyScreenViewModel

@Composable
fun ScreenCopyScreen(
    navController: NavController,
    modifier: Modifier=Modifier,
    viewModel: ScreenCopyScreenViewModel = hiltViewModel()
){
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        AndroidView(
            factory = {
                viewModel.surfaceViewState.value
            }
        )
    }

}