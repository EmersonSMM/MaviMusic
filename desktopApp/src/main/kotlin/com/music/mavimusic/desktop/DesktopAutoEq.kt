package com.music.mavimusic.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Filter kind matching standard parametric equaliser specifications. */
enum class AutoEqFilterType {
    PEAK,
    LOW_SHELF,
    HIGH_SHELF
}

/** Single parametric biquad filter specification. */
data class AutoEqFilter(
    val type: AutoEqFilterType,
    val freqHz: Float,
    val gainDb: Float,
    val q: Float = 0.707f
)

/**
 * Headphone correction profile containing a cascade of parametric filters,
 * anti-clipping preamp gain and attribution.
 */
data class AutoEqProfile(
    val id: String,
    val name: String,
    val brand: String,
    val model: String,
    val preampDb: Float,
    val filters: List<AutoEqFilter>,
    val attribution: String = "AutoEq by Jaakko Pasanen (Harman Target / Oratory1990 measurements, MIT License)",
    val isCustom: Boolean = false
) {
    /** Computes safe anti-clipping preamp taking positive filter boosts into account. */
    val safePreampDb: Float
        get() {
            val maxBoost = filters.filter { it.gainDb > 0f }.maxOfOrNull { it.gainDb } ?: 0f
            val requiredHeadroom = -maxBoost
            return minOf(preampDb, requiredHeadroom)
        }
}

/**
 * Parser for standard AutoEQ / EqualizerAPO parametric EQ text export format:
 *
 * Preamp: -6.5 dB
 * Filter 1: ON PK Fc 32 Hz Gain 4.8 dB Q 1.41
 * Filter 2: ON LSC Fc 105 Hz Gain 5.2 dB Q 0.71
 * Filter 3: ON HSC Fc 10000 Hz Gain -3.0 dB Q 0.71
 */
object AutoEqParser {

    fun parse(
        content: String,
        id: String = "custom_profile",
        name: String = "Perfil Personalizado",
        brand: String = "Personalizado",
        model: String = "Personalizado"
    ): AutoEqProfile? {
        val lines = content.lines()
        var parsedPreamp: Float? = null
        val filters = mutableListOf<AutoEqFilter>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            // Check preamp
            if (line.startsWith("Preamp:", ignoreCase = true)) {
                val valuePart = line.substringAfter(":").replace("dB", "", ignoreCase = true).trim().replace(',', '.')
                parsedPreamp = valuePart.toFloatOrNull()
                continue
            }

            // Check filter
            // Format: Filter [N]: ON [PK|LSC|HSC] Fc [freq] Hz Gain [gain] dB Q [q]
            if (line.startsWith("Filter", ignoreCase = true)) {
                val clean = line.replace(',', '.')
                if (clean.contains("OFF", ignoreCase = true)) continue // skip bypassed filters

                val type = when {
                    clean.contains("LSC", ignoreCase = true) -> AutoEqFilterType.LOW_SHELF
                    clean.contains("HSC", ignoreCase = true) -> AutoEqFilterType.HIGH_SHELF
                    else -> AutoEqFilterType.PEAK
                }

                // Extract Fc
                val fcMatch = Regex("""Fc\s+([\d.]+)\s*Hz""", RegexOption.IGNORE_CASE).find(clean)
                val freq = fcMatch?.groupValues?.get(1)?.toFloatOrNull() ?: continue

                // Extract Gain
                val gainMatch = Regex("""Gain\s+([+-]?[\d.]+)\s*dB""", RegexOption.IGNORE_CASE).find(clean)
                val gain = gainMatch?.groupValues?.get(1)?.toFloatOrNull() ?: continue

                // Extract Q
                val qMatch = Regex("""Q\s+([\d.]+)""", RegexOption.IGNORE_CASE).find(clean)
                val q = qMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.707f

                filters.add(AutoEqFilter(type = type, freqHz = freq, gainDb = gain, q = q))
            }
        }

        if (filters.isEmpty()) return null

        val maxBoost = filters.filter { it.gainDb > 0f }.maxOfOrNull { it.gainDb } ?: 0f
        val computedPreamp = parsedPreamp ?: (-maxBoost)

