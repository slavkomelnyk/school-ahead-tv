package com.stabrise.school.data

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/** Cache-Control value that answers from the disk cache only, however old the entry is. */
const val FROM_CACHE = "only-if-cached, max-stale=2147483647"

const val PAGE_SIZE = 20

interface SchoolApiService {
    @GET("api/public/subjects")
    suspend fun getSubjects(@Header("Cache-Control") cache: String? = null): List<Subject>

    @GET("api/public/subjects/{subject_id}/lesson-topics")
    suspend fun getLessonTopics(
        @Path("subject_id") subjectId: Int,
        @Header("Cache-Control") cache: String? = null
    ): List<LessonTopic>

    @GET("api/public/subjects/{subject_id}/lessons-page")
    suspend fun getLessonsPage(
        @Path("subject_id") subjectId: Int,
        @Query("topic_id") topicId: Int? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Header("Cache-Control") cache: String? = null
    ): LessonPageResponse

    @GET("api/public/lessons/{lesson_id}")
    suspend fun getLessonDetails(@Path("lesson_id") lessonId: Int): LessonDetail
}

object NetworkModule {
    private var cacheDir: File? = null

    /** Call before the first request so responses are kept between app starts. */
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "api")
    }

    val api: SchoolApiService by lazy {
        val client = OkHttpClient.Builder()
            .cache(cacheDir?.let { Cache(it, 10L * 1024 * 1024) })
            .build()
        Retrofit.Builder()
            .baseUrl("https://api-school.stabrise.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SchoolApiService::class.java)
    }
}
