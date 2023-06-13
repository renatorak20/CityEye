package com.nullpointerexception.cityeye.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.nullpointerexception.cityeye.LoginActivity
import com.nullpointerexception.cityeye.R
import com.nullpointerexception.cityeye.data.SharedViewModel
import com.nullpointerexception.cityeye.databinding.FragmentCaptureBinding
import com.nullpointerexception.cityeye.util.CameraUtil
import com.nullpointerexception.cityeye.util.OtherUtilities
import com.nullpointerexception.cityeye.util.PermissionUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException


class CaptureFragment : Fragment() {

    private lateinit var binding: FragmentCaptureBinding
    private lateinit var viewModel: SharedViewModel
    private val REQUEST_IMAGE_CAPTURE = 1
    private var dialog: AlertDialog? = null
    private lateinit var myMap: SupportMapFragment
    private var shouldCheckLocation = true


    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->

        val uiSettings = googleMap.uiSettings
        uiSettings.isMyLocationButtonEnabled = true
        uiSettings.isCompassEnabled = true

        googleMap.isMyLocationEnabled = true

        googleMap.clear()

        val myLocation =
            LatLng(
                viewModel.myCoordinates.value!!.latitude,
                viewModel.myCoordinates.value!!.longitude
            )
        googleMap.clear()

        if (!viewModel.hasZoomedIn) {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    myLocation, 17.0f
                )
            )
            viewModel.hasZoomedIn = true
        }

        viewModel.getCoordinates().observe(viewLifecycleOwner) {
            if (!viewModel.areItemsLoaded) {
                viewModel.getLatestMapItems()
            }
        }
        viewModel.getMapItems().observe(viewLifecycleOwner) {
            makeHeatmap(googleMap)
            binding.indicator.hide()

            for (item in viewModel.getMapItems().value!!) {

                if (item.type == "solarnaKlupa") {

                    val bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_solar)

                    item.lat?.let { it1 -> LatLng(it1, item.lng!!) }?.let { it2 ->
                        MarkerOptions()
                            .position(it2)
                            .icon(bitmapDescriptor)
                    }?.let { it3 -> googleMap.addMarker(it3) }

                } else if (item.type == "autoPunjac") {

                    val bitmapDescriptor =
                        BitmapDescriptorFactory.fromResource(R.drawable.ic_car_charger)

                    item.lat?.let { it1 -> item.lng?.let { it2 -> LatLng(it1, it2) } }?.let { it2 ->
                        MarkerOptions()
                            .position(it2)
                            .icon(bitmapDescriptor)
                    }?.let { it3 -> googleMap.addMarker(it3) }
                }
            }

            viewModel.areItemsLoaded = true

        }

        binding.capture.visibility = View.VISIBLE
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            100
        )
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        binding = FragmentCaptureBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Firebase.auth.currentUser == null) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        viewModel.getMyFusedLocationNow(requireActivity())
        viewModel.getlatestSupportedCities()

        viewModel.getMyCoordinates().observe(viewLifecycleOwner) {
            if (viewModel.getSupportedCities().value?.cities?.isNotEmpty() == true) {

                viewModel.checkIfInSupportedCity(
                    requireContext(), viewModel.myCoordinates.value!!,
                    viewModel.getSupportedCities().value!!
                )

                viewModel.getInSupportedCity().observe(viewLifecycleOwner) {
                    if (it) {
                        myMap =
                            (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
                        myMap.getMapAsync(callback)
                    } else {
                        shouldCheckLocation = false
                        dialog?.dismiss()
                        dialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.notInSupportedCityTitle))
                            .setMessage(getString(R.string.notInSupportedCityDescription))
                            .setPositiveButton(getString(R.string.retry)) { dialog, which ->
                                shouldCheckLocation = true
                                viewModel.getlatestSupportedCities()
                            }
                            .show()
                    }
                }
            }
        }

        viewModel.loadProblemCoordinates()
        viewModel.getMyFusedLocationNow(requireActivity())



        binding.indicator.show()

        binding.capture.setOnClickListener {
            if (PermissionUtils.requestPermission(requireActivity())) {
                startCamera()
            }
        }

    }

    override fun onStart() {
        super.onStart()
        when {
            PermissionUtils.isAccessFineLocationGranted(this.activity as AppCompatActivity) -> {
                when {
                    PermissionUtils.isLocationEnabled(this.activity as AppCompatActivity) -> {
                        setUpLocationListener(this.activity as AppCompatActivity)
                    }

                    else -> {
                        PermissionUtils.showGPSNotEnabledDialog(this.activity as AppCompatActivity)
                    }
                }
            }

            else -> {
                PermissionUtils.requestAccessFineLocationPermission(
                    this.activity as AppCompatActivity,
                    10
                )
            }
        }
    }

    private fun startCamera() {
        val photoFile: File? = try {
            CameraUtil.createImageFile(requireContext())
        } catch (ex: IOException) {
            null
        }

        photoFile?.also {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "com.nullpointerexception.cityeye.fileprovider",
                it
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (viewModel.myCoordinates.value != null) {
                OtherUtilities().startProblemPreviewActivity(
                    CameraUtil.retrieveImage(),
                    requireActivity(),
                    viewModel.myCoordinates.value!!.latitude,
                    viewModel.myCoordinates.value!!.longitude
                )
            }
        }

    }


    @SuppressLint("MissingPermission")
    private fun setUpLocationListener(activity: AppCompatActivity) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        val locationRequest = LocationRequest().setInterval(8000).setFastestInterval(8000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        if (ActivityCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                activity.applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val snackbar = Snackbar.make(
                requireView(),
                "Location permission is required",
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction("Settings") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            GlobalScope.launch {
                delay(2000)
                snackbar.anchorView = requireActivity().findViewById(R.id.nav_view)
                snackbar.show()
            }

        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    if (shouldCheckLocation) {
                        for (location in locationResult.locations) {
                            viewModel.setMyCoordinates(
                                LatLng(
                                    location.latitude,
                                    location.longitude
                                )
                            )

                        }
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun makeHeatmap(googleMap: GoogleMap) {
        if (viewModel.getCoordinates().value!!.isNotEmpty()) {
            val heatmapProvider = HeatmapTileProvider.Builder()
                .data(viewModel.getCoordinates().value)
                .radius(15)
                .build()

            val tileOverlayOptions =
                TileOverlayOptions().tileProvider(heatmapProvider).fadeIn(false)
            googleMap.addTileOverlay(tileOverlayOptions)
        }
    }
}