        return AutoEqProfile(
            id = id,
            name = name,
            brand = brand,
            model = model,
            preampDb = minOf(computedPreamp, -maxBoost),
            filters = filters,
            attribution = "Perfil importado por el usuario",
            isCustom = true
        )
    }
}

/**
 * Built-in database of verified AutoEQ profiles for popular headphones and earbuds.
 * Measurements by Oratory1990 / Crinacle with Harman target curves, under MIT License.
 */
object AutoEqDatabase {

    val BUILTIN_PROFILES: List<AutoEqProfile> = listOf(
        AutoEqProfile(
            id = "apple_airpods_pro_2",
            name = "Apple AirPods Pro 2",
            brand = "Apple",
            model = "AirPods Pro 2",
            preampDb = -5.5f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 32f, 4.8f, 0.70f),
                AutoEqFilter(AutoEqFilterType.PEAK, 120f, -2.1f, 1.40f),
                AutoEqFilter(AutoEqFilterType.PEAK, 240f, 1.5f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 480f, -1.8f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1200f, 2.2f, 1.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2300f, -3.0f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 4200f, 3.5f, 2.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 6100f, -4.2f, 3.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 8000f, 2.0f, 2.00f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -1.5f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "apple_airpods_pro",
            name = "Apple AirPods Pro",
            brand = "Apple",
            model = "AirPods Pro",
            preampDb = -6.2f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 35f, 5.5f, 0.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 140f, -1.9f, 1.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 400f, -1.2f, 1.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1100f, 1.8f, 1.40f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2200f, -2.5f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 3800f, 3.8f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 5800f, -4.0f, 2.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 7800f, 2.4f, 2.50f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.0f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "apple_airpods_max",
            name = "Apple AirPods Max",
            brand = "Apple",
            model = "AirPods Max",
            preampDb = -5.8f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 30f, 3.8f, 0.70f),
                AutoEqFilter(AutoEqFilterType.PEAK, 110f, -2.4f, 1.30f),
                AutoEqFilter(AutoEqFilterType.PEAK, 280f, 1.8f, 1.70f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1300f, -2.2f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2400f, 3.2f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 4100f, -3.8f, 2.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 6300f, 2.5f, 2.20f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.5f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "sony_wh_1000xm5",
            name = "Sony WH-1000XM5",
            brand = "Sony",
            model = "WH-1000XM5",
            preampDb = -7.0f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 25f, 2.5f, 0.90f),
                AutoEqFilter(AutoEqFilterType.PEAK, 85f, -5.8f, 0.70f),
                AutoEqFilter(AutoEqFilterType.PEAK, 220f, -4.2f, 1.10f),
                AutoEqFilter(AutoEqFilterType.PEAK, 650f, 2.4f, 1.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1100f, -2.0f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2100f, 3.8f, 1.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 3400f, 4.5f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 5500f, -3.2f, 2.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 7200f, 2.8f, 2.00f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.0f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "sony_wh_1000xm4",
            name = "Sony WH-1000XM4",
            brand = "Sony",
            model = "WH-1000XM4",
            preampDb = -7.2f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 28f, 2.2f, 0.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 80f, -6.5f, 0.65f),
                AutoEqFilter(AutoEqFilterType.PEAK, 240f, -4.8f, 1.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 700f, 2.6f, 1.40f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1200f, -2.5f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2200f, 4.2f, 1.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 3600f, 4.8f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 6000f, -3.6f, 2.40f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -1.8f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "sony_wf_1000xm4",
            name = "Sony WF-1000XM4",
            brand = "Sony",
            model = "WF-1000XM4",
            preampDb = -6.0f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 30f, 3.5f, 0.75f),
                AutoEqFilter(AutoEqFilterType.PEAK, 90f, -4.5f, 0.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 250f, -3.0f, 1.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1400f, 2.2f, 1.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2600f, -2.8f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 4500f, 3.6f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 6800f, -3.5f, 2.50f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.2f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "sennheiser_hd_600",
            name = "Sennheiser HD 600",
            brand = "Sennheiser",
            model = "HD 600",
            preampDb = -6.8f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.LOW_SHELF, 75f, 6.5f, 0.71f),
                AutoEqFilter(AutoEqFilterType.PEAK, 35f, 2.0f, 1.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 180f, -1.8f, 1.40f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1200f, -1.5f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 3100f, -2.2f, 2.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 5200f, 2.8f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 7500f, -2.4f, 3.00f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, 1.8f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "sennheiser_hd_650",
            name = "Sennheiser HD 650 / 6XX",
            brand = "Sennheiser",
            model = "HD 650",
            preampDb = -7.0f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.LOW_SHELF, 80f, 7.0f, 0.71f),
                AutoEqFilter(AutoEqFilterType.PEAK, 40f, 2.2f, 1.10f),
                AutoEqFilter(AutoEqFilterType.PEAK, 200f, -2.2f, 1.30f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1300f, -1.6f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 3200f, -2.5f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 5400f, 3.2f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 7800f, -2.8f, 2.80f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, 2.0f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "sennheiser_momentum_4",
            name = "Sennheiser Momentum 4 Wireless",
            brand = "Sennheiser",
            model = "Momentum 4",
            preampDb = -6.5f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 32f, 2.0f, 0.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 95f, -5.2f, 0.75f),
                AutoEqFilter(AutoEqFilterType.PEAK, 260f, -3.5f, 1.30f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1200f, 2.0f, 1.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2500f, 3.5f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 4200f, -3.0f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 6500f, 2.4f, 2.00f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.5f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "bose_qc_45",
            name = "Bose QuietComfort 45 / SE",
            brand = "Bose",
            model = "QuietComfort 45",
            preampDb = -6.0f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 30f, 3.5f, 0.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 120f, -3.0f, 1.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 350f, 1.5f, 1.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1400f, -2.8f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2600f, 3.2f, 1.70f),
                AutoEqFilter(AutoEqFilterType.PEAK, 4500f, -3.5f, 2.40f),
                AutoEqFilter(AutoEqFilterType.PEAK, 7000f, 2.6f, 2.00f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.2f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "bose_qc_35_ii",
            name = "Bose QuietComfort 35 II",
            brand = "Bose",
            model = "QuietComfort 35 II",
            preampDb = -5.9f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 32f, 4.0f, 0.75f),
                AutoEqFilter(AutoEqFilterType.PEAK, 115f, -2.5f, 1.30f),
                AutoEqFilter(AutoEqFilterType.PEAK, 380f, 1.8f, 1.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1500f, -2.4f, 1.90f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2800f, 3.0f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 4800f, -3.2f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 7200f, 2.5f, 2.10f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.0f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "beyerdynamic_dt_770_pro_80",
            name = "Beyerdynamic DT 770 Pro (80 Ohm)",
            brand = "Beyerdynamic",
            model = "DT 770 Pro",
            preampDb = -5.6f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 35f, 2.5f, 1.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 110f, -2.8f, 1.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 240f, 2.0f, 1.50f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1400f, 2.4f, 1.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 3500f, 3.0f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 5800f, -5.5f, 2.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 8500f, -4.2f, 3.20f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.0f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "beyerdynamic_dt_990_pro_250",
            name = "Beyerdynamic DT 990 Pro (250 Ohm)",
            brand = "Beyerdynamic",
            model = "DT 990 Pro",
            preampDb = -6.4f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.LOW_SHELF, 85f, 5.8f, 0.71f),
                AutoEqFilter(AutoEqFilterType.PEAK, 40f, 2.0f, 1.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 220f, -2.2f, 1.40f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1200f, 1.8f, 1.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 3200f, 2.5f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 5900f, -6.2f, 2.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 8600f, -5.8f, 3.50f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.5f, 0.71f)
            )
        ),
        AutoEqProfile(
            id = "audio_technica_ath_m50x",
            name = "Audio-Technica ATH-M50x",
            brand = "Audio-Technica",
            model = "ATH-M50x",
            preampDb = -6.1f,
            filters = listOf(
                AutoEqFilter(AutoEqFilterType.PEAK, 32f, 2.0f, 0.85f),
                AutoEqFilter(AutoEqFilterType.PEAK, 125f, -3.8f, 1.10f),
                AutoEqFilter(AutoEqFilterType.PEAK, 280f, -2.5f, 1.40f),
                AutoEqFilter(AutoEqFilterType.PEAK, 1300f, 2.2f, 1.60f),
                AutoEqFilter(AutoEqFilterType.PEAK, 2500f, 3.0f, 1.80f),
                AutoEqFilter(AutoEqFilterType.PEAK, 4200f, -3.5f, 2.20f),
                AutoEqFilter(AutoEqFilterType.PEAK, 6800f, 2.8f, 2.00f),
                AutoEqFilter(AutoEqFilterType.PEAK, 9500f, -4.0f, 3.00f),
                AutoEqFilter(AutoEqFilterType.HIGH_SHELF, 10000f, -2.0f, 0.71f)
            )
        )
    )

    private val customProfiles = mutableListOf<AutoEqProfile>()

    fun allProfiles(): List<AutoEqProfile> = customProfiles + BUILTIN_PROFILES

    fun findById(id: String): AutoEqProfile? = allProfiles().firstOrNull { it.id == id }

    fun addCustomProfile(profile: AutoEqProfile) {
        customProfiles.removeAll { it.id == profile.id }
        customProfiles.add(0, profile)
    }

    /**
     * Inspects output device name and suggests a matching profile if detected.
     */
    fun suggestProfileForDevice(deviceName: String): AutoEqProfile? {
        val clean = deviceName.lowercase()
        return when {
            clean.contains("airpods pro 2") || clean.contains("airpods pro (2") -> findById("apple_airpods_pro_2")
            clean.contains("airpods pro") -> findById("apple_airpods_pro")
            clean.contains("airpods max") -> findById("apple_airpods_max")
            clean.contains("1000xm5") || clean.contains("wh-1000xm5") -> findById("sony_wh_1000xm5")
            clean.contains("1000xm4") && clean.contains("wf") -> findById("sony_wf_1000xm4")
            clean.contains("1000xm4") -> findById("sony_wh_1000xm4")
            clean.contains("hd 600") || clean.contains("hd600") -> findById("sennheiser_hd_600")
            clean.contains("hd 650") || clean.contains("hd650") || clean.contains("hd 6xx") || clean.contains("hd6xx") -> findById("sennheiser_hd_650")
            clean.contains("momentum 4") -> findById("sennheiser_momentum_4")
            clean.contains("qc45") || clean.contains("qc 45") || clean.contains("quietcomfort 45") -> findById("bose_qc_45")
            clean.contains("qc35") || clean.contains("qc 35") || clean.contains("quietcomfort 35") -> findById("bose_qc_35_ii")
            clean.contains("dt 770") || clean.contains("dt770") -> findById("beyerdynamic_dt_770_pro_80")
            clean.contains("dt 990") || clean.contains("dt990") -> findById("beyerdynamic_dt_990_pro_250")
            clean.contains("m50x") || clean.contains("ath-m50x") -> findById("audio_technica_ath_m50x")
            else -> null
        }
    }
}

