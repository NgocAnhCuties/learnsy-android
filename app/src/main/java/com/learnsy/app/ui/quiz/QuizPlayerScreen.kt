package com.learnsy.app.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.learnsy.app.ui.dashboard.DashboardIcon
import com.learnsy.app.ui.theme.NunitoFontFamily

/**
 * ── QuizPlayerScreen ──
 * Tương đương function QuizPlayer({lesson,onBack,dark,setDark,onSaveHistory})
 * trong quiz-player.jsx — điểm ghép nối chính toàn bộ màn hình làm quiz.
 *
 * Vuốt trái/phải để chuyển câu dùng detectHorizontalDragGestures (thay
 * touchstart/touchmove/touchend thủ công của bản web). Bàn phím vật lý
 * (A/B/C/D, mũi tên) không convert vì Android chủ yếu dùng cảm ứng.
 *
 * CHƯA convert: Export bottom sheet (đã bỏ theo yêu cầu), lưu kết quả lên
 * Supabase thật trong onSaveHistory (hiện chỉ nhận HistoryRecord, nơi gọi
 * màn hình này cần tự lưu Supabase + cập nhật Dashboard history list).
 */
@Composable
fun QuizPlayerScreen(
    lesson: Lesson,
    dark: Boolean,
    onToggleDark: () -> Unit,
    onBack: () -> Unit,
    onSaveHistory: (HistoryRecord, List<PerQuestionResult>) -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    val C = quizColors(dark)
    val questions by viewModel.questions.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val flags by viewModel.flags.collectAsState()
    val cur by viewModel.cur.collectAsState()
    val submitted by viewModel.submitted.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val bestStreak by viewModel.bestStreak.collectAsState()
    val answerTimesSec by viewModel.answerTimesSec.collectAsState()
    val scoreModalVisible by viewModel.scoreModalVisible.collectAsState()
    val warnModal by viewModel.warnModal.collectAsState()
    val showStats by viewModel.showStats.collectAsState()
    val confettiOn by viewModel.confettiOn.collectAsState()
    val practiceMode by viewModel.practiceMode.collectAsState()
    val bgmOn by viewModel.bgmOn.collectAsState()
    val muted by viewModel.muted.collectAsState()
    val (score, total) = viewModel.score.collectAsState().value

    LaunchedEffect(lesson) {
        viewModel.initQuiz(lesson, onSaveHistory)
    }

    if (questions.isEmpty()) return
    val q = questions.getOrNull(cur) ?: return

    val pct = if (total > 0) score.toDouble() / total else 0.0
    val rc = resultColor(pct)

    val answeredCur = remember(cur, answers) {
        when (q.type) {
            QuestionType.MULTIPLE -> (answers.getOrNull(cur) as? Answer.Single)?.index != null
            QuestionType.MULTI_SELECT -> (answers.getOrNull(cur) as? Answer.Multi)?.indices?.isNotEmpty() == true
            QuestionType.FILL_BLANK -> !(answers.getOrNull(cur) as? Answer.Text)?.value.isNullOrBlank()
            QuestionType.TRUE_FALSE -> (answers.getOrNull(cur) as? Answer.TrueFalseAnswer)?.values?.any { it != null } == true
        }
    }
    val questionRevealed = submitted || (practiceMode && answeredCur)

    val dotStates = remember(answers, submitted, practiceMode) {
        questions.mapIndexed { i, question ->
            val ans = answers.getOrElse(i) { Answer.Empty }
            val isAnswered = when (question.type) {
                QuestionType.MULTIPLE -> (ans as? Answer.Single)?.index != null
                QuestionType.MULTI_SELECT -> (ans as? Answer.Multi)?.indices?.isNotEmpty() == true
                QuestionType.FILL_BLANK -> !(ans as? Answer.Text)?.value.isNullOrBlank()
                QuestionType.TRUE_FALSE -> (ans as? Answer.TrueFalseAnswer)?.values?.none { it == null } == true
            }
            if (!isAnswered) DotState.UNANSWERED
            else if (submitted) {
                if (isAnswerCorrect(question, ans)) DotState.CORRECT else DotState.WRONG
            } else DotState.ANSWERED
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ══════ Header ══════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.headerBg)
                    .padding(horizontal = 15.dp, vertical = 11.dp)
            ) {
                if (!submitted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        timeLeft?.let { TimerBadge(secondsLeft = it) }
                        if (streak >= 2 && practiceMode) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0x1AFCD34D), RoundedCornerShape(50))
                                    .border(1.5.dp, Color(0x66FCD34D), RoundedCornerShape(50))
                                    .padding(horizontal = 11.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DashboardIcon(name = "fire", size = 11.dp, color = Color(0xFFF59E0B))
                                Text(text = " $streak liên tiếp", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFC89700), fontFamily = NunitoFontFamily)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .background(if (practiceMode) Color(0x1F6EE7B7) else C.navBtn, RoundedCornerShape(50))
                                .border(1.5.dp, if (practiceMode) Color(0x736EE7B7) else C.navBtnBorder, RoundedCornerShape(50))
                                .clickable { viewModel.togglePracticeMode() }
                                .padding(horizontal = 11.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DashboardIcon(name = "book", size = 12.dp, color = if (practiceMode) Color(0xFF10B981) else C.navBtnText)
                            Text(
                                text = " " + (if (practiceMode) "Luyện tập" else "Thi cử"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (practiceMode) Color(0xFF10B981) else C.navBtnText,
                                fontFamily = NunitoFontFamily
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    HeaderIconButton(icon = "chevronLeft", onClick = onBack, colors = C, label = "Quay lại", showArrow = true)

                    Text(
                        text = lesson.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = C.text,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = NunitoFontFamily,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        HeaderIconButton(icon = if (muted) "sad" else "sparkle", onClick = { viewModel.toggleMuted() }, colors = C)
                        HeaderIconButton(
                            icon = "book",
                            onClick = { viewModel.toggleBgm() },
                            colors = C,
                            activeColor = if (bgmOn) Color(0xFF10B981) else null
                        )
                        HeaderIconButton(icon = if (dark) "sun" else "moon", onClick = onToggleDark, colors = C)
                    }
                }

                if (submitted) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .background(Brush.linearGradient(listOf(rc, rc.copy(alpha = 0.7f))), RoundedCornerShape(50))
                                .padding(horizontal = 13.dp, vertical = 5.dp)
                        ) {
                            Text(text = "${fmtS(pct * 10)}/10", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = NunitoFontFamily)
                        }
                    }
                }

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(C.progressBg, RoundedCornerShape(99))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((cur + 1) / questions.size.coerceAtLeast(1).toFloat())
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFF472B6), Color(0xFFA855F7), Color(0xFF818CF8))),
                                RoundedCornerShape(99)
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val info = questionTypeInfo(q.type)
                    Box(
                        modifier = Modifier
                            .background(C.optBg, RoundedCornerShape(50))
                            .border(1.dp, info.color.copy(alpha = 0.13f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(text = info.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = info.color, fontFamily = NunitoFontFamily)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(
                            modifier = Modifier
                                .clickable { viewModel.toggleFlag(cur) },
                        ) {
                            DashboardIcon(name = "star", size = 14.dp, color = if (flags.getOrElse(cur) { false }) Color(0xFFF87171) else C.textMid)
                        }
                        Text(text = "${cur + 1} / ${questions.size}", fontSize = 11.sp, color = C.textMid, fontWeight = FontWeight.Bold, fontFamily = NunitoFontFamily)
                    }
                }
            }

            // ══════ Question area (scroll + swipe) ══════
            var dragOffsetX by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .pointerInput(submitted) {
                        if (submitted) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffsetX <= -64f && cur < questions.size - 1) viewModel.goNext()
                                else if (dragOffsetX >= 64f && cur > 0) viewModel.goPrev()
                                dragOffsetX = 0f
                            }
                        ) { _, dragAmount ->
                            dragOffsetX += dragAmount
                        }
                    }
                    .padding(14.dp)
            ) {
                AnimatedContent(
                    targetState = cur,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                    label = "questionContent"
                ) { _ ->
                    Column {
                        if (q.type == QuestionType.TRUE_FALSE) {
                            if (q.passage.isNotBlank()) {
                                PassageCard(passage = q.passage, source = q.source.ifBlank { null }, dark = dark)
                                Spacer(modifier = Modifier.height(13.dp))
                            }
                            TrueFalseQuestionView(
                                question = q,
                                values = (answers.getOrNull(cur) as? Answer.TrueFalseAnswer)?.values ?: q.items.map { null as Boolean? },
                                submitted = submitted,
                                practiceMode = practiceMode,
                                dark = dark,
                                onValueChange = { ii, v ->
                                    if (submitted) return@TrueFalseQuestionView
                                    val cur0 = (answers.getOrNull(cur) as? Answer.TrueFalseAnswer)?.values?.toMutableList()
                                        ?: q.items.map { null as Boolean? }.toMutableList()
                                    cur0[ii] = v
                                    viewModel.setAnswer(cur, Answer.TrueFalseAnswer(cur0))
                                }
                            )
                        } else {
                            QuestionTextCard(question = q, dark = dark)
                            Spacer(modifier = Modifier.height(13.dp))
                            when (q.type) {
                                QuestionType.FILL_BLANK -> FillBlankQuestionView(
                                    question = q,
                                    value = (answers.getOrNull(cur) as? Answer.Text)?.value ?: "",
                                    submitted = submitted,
                                    revealed = questionRevealed,
                                    dark = dark,
                                    onValueChange = { viewModel.setAnswer(cur, Answer.Text(it)) },
                                    onSubmitNext = {
                                        viewModel.playClickSound()
                                        if (cur < questions.size - 1) viewModel.goNext() else viewModel.handleSubmit()
                                    }
                                )
                                else -> ChoiceQuestionView(
                                    question = q,
                                    singleSelected = (answers.getOrNull(cur) as? Answer.Single)?.index,
                                    multiSelected = (answers.getOrNull(cur) as? Answer.Multi)?.indices ?: emptyList(),
                                    submitted = submitted,
                                    revealed = questionRevealed,
                                    dark = dark,
                                    onSingleChange = { viewModel.setAnswer(cur, Answer.Single(it)) },
                                    onMultiChange = { viewModel.setAnswer(cur, Answer.Multi(it)) }
                                )
                            }
                        }

                        if (questionRevealed && !q.explanation.isNullOrBlank()) {
                            ExplanationCard(explanation = q.explanation, dark = dark)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NavArrowButton(enabled = cur > 0, direction = -1) { viewModel.goPrev() }
                            Box(modifier = Modifier.weight(1f)) {
                                NavDots(total = questions.size, current = cur, states = dotStates, flags = flags, dark = dark, onDotClick = { viewModel.navTo(it) })
                            }
                            NavArrowButton(enabled = cur < questions.size - 1, direction = 1) { viewModel.goNext() }
                        }
                    }
                }
            }

            // ══════ Submit bar ══════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.stickyBg)
                    .padding(horizontal = 15.dp, vertical = 14.dp)
            ) {
                if (!submitted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFA855F7))), RoundedCornerShape(50))
                            .clickable { viewModel.handleSubmit() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DashboardIcon(name = "heart", size = 14.dp, color = Color.White)
                            Text(text = "Nộp bài", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = NunitoFontFamily)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, Color(0x47FF96C8), RoundedCornerShape(50))
                                .clickable { viewModel.resetQuiz() }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Làm lại", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBAFCE), fontFamily = NunitoFontFamily)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF6EE7B7))), RoundedCornerShape(50))
                                .clickable { viewModel.openStats() }
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Thống kê", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = NunitoFontFamily)
                        }
                    }
                }
            }
        }

        // ══════ Confetti overlay ══════
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            ConfettiCanvas(
                active = confettiOn,
                widthPx = with(density) { maxWidth.toPx() },
                heightPx = with(density) { maxHeight.toPx() }
            )
        }

        // ══════ Modals ══════
        StatsModal(
            visible = showStats && submitted,
            score = score,
            total = total,
            pct = pct,
            bestStreak = bestStreak,
            answerTimesSec = answerTimesSec,
            questions = questions,
            answers = answers,
            dark = dark,
            onDismiss = { viewModel.closeStats() },
            onRetry = { viewModel.resetQuiz() }
        )

        WarnModal(
            unansweredQuestionNumbers = warnModal,
            dark = dark,
            onReview = { viewModel.dismissWarnAndReview() },
            onSubmitAnyway = { viewModel.dismissWarnAndSubmitAnyway() },
            onDismiss = { }
        )

        val correctCount = questions.count { isAnswerCorrect(it, answers.getOrElse(questions.indexOf(it)) { Answer.Empty }) }
        ScoreIsland(
            visible = scoreModalVisible,
            pct = pct,
            correctCount = score,
            wrongCount = total - score,
            onClose = { viewModel.closeScoreModal() }
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: String,
    onClick: () -> Unit,
    colors: QuizColors,
    label: String? = null,
    showArrow: Boolean = false,
    activeColor: Color? = null
) {
    if (label != null) {
        Row(
            modifier = Modifier
                .background(colors.navBtn, RoundedCornerShape(50))
                .border(1.5.dp, colors.navBtnBorder, RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showArrow) DashboardIcon(name = icon, size = 11.dp, color = colors.navBtnText)
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = colors.navBtnText, fontFamily = NunitoFontFamily)
        }
    } else {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(colors.navBtn, CircleShape)
                .border(1.5.dp, activeColor?.copy(alpha = 0.5f) ?: colors.navBtnBorder, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            DashboardIcon(name = icon, size = 13.dp, color = activeColor ?: colors.navBtnText)
        }
    }
}

@Composable
private fun NavArrowButton(enabled: Boolean, direction: Int, onClick: () -> Unit) {
    val C = quizColors(false) // màu nav dùng chung, alpha xử lý riêng qua enabled
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(C.navBtn, CircleShape)
            .border(1.5.dp, C.navBtnBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DashboardIcon(
            name = if (direction < 0) "chevronLeft" else "chevronRight",
            size = 13.dp,
            color = C.navBtnText.copy(alpha = if (enabled) 1f else 0.28f)
        )
    }
}
