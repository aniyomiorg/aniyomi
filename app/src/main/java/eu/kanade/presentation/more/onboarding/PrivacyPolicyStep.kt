package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

class PrivacyPolicyStep : OnboardingStep {
    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    @Composable
    override fun Content() {
        var isAgreed by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()

        val isScrolledToEnd = remember(scrollState) {
            derivedStateOf {
                val maxScroll = scrollState.maxValue
                scrollState.value >= maxScroll
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .height(500.dp)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
            ) {
                Column {
                    PrivacyPolicyIntro()
                    PrivacyInformationCollectionUse()
                }
            }

            // Agreement Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(MR.strings.onboarding_privacy_agree_to_terms),
                    style = TextStyle(
                        fontSize = 18.sp,
                    ),
                )
                Checkbox(
                    enabled = isScrolledToEnd.value,
                    checked = isAgreed,
                    onCheckedChange = {
                        isAgreed = it
                        _isComplete = it
                    },
                )
            }
        }
    }

    @Composable
    fun HeadingText(text: String, modifier: Modifier = Modifier) {
        Text(
            modifier = modifier,
            text = text,
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
    }

    @Composable
    fun BodyText(text: String, modifier: Modifier = Modifier) {
        Text(
            modifier = modifier,
            text = text,
            style = TextStyle(fontSize = 16.sp),
        )
    }

    @Composable
    fun HorizontalDivider() {
        HorizontalDivider(color = Color.Gray)
    }

    @Composable
    fun Spacer(size: Int, modifier: Modifier = Modifier) {
        Spacer(modifier = modifier.height(size.dp))
    }

    @Composable
    fun BulletPoint(text: String, url: String, modifier: Modifier = Modifier) {
        val uriHandler = LocalUriHandler.current

        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xFF0645AD), fontSize = 16.sp)) {
                append("• $text")
            }
            addStringAnnotation(
                tag = "URL",
                annotation = url,
                start = 2,
                end = 2 + text.length,
            )
        }

        ClickableText(
            modifier = modifier,
            text = annotatedString,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        uriHandler.openUri(annotation.item)
                    }
            },
            style = TextStyle(fontSize = 16.sp),
        )
    }

    @Composable
    fun MoreInfoText(url: String, modifier: Modifier = Modifier) {
        val uriHandler = LocalUriHandler.current

        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xFF0645AD), fontSize = 16.sp)) {
                append(stringResource(MR.strings.learn_more))
            }
            addStringAnnotation(
                tag = "URL",
                annotation = url,
                start = 0,
                end = stringResource(MR.strings.learn_more).length,
            )
        }

        ClickableText(
            text = annotatedString,
            modifier = modifier,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        uriHandler.openUri(annotation.item)
                    }
            },
            style = TextStyle(fontSize = 16.sp),
        )
    }

    @Composable
    fun PrivacyPolicyIntro(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            HeadingText(stringResource(MR.strings.privacy_policy))
            Spacer(20)

            BodyText(stringResource(MR.strings.onboarding_privacy_intro))
            Spacer(10)

            BodyText(stringResource(MR.strings.onboarding_privacy_info_overview))
            Spacer(10)

            BodyText(stringResource(MR.strings.onboarding_privacy_agreement_acknowledgement))
            Spacer(20)

            HorizontalDivider()
            Spacer(20)
        }
    }

    @Composable
    fun PrivacyInformationCollectionUse(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            HeadingText(stringResource(MR.strings.onboarding_privacy_information_collection_use))
            Spacer(20)

            BodyText(stringResource(MR.strings.onboarding_privacy_information_collection_use_intro))
            Spacer(10)

            BodyText(stringResource(MR.strings.onboarding_privacy_information_collection_use_data))
            Spacer(10)

            Spacer(20)
            HorizontalDivider()
            Spacer(20)

            MoreInfoText("https://aniyomi.org/privacy/")
        }
    }
}