/**
 * Direct Form II Transposed biquad section for 32-bit float audio processing.
 */
internal class AutoEqBiquadSection {
    var b0 = 1f; var b1 = 0f; var b2 = 0f
    var a1 = 0f; var a2 = 0f

    private var s1 = FloatArray(2)
    private var s2 = FloatArray(2)

    fun reset(channels: Int) {
        val count = channels.coerceAtLeast(1)
        if (s1.size != count) {
            s1 = FloatArray(count)
            s2 = FloatArray(count)
        } else {
            s1.fill(0f)
            s2.fill(0f)
        }
    }

    fun compute(filter: AutoEqFilter, sampleRate: Int) {
        if (sampleRate <= 0) return
        val f0 = filter.freqHz.coerceIn(10f, sampleRate * 0.49f)
        val q = filter.q.coerceAtLeast(0.05f)
        val gainDb = filter.gainDb
        val w0 = 2.0 * PI * f0 / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / (2.0 * q)
        val a = 10.0.pow(gainDb / 40.0)

        val b0Raw: Double
        val b1Raw: Double
        val b2Raw: Double
        val a0Raw: Double
        val a1Raw: Double
        val a2Raw: Double

        when (filter.type) {
            AutoEqFilterType.PEAK -> {
                b0Raw = 1.0 + alpha * a
                b1Raw = -2.0 * cosW
                b2Raw = 1.0 - alpha * a
                a0Raw = 1.0 + alpha / a
                a1Raw = -2.0 * cosW
                a2Raw = 1.0 - alpha / a
            }
            AutoEqFilterType.LOW_SHELF -> {
                val beta = 2.0 * sqrt(a) * alpha
                b0Raw = a * ((a + 1.0) - (a - 1.0) * cosW + beta)
                b1Raw = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW)
                b2Raw = a * ((a + 1.0) - (a - 1.0) * cosW - beta)
                a0Raw = (a + 1.0) + (a - 1.0) * cosW + beta
                a1Raw = -2.0 * ((a - 1.0) + (a + 1.0) * cosW)
                a2Raw = (a + 1.0) + (a - 1.0) * cosW - beta
            }
            AutoEqFilterType.HIGH_SHELF -> {
                val beta = 2.0 * sqrt(a) * alpha
                b0Raw = a * ((a + 1.0) + (a - 1.0) * cosW + beta)
                b1Raw = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW)
                b2Raw = a * ((a + 1.0) + (a - 1.0) * cosW - beta)
                a0Raw = (a + 1.0) - (a - 1.0) * cosW + beta
                a1Raw = 2.0 * ((a - 1.0) - (a + 1.0) * cosW)
                a2Raw = (a + 1.0) - (a - 1.0) * cosW - beta
            }
        }

        val invA0 = 1.0 / a0Raw
        b0 = (b0Raw * invA0).toFloat()
        b1 = (b1Raw * invA0).toFloat()
        b2 = (b2Raw * invA0).toFloat()
        a1 = (a1Raw * invA0).toFloat()
        a2 = (a2Raw * invA0).toFloat()
    }

    inline fun process(sample: Float, channel: Int): Float {
        val ch = if (channel < s1.size) channel else 0
        val y = b0 * sample + s1[ch]
        s1[ch] = b1 * sample - a1 * y + s2[ch]
        s2[ch] = b2 * sample - a2 * y
        // Denormal flushing
        if (abs(s1[ch]) < 1e-15f) s1[ch] = 0f
        if (abs(s2[ch]) < 1e-15f) s2[ch] = 0f
        return y
    }
}

