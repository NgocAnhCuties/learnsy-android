package com.learnsy.app.ui.listening

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy.app.ui.dashboard.DashboardIcon
import com.learnsy.app.ui.dashboard.ReactLogoIcon
import com.learnsy.app.ui.quiz.quizColors
import com.learnsy.app.ui.theme.NunitoFontFamily

private data class StAnsUi(val color: Color, val bg: Color, val border: Color, val label: String)

private fun statementAnswerUi(a: StatementAnswer): StAnsUi = when (a) {
    StatementAnswer.TRUE -> StAnsUi(Color(0xFF16A34A), Color(0x1A16A34A), Color(0x5916A34A), "Đúng")
    StatementAnswer.FALSE -> StAnsUi(Color(0xFFDC2626), Color(0x14DC2626), Color(0x52DC2626), "Sai")
    StatementAnswer.NOT_MENTIONED -> StAnsUi(Color(0xFF6366F1), Color(0x146366F1), Color(0x526366F1), "NM")
}

@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun ListeningDetailScreen(
    item: ListeningItem,
    wordBoxDisplay: List<String>,
    blanks: List<String>,
    stmtSel: List<StatementAnswer?>,
    submitted: Boolean,
    isPlaying: Boolean,
    speechRate: Float,
    isRestarting: Boolean,
    showScoreToast: Boolean,
    correct: Int,
    total: Int,
    dark: Boolean,
    onBackToList: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onRateChange: (Float) -> Unit,
    onRestart: () -> Unit,
    onReshuffleWordBox: () -> Unit,
    onBlankChange: (Int, String) -> Unit,
    onStatementChange: (Int, StatementAnswer) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onDismissScoreToast: () -> Unit
) {
    val C = quizColors(dark)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.headerBg)
                .padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(C.navBtn, RoundedCornerShape(50))
                    .border(1.5.dp, C.navBtnBorder, RoundedCornerShape(50))
                    .clickable(onClick = onBackToList)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DashboardIcon(name = "chevronLeft", size = 11.dp, color = C.navBtnText)
                Text(text = "Danh sách", fontSize = 12.sp, fontWeight = FontWeight.Black, color = C.navBtnText, fontFamily = NunitoFontFamily)
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo đồng bộ với màn hình đăng nhập: icon mũ tốt nghiệp +
                // "TA&NA" thay cho icon "book" + "Listening" trước đây.
                DashboardIcon(name = "graduationCap", size = 15.dp, color = Color(0xFF6366F1))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "TA&NA", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF6366F1), fontFamily = NunitoFontFamily)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (submitted) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399))), RoundedCornerShape(50))
                            .padding(horizontal = 13.dp, vertical = 5.dp)
                    ) {
                        Text(text = "$correct/$total", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = NunitoFontFamily)
                    }
                } else {
                    Spacer(modifier = Modifier.width(30.dp))
                }
                // Logo React bên phải header, giữ đúng màu xanh cyan gốc.
                ReactLogoIcon(size = 18.dp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.surfaceQ, RoundedCornerShape(18.dp))
                    .border(1.5.dp, if (isRestarting) Color(0xFFF59E0B) else C.borderQ, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Brush.linearGradient(
                                if (isPlaying) listOf(Color(0xFFF59E0B), Color(0xFFF97316))
                                else listOf(Color(0xFF10B981), Color(0xFF34D399))
                            ),
                            CircleShape
                        )
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    DashboardIcon(name = if (isPlaying) "close" else "chevronRight", size = 16.dp, color = Color.White)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "0.8x", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.textMid, fontFamily = NunitoFontFamily)
                        Text(text = "%.1fx".format(speechRate), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.text, fontFamily = NunitoFontFamily)
                        Text(text = "1.2x", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = C.textMid, fontFamily = NunitoFontFamily)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = onRateChange,
                        valueRange = 0.8f..1.2f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFB07CF0),
                            activeTrackColor = Color(0xFFB07CF0),
                            inactiveTrackColor = if (dark) Color(0x26FFFFFF) else Color(0x1F000000)
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .background(
                            if (isRestarting) Color(0x26F59E0B) else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .border(1.5.dp, if (isRestarting) Color(0xFFF59E0B) else C.borderQ, RoundedCornerShape(50))
                        .clickable(onClick = onRestart)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isRestarting) {
                        val rotation by rememberInfiniteTransition(label = "restartSpin").animateFloat(
                            0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "restartSpinRotation"
                        )
                        Box(modifier = Modifier.graphicsLayer { rotationZ = rotation }) {
                            DashboardIcon(name = "spinner", size = 11.dp, color = Color(0xFFF59E0B))
                        }
                        Text(text = "Đang tải...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontFamily = NunitoFontFamily)
                    } else {
                        DashboardIcon(name = "chevronLeft", size = 11.dp, color = C.text2)
                        Text(text = "Phát lại", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = C.text2, fontFamily = NunitoFontFamily)
                    }
                }
            }

            if (item.wordBox.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0F6366F1), RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0x386366F1), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            DashboardIcon(name = "folder", size = 11.dp, color = Color(0xFF6366F1))
                            Text(text = "WORD BOX", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF6366F1), letterSpacing = 1.sp, fontFamily = NunitoFontFamily)
                        }
                        if (item.shuffleWordBox && wordBoxDisplay.size > 1) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0x1A6366F1), RoundedCornerShape(50))
                                    .border(1.5.dp, Color(0x596366F1), RoundedCornerShape(50))
                                    .clickable(onClick = onReshuffleWordBox)
                                    .padding(horizontal = 9.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DashboardIcon(name = "shuffle", size = 9.dp, color = Color(0xFF4338CA))
                                Text(text = "Tráo lại", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF4338CA), fontFamily = NunitoFontFamily)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        wordBoxDisplay.forEach { w ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0x1F6366F1), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(text = w, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA), fontFamily = NunitoFontFamily)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.surfaceQ, RoundedCornerShape(18.dp))
                    .border(1.5.dp, C.borderQ, RoundedCornerShape(18.dp))
                    .padding(horizontal = 17.dp, vertical = 15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DashboardIcon(name = "book", size = 11.dp, color = Color(0xFFB07CF0))
                    Text(text = "ĐOẠN VĂN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFB07CF0), letterSpacing = 1.2.sp, fontFamily = NunitoFontFamily)
                }
                Spacer(modifier = Modifier.height(8.dp))
                PassageWithBlanks(item = item, blanks = blanks, submitted = submitted, dark = dark, onBlankChange = onBlankChange)
            }

            if (item.statements.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    item.statements.forEachIndexed { i, st ->
                        StatementRow(
                            index = i, statement = st, selected = stmtSel.getOrNull(i),
                            submitted = submitted, dark = dark, onSelect = { onStatementChange(i, it) }
                        )
                    }
                }
            }

            if (!submitted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFA855F7))), RoundedCornerShape(50))
                        .clickable(onClick = onSubmit)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DashboardIcon(name = "check", size = 13.dp, color = Color.White)
                        Text(text = "Nộp bài", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = NunitoFontFamily)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.navBtn, RoundedCornerShape(50))
                        .border(1.5.dp, C.navBtnBorder, RoundedCornerShape(50))
                        .clickable(onClick = onRetry)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DashboardIcon(name = "chevronLeft", size = 13.dp, color = C.navBtnText)
                        Text(text = "Làm lại", fontSize = 14.sp, fontWeight = FontWeight.Black, color = C.navBtnText, fontFamily = NunitoFontFamily)
                    }
                }
            }
        }
    }

    ScoreToastListening(visible = showScoreToast, correct = correct, total = total, onClose = onDismissScoreToast)
}

