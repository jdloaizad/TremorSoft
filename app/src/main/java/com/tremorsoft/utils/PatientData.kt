/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.utils

data class PatientData(
    val patientID: String? = null,
    val age: String? = null,
    val gender: String? = null,
    val familyHistory: String? = null,
    val diagnosis: String? = null,
    val method: String? = null,
    val onsetAge: String? = null,
    val bilaterality: String? = null,
    val treatments: String? = null,
    val medication: String? = null,
    val concomitances: String? = null,
    val comorbidities: String? = null,
    val isPatientDataCompleted: Boolean = false
)
