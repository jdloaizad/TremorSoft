/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.utils

import org.json.JSONArray
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class APIService {
    interface APIInterface {
        @Headers("Content-type: application/json")

        @POST("/upload")
        fun uploadData(@Body body: JSONArray): Call<ResponseBody>

        @POST("/submit")
        fun submitData(@Body body: JSONArray): Call<ResponseBody>
    }

    companion object {
        private  val retrofit = Retrofit.Builder()
            .baseUrl("https://tremorsoft-app.herokuapp.com/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        var service: APIInterface = retrofit.create(APIInterface::class.java)
    }
}
