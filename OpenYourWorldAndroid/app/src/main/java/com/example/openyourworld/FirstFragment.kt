package com.example.openyourworld

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import com.example.openyourworld.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private var TAG: String = javaClass.simpleName
    private val PACKAGE_NAME = "com.example.openyourworld"
    private var _binding: FragmentFirstBinding? = null
    // A reference to the service used to get location updates.
    private var mService: LocationTrackingService? = null;
    private var workManager: WorkManager? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        workManager = WorkManager.getInstance(requireContext().applicationContext)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e(TAG, "onViewCreated")

        // Find the ComposeView and set the content
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            // Get location updates here
            Log.d(TAG, "Getting location updates started")
            //todo: add log of current location

            // Todo: check how to create LocationTrackingService constructor to create mService
            //mService = LocationTrackingService(requireContext().applicationContext, ????)
            val serviceName = LocationTrackingService::class.java.name
            val componentName = ComponentName(PACKAGE_NAME, serviceName)
            val oneTimeWorkRequest = buildOneTimeWorkRemoteWorkRequest(
                componentName,
                LocationTrackingService::class.java
            )
            workManager?.enqueue(oneTimeWorkRequest)
            // todo: continue develop using next example: https://github.com/android/architecture-components-samples/blob/main/WorkManagerMultiprocessSample/app/src/main/java/com/example/background/multiprocess/MainActivity.kt
            //find the way to call mService?.BgLocationAccessScreen()
            Log.d(TAG, "Getting location updates ended")
        }

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildOneTimeWorkRemoteWorkRequest(
        componentName: ComponentName
        , listenableWorkerClass: Class<out ListenableWorker>
    ): OneTimeWorkRequest {

        // ARGUMENT_PACKAGE_NAME and ARGUMENT_CLASS_NAME are used to determine the service
        // that a Worker binds to. By specifying these parameters, we can designate the process a
        // Worker runs in.
        val data: Data = Data.Builder()
            .putString(ARGUMENT_PACKAGE_NAME, componentName.packageName)
            .putString(ARGUMENT_CLASS_NAME, componentName.className)
            .build()

        return OneTimeWorkRequest.Builder(listenableWorkerClass)
            .setInputData(data)
            .build()
    }
}