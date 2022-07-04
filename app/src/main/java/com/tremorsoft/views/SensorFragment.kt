/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.views

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.tremorsoft.R
import com.tremorsoft.databinding.FragmentSensorBinding
import com.tremorsoft.utils.APIService
import com.tremorsoft.utils.PatientData
import com.tremorsoft.utils.RecordingData
import com.tremorsoft.utils.SensorSignals
import com.tremorsoft.viewmodels.DataViewModel
import com.xsens.dot.android.sdk.events.XsensDotData
import com.xsens.dot.android.sdk.interfaces.XsensDotDeviceCallback
import com.xsens.dot.android.sdk.interfaces.XsensDotRecordingCallback
import com.xsens.dot.android.sdk.interfaces.XsensDotScannerCallback
import com.xsens.dot.android.sdk.models.FilterProfileInfo
import com.xsens.dot.android.sdk.models.XsensDotDevice
import com.xsens.dot.android.sdk.models.XsensDotRecordingFileInfo
import com.xsens.dot.android.sdk.models.XsensDotRecordingState
import com.xsens.dot.android.sdk.recording.XsensDotRecordingManager
import com.xsens.dot.android.sdk.utils.XsensDotScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


class SensorFragment : BaseFragment(), XsensDotDeviceCallback, XsensDotScannerCallback, XsensDotRecordingCallback {

    private lateinit var dataViewModel: DataViewModel
    private var _binding: FragmentSensorBinding? = null
    private val binding get() = _binding!!

    private var mXsDotScanner: XsensDotScanner? = null
    private var mXsDevice: XsensDotDevice? = null
    private var mManager: XsensDotRecordingManager? = null
    private var mSelectExportedDataIds: ByteArray? = null

    private var checkedSensor: Int? = null
    private var recordId = 0
    private var restId: Int? = null
    private var postureId: Int? = null

    private var sensorsData: MutableList<SensorSignals> = ArrayList()
    private var restData: MutableList<SensorSignals> = ArrayList()
    private var postureData: MutableList<SensorSignals> = ArrayList()
    private var tremorData: MutableList<SensorSignals> = ArrayList()

    private var gyrX: String? = null
    private var gyrY: String? = null
    private var gyrZ: String? = null
    private var accX: String? = null
    private var accY: String? = null
    private var accZ: String? = null
    private var classification: String? = null

    var serverResponse: String? = null
    var userType = "user"

    private var patientDataJSON: JSONObject = JSONObject()
    private var dataArray: JSONArray = JSONArray()
    private var tremorsoftData: JSONArray? = JSONArray()

    private var restRecordStatus = false
    private var postureRecordStatus = false
    private var recordingStatus = false
    private var isDataExported = false

    private var patientData: PatientData? = null
    private var recordingData: RecordingData? = null

    private var validForm: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel = ViewModelProvider(this)[DataViewModel::class.java]

        val viewModel: DataViewModel by activityViewModels()

        viewModel.recordingData.observe(viewLifecycleOwner, Observer { data ->
            if (data == null) {
                return@Observer
            }
            if (data.isRecordingDataCompleted) {
                validForm = true
                recordingData = data
            }

        })

        viewModel.patientData.observe(viewLifecycleOwner, Observer { data ->
            if (data == null) {
                return@Observer
            }
            if (data.isPatientDataCompleted) {
                patientData = data
            }
        })

