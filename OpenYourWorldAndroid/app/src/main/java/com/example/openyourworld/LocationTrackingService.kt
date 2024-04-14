/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.openyourworld

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.TimeUnit

// Implement a background service for continuous location tracking.
// Use the Android Location API to obtain location updates.
// based on this example https://github.com/android/platform-samples/tree/main/samples/location/src/main/java/com/example/platform/location/bglocationaccess
class LocationTrackingService(context: Context, param: WorkerParameters) :
    CoroutineWorker(context, param) {
    private val TAG: String = LocationTrackingService::class.java.getSimpleName();

    companion object {
        // unique name for the work
        val workName = "LocationTrackingService"
        private const val TAG = "BackgroundLocationWork"
    }
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun doWork(): Result {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }
        locationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token,
        ).addOnSuccessListener { location ->
            location?.let {
                Log.d(
                    TAG,
                    "Current Location = [lat : ${location.latitude}, lng : ${location.longitude}]",
                )
            }
        }
        return Result.success()
    }

    //name = "Location - Background Location updates";
    //description = "This Sample demonstrate how to access location and get location updates when app is in background";
    //documentation = "https://developer.android.com/training/location/background";
    @Composable
    fun BgLocationAccessScreen() {
        // Request for foreground permissions first
        PermissionBox(
            permissions = listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            requiredPermissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            onGranted = {
                // From Android 10 onwards request for background permission only after fine or coarse is granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PermissionBox(permissions = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        BackgroundLocationControls()
                    }
                } else {
                    BackgroundLocationControls()
                }
            },
        )
    }
    @Composable
    private fun BackgroundLocationControls() {
        val context = LocalContext.current
        val workManager = WorkManager.getInstance(context)

        // Component UI state holder
        data class ControlsState(val text: String, val action: String, val onClick: () -> Unit)

        // Observe the worker state to show enable/disable UI
        val workerState by workManager.getWorkInfosForUniqueWorkLiveData(LocationTrackingService.workName)
            .observeAsState()

        val controlsState = remember(workerState) {
            // Find if there is any enqueued or running worker and provide UI state
            val enqueued = workerState?.find { !it.state.isFinished } != null
            if (enqueued) {
                ControlsState(
                    text = "Check the logcat for location updates every 15 min",
                    action = "Disable updates",
                    onClick = {
                        workManager.cancelUniqueWork(LocationTrackingService.workName)
                    },
                )
            } else {
                ControlsState(
                    text = "Enable location updates and bring the app in the background.",
                    action = "Enable updates",
                    onClick = {
                        // Schedule a periodic worker to check for location every 15 min
                        workManager.enqueueUniquePeriodicWork(
                            LocationTrackingService.workName,
                            ExistingPeriodicWorkPolicy.KEEP,
                            PeriodicWorkRequestBuilder<LocationTrackingService>(
                                15,
                                TimeUnit.MINUTES,
                            ).build(),
                        )
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = controlsState.text)
            Button(onClick = controlsState.onClick) {
                Text(text = controlsState.action)
            }
        }
    }
}