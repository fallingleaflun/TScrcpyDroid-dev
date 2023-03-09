package com.example.tscrcpydroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tscrcpydroid.NavRoute
import com.example.tscrcpydroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    modifier: Modifier = Modifier
){
    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
            ) {
            Button(
                onClick = {
                    navController.navigate(
                        NavRoute.SettingScreen.route
                    )
                }
            ) {
                Text(
                    text = stringResource(id = R.string.main_screen_controller),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Button(onClick = {
                navController.navigate(NavRoute.ShowIPScreen.route)
            }) {
                Text(
                    text = stringResource(id = R.string.main_screen_controlled),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
@Preview
fun MainScreenPreview(){
    MainScreen(navController = rememberNavController())
}

