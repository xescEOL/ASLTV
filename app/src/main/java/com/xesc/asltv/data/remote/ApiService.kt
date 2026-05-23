package com.xesc.asltv.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {
    // URL dinámica para descargar el JSON de la lista
    @GET
    suspend fun fetchList(@Url url: String): ResponseBody

    // URL dinámica para descargar el XMLTV del EPG
    @Streaming
    @GET
    suspend fun fetchEpg(@Url url: String): ResponseBody
}