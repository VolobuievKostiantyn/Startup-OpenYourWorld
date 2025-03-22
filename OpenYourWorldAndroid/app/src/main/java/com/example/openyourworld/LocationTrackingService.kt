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
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import android.util.Log
import android.widget.Toast
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
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteListenableWorker
import com.example.openyourworld.LocationTrackingService.GlobalVariables.dbHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.TimeUnit

// Implement a background service for continuous location tracking.
// Use the Android Location API to obtain location updates.
// based on this example https://github.com/android/platform-samples/tree/main/samples/location/src/main/java/com/example/platform/location/bglocationaccess
class LocationTrackingService(context: Context, param: WorkerParameters) : Worker(context, param) {
    private val TAG: String = LocationTrackingService::class.java.getSimpleName();

    // unique name for the work
    private val workName = "LocationTrackingService"
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    object GlobalVariables {
        var latitude: Double? = null
        var longitude: Double? = null
        lateinit var dbHelper: LocationDatabaseHelper
    }

    override fun doWork(): Result {
        Log.i(TAG,"doWork start")
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG,"checkSelfPermission: location permissions denied")
            return Result.failure()
        }
        locationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token,
        ).addOnSuccessListener { location ->
            if (location != null) {
                // Use the location object
                GlobalVariables.latitude = location.latitude
                GlobalVariables.longitude = location.longitude
                Toast.makeText(applicationContext, "Current location: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
                // Todo: add current position to data base and then
                dbHelper = LocationDatabaseHelper(applicationContext)

                // Insert a sample location
                val insertedId = dbHelper.insertLocation(location.latitude, location.longitude) // Example: San Francisco
                Toast.makeText(applicationContext, "Location saved with ID: $insertedId", Toast.LENGTH_SHORT).show()

                // Fetch all locations
                val locations = dbHelper.getAllLocations()
                locations.forEach {
                    println("ID: ${it.id}, Lat: ${it.latitude}, Lon: ${it.longitude}")
                }
                // Todo: draw current position on map
            } else {
                Toast.makeText(applicationContext, "Location not available. Turn on location", Toast.LENGTH_SHORT).show()
            }
        }

        scheduleWork(applicationContext)

        Log.i(TAG,"doWork end")
        return Result.success()
    }

    //name = "Location - Background Location updates";
    //description = "This Sample demonstrate how to access location and get location updates when app is in background";
    //documentation = "https://developer.android.com/training/location/background";
    @Composable
    fun BgLocationAccessScreen() {
        Log.d(
            TAG,
            "Get Background Location Access screen",
        )
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
                    Log.d(
                        TAG,
                        "Current Android Version is less than Android 10",
                    )
                    PermissionBox(permissions = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        BackgroundLocationControls()
                    }
                } else {
                    Log.d(
                        TAG,
                        "Current Android Version is more than Android 10",
                    )
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
        val workerState by workManager.getWorkInfosForUniqueWorkLiveData(workName)
            .observeAsState()

        val controlsState = remember(workerState) {
            // Find if there is any enqueued or running worker and provide UI state
            val enqueued = workerState?.find { !it.state.isFinished } != null
            if (enqueued) {
                ControlsState(
                    text = "Check the logcat for location updates every 15 min",
                    action = "Disable updates",
                    onClick = {
                        workManager.cancelUniqueWork(workName)
                    },
                )
            } else {
                ControlsState(
                    text = "Enable location updates and bring the app in the background.",
                    action = "Enable updates",
                    onClick = {
                        // Schedule a periodic worker to check for location every 15 min
                        workManager.enqueueUniquePeriodicWork(
                            workName,
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

    // extra static values and methods
    companion object {
        private const val LOCATION_UPDATE_INTERVAL = 5L // todo: modify duration if needed
        private var workManager: WorkManager? = null

        fun scheduleWork(context: Context) {
            val serviceName = LocationTrackingService::class.java.name
            val componentName = ComponentName(PACKAGE_NAME, serviceName)
            val oneTimeWorkRequest = buildOneTimeWorkRemoteWorkRequest(
                componentName,
                LocationTrackingService::class.java
            )
            workManager = WorkManager.getInstance(context)
            workManager?.enqueue(oneTimeWorkRequest)
        }

        fun buildOneTimeWorkRemoteWorkRequest(
            componentName: ComponentName
            , listenableWorkerClass: Class<out ListenableWorker>
        ): OneTimeWorkRequest {
            // ARGUMENT_PACKAGE_NAME and ARGUMENT_CLASS_NAME are used to determine the service
            // that a Worker binds to. By specifying these parameters, we can designate the process a
            // Worker runs in.
            val data: Data = Data.Builder()
                .putString(RemoteListenableWorker.ARGUMENT_PACKAGE_NAME, componentName.packageName)
                .putString(RemoteListenableWorker.ARGUMENT_CLASS_NAME, componentName.className)
                .build()

            return OneTimeWorkRequest.Builder(listenableWorkerClass)
                .setInputData(data)
                .setInitialDelay(LOCATION_UPDATE_INTERVAL, TimeUnit.SECONDS)
                .build()
        }

        // todo: remove if it will be unused
        fun getLastKnownLocation(fusedLocationClient: FusedLocationProviderClient, context: Context) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                if (location != null) {
                    // Use the location object
                    Toast.makeText(
                        context,
                        "Current location: ${location.latitude}, ${location.longitude}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Location not available. Please turn on location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}