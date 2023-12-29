package eu.kanade.tachiyomi.util

import android.annotation.SuppressLint
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import eu.kanade.tachiyomi.databinding.PlayerActivityBinding
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import `is`.xyz.mpv.MPVLib
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy

class AniSkipApi {
    private val client = OkHttpClient()
    private val json: Json by injectLazy()

    // credits: Saikou
    fun getResult(malId: Int, episodeNumber: Int, episodeLength: Long): List<Stamp>? {
        val url =
            "https://api.aniskip.com/v2/skip-times/$malId/$episodeNumber?types[]=ed" +
                "&types[]=mixed-ed&types[]=mixed-op&types[]=op&types[]=recap&episodeLength=$episodeLength"
        return try {
            val a = client.newCall(GET(url)).execute().body.string()
            val res = json.decodeFromString<AniSkipResponse>(a)
            if (res.found) res.results else null
        } catch (e: Exception) {
            null
        }
    }

    fun getMalIdFromAL(id: Long): Long {
        val query = """
                query{
                Media(id:$id){idMal}
                }
        """.trimMargin()
        val response = try {
            client.newCall(
                POST(
                    "https://graphql.anilist.co",
                    body = buildJsonObject { put("query", query) }.toString()
                        .toRequestBody(jsonMime),
                ),
            ).execute()
        } catch (e: Exception) {
            return 0
        }
        return response.body.string().substringAfter("idMal\":").substringBefore("}")
            .toLongOrNull() ?: 0
    }

    class PlayerUtils(
        private val binding: PlayerActivityBinding,
        private val aniSkipResponse: List<Stamp>,
    ) {
        private val activity: PlayerActivity get() = binding.root.context as PlayerActivity

        internal fun showSkipButton(skipType: SkipType) {
            val skipButtonString = when (skipType) {
                SkipType.ED -> MR.strings.player_aniskip_ed
                SkipType.OP -> MR.strings.player_aniskip_op
                SkipType.RECAP -> MR.strings.player_aniskip_recap
                SkipType.MIXED_OP -> MR.strings.player_aniskip_mixedOp
            }
            activity.viewModel.updateSkipIntroText(activity.stringResource(skipButtonString))
        }

        // this is used when netflixStyle is enabled
        @SuppressLint("SetTextI18n")
        fun showSkipButton(skipType: SkipType, waitingTime: Int) {
            val skipTime = when (skipType) {
                SkipType.ED -> aniSkipResponse.first { it.skipType == SkipType.ED }.interval
                SkipType.OP -> aniSkipResponse.first { it.skipType == SkipType.OP }.interval
                SkipType.RECAP -> aniSkipResponse.first { it.skipType == SkipType.RECAP }.interval
                SkipType.MIXED_OP -> aniSkipResponse.first { it.skipType == SkipType.MIXED_OP }.interval
            }
            if (waitingTime > -1) {
                if (waitingTime > 0) {
                    activity.viewModel.updateSkipIntroText(activity.stringResource(MR.strings.player_aniskip_dontskip))
                } else {
                    seekTo(skipTime.endTime)
                    skipAnimation(skipType)
                }
            } else {
                // when waitingTime is -1, it means that the user cancelled the skip
                showSkipButton(skipType)
            }
        }

        fun skipAnimation(skipType: SkipType) {
            binding.secondsView.binding.doubleTapSeconds.text = activity.stringResource(
                MR.strings.player_aniskip_skip,
                skipType.getString(),
            )

            binding.secondsView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                leftToLeft = ConstraintLayout.LayoutParams.UNSET
            }
            binding.secondsView.visibility = View.VISIBLE
            binding.secondsView.isForward = true

            binding.ffwdBg.visibility = View.VISIBLE
            binding.ffwdBg.animate().alpha(0.15f).setDuration(100).withEndAction {
                binding.secondsView.animate().alpha(1f).setDuration(500).withEndAction {
                    binding.secondsView.animate().alpha(0f).setDuration(500).withEndAction {
                        binding.ffwdBg.animate().alpha(0f).setDuration(100).withEndAction {
                            binding.ffwdBg.visibility = View.GONE
                            binding.secondsView.visibility = View.GONE
                            binding.secondsView.alpha = 1f
                        }
                    }
                }
            }.start()
        }

        private fun seekTo(time: Double) {
            MPVLib.command(arrayOf("seek", time.toString(), "absolute"))
        }
    }
}

@Serializable
data class AniSkipResponse(
    val found: Boolean,
    val results: List<Stamp>?,
    val message: String?,
    val statusCode: Int,
)

@Serializable
data class Stamp(
    val interval: AniSkipInterval,
    val skipType: SkipType,
    val skipId: String,
    val episodeLength: Double,
)

@Serializable
enum class SkipType {
    @SerialName("op")
    OP,

    @SerialName("ed")
    ED,

    @SerialName("recap")
    RECAP,

    @SerialName("mixed-op")
    MIXED_OP, ;

    fun getString(): String {
        return when (this) {
            OP -> "Opening"
            ED -> "Ending"
            RECAP -> "Recap"
            MIXED_OP -> "Mixed-op"
        }
    }
}

@Serializable
data class AniSkipInterval(
    val startTime: Double,
    val endTime: Double,
)
