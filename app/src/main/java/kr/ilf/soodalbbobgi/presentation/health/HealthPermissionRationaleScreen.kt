package kr.ilf.soodalbbobgi.presentation.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.BackLink
import kr.ilf.soodalbbobgi.core.ui.ButtonStyle
import kr.ilf.soodalbbobgi.core.ui.SoodalButton
import kr.ilf.soodalbbobgi.core.ui.SoodalCard
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop

/**
 * Health Connect 권한 사용 근거 화면 — 앱이 읽는 건강 데이터와 용도, 보관 원칙, 철회 방법을 보여 준다.
 *
 * 시스템 설정 > 앱 권한이나 HC 권한 요청 화면의 "개인정보처리방침"에서 열리므로
 * 로그인 상태와 무관하게 그려진다. 레이아웃은 라이선스 화면과 같은 서브 화면 패턴이다.
 *
 * @param onBack 돌아가기 — 액티비티 종료
 * @param onOpenPrivacyPolicy 개인정보처리방침 전문(웹) 열기
 */
@Composable
fun HealthPermissionRationaleScreen(onBack: () -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .soodalScreenBackdrop()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackLink(onBack)
            Text(HealthPermissionRationaleCopy.TITLE, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
            Spacer(Modifier.width(80.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4)
                .padding(top = spacing.s4, bottom = spacing.s6)
                .navigationBarsPadding(),
        ) {
            Text(
                HealthPermissionRationaleCopy.INTRO,
                fontSize = 13.sp, color = colors.textSecondary, lineHeight = 20.sp,
            )

            Spacer(Modifier.height(spacing.s5))
            SectionLabel("읽어 오는 데이터")
            Spacer(Modifier.height(12.dp))
            SoodalCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HealthPermissionRationaleCopy.dataItems.forEach { item ->
                        Column {
                            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(Modifier.height(2.dp))
                            Text(item.description, fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.s5))
            SectionLabel("사용 목적")
            Spacer(Modifier.height(12.dp))
            BulletCard(HealthPermissionRationaleCopy.purposes)

            Spacer(Modifier.height(spacing.s5))
            SectionLabel("보관과 공유")
            Spacer(Modifier.height(12.dp))
            BulletCard(HealthPermissionRationaleCopy.storage)

            Spacer(Modifier.height(spacing.s5))
            SectionLabel("권한 철회")
            Spacer(Modifier.height(12.dp))
            BulletCard(HealthPermissionRationaleCopy.revoke)

            Spacer(Modifier.height(spacing.s5))
            SoodalButton(
                text = "개인정보처리방침 전문 보기",
                onClick = onOpenPrivacyPolicy,
                style = ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 설정 화면과 같은 섹션 라벨. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = SoodalDesign.colors.textSecondary,
        letterSpacing = 0.7.sp,
    )
}

/** 글머리 기호 문장 목록을 담은 카드. */
@Composable
private fun BulletCard(lines: List<String>) {
    val colors = SoodalDesign.colors
    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEach { line ->
                Row {
                    Text("•", fontSize = 12.sp, color = colors.textTertiary, lineHeight = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(line, fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
                }
            }
        }
    }
}
