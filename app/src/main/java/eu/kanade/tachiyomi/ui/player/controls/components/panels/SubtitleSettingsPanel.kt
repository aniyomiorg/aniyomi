/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player.controls.components.panels

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.kanade.tachiyomi.ui.player.controls.CARDS_MAX_WIDTH
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SubtitleSettingsPanel(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismissRequest)
    val orientation = LocalConfiguration.current.orientation

    ConstraintLayout(modifier = modifier.fillMaxSize()) {
        val subSettingsCards = createRef()
        val cards: @Composable (Int, Modifier) -> Unit = { value, cardsModifier ->
            when (value) {
                0 -> SubtitleSettingsTypographyCard(cardsModifier)
                1 -> SubtitleSettingsColorsCard(cardsModifier)
                2 -> SubtitlesMiscellaneousCard(cardsModifier)
                else -> {}
            }
        }

        val pagerState = rememberPagerState { 3 }
        if (orientation == ORIENTATION_PORTRAIT) {
            Column(
                modifier = Modifier.constrainAs(subSettingsCards) {
                    top.linkTo(parent.top, 32.dp)
                    start.linkTo(parent.start)
                },
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(AYMR.strings.player_sheets_subtitles_settings_title),
                            style = MaterialTheme.typography.headlineMedium.copy(shadow = Shadow(blurRadius = 20f)),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
                )
                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp * 0.9f),
                    verticalAlignment = Alignment.Top,
                    pageSpacing = MaterialTheme.padding.small,
                    contentPadding = PaddingValues(horizontal = MaterialTheme.padding.small),
                    beyondViewportPageCount = 1,
                ) { page ->
                    cards(page, Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                modifier = Modifier
                    .constrainAs(subSettingsCards) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end, 32.dp)
                    }
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .width(CARDS_MAX_WIDTH),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(AYMR.strings.player_sheets_subtitles_settings_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            shadow = Shadow(blurRadius = 20f),
                        ),
                    )
                    IconButton(onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }
                repeat(3) { cards(it, Modifier) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
