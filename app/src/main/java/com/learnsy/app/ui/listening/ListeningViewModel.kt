package com.learnsy.app.ui.listening

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.learnsy.app.audio.QuizAudioEngine
import com.learnsy.app.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable

@Serializable
private data class ListeningItemRow(
    val id: String,
    val text: String? = null,
    val word_box: List<String>? = null,
    val answers: List<String>? = null,
    val statements: List<StatementRow>? = null,
    val shuffle_statements: Boolean? = null,
    val shuffle_word_box: Boolean? = null
)

@Serializable
private data class StatementRow(val statement: String, val answer: String)

private fun StatementRow.toStatement(): Statement = Statement(
    text = statement,
    answer = when (answer) {
        "True" -> StatementAnswer.TRUE
        "False" -> StatementAnswer.FALSE
        else -> StatementAnswer.NOT_MENTIONED
    }
)

/**
 * ── ListeningViewModel ──
 * Thay state đầu ListeningPractice trong listening-practice.jsx: load items
 * từ Supabase (bảng listening_items), quản lý bài đang mở (selected),
 * blanks/stmtSel, TTS state, tính điểm.
 */
class ListeningViewModel(application: Application) : AndroidViewModel(application) {

    private val tts = ListeningTts(application)
    private val cache = com.learnsy.app.data.OfflineCacheStore(application)
    // Dùng chung engine âm thanh với QuizPlayerScreen — bấm Đúng/Sai/NM
    // phát cùng tiếng "click" như khi chọn đáp án trắc nghiệm.
    private val audio = QuizAudioEngine(application, viewModelScope)

    private val _items = MutableStateFlow<List<ListeningItem>>(emptyList())
    val items: StateFlow<List<ListeningItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    private val _selected = MutableStateFlow<ListeningItem?>(null)
    val selected: StateFlow<ListeningItem?> = _selected.asStateFlow()

    private val _wordBoxDisplay = MutableStateFlow<List<String>>(emptyList())
    val wordBoxDisplay: StateFlow<List<String>> = _wordBoxDisplay.asStateFlow()

    private val _blanks = MutableStateFlow<List<String>>(emptyList())
    val blanks: StateFlow<List<String>> = _blanks.asStateFlow()

    private val _stmtSel = MutableStateFlow<List<StatementAnswer?>>(emptyList())
    val stmtSel: StateFlow<List<StatementAnswer?>> = _stmtSel.asStateFlow()

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

    private val _showScoreToast = MutableStateFlow(false)
    val showScoreToast: StateFlow<Boolean> = _showScoreToast.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _isRestarting = MutableStateFlow(false)
    val isRestarting: StateFlow<Boolean> = _isRestarting.asStateFlow()

    init {
        tts.setListeners(
            onStart = { _isPlaying.value = true; _isRestarting.value = false },
            onEnd = { _isPlaying.value = false; _isRestarting.value = false },
            onError = { _isPlaying.value = false; _isRestarting.value = false }
        )
        loadItems()
    }

    override fun onCleared() {
        super.onCleared()
        tts.shutdown()
    }

    private fun loadItems() {
        viewModelScope.launch {
            _downloadedIds.value = cache.downloadedListeningIds()
            try {
                // withTimeout: khi mất mạng hoàn toàn, socket connect có thể
                // treo rất lâu trước khi hệ thống tự báo lỗi — giới hạn 6s để
                // chủ động rơi về cache offline thay vì kẹt màn hình loading.
                val rows = withTimeout(6000) {
                    SupabaseClientProvider.client.postgrest["listening_items"]
                        .select()
                        .decodeList<ListeningItemRow>()
                }
                val loaded = rows.map { r ->
                    ListeningItem(
                        id = r.id,
                        text = r.text ?: "",
                        wordBox = r.word_box ?: emptyList(),
                        answers = r.answers ?: emptyList(),
                        statements = r.statements?.map { it.toStatement() } ?: emptyList(),
                        shuffleStatements = r.shuffle_statements ?: false,
                        shuffleWordBox = r.shuffle_word_box ?: false
                    )
                }
                _items.value = loaded
                _isOffline.value = false
                cache.saveListeningItems(loaded)
            } catch (e: Exception) {
                // Mất mạng — dùng bài đã cache offline thay vì báo lỗi trắng trang.
                val cached = cache.loadListeningItems()
                if (cached.isNotEmpty()) {
                    _items.value = cached
                    _isOffline.value = true
                    _loadError.value = false
                } else {
                    _loadError.value = true
                }
            }
            _loading.value = false
        }
    }

    /** Học sinh chủ động bấm "Tải về" 1 bài — chỉ đánh dấu để hiển thị icon,
     *  vì nội dung bài đã có sẵn trong cache tự động ở trên rồi. */
    fun downloadItem(itemId: String) {
        viewModelScope.launch {
            cache.markListeningDownloaded(itemId)
            _downloadedIds.value = cache.downloadedListeningIds()
        }
    }

    fun openItem(item: ListeningItem) {
        tts.stop()
        _isPlaying.value = false
        _isRestarting.value = false

        val stmts = if (item.shuffleStatements) shuffleList(item.statements) else item.statements
        val wb = if (item.shuffleWordBox) shuffleList(item.wordBox) else item.wordBox

        _selected.value = item.copy(statements = stmts)
        _wordBoxDisplay.value = wb
        _blanks.value = item.answers.map { "" }
        _stmtSel.value = stmts.map { null }
        _submitted.value = false
        _showScoreToast.value = false
    }

    fun closeItem() {
        tts.stop()
        _isPlaying.value = false
        _isRestarting.value = false
        _selected.value = null
        _submitted.value = false
    }

    fun reshuffleWordBox() {
        val current = _wordBoxDisplay.value
        if (current.size <= 1) return
        var next: List<String>
        do {
            next = shuffleList(current)
        } while (next == current)
        _wordBoxDisplay.value = next
    }

    fun setBlank(index: Int, value: String) {
        val list = _blanks.value.toMutableList()
        if (index in list.indices) list[index] = value
        _blanks.value = list
    }

    fun setStatementAnswer(index: Int, value: StatementAnswer) {
        audio.playClick()
        val list = _stmtSel.value.toMutableList()
        if (index in list.indices) list[index] = value
        _stmtSel.value = list
    }

    fun togglePlayPause() {
        val sel = _selected.value ?: return
        if (tts.isSpeaking()) {
            tts.pause()
        } else {
            _isPlaying.value = true
            tts.speak(sel.text, _speechRate.value)
        }
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        val sel = _selected.value ?: return
        if (tts.isSpeaking()) {
            _isRestarting.value = true
            tts.speak(sel.text, rate)
        }
    }

    fun restart() {
        val sel = _selected.value ?: return
        tts.stop()
        _isPlaying.value = false
        _isRestarting.value = true
        tts.speak(sel.text, _speechRate.value)
    }

    /** Tương đương score useMemo — trả về (correct, total). */
    fun computeScore(): Pair<Int, Int> {
        val sel = _selected.value ?: return 0 to 0
        var correct = 0
        var total = 0
        sel.answers.forEachIndexed { i, ans ->
            total++
            if (normAnswer(_blanks.value.getOrNull(i)) == normAnswer(ans)) correct++
        }
        sel.statements.forEachIndexed { i, st ->
            total++
            if (_stmtSel.value.getOrNull(i) == st.answer) correct++
        }
        return correct to total
    }

    fun submit() {
        _submitted.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(420)
            _showScoreToast.value = true
        }
    }

    fun dismissScoreToast() {
        _showScoreToast.value = false
    }
}