@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
private fun PassageWithBlanks(
    item: ListeningItem,
    blanks: List<String>,
    submitted: Boolean,
    dark: Boolean,
    onBlankChange: (Int, String) -> Unit
) {
    val C = quizColors(dark)
    val parts = splitPassage(item.text, item.answers.size)

    // FlowRow cần từng "item" nhỏ (từ) để tự do wrap dòng như trình duyệt —
    // nếu đưa cả 1 câu dài vào 1 Text() duy nhất, FlowRow sẽ coi đó là 1
    // khối không thể chia, và ô input theo sau luôn bị đẩy xuống dòng mới
    // vì không đủ chỗ trên dòng hiện tại. Tách mỗi TextPart thành các từ
    // riêng (giữ khoảng trắng) để mô phỏng đúng cách reflow của web.
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        parts.forEach { part ->
            when (part) {
                is PassagePart.TextPart -> {
                    // Chuẩn hoá mọi whitespace (kể cả \n nếu dữ liệu gốc có
                    // xuống dòng cứng giữa các câu) thành 1 dấu cách đơn —
                    // nếu không, \n lọt vào trong Text() sẽ ép xuống dòng
                    // ngay tại đó, bất kể FlowRow còn chỗ trên dòng hay không.
                    val normalized = part.content.replace(Regex("\\s+"), " ")
                    val words = Regex("(\\S+\\s?)").findAll(normalized).map { it.value }.toList()
                    words.forEach { word ->
                        if (word.isNotEmpty()) {
                            Text(text = word, color = C.text2, fontSize = 13.5.sp, lineHeight = 24.sp, fontFamily = NunitoFontFamily)
                        }
                    }
                }
                is PassagePart.BlankPart -> {
                    val bi = part.index
                    if (bi >= item.answers.size) {
                        Text(text = "___ ", color = C.textMid, fontFamily = NunitoFontFamily)
                    } else {
                        val value = blanks.getOrNull(bi) ?: ""
                        val isOk = submitted && normAnswer(value) == normAnswer(item.answers[bi])
                        val isBad = submitted && !isOk
                        val color = if (isOk) Color(0xFF059669) else if (isBad) Color(0xFFDC2626) else C.text
                        val bg = if (isOk) Color(0x1A059669) else if (isBad) Color(0x14DC2626) else C.optBg
                        val border = if (isOk) Color(0xFF059669) else if (isBad) Color(0xFFDC2626) else C.optBorder

                        // Chiều rộng tự co theo độ dài đáp án — giống công
                        // thức web (Math.max(60, length*12+20)), quy đổi
                        // sang dp. Có cận trên để đáp án dài không làm ô
                        // input tràn hết cả dòng khiến FlowRow phải ngắt.
                        val answerLen = item.answers[bi].length
                        val calcWidth = (answerLen * 8 + 28).coerceIn(44, 160)

                        // Ô input nhỏ gọn, cao bằng dòng chữ (không phải form
                        // field 46dp) — giống <input> inline bên bản web.
                        Box(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = value,
                                onValueChange = { onBlankChange(bi, it) },
                                enabled = !submitted,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = color, fontFamily = NunitoFontFamily
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(C.text),
                                modifier = Modifier
                                    .width(calcWidth.dp)
                                    .semantics { contentDescription = "Ô điền từ số ${bi + 1}" },
                                decorationBox = { inner ->
                                    Box(
                                        modifier = Modifier
                                            .background(bg, RoundedCornerShape(8.dp))
                                            .border(1.5.dp, border, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (value.isEmpty()) {
                                            Text(text = "...", fontSize = 13.5.sp, color = C.textMid, fontFamily = NunitoFontFamily)
                                        }
                                        inner()
                                    }
                                }
                            )
                            if (submitted && isBad) {
                                Text(
                                    text = item.answers[bi], fontSize = 9.sp, fontWeight = FontWeight.Black,
                                    color = Color(0xFFFCA5A5), fontFamily = NunitoFontFamily,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementRow(
    index: Int,
    statement: Statement,
    selected: StatementAnswer?,
    submitted: Boolean,
    dark: Boolean,
    onSelect: (StatementAnswer) -> Unit
) {
    val C = quizColors(dark)
    val ok = submitted && selected == statement.answer
    val bad = submitted && selected != statement.answer && selected != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (ok) Color(0x1A16A34A) else if (bad) Color(0x14DC2626) else C.surfaceQ, RoundedCornerShape(16.dp))
            .border(1.5.dp, if (ok) Color(0xFF16A34A) else if (bad) Color(0xFFDC2626) else C.borderQ, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                modifier = Modifier.size(22.dp).background(Color(0x2EB07CF0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = (index + 1).toString(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFB07CF0), fontFamily = NunitoFontFamily)
            }
            Text(
                text = statement.text.replace(Regex("</?u>"), ""),
                color = C.text2, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                fontFamily = NunitoFontFamily, modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(11.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(StatementAnswer.TRUE, StatementAnswer.FALSE, StatementAnswer.NOT_MENTIONED).forEach { ans ->
                val ui = statementAnswerUi(ans)
                val isSel = selected == ans
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSel) ui.color else ui.bg, RoundedCornerShape(11.dp))
                        .border(1.5.dp, if (isSel) ui.color else ui.border, RoundedCornerShape(11.dp))
                        .clickable(enabled = !submitted) { onSelect(ans) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ui.label, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = if (isSel) Color.White else ui.color, fontFamily = NunitoFontFamily)
                }
            }
        }

        if (submitted && bad) {
            Spacer(modifier = Modifier.height(7.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(modifier = Modifier.background(Color(0x26C4B5FD), RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 2.dp)) {
                    Text(text = "Đáp án: ${statementAnswerUi(statement.answer).label}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFC084FC), fontFamily = NunitoFontFamily)
                }
            }
        }
    }
}
