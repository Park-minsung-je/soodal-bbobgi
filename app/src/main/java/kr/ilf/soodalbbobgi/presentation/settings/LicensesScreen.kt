package kr.ilf.soodalbbobgi.presentation.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import kr.ilf.soodalbbobgi.R
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop
import kr.ilf.soodalbbobgi.core.ui.SoodalCard
import kr.ilf.soodalbbobgi.core.ui.BackLink

/**
 * 오픈소스 라이선스 목록 화면 — AboutLibraries가 빌드 시 수집한
 * 의존성 메타데이터(raw/aboutlibraries.json)를 앱 디자인으로 렌더한다.
 * 행을 탭하면 라이선스 본문이 펼쳐진다.
 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val context = LocalContext.current

    val libraries = remember {
        val json = context.resources.openRawResource(R.raw.aboutlibraries)
            .bufferedReader().use { it.readText() }
        Libs.Builder().withJson(json).build()
            .libraries.sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .soodalScreenBackdrop()
            .statusBarsPadding(),
    ) {
        // -- Header (SettingsScreen과 동일 패턴) --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackLink(onBack)
            Text("오픈소스 라이선스", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
            Spacer(Modifier.width(80.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.s4, end = spacing.s4, top = spacing.s4, bottom = spacing.s6,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(libraries, key = { it.uniqueId }) { lib ->
                LibraryRow(lib)
            }
        }
    }
}

/** 라이브러리 한 줄 — 탭하면 라이선스 본문 펼침/접힘. */
@Composable
private fun LibraryRow(lib: Library) {
    val colors = SoodalDesign.colors
    var expanded by remember { mutableStateOf(false) }

    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(onClick = { expanded = !expanded }),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(lib.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    val licenseNames = lib.licenses.joinToString { it.name }
                    Text(
                        text = listOfNotNull(lib.artifactVersion, licenseNames.ifBlank { null }).joinToString(" · "),
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                    )
                }
                Text(if (expanded) "▾" else "▸", fontSize = 13.sp, color = colors.textTertiary)
            }
            if (expanded) {
                val content = lib.licenses.firstOrNull()?.licenseContent
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (content.isNullOrBlank()) "라이선스 본문이 제공되지 않았어요." else content,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
