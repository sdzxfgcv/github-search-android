package com.example.ghsearch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LANGUAGES = listOf(
    "", "Kotlin", "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust", "C++", "Swift", "Dart", "PHP"
)
private val TOPICS = listOf(
    "", "android", "kotlin", "ai", "llm", "flutter", "jetpack-compose", "mvvm", "game", "tool"
)
private val STAR_OPTIONS = listOf(0, 100, 500, 1000, 5000)
private val DAY_OPTIONS = listOf(0, 7, 30, 365)

private data class Preset(
    val label: String,
    val keyword: String,
    val language: String,
    val topic: String,
    val minStars: Int,
    val days: Int,
    val sort: String
)

private val PRESETS = listOf(
    Preset("Android·Kotlin", "", "Kotlin", "android", 100, 0, "stars"),
    Preset("Android·Java", "", "Java", "android", 500, 0, "stars"),
    Preset("AI / LLM", "", "", "llm", 500, 0, "stars"),
    Preset("高星精选", "", "", "", 5000, 0, "stars"),
    Preset("近期活跃", "", "", "", 0, 7, "updated"),
    Preset("重置筛选", "", "", "", 0, 0, "best")
)

private fun sortName(sort: String) = when (sort) {
    "stars" -> "最多 Star"
    "updated" -> "最近更新"
    else -> "最佳匹配"
}

private fun starName(n: Int) = if (n == 0) "不限星" else "$n+ Star"
private fun dayName(n: Int) = when (n) {
    0 -> "不限时间"
    7 -> "近一周"
    30 -> "近一月"
    else -> "近一年"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var keyword by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var minStars by rememberSaveable { mutableIntStateOf(0) }
    var days by rememberSaveable { mutableIntStateOf(0) }
    var sort by rememberSaveable { mutableStateOf("best") }
    var token by rememberSaveable { mutableStateOf("") }

    var results by remember { mutableStateOf<List<Repo>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        token = context.getSharedPreferences("ghsearch", Context.MODE_PRIVATE)
            .getString("token", "").orEmpty()
    }

    fun doSearch() {
        if (keyword.isBlank() && language.isBlank() && topic.isBlank() && minStars == 0 && days == 0) {
            Toast.makeText(context, "请输入关键词或选择分类", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            loading = true
            message = null
            val result = withContext(Dispatchers.IO) {
                GitHubApi.search(keyword, language, topic, minStars, days, sort, token)
            }
            loading = false
            hasSearched = true
            when (result) {
                is SearchResult.Success -> {
                    results = result.items
                    total = result.totalCount
                    if (result.items.isEmpty()) message = "没有找到匹配的仓库，试试放宽筛选条件"
                }
                is SearchResult.Failure -> {
                    results = emptyList()
                    message = result.message
                }
            }
        }
    }

    fun copyUrl(url: String) {
        clipboard.setText(AnnotatedString(url))
        Toast.makeText(context, "仓库网址已复制", Toast.LENGTH_SHORT).show()
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub 搜索", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("输入关键词，如 android mvvm") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() })
                )
            }

            item {
                SectionTitle("快捷分类（点选即搜）")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PRESETS.forEach { p ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                keyword = p.keyword
                                language = p.language
                                topic = p.topic
                                minStars = p.minStars
                                days = p.days
                                sort = p.sort
                                doSearch()
                            },
                            label = { Text(p.label) }
                        )
                    }
                }
            }

            item {
                SectionTitle("编程语言")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LANGUAGES.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(if (lang.isEmpty()) "全部语言" else lang) }
                        )
                    }
                }
            }

            item {
                SectionTitle("Topic 主题（GitHub 官方标签）")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TOPICS.forEach { t ->
                        FilterChip(
                            selected = topic == t,
                            onClick = { topic = t },
                            label = { Text(if (t.isEmpty()) "全部主题" else "#$t") }
                        )
                    }
                }
            }

            item {
                SectionTitle("Star 数与更新时间")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    STAR_OPTIONS.forEach { n ->
                        FilterChip(
                            selected = minStars == n,
                            onClick = { minStars = n },
                            label = { Text(starName(n)) }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    DAY_OPTIONS.forEach { n ->
                        FilterChip(
                            selected = days == n,
                            onClick = { days = n },
                            label = { Text(dayName(n)) }
                        )
                    }
                }
            }

            item {
                SectionTitle("排序")
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("best", "stars", "updated").forEach { s ->
                        FilterChip(
                            selected = sort == s,
                            onClick = { sort = s },
                            label = { Text(sortName(s)) }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { doSearch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("搜索仓库", fontSize = 15.sp)
                }
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }

            message?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = if (hasSearched && results.isEmpty()) Color(0xFFC62828) else Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (hasSearched && results.isNotEmpty()) {
                item {
                    Text(
                        text = "共找到 $total 个仓库（显示前 ${results.size} 个）",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            items(results) { repo ->
                RepoCard(
                    repo = repo,
                    onCopy = { copyUrl(repo.htmlUrl) },
                    onOpen = { openUrl(repo.htmlUrl) }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("GitHub Token（可选）") },
            text = {
                Column {
                    Text(
                        "不填也能用，但匿名搜索每小时仅 10 次。填入 Personal Access Token 可提升到 30 次/分钟。Token 仅保存在本机，不会上传。",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("ghp_...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSharedPreferences("ghsearch", Context.MODE_PRIVATE)
                        .edit()
                        .putString("token", token.trim())
                        .apply()
                    showSettings = false
                    Toast.makeText(context, "Token 已保存到本机", Toast.LENGTH_SHORT).show()
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun RepoCard(
    repo: Repo,
    onCopy: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = repo.fullName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2F6FED)
            )
            if (repo.description.isNotBlank()) {
                Text(
                    text = repo.description,
                    fontSize = 12.5.sp,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (repo.topics.isNotEmpty()) {
                Text(
                    text = repo.topics.take(4).joinToString("  ") { "#$it" },
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 9.dp)
            ) {
                repo.language?.let {
                    Text("● $it", fontSize = 11.5.sp, color = Color(0xFF6C757D))
                    Spacer(Modifier.width(10.dp))
                }
                Text("★ ${repo.stars}", fontSize = 11.5.sp)
                Spacer(Modifier.width(8.dp))
                Text("⑂ ${repo.forks}", fontSize = 11.5.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    repo.updatedAt.take(10),
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )
            }
            Row(modifier = Modifier.padding(top = 10.dp)) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("复制网址", fontSize = 11.5.sp)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Icon(
                        Icons.Filled.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("打开 GitHub", fontSize = 11.5.sp)
                }
            }
        }
    }
}
