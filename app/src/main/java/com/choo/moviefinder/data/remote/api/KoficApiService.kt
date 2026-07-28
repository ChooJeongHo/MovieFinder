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

    // 주간/주말/주중 박스오피스 TOP 10 조회.
    // weekGb는 KOFIC이 기본값을 보장하지 않고 잘못된 값에 faultInfo(320105)를 반환하므로 필수 인자로 둔다.
    @GET("boxoffice/searchWeeklyBoxOfficeList.json")
    suspend fun getWeeklyBoxOffice(
        @Query("targetDt") targetDt: String,
        @Query("weekGb") weekGb: String,
        @Query("itemPerPage") itemPerPage: Int = 10
    ): KoficBoxOfficeResponse
}