        with(binding) {
            setProgressBar(progressBar)

            cvSensors.isVisible = true
            cvDeviceInfo.isVisible = false

            cvInstructions.isVisible = false
            cvPosition.isVisible = false
            cvFigure.isVisible = false
            cvButtons.isVisible = false

            rbXsensDOT.isEnabled = false

            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.BLUETOOTH_SCAN
                ) -> rbXsensDOT.isEnabled = true
                else -> requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            }

            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) -> rbXsensDOT.isEnabled = true
                else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) -> rbXsensDOT.isEnabled = true
                else -> requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }

            rgSensor.setOnCheckedChangeListener { _, sensor ->
                checkedSensor = sensor

                when (sensor) {
                    R.id.rbSmartphone -> {
                        endXsConnection()
                        cvDeviceInfo.isVisible = false
                        btnAction.isEnabled = true
                        switchConnect.isChecked = false
                    }
                    R.id.rbXsensDOT -> {
                        cvDeviceInfo.isVisible = true
                        btnAction.isEnabled = false
                        initXsScanner()
                        mXsDotScanner?.startScan()
                        showProgressBar()
                        tvState.text = getString(R.string.scanning)
                    }
                }
            }

            switchConnect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    hideProgressBar()
                    mXsDotScanner?.stopScan()
                    mXsDevice?.connect()
                } else {
                    mXsDevice?.disconnect()
                }
            }

            btnAction.setOnClickListener {
                if (checkedSensor == R.id.rbSmartphone) {
                    findNavController().navigate(R.id.action_nav_sensor_to_nav_recording)
                } else if (checkedSensor == R.id.rbXsensDOT) {
                    if (mXsDevice!!.isInitDone) {
                        runBlocking { // this: CoroutineScope
                            launch { // launch a new coroutine and continue
                                delay(500L)
                                mManager = XsensDotRecordingManager(context!!, mXsDevice!!, this@SensorFragment)
                                mManager?.enableDataRecordingNotification()
                                setInstructions("default_configuration", 0f)
                            }
                        }
                    }
                }
            }

            rgPosition.setOnCheckedChangeListener { _, pos ->
                btnAction1.isEnabled = true
                when (pos) {
                    R.id.rb_rest -> {
                        title.text = getString(R.string.rest)
                        tvInstructions.text = getString(R.string.rest_instructions)
                        cvFigure.isVisible = true
                        cvButtons.isVisible = true
                        positionFig.setImageResource(R.drawable.rest)
                        rbPosture.isEnabled = false
                        restRecordStatus = false
                    }
                    R.id.rb_posture -> {
                        title.text = getString(R.string.posture)
                        tvInstructions.text = getString(R.string.posture_instructions)
                        cvFigure.isVisible = true
                        cvButtons.isVisible = true
                        positionFig.setImageResource(R.drawable.posture)
                        rbRest.isEnabled = false
                        postureRecordStatus = false
                    }
                }
            }

            btnAction1.setOnClickListener {
                recordingStatus = false
                sensorsData.clear()
                tvInstructions.textAlignment = View.TEXT_ALIGNMENT_GRAVITY

                when (it.tag) {
                    "start" -> {
                        recordingStatus = true
                        recordId += 1
                        mManager?.startTimedRecording(15)
                        showProgressBar()
                        btnAction1.tag = "cancel"
                        btnAction1.text = getString(R.string.cancel)
                    }
                    "cancel" -> {
                        hideProgressBar()
                        mManager?.stopRecording()
                        when {
                            rbRest.isChecked -> {
                                rgPosition.clearCheck()
                                if (!checkPosture.isVisible) {
                                    rbPosture.isEnabled = true
                                }
                            }
                            rbPosture.isChecked -> {
                                rgPosition.clearCheck()
                                if (!checkRest.isVisible) {
                                    rbRest.isEnabled = true
                                }
                            }
                        }

                        btnAction1.text = getString(R.string.start)
                        btnAction1.tag = "start"
                        tvInstructions.text = getString(R.string.cancel_instructions)
                        cvFigure.isVisible = false
                        cvButtons.isVisible = false
                    }
                    "restart" -> {
                        recordingStatus = true
                        recordId += 1
                        mManager?.startTimedRecording(15)
                        showProgressBar()
                        btnAction1.tag = "cancel"
                        btnAction1.text = getString(R.string.cancel)
                        btnAction2.isEnabled = false
                    }
                    "reset" -> {
                        endXsConnection()
                        findNavController().navigate(R.id.action_nav_sensor_self)
                    }
                }
            }

            btnAction2.setOnClickListener {
                when (it.tag) {
                    "confirm" -> {
                        btnAction1.text = getString(R.string.start)
                        btnAction1.tag = "start"
                        btnAction2.isEnabled = false
                        tvInstructions.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
                        title.text = getString(R.string.record_save)

                        if (rbRest.isChecked) {
                            checkRest.visibility = View.VISIBLE
                            restId = recordId
                            rgPosition.clearCheck()
                            rbRest.isEnabled = false
                            rbPosture.isEnabled = true
                            tvInstructions.text = getString(R.string.record_instructions)
                            restRecordStatus = true
                        } else if (rbPosture.isChecked) {
                            checkPosture.visibility = View.VISIBLE
                            postureId = recordId
                            rgPosition.clearCheck()
                            rbRest.isEnabled = true
                            rbPosture.isEnabled = false
                            tvInstructions.text = getString(R.string.record_instructions)
                            postureRecordStatus = true
                        }
                        btnAction1.isEnabled = false
                        if (restRecordStatus and postureRecordStatus) {
                            title.text = getString(R.string.completed)
                            rbRest.isEnabled = false
                            rbPosture.isEnabled = false
                            btnAction1.isEnabled = true
                            btnAction1.text = getString(R.string.reset)
                            btnAction1.tag = "reset"
                            btnAction2.isEnabled = true
                            btnAction2.text = getString(R.string.upload)
                            btnAction2.tag = "upload"
                            tvInstructions.text = getString(R.string.bothPos_recorded)
                            cvPosition.isVisible = false
                            cvFigure.isVisible = false
                        }
                    }
                    "upload" -> {
                        if (isDataExported) {
                            setInstructions("upload_data", 0f)
                            uploadData()
                        } else {
                            mManager?.enableDataRecordingNotification()
                        }
                    }
                    "send" -> {
                        viewModel.recordingDataChanged(dataArray, classification)
                        if (validForm) {
                            mManager?.clear()
                            submitData()
                        }
                    }
                    "new" -> {
                        viewModel.initSetup()
                        findNavController().navigate(R.id.action_nav_sensor_to_nav_home)
                    }
                }
            }
            JUSTIFICATION_MODE_INTER_WORD.also { tvInstructions.justificationMode = it }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            binding.rbXsensDOT.isEnabled = isGranted
        }


    private fun initXsScanner() {
        mXsDotScanner = XsensDotScanner(context, this)
        mXsDotScanner?.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
    }

    private fun endXsConnection() {
        hideProgressBar()
        if (mXsDotScanner != null) mXsDotScanner?.stopScan()
        if (mXsDevice != null) mXsDevice?.disconnect()
        if (mManager != null) mManager?.clear()
    }

    private fun setInstructions(task: String, time: Float) {
        with(binding) {
            when (task) {
                "default_configuration" -> {
                    btnAction1.tag = "start"
                    btnAction1.text = getString(R.string.start)
                    btnAction1.isEnabled = false

                    btnAction2.tag = "confirm"
                    btnAction2.text = getString(R.string.confirm)
                    btnAction2.isEnabled = false

                    btnAction.isEnabled = false
                    btnAction.text = getString(R.string.confirm)

                    cvSensors.isVisible = false
                    cvXsensDot.isVisible = false

                    cvInstructions.isVisible = true
                    cvPosition.isVisible = true
                    cvFigure.isVisible = false
                    cvButtons.isVisible = false

                    setInstructions("initialize_xsens", 0f)
                }

                "initialize_xsens" -> {
                    showProgressBar()
                    binding.title.text = getString(R.string.initializing)
                    binding.rbRest.isEnabled = false
                    binding.rbPosture.isEnabled = false
                    binding.tvInstructions.text = getString(R.string.initializing_xsens)
                }

                "initialize_record" -> {
                    hideProgressBar()
                    title.text = getString(R.string.initial_title)
                    rbRest.isEnabled = true
                    rbPosture.isEnabled = true
                    tvInstructions.text = getString(R.string.initial_instructions)
                }

                "recording_tremor" -> {
                    tvInstructions.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    tvInstructions.text = getString(R.string.time_instructions, time)
                }

                "position_recorded" -> {
                    hideProgressBar()
                    binding.title.text = getString(R.string.position_recorded)
                    binding.btnAction1.text = getString(R.string.restart)
                    binding.btnAction1.tag = "restart"
                    binding.tvInstructions.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
                    binding.tvInstructions.text = getString(R.string.recordDone_instructions)
                    binding.btnAction2.isEnabled = true
                }

                "export_data" -> {
                    title.text = getString(R.string.exporting_data)
                    tvInstructions.text = getString(R.string.instruction_export)
                    showProgressBar()
                    btnAction1.isEnabled = false
                    btnAction2.isEnabled = false
                }

                "upload_data" -> {
                    tvInstructions.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    title.text = getString(R.string.uploading)
                    tvInstructions.text = getString(R.string.wait_analyzing)
                    btnAction1.isEnabled = false
                    btnAction2.isEnabled = false
                }

                "show_result" -> {
                    hideProgressBar()
                    btnAction1.isEnabled = true
                    btnAction2.isEnabled = true

                    title.text = getString(R.string.result)

                    if (userType == "user") {
                        btnAction2.text = getString(R.string.send)
                        btnAction2.tag = "send"
                    } else if (userType == "guest") {
                        btnAction2.isEnabled = false
                    }

                    when {
                        serverResponse?.contains("PD")!! -> {
                            tvInstructions.text = getString(R.string.pd_result)
                            classification = "PD"
                        }
                        serverResponse?.contains("ET")!! -> {
                            tvInstructions.text = getString(R.string.et_result)
                            classification = "ET"
                        }
                        serverResponse?.contains("HS")!! -> {
                            tvInstructions.text = getString(R.string.hs_result)
                            classification = "HS"
                        }

                    }
                }
                "assessment_finalized" -> {
                    btnAction1.text = getString(R.string.reset)
                    btnAction1.tag = "reset"
                    btnAction2.text = getString(R.string.New)
                    btnAction2.tag = "new"
                    tvInstructions.text = getString(R.string.assessment_finalized)
                    title.text = getString(R.string.submitted)
                }
            }
        }
    }

    private fun uploadData() {

        with(binding) {
            tvInstructions.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            APIService.service
                .uploadData(dataArray) //send jsonObj
                .enqueue(object : Callback<ResponseBody?> {

                    override fun onFailure(call: Call<ResponseBody?>?, t: Throwable?) {
                        println("---T :: POST Throwable EXCEPTION:: " + t?.message)
                        hideProgressBar()
                        btnAction1.isEnabled = true
                        btnAction2.isEnabled = true
                        Toast.makeText(context, "Upload failed.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call<ResponseBody?>?, response: Response<ResponseBody?>?) {
                        title.text = getString(R.string.analyzing)
                        try {
                            serverResponse = response?.body()?.string()
                            serverResponse = serverResponse?.replace("\"", "")
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        hideProgressBar()
                        btnAction1.isEnabled = true
                        btnAction2.isEnabled = true
                        title.text = getString(R.string.result)
                        when {
                            serverResponse?.contains("PD")!! -> {
                                tvInstructions.text = getString(R.string.pd_result)
                                classification = "PD"
                            }
                            serverResponse?.contains("ET")!! -> {
                                tvInstructions.text = getString(R.string.et_result)
                                classification = "ET"
                            }
                            serverResponse?.contains("HS")!! -> {
                                tvInstructions.text = getString(R.string.hs_result)
                                classification = "HS"
                            }
                        }

                        if (userType == "user") {
                            btnAction2.text = getString(R.string.send)
                            btnAction2.tag = "send"
                        } else if (userType == "guest") {
                            btnAction2.isEnabled = false
                        }
                    }
                })
        }
    }

    private fun submitData() {

        patientDataJSON.put("patient_ID", patientData?.patientID)
        patientDataJSON.put("Age", patientData?.age)
        patientDataJSON.put("Gender", patientData?.gender)
        patientDataJSON.put("Family_History", patientData?.familyHistory)
        patientDataJSON.put("Classification", recordingData?.classification)
        patientDataJSON.put("Diagnosis", patientData?.diagnosis)
        patientDataJSON.put("Method", patientData?.method)
        patientDataJSON.put("Onset_Age", patientData?.onsetAge)
        patientDataJSON.put("Bilaterality", patientData?.bilaterality)
        patientDataJSON.put("Treatments", patientData?.treatments)
        patientDataJSON.put("Medication", patientData?.medication)
        patientDataJSON.put("Concomitances", patientData?.concomitances)
        patientDataJSON.put("Comorbidities", patientData?.comorbidities)

        tremorsoftData = recordingData?.arrayData

        tremorsoftData?.put(patientDataJSON)

        tremorsoftData?.let {
            APIService.service
                .submitData(it) //send jsonObj
                .enqueue(object : Callback<ResponseBody?> {
                    override fun onFailure(call: Call<ResponseBody?>?, t: Throwable?) {
                        println("---T :: POST Throwable EXCEPTION:: " + t?.message)
                        Toast.makeText(context, "Upload failed.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call<ResponseBody?>?, response: Response<ResponseBody?>?) {
                        try {
                            serverResponse = response?.body()?.string()
                            serverResponse = serverResponse?.replace("\"", "")
                            Toast.makeText(context, serverResponse, Toast.LENGTH_SHORT).show()
                            setInstructions("assessment_finalized", 0f)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        endXsConnection()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        endXsConnection()
    }

    override fun onXsensDotConnectionChanged(address: String?, state: Int) {
        Log.i(TAG, "onXsensDotConnectionChanged() - address = $address, state = $state")
        when (state) {
            XsensDotDevice.CONN_STATE_DISCONNECTED -> synchronized(this) {
                binding.tvState.text = getString(R.string.disconnected)
            }

            XsensDotDevice.CONN_STATE_CONNECTING -> {
                binding.tvState.text = getString(R.string.connecting)
            }

            XsensDotDevice.CONN_STATE_CONNECTED -> {
                binding.tvState.text = getString(R.string.connected)
            }

            XsensDotDevice.CONN_STATE_RECONNECTING -> {
                binding.tvState.text = getString(R.string.reconnecting)
            }
        }

    }

    override fun onXsensDotServicesDiscovered(address: String, status: Int) {
        Log.i(TAG, "onXsensDotServicesDiscovered() - address = $address, status = $status")

    }

    override fun onXsensDotFirmwareVersionRead(address: String, version: String) {
        Log.i(TAG, "onXsensDotFirmwareVersionRead() - address = $address, version = $version")
    }

    override fun onXsensDotTagChanged(address: String, tag: String) {
        // This callback function will be triggered in the connection precess.
        Log.i(TAG, "onXsensDotTagChanged() - address = $address, tag = $tag")

    }

    override fun onXsensDotBatteryChanged(address: String, status: Int, percentage: Int) {
        // This callback function will be triggered in the connection precess.
        Log.i(
            TAG, "onXsensDotBatteryChanged() - address = $address, status = $status, percentage = $percentage"
        )

        if (status != -1 && percentage != -1) {
            binding.tvBattery.text = "$percentage% "
        }
    }

    override fun onXsensDotDataChanged(address: String, data: XsensDotData?) {
        Log.i(TAG, "onXsensDotDataChanged() - address = $address")
    }

    override fun onXsensDotInitDone(address: String) {
        Log.i(TAG, "onXsensDotInitDone() - address = $address")
        if (activity != null) {
            activity!!.runOnUiThread {
                if (mXsDevice?.tag != null) binding.tvSensor.text = mXsDevice?.tag
                binding.btnAction.isEnabled = true
            }
        }
    }

    override fun onXsensDotButtonClicked(address: String, timestamp: Long) {
        Log.i(TAG, "onXsensDotButtonClicked() - address = $address, timestamp = $timestamp")
    }

    override fun onXsensDotPowerSavingTriggered(address: String) {
        Log.i(TAG, "onXsensDotPowerSavingTriggered() - address = $address")
    }

    override fun onReadRemoteRssi(address: String, rssi: Int) {
        Log.i(TAG, "onReadRemoteRssi() - address = $address, rssi = $rssi")
    }

    override fun onXsensDotOutputRateUpdate(address: String, outputRate: Int) {
        Log.i(TAG, "onXsensDotOutputRateUpdate() - address = $address, outputRate = $outputRate")
        if (outputRate != 120) {
            mXsDevice?.setOutputRate(120)
        }
    }

    override fun onXsensDotFilterProfileUpdate(address: String, filterProfileIndex: Int) {
        Log.i(
            TAG, "onXsensDotFilterProfileUpdate() - address = $address, filterProfileIndex = $filterProfileIndex"
        )
        if (filterProfileIndex != 0) {
            mXsDevice?.setFilterProfile(0)
        }
    }

    override fun onXsensDotGetFilterProfileInfo(address: String, filterProfileInfoList: ArrayList<FilterProfileInfo?>) {
        Log.i(
            TAG,
            "onXsensDotGetFilterProfileInfo() - address = " + address + ", size = " + filterProfileInfoList.size
        )
    }

    override fun onSyncStatusUpdate(address: String, isSynced: Boolean) {
        Log.i(TAG, "onSyncStatusUpdate() - address = $address, isSynced = $isSynced")
    }

    override fun onXsensDotScanned(device: BluetoothDevice?, rssi: Int) {
        Log.i(TAG, "onXsensDotScanned() - Name: ${device?.name}, Address: ${device?.address}")
        hideProgressBar()
        mXsDevice = XsensDotDevice(context, device, this)

        if (device?.name != null) {
            binding.tvSensor.text = device.name
            binding.tvMacAddress.text = "(${device.address})"
            binding.tvState.text = getString(R.string.disconnected)
        }
    }

    override fun onXsensDotRecordingNotification(address: String?, isEnabled: Boolean) {
        Log.i(TAG, "onXsensDotRecordingNotification() - address = $address, isEnabled = $isEnabled")

        if (isEnabled) {
            mManager?.requestFlashInfo()
        }
    }

    override fun onXsensDotEraseDone(address: String?, isSuccess: Boolean) {
        Log.i(TAG, "onXsensDotEraseDone() - address = $address, isSuccess = $isSuccess")
        if (activity != null) {
            activity!!.runOnUiThread {
                setInstructions("initialize_record", 0f)
            }
        }
    }

    override fun onXsensDotRequestFlashInfoDone(address: String?, usedFlashSpace: Int, totalFlashSpace: Int) {
        Log.i(
            TAG, "onXsensDotRequestFlashInfoDone() - address = $address, usedFlashSpace = $usedFlashSpace," +
                    "totalFlashSpace = $totalFlashSpace"
        )

        when (binding.btnAction2.tag) {
            "confirm" -> {
                setInstructions("initialize_xsens", 0f)
                mManager?.eraseRecordingData()
            }
            "upload" -> {
                mManager?.requestFileInfo()
            }
        }
    }

    override fun onXsensDotRecordingAck(
        address: String?, recordingId: Int, isSuccess: Boolean,
        recordingState: XsensDotRecordingState?,
    ) {
        Log.i(
            TAG, "onXsensDotRecordingAck() - address = $address, recordingId = $recordingId," +
                    "isSuccess = $isSuccess, recordingState = $recordingState"
        )

        if (recordingId == XsensDotRecordingManager.RECORDING_ID_START_RECORDING) {
            // start recording result, check recordingState, it should be success or fail. ...
            if (recordingState == XsensDotRecordingState.success) {
                if (recordingStatus) {
                    mManager?.requestRecordingState()
                }
            }
        } else if (recordingId == XsensDotRecordingManager.RECORDING_ID_STOP_RECORDING) {
            // stop recording result, check recordingState, it should be success or fail. ...
            if (recordingState == XsensDotRecordingState.success) {
                if (activity != null) {
                    activity!!.runOnUiThread {
                        setInstructions("position_recorded", 0f)
                    }
                }
                mManager?.requestRecordingState()
            }
        }

        if (recordingId == XsensDotRecordingManager.RECORDING_ID_GET_STATE) {
            when (recordingState) {
                XsensDotRecordingState.onErasing -> {

                }
                XsensDotRecordingState.onExportFlashInfo -> {

                }
                XsensDotRecordingState.onRecording -> {
                    mManager?.requestRecordingTime()
                }
                XsensDotRecordingState.onExportRecordingFileInfo -> {

                }
                XsensDotRecordingState.onExportRecordingFileData -> {

                }
                XsensDotRecordingState.idle -> {

                }

                else -> {}
            }
        }

    }

    override fun onXsensDotGetRecordingTime(
        address: String?, startUTCSeconds: Int, totalRecordingSeconds: Int,
        remainingRecordingSeconds: Int,
    ) {
        if (activity != null) {
            activity!!.runOnUiThread {
                setInstructions("recording_tremor", remainingRecordingSeconds.toFloat())
                mManager?.requestRecordingTime()
            }
        }

        if (remainingRecordingSeconds == 0) {
            recordingStatus = false
            mManager?.requestRecordingState()
        }
    }

    override fun onXsensDotRequestFileInfoDone(
        address: String?, list: ArrayList<XsensDotRecordingFileInfo>?,
        isSuccess: Boolean,
    ) {
        Log.i(TAG, "onXsensDotRequestFileInfoDone() - address = $address, list = $list, isSuccess = $isSuccess")

        mSelectExportedDataIds = ByteArray(3)
        mSelectExportedDataIds!![0] = XsensDotRecordingManager.RECORDING_DATA_ID_TIMESTAMP
        mSelectExportedDataIds!![1] = XsensDotRecordingManager.RECORDING_DATA_ID_CALIBRATED_GYR
        mSelectExportedDataIds!![2] = XsensDotRecordingManager.RECORDING_DATA_ID_CALIBRATED_ACC
        mManager?.selectExportedData(mSelectExportedDataIds!!)

        if (list != null) {
            if (activity != null) {
                activity!!.runOnUiThread {
                    setInstructions("export_data", 0f)
                }
            }

            val filteredList = list.filterIndexed { _, fileInfo: XsensDotRecordingFileInfo? ->
                fileInfo?.fileId == restId || fileInfo?.fileId == postureId
            } as ArrayList<XsensDotRecordingFileInfo?>

            mManager?.startExporting(filteredList)
        }
    }

    override fun onXsensDotDataExported(
        address: String?, fileInfo: XsensDotRecordingFileInfo?,
        exportedData: XsensDotData?,
    ) {
        // When the export is in progress, this callback will be called, returning each exported data XsensDotData,
        // corresponding to the selected field Data can be stored through and written to the csv file // E.g
        val sensorValues: MutableList<SensorSignals> = ArrayList()
        val gyr = exportedData?.gyr
        val acc = exportedData?.acc

        gyrX = gyr?.get(0)?.times(0.0174533).toString()
        gyrY = gyr?.get(1)?.times(0.0174533).toString()
        gyrZ = gyr?.get(2)?.times(0.0174533).toString()
        accX = acc?.get(0)?.times(0.1019716)?.minus(1.0).toString()
        accY = acc?.get(1)?.times(0.1019716)?.minus(1.0).toString()
        accZ = acc?.get(2)?.times(0.1019716)?.minus(1.0).toString()

        when (fileInfo?.fileId) {
            restId -> {
                if (restData.size == 0) {
                    sensorValues.add(
                        SensorSignals(
                            "gyrX_R", "gyrY_R", "gyrZ_R",
                            "accX_R", "accY_R", "accZ_R"
                        )
                    )
                }
                sensorValues.add(SensorSignals(gyrX, gyrY, gyrZ, accX, accY, accZ))
                restData.addAll(sensorValues)

            }
            postureId -> {
                if (postureData.size == 0) {
                    sensorValues.add(
                        SensorSignals(
                            "gyrX_P", "gyrY_P", "gyrZ_P",
                            "accX_P", "accY_P", "accZ_P"
                        )
                    )
                }

                sensorValues.add(SensorSignals(gyrX, gyrY, gyrZ, accX, accY, accZ))
                postureData.addAll(sensorValues)
            }
        }
    }

    override fun onXsensDotDataExported(address: String?, fileInfo: XsensDotRecordingFileInfo?) {
        Log.i(TAG, "onXsensDotDataExported() - address = $address, fileInfo = $fileInfo")
    }

    override fun onXsensDotAllDataExported(address: String?) {
        Log.i(TAG, "onXsensDotAllDataExported() - address = $address")
        isDataExported = true

        hideProgressBar()
        tremorData.addAll(restData)
        tremorData.addAll(postureData)

        tremorData.removeIf {
            it.accX == "NaN" || it.gyrX == "NaN" || it.accX == null || it.gyrX == null
        }

        for (signals in tremorData) {
            val signalsJSON = JSONObject()
            signalsJSON.put("gyrX", signals.gyrX)
            signalsJSON.put("gyrY", signals.gyrY)
            signalsJSON.put("gyrZ", signals.gyrZ)
            signalsJSON.put("accX", signals.accX)
            signalsJSON.put("accY", signals.accY)
            signalsJSON.put("accZ", signals.accZ)
            dataArray.put(signalsJSON)
        }
        if (activity != null) {
            activity!!.runOnUiThread {
                setInstructions("upload_data", 0f)
                uploadData()
            }
        }
    }

    override fun onXsensDotStopExportingData(address: String?) {

    }

    companion object {
        private val TAG = SensorFragment::class.java.simpleName
    }
}