/**
 * Headphone correction DSP processor.
 * Cascades Direct Form II Transposed biquad filters with smooth anti-clipping preamp gliding
 * and level-matched A/B comparison.
 */
internal class DesktopAutoEqProcessor {

    private var channels = 2
    private var sampleRate = 44100

    @Volatile private var isEnabled = false
    @Volatile private var isAbBypass = false
    @Volatile private var currentProfile: AutoEqProfile? = null

    private val sections = mutableListOf<AutoEqBiquadSection>()

    private var currentLinearGain = 1.0f
    private var targetLinearGain = 1.0f

    fun configure(channels: Int, sampleRate: Int) {
        if (channels == this.channels && sampleRate == this.sampleRate) return
        this.channels = channels.coerceAtLeast(1)
        this.sampleRate = sampleRate.coerceAtLeast(8000)
        rebuildSections()
    }

    fun reset() {
        sections.forEach { it.reset(channels) }
        currentLinearGain = targetLinearGain
    }

    fun setCorrection(enabled: Boolean, profile: AutoEqProfile?, abBypass: Boolean) {
        val profileChanged = this.currentProfile?.id != profile?.id
        this.isEnabled = enabled
        this.isAbBypass = abBypass
        this.currentProfile = profile

        if (enabled && profile != null) {
            val safePreamp = profile.safePreampDb
            targetLinearGain = 10.0f.pow(safePreamp / 20.0f)
            if (profileChanged) {
                rebuildSections()
            }
        } else {
            targetLinearGain = 1.0f
        }
    }

