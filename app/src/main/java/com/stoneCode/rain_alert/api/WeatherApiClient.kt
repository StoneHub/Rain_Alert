package com.stoneCode.rain_alert.api

import com.stoneCode.rain_alert.data.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Data classes for the Weather.gov API responses
 */
data class PointsResponse(
    val properties: PointsProperties
)

data class PointsProperties(
    val forecast: String,
    val forecastHourly: String
)

data class ForecastResponse(
    val properties: ForecastProperties
)

data class ForecastProperties(
    val periods: List<ForecastPeriod>
)

data class ForecastPeriod(
    val number: Int,
    val name: String,
    val startTime: String,
    val endTime: String,
    val isDaytime: Boolean,
    val temperature: Int,
    val temperatureUnit: String,
    val temperatureTrend: String?,
    val probabilityOfPrecipitation: ProbabilityOfPrecipitation?,
    val dewpoint: Measurement?,
    val relativeHumidity: Measurement?,
    val windSpeed: String,
    val windDirection: String,
    val shortForecast: String,
    val detailedForecast: String
)

data class ProbabilityOfPrecipitation(
    val value: Int?
)

data class Measurement(
    val value: Double?
)

// Retrofit service interface
interface WeatherApiService {
    @Headers(
        "User-Agent: ${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})",
        "Accept: application/geo+json"
    )
    @GET("points/{latitude},{longitude}")
    suspend fun getPoints(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double
    ): Response<PointsResponse>

    @Headers(
        "User-Agent: ${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})",
        "Accept: application/geo+json"
    )
    @GET
    suspend fun getForecast(@Url url: String): Response<ForecastResponse>
}

/**
 * Client class to handle Weather.gov API requests
 */
class WeatherApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.WEATHER_API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    private val service = retrofit.create(WeatherApiService::class.java)

    suspend fun getForecast(latitude: Double, longitude: Double): Result<ForecastResponse> = withContext(Dispatchers.IO) {
        try {
            // First get the points data to find the forecast URL
            val pointsResponse = service.getPoints(latitude, longitude)
            if (!pointsResponse.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Error fetching points data: ${pointsResponse.code()} ${pointsResponse.message()}")
                )
            }

            // Now get the forecast using the URL from the points response
            val forecastUrl = pointsResponse.body()?.properties?.forecast
                ?: return@withContext Result.failure(Exception("Forecast URL not found"))

            val forecastResponse = service.getForecast(forecastUrl)
            if (!forecastResponse.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Error fetching forecast: ${forecastResponse.code()} ${forecastResponse.message()}")
                )
            }

            val forecast = forecastResponse.body()
                ?: return@withContext Result.failure(Exception("Forecast data not found"))

            Result.success(forecast)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHourlyForecast(latitude: Double, longitude: Double): Result<ForecastResponse> = withContext(Dispatchers.IO) {
        try {
            // First get the points data to find the forecast URL
            val pointsResponse = service.getPoints(latitude, longitude)
            if (!pointsResponse.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Error fetching points data: ${pointsResponse.code()} ${pointsResponse.message()}")
                )
            }

            // Now get the hourly forecast using the URL from the points response
            val forecastHourlyUrl = pointsResponse.body()?.properties?.forecastHourly
                ?: return@withContext Result.failure(Exception("Hourly forecast URL not found"))

            val forecastResponse = service.getForecast(forecastHourlyUrl)
            if (!forecastResponse.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Error fetching hourly forecast: ${forecastResponse.code()} ${forecastResponse.message()}")
                )
            }

            val forecast = forecastResponse.body()
                ?: return@withContext Result.failure(Exception("Hourly forecast data not found"))

            Result.success(forecast)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}