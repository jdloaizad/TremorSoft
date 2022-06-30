/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tremorsoft.utils.PatientData
import com.tremorsoft.utils.RecordingData
import org.json.JSONArray

class DataViewModel : ViewModel() {
    private val _patientData = MutableLiveData<PatientData>()
    val patientData: LiveData<PatientData> = _patientData

    private val _recordingData = MutableLiveData<RecordingData>()
    val recordingData: LiveData<RecordingData> = _recordingData

    private var isPatientDataCompleted: Boolean = false
    private var isRecordingDataCompleted: Boolean = false

    fun patientDataChanged(patientID: String?, age: String?, gender: String?, familyHistory: String?,
                           diagnosis: String?, method: String?, onsetAge: String?, bilaterality: String?,
                           treatments: String?, medication: String?, concomitances: String?, comorbidities: String?) {

        if (patientID.isNullOrEmpty() or age.isNullOrEmpty() or gender.isNullOrEmpty() or familyHistory.isNullOrEmpty()
            or diagnosis.isNullOrEmpty() or method.isNullOrEmpty() or onsetAge.isNullOrEmpty() or
            bilaterality.isNullOrEmpty() or treatments.isNullOrEmpty() or medication.isNullOrEmpty() or
            concomitances.isNullOrEmpty() or comorbidities.isNullOrEmpty()) {
            isPatientDataCompleted = false
        } else {
            isPatientDataCompleted = true
            _patientData.value = PatientData(patientID = patientID, age = age, gender = gender,
                familyHistory = familyHistory, diagnosis = diagnosis, method = method, onsetAge = onsetAge,
                bilaterality = bilaterality, treatments = treatments, medication = medication,
                concomitances = concomitances, comorbidities = comorbidities,
                isPatientDataCompleted = isPatientDataCompleted
            )
        }
    }

    fun recordingDataChanged(arrayData: JSONArray?, classification: String?) {
        if (arrayData == null) {
            isRecordingDataCompleted = false
        } else if (classification.isNullOrEmpty()) {
            isRecordingDataCompleted = false
        } else {
            isRecordingDataCompleted = true
            _recordingData.value = RecordingData(arrayData = arrayData, classification = classification,
                isRecordingDataCompleted = isRecordingDataCompleted)
        }
    }

    fun initSetup() {
        isPatientDataCompleted = false
        isRecordingDataCompleted = false
    }
}