    private fun rebuildSections() {
        val profile = currentProfile
        if (profile == null || sampleRate <= 0) {
            sections.clear()
            return
        }

        while (sections.size < profile.filters.size) {
            sections.add(AutoEqBiquadSection().apply { reset(channels) })
        }
        while (sections.size > profile.filters.size) {
            sections.removeAt(sections.size - 1)
        }

        for (i in profile.filters.indices) {
            sections[i].compute(profile.filters[i], sampleRate)
            sections[i].reset(channels)
        }
    }

    /**
     * Processes [count] interleaved 32-bit float samples in place.
     */
    fun process(block: FloatArray, count: Int) {
        if (count <= 0 || channels <= 0) return

        val active = isEnabled && currentProfile != null
        // Bypass if completely disabled and gain settled at 1.0
        if (!active && abs(currentLinearGain - 1.0f) < 1e-4f && targetLinearGain == 1.0f) {
            return
        }

        val frameCount = count / channels
        val gainStep = (targetLinearGain - currentLinearGain) / frameCount.coerceAtLeast(1)

        val bypassBiquads = isAbBypass || !active
        val filterCount = if (bypassBiquads) 0 else sections.size

        var sampleIdx = 0
        for (f in 0 until frameCount) {
            currentLinearGain += gainStep

            for (ch in 0 until channels) {
                if (sampleIdx >= count) break
                var sample = block[sampleIdx] * currentLinearGain

                for (s in 0 until filterCount) {
                    sample = sections[s].process(sample, ch)
                }

                block[sampleIdx] = sample
                sampleIdx++
            }
        }
        currentLinearGain = targetLinearGain
    }
}

