package com.choo.moviefinder.data.remote.api

import com.choo.moviefinder.data.remote.dto.KoficBoxOfficeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface KoficApiService {

    // 일별 박스오피스 TOP 10 조회 (key는 KoficOkHttpClient 인터셉터가 자동 주입)
    @GET("boxoffice/searchDailyBoxOfficeList.json")
    suspend fun getDailyBoxOffice(
        @Query("targetDt") targetDt: String,
        @Query("itemPerPage") itemPerPage: Int = 10
    ): KoficBoxOfficeResponse
}
