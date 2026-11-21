package com.example.ghsearch

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Repo(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String,
    val htmlUrl: String,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val updatedAt: String,
    val owner: String,
    val topics: List<String>
)

sealed class SearchResult {
    data class Success(val items: List<Repo>, val totalCount: Int) : SearchResult()
    data class Failure(val message: String) : SearchResult()
}

object GitHubApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun search(
        keyword: String,
        language: String,
        topic: String,
        minStars: Int,
        updatedWithinDays: Int,
        sort: String,
        token: String,
        page: Int = 1
    ): SearchResult {
        // 把界面上的分类选项拼成 GitHub 搜索语法
        val parts = mutableListOf<String>()
        if (keyword.isNotBlank()) parts.add(keyword.trim())
        if (language.isNotBlank()) parts.add("language:$language")
        if (topic.isNotBlank()) parts.add("topic:$topic")
        if (minStars > 0) parts.add("stars:>=$minStars")
        if (updatedWithinDays > 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -updatedWithinDays)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            parts.add("pushed:>=$date")
        }
        val q = parts.joinToString(" ")
        if (q.isBlank()) return SearchResult.Failure("请输入关键词或选择筛选条件")

        val encoded = URLEncoder.encode(q, "UTF-8")
        val url =
            "https://api.github.com/search/repositories?q=$encoded&sort=$sort&order=desc&per_page=30&page=$page"

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "github-search-android")
            .header("X-GitHub-Api-Version", "2022-11-28")
        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${token.trim()}")
        }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val remoteMsg = try {
                        JSONObject(body).optString("message", "请求失败")
                    } catch (e: Exception) {
                        "请求失败"
                    }
                    val hint = when (response.code) {
                        401 -> "Token 无效或已过期，请到设置里检查"
                        403 -> "请求次数超限（未填 Token 每小时仅 10 次），建议填写 Token"
                        422 -> "搜索条件不被接受：$remoteMsg"
                        else -> remoteMsg
                    }
                    return SearchResult.Failure("HTTP ${response.code}：$hint")
                }
                val root = JSONObject(body)
                val total = root.optInt("total_count", 0)
                val array = root.optJSONArray("items")
                val items = mutableListOf<Repo>()
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        val topics = mutableListOf<String>()
                        val ta = o.optJSONArray("topics")
                        if (ta != null) {
                            for (j in 0 until ta.length()) {
                                val t = ta.optString(j)
                                if (t.isNotBlank()) topics.add(t)
                            }
                        }
                        items.add(
                            Repo(
                                id = o.optLong("id"),
                                name = o.optString("name", ""),
                                fullName = o.optString("full_name", ""),
                                description = o.optString("description", ""),
                                htmlUrl = o.optString("html_url", ""),
                                language = o.optString("language", "").ifBlank { null },
                                stars = o.optInt("stargazers_count", 0),
                                forks = o.optInt("forks_count", 0),
                                updatedAt = o.optString("pushed_at", ""),
                                owner = o.optJSONObject("owner")?.optString("login", "").orEmpty(),
                                topics = topics
                            )
                        )
                    }
                }
                SearchResult.Success(items, total)
            }
        } catch (e: Exception) {
            SearchResult.Failure("网络错误：${e.message ?: "未知错误"}")
        }
    }
}
