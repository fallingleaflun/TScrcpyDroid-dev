package com.example.tscrcpydroid

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tscrcpydroid.ui.screens.MainScreen
import com.example.tscrcpydroid.ui.screens.ScreenCopyScreen
import com.example.tscrcpydroid.ui.screens.SettingScreen
import com.example.tscrcpydroid.ui.screens.ShowIPScreen

sealed class NavRoute(val route:String){
    object MainScreen: NavRoute("main_screen_route")
    object SettingScreen: NavRoute("setting_screen_route")
    object ScreenCopyScreen: NavRoute("screencopy_screen_route")
    object ShowIPScreen: NavRoute("showip_screen_route")
}

@Composable
fun TScrcpyDroidNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.MainScreen.route
    ){
        composable(
            route = NavRoute.MainScreen.route
        ){
            MainScreen(navController = navController)
        }
        composable(
            route = NavRoute.SettingScreen.route
        ){
            SettingScreen(navController = navController)
        }
        composable(
            route = NavRoute.ShowIPScreen.route
        ){
            ShowIPScreen(navController = navController)
        }
        composable(
            route = NavRoute.ScreenCopyScreen.route+"/{targetIP}/{width}/{height}",
            arguments = listOf(
                navArgument("targetIP"){type= NavType.StringType},
                navArgument("width"){type= NavType.IntType},
                navArgument("height"){type= NavType.IntType}
            )
        ){
            ScreenCopyScreen(navController = navController)
        }
    }
}