/**
 * Settings and state management for AutoEQ headphone correction.
 */
internal object DesktopAutoEqSettings {

    private val persistence = DesktopPersistence()

    private const val KEY_ENABLED = "autoeq_feature_enabled"
    private const val KEY_AB_BYPASS = "autoeq_ab_bypass"
    private const val KEY_PROFILE_ID = "autoeq_selected_profile_id"
    private const val KEY_DEVICE_PREFIX = "autoeq_device_"

    private val _enabled = MutableStateFlow(persistence.boolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled

    private val _abBypass = MutableStateFlow(persistence.boolean(KEY_AB_BYPASS, false))
    val abBypass: StateFlow<Boolean> = _abBypass

    private val initialProfileId = persistence.string(KEY_PROFILE_ID, "apple_airpods_pro_2")
    private val _currentProfile = MutableStateFlow(
        AutoEqDatabase.findById(initialProfileId) ?: AutoEqDatabase.BUILTIN_PROFILES.first()
    )
    val currentProfile: StateFlow<AutoEqProfile> = _currentProfile

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        persistence.saveBoolean(KEY_ENABLED, value)
    }

    fun setAbBypass(value: Boolean) {
        _abBypass.value = value
        persistence.saveBoolean(KEY_AB_BYPASS, value)
    }

    fun selectProfile(profile: AutoEqProfile, forDevice: String? = null) {
        _currentProfile.value = profile
        persistence.saveString(KEY_PROFILE_ID, profile.id)

        if (!forDevice.isNullOrBlank()) {
            val deviceKey = KEY_DEVICE_PREFIX + forDevice.trim().lowercase().hashCode()
            persistence.saveString(deviceKey, profile.id)
        }
    }

    /**
     * Called when the active audio output device changes.
     * Restores saved profile for this device or detects an automatic suggestion.
     */
    fun onAudioDeviceChanged(deviceName: String) {
        if (deviceName.isBlank()) return
        val deviceKey = KEY_DEVICE_PREFIX + deviceName.trim().lowercase().hashCode()
        val savedProfileId = persistence.string(deviceKey, "")
        if (savedProfileId.isNotBlank()) {
            AutoEqDatabase.findById(savedProfileId)?.let {
                _currentProfile.value = it
                return
            }
        }
        val suggested = AutoEqDatabase.suggestProfileForDevice(deviceName)
        if (suggested != null) {
            _currentProfile.value = suggested
        }
    }
}
