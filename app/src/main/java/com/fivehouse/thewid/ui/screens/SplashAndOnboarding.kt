package com.fivehouse.thewid.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.components.DottedRing
import com.fivehouse.thewid.ui.components.Eyebrow
import com.fivehouse.thewid.ui.components.InfoChip
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.shapes.ClockMarks
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import kotlinx.coroutines.launch

private const val SPLASH_MILLIS = 1_400
private const val ONBOARDING_PAGES = 3

@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_MILLIS))
        onFinished()
    }
    SplashContent({ progress.value }, modifier)
}

@Composable
private fun SplashContent(progress: () -> Float, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            DottedRing(progress, Modifier.fillMaxSize())
            GlossyShape(ShapeCatalog.Cookie9Sided, null, Modifier.size(190.dp), castShadow = true)
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.product_name), style = MaterialTheme.typography.displayMedium)
        Text(stringResource(R.string.product_tagline), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Eyebrow(stringResource(R.string.product_made_with))
    }
}

@Composable
fun OnboardingScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { ONBOARDING_PAGES })
    val scope = rememberCoroutineScope()
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))
                when (page) {
                    0 -> RulesPage()
                    1 -> HeroPage()
                    else -> PrivacyPage()
                }
            }
        }
        OnboardingFooter(
            page = pager.currentPage,
            onNext = {
                if (pager.currentPage == ONBOARDING_PAGES - 1) {
                    onFinished()
                } else {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
        )
    }
}

@Composable
private fun OnboardingFooter(page: Int, onNext: () -> Unit, modifier: Modifier = Modifier) {
    if (page == ONBOARDING_PAGES - 1) {
        Column(
            modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PageDots(page)
            PrimaryPill(stringResource(R.string.onboarding_start), onNext)
        }
    } else {
        Row(
            modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageDots(page)
            Spacer(Modifier.weight(1f))
            FilledIconButton(
                onClick = onNext,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(painterResource(R.drawable.ic_arrow_forward), stringResource(R.string.action_next))
            }
        }
    }
}

@Composable
private fun PageDots(page: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(ONBOARDING_PAGES) { index ->
            Box(
                Modifier
                    .size(width = if (index == page) 16.dp else 4.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(LocalContentColor.current.copy(alpha = if (index == page) 1f else 0.3f))
            )
        }
    }
}

@Composable
private fun PageTitle(title: String, body: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.displaySmall)
        if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RuleSample(label: String, modifier: Modifier = Modifier, shape: @Composable () -> Unit) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shape()
        InfoChip(label)
    }
}

@Composable
private fun RulesPage(modifier: Modifier = Modifier) {
    Column(modifier) {
        Eyebrow(stringResource(R.string.onboarding1_eyebrow))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RuleSample(stringResource(R.string.onboarding_rule_fill)) {
                GlossyShape(ShapeCatalog.SoftBurst, null, Modifier.size(130.dp), fill = 0.5f)
            }
            RuleSample(stringResource(R.string.onboarding_rule_outline)) {
                GlossyShape(ShapeCatalog.Fan, null, Modifier.size(90.dp), surface = ShapeSurface.Dashed)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RuleSample(stringResource(R.string.onboarding_rule_morph)) {
                GlossyShape(ShapeCatalog.Flower, null, Modifier.size(110.dp))
            }
            RuleSample(stringResource(R.string.onboarding_rule_spikes)) {
                GlossyShape(ShapeCatalog.Burst, null, Modifier.size(90.dp), tint = GlossTint.Accent)
            }
        }
        Spacer(Modifier.height(24.dp))
        PageTitle(stringResource(R.string.onboarding1_title), stringResource(R.string.onboarding1_body))
    }
}

@Composable
private fun HeroPage(modifier: Modifier = Modifier) {
    Column(modifier) {
        Eyebrow(stringResource(R.string.onboarding2_eyebrow))
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            FloatingShape(ShapeCatalog.Cookie9Sided, Alignment.TopStart, 56)
            FloatingShape(ShapeCatalog.Puffy, Alignment.TopEnd, 48)
            FloatingShape(ShapeCatalog.PuffyDiamond, Alignment.BottomStart, 40)
            FloatingShape(ShapeCatalog.Clover4Leaf, Alignment.BottomEnd, 44)
            GlossyShape(
                shape = ShapeCatalog.Sunny,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(200.dp),
                tint = GlossTint.Sun,
                castShadow = true,
                clock = ClockMarks(hourDegrees = 291f, minuteDegrees = 252f)
            )
        }
        Spacer(Modifier.height(16.dp))
        PageTitle(stringResource(R.string.onboarding2_title), stringResource(R.string.onboarding2_body))
    }
}

@Composable
private fun BoxScope.FloatingShape(shape: ShapeCatalog, alignment: Alignment, sizeDp: Int) {
    GlossyShape(
        shape,
        null,
        Modifier
            .align(alignment)
            .offset(y = 8.dp)
            .size(sizeDp.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivacyPage(modifier: Modifier = Modifier) {
    Column(modifier) {
        Eyebrow(stringResource(R.string.onboarding3_eyebrow))
        Spacer(Modifier.height(24.dp))
        GlossyShape(
            shape = ShapeCatalog.Cookie9Sided,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(220.dp),
            castShadow = true
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_lock),
                    null,
                    Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PageTitle(stringResource(R.string.onboarding3_title), null)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(stringResource(R.string.onboarding3_no_account), icon = R.drawable.ic_check)
            InfoChip(stringResource(R.string.onboarding3_offline), icon = R.drawable.ic_check)
            InfoChip(stringResource(R.string.onboarding3_no_tracking), icon = R.drawable.ic_check)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SplashPreview() {
    ScreenPreview(Atmosphere.CoolMist) { SplashContent({ 0.7f }) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingPreview() {
    ScreenPreview(Atmosphere.Lilac) { OnboardingScreen(onFinished = {}) }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun OnboardingHeroPagePreview() {
    ScreenPreview(Atmosphere.Lilac) { Column(Modifier.padding(24.dp)) { HeroPage() } }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun OnboardingPrivacyPagePreview() {
    ScreenPreview(Atmosphere.Lilac) { Column(Modifier.padding(24.dp)) { PrivacyPage() } }
}
