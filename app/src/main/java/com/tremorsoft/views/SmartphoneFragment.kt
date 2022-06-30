/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.views

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.tremorsoft.R
import com.tremorsoft.databinding.FragmentRecordingBinding
import com.tremorsoft.utils.APIService.Companion.service
import com.tremorsoft.utils.PatientData
import com.tremorsoft.utils.RecordingData
import com.tremorsoft.utils.SensorSignals
import com.tremorsoft.viewmodels.DataViewModel
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable

class SmartphoneFragment : BaseFragment(), SensorEventListener, Serializable {

    private lateinit var dataViewModel: DataViewModel
    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null

    private var recordingTime = 0f
    private var waitingTime = 0f
    private var recordingStart = 0f
    private var waitingStart = 0f
    private var time = 0f

    private var sensorsData: MutableList<SensorSignals> = ArrayList()
    private var restData: MutableList<SensorSignals> = ArrayList()
    private var postureData: MutableList<SensorSignals> = ArrayList()
    private var tremorData: MutableList<SensorSignals> = ArrayList()

    private var position: String? = null
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
    private var arrayData: JSONArray = JSONArray()
    private var tremorsoftData: JSONArray? = JSONArray()

    private var recordingStatus = false
    private var recordingInitialization = false

    private var patientData: PatientData? = null
    private var recordingData: RecordingData? = null

    private var validForm: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

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

            btnAction1.tag = "start"
            btnAction1.text = getString(R.string.start)
            btnAction1.isEnabled = false

            btnAction2.tag = "confirm"
            btnAction2.text = getString(R.string.confirm)
            btnAction2.isEnabled = false

            cvFigure.isVisible = false
            cvPosition.isVisible = true
            cvButtons.isVisible = false

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
                        position = "_R"
                    }
                    R.id.rb_posture -> {
                        title.text = getString(R.string.posture)
                        tvInstructions.text = getString(R.string.posture_instructions)
                        cvFigure.isVisible = true
                        cvButtons.isVisible = true
                        positionFig.setImageResource(R.drawable.posture)
                        rbRest.isEnabled = false
                        position = "_P"
                    }
                }
            }

            btnAction1.setOnClickListener {
                recordingStatus = false
                recordingInitialization = false
                recordingTime = 0f
                waitingTime = 0f
                recordingStart = 0f
                waitingStart = 0f
                sensorsData.clear()
                tvInstructions.textAlignment = View.TEXT_ALIGNMENT_GRAVITY

                when (it.tag) {
                    "start" -> {
                        recordingStatus = true
                        showProgressBar()
                        btnAction1.tag = "cancel"
                        btnAction1.text = getString(R.string.cancel)
                    }
                    "cancel" -> {
                        hideProgressBar()
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
                        showProgressBar()
                        btnAction1.text = getString(R.string.cancel)
                        btnAction1.tag = "cancel"
                        btnAction2.isEnabled = false
                    }
                    "reset" -> {
                        findNavController().navigate(R.id.action_nav_recording_self)
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
                            restData.addAll(sensorsData)
                            rgPosition.clearCheck()
                            rbRest.isEnabled = false
                            rbPosture.isEnabled = true
                            tvInstructions.text = getString(R.string.record_instructions)
                        } else if (rbPosture.isChecked) {
                            checkPosture.visibility = View.VISIBLE
                            postureData.addAll(sensorsData)
                            rgPosition.clearCheck()
                            rbRest.isEnabled = true
                            rbPosture.isEnabled = false
                            tvInstructions.text = getString(R.string.record_instructions)
                        }
                        btnAction1.isEnabled = false
                        if (restData.isNotEmpty() and postureData.isNotEmpty()) {
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
                        showProgressBar()
                        title.text = getString(R.string.uploading)
                        tvInstructions.textAlignment = View.TEXT_ALIGNMENT_CENTER
                        tvInstructions.text = getString(R.string.wait_analyzing)
                        btnAction1.isEnabled = false
                        btnAction2.isEnabled = false

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
                            arrayData.put(signalsJSON)
                        }

                        uploadData()
                    }
                    "send" -> {
                        viewModel.recordingDataChanged(arrayData, classification)
                        if (validForm) {
                            submitData()
                        }
                    }

                    "new" -> {
                        viewModel.initSetup()
                        findNavController().navigate(R.id.action_nav_recording_to_nav_home)
                    }
                }
            }
            JUSTIFICATION_MODE_INTER_WORD.also { tvInstructions.justificationMode = it }
        }
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
            service
                .uploadData(arrayData) //send jsonObj
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
            service
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

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        mAccelerometer?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, 10000)
        }
        mGyroscope?.also { gyroscope ->
            sensorManager.registerListener(this, gyroscope, 10000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensorValues: MutableList<SensorSignals> = ArrayList()
        val microSec = event.timestamp
        val currentTime = microSec.times((1.0f / 1000000000.0f))

        with(binding) {
            if (recordingStatus) {
                if (!recordingInitialization) {
                    waitingStart = currentTime
                    recordingInitialization = true
                    title.text = getString(R.string.initializing)
                }
                when {
                    waitingTime <= 5.0f -> {
                        waitingTime = currentTime - waitingStart
                        time = 5.0f - waitingTime
                        tvInstructions.textAlignment = View.TEXT_ALIGNMENT_CENTER
                        tvInstructions.text = getString(R.string.wait_instructions, time)
                    }
                    waitingTime > 5.0f -> {
                        if (sensorsData.size == 0) {
                            title.text = getString(R.string.recording)
                            recordingStart = currentTime
                            sensorValues.add(
                                SensorSignals(
                                    "gyrX$position", "gyrY$position", "gyrZ$position",
                                    "accX$position", "accY$position", "accZ$position"
                                )
                            )
                        }
                        when {
                            recordingTime <= 15.0f -> {
                                when (event.sensor?.type) {
                                    Sensor.TYPE_GYROSCOPE -> {
                                        gyrX = event.values[0].toString()
                                        gyrY = event.values[1].toString()
                                        gyrZ = event.values[2].toString()
                                    }
                                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                                        accX = event.values[0].toString()
                                        accY = event.values[1].toString()
                                        accZ = event.values[2].toString()
                                    }
                                }
                                sensorValues.add(SensorSignals(gyrX, gyrY, gyrZ, accX, accY, accZ))
                                sensorsData.addAll(sensorValues)
                                recordingTime = currentTime - recordingStart
                                time = 15.0f - recordingTime
                                tvInstructions.text = getString(R.string.time_instructions, time)
                            }
                            recordingTime > 15.0f -> {
                                recordingStatus = false
                                hideProgressBar()
                                title.text = getString(R.string.position_recorded)
                                btnAction1.text = getString(R.string.restart)
                                btnAction1.tag = "restart"
                                tvInstructions.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
                                tvInstructions.text = getString(R.string.recordDone_instructions)
                                btnAction2.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}