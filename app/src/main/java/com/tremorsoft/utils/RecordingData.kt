/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.utils

import org.json.JSONArray

data class RecordingData(
    var arrayData: JSONArray? = null,
    var classification: String? = null,
    var isRecordingDataCompleted: Boolean = false
)
