package com.music.mavimusic.desktop

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader

enum class GpuVisualizerStyle(val displayName: String) {
    NEON_TUNNEL("Túnel Neón GPU"),
    WATER_SURFACE("Superficie Líquida GPU"),
    COSMIC_PARTICLES("Partículas Cósmicas GPU"),
}

object DesktopGpuSettings {
    private const val PREF_KEY_ENABLED = "gpu_visualizers_enabled"
    private const val PREF_KEY_FPS = "gpu_visualizer_fps"

    private val persistence = DesktopPersistence()

    private val _enabled = MutableStateFlow(persistence.boolean(PREF_KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _targetFps = MutableStateFlow(
        persistence.string(PREF_KEY_FPS, "60").toIntOrNull()?.coerceIn(30, 120) ?: 60
    )
    val targetFps: StateFlow<Int> = _targetFps.asStateFlow()

    fun isEnabled(): Boolean = _enabled.value

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        persistence.saveBoolean(PREF_KEY_ENABLED, value)
        if (!value) {
            DesktopGpuVisualizerEngine.release()
        }
    }

    fun setTargetFps(fps: Int) {
        val clamped = when {
            fps <= 45 -> 30
            fps <= 90 -> 60
            else -> 120
        }
        _targetFps.value = clamped
        persistence.saveString(PREF_KEY_FPS, clamped.toString())
    }
}

object DesktopGpuVisualizerEngine {

    private const val NEON_TUNNEL_SKSL = """
        uniform float2 u_resolution;
        uniform float u_time;
        uniform float u_bass;
        uniform float u_mids;
        uniform float u_treble;
        uniform half4 u_primary;
        uniform half4 u_secondary;
        uniform half4 u_accent;

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * u_resolution) / u_resolution.y;
            float r = length(uv);
            if (r < 0.001) return half4(0.0, 0.0, 0.0, 1.0);
            
            float a = atan(uv.y, uv.x);
            float z = 1.0 / (r + 0.02 * sin(u_time * 2.0 + a * 4.0));
            
            // Warp speed driven by bass
            float speed = u_time * (1.2 + u_bass * 1.8);
            float2 tunnelUv = float2(a * 3.14159265 / 3.0, z + speed);
            
            // Grid lines
            float2 grid = abs(fract(tunnelUv) - 0.5);
            float line = smoothstep(0.08 + u_mids * 0.04, 0.0, min(grid.x, grid.y));
            
            // Fog depth falloff
            float depth = clamp(1.0 - r * 1.4, 0.0, 1.0);
            float glow = 0.04 / (r + 0.05) * (0.8 + u_bass * 1.2);
            
            half4 col = mix(u_secondary, u_primary, half(clamp(sin(z * 0.5 + u_time) * 0.5 + 0.5, 0.0, 1.0)));
            col += u_accent * half(line * 1.5 + glow);
            col *= half(depth);
            col.a = 1.0;
            return col;
        }
    """

    private const val WATER_SURFACE_SKSL = """
        uniform float2 u_resolution;
        uniform float u_time;
        uniform float u_bass;
        uniform float u_mids;
        uniform float u_treble;
        uniform half4 u_primary;
        uniform half4 u_secondary;
        uniform half4 u_accent;

        half4 main(float2 fragCoord) {
            float2 p = fragCoord / u_resolution;
            float2 uv = (fragCoord - 0.5 * u_resolution) / u_resolution.y;
            
            // Multi-frequency wave heights
            float t = u_time * 1.5;
            float w1 = sin(uv.x * 6.0 + t + u_bass * 3.0) * (0.12 + u_bass * 0.25);
            float w2 = cos(uv.y * 10.0 - t * 1.3 + u_mids * 2.0) * (0.08 + u_mids * 0.15);
            float w3 = sin((uv.x + uv.y) * 18.0 + t * 2.5) * (0.04 + u_treble * 0.10);
            float h = w1 + w2 + w3;
            
            // Fake surface normal and caustic refraction
            float distFromCenter = length(uv);
            float caustics = pow(clamp(h * 2.5 + 0.5, 0.0, 1.0), 3.0) * (1.0 + u_treble * 2.0);
            float specular = pow(clamp(1.0 - abs(uv.y - h * 0.4), 0.0, 1.0), 16.0) * (0.6 + u_bass * 1.5);
            
            half4 base = mix(u_primary * 0.3, u_secondary, half(clamp(p.y + h * 0.5, 0.0, 1.0)));
            half4 col = base + u_accent * half(caustics * 0.45 + specular * 0.8);
            col *= half(clamp(1.2 - distFromCenter * 0.6, 0.2, 1.0));
            col.a = 1.0;
            return col;
        }
    """

    private const val COSMIC_PARTICLES_SKSL = """
        uniform float2 u_resolution;
        uniform float u_time;
        uniform float u_bass;
        uniform float u_mids;
        uniform float u_treble;
        uniform half4 u_primary;
        uniform half4 u_secondary;
        uniform half4 u_accent;

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * u_resolution) / u_resolution.y;
            float r = length(uv);
            float a = atan(uv.y, uv.x);
            
            // Swirling vortex angle
            float spiral = a + r * (4.0 + u_mids * 5.0) - u_time * (0.8 + u_bass * 1.2);
            
            // Harmonic particle rings
            float ring1 = abs(sin(r * 22.0 - u_time * 3.0 + u_bass * 4.0));
            float ring2 = abs(sin(spiral * 5.0 + r * 15.0));
            float starfield = pow(clamp(1.0 - ring1 * ring2, 0.0, 1.0), 14.0) * (1.2 + u_treble * 2.5);
            
            // Core singularity glow
            float core = 0.06 / (r + 0.04) * (0.9 + u_bass * 1.8);
            
            half4 col = mix(u_primary * 0.2, u_secondary, half(clamp(sin(spiral) * 0.5 + 0.5, 0.0, 1.0)));
            col += u_accent * half(starfield * 0.8 + core * 0.35);
            col *= half(clamp(1.4 - r * 1.1, 0.1, 1.0));
            col.a = 1.0;
            return col;
        }
    """

    @Volatile private var tunnelEffect: RuntimeEffect? = null
    @Volatile private var waterEffect: RuntimeEffect? = null
    @Volatile private var cosmicEffect: RuntimeEffect? = null
    @Volatile private var compileFailed: Boolean = false

    private val cachedPaint = Paint()

    /**
     * Builds and returns a Skia [Shader] for the requested [style].
     * Returns null if feature is disabled, or if shader compilation fails (graceful 2D fallback).
     */
    fun createShader(
        style: GpuVisualizerStyle,
        width: Float,
        height: Float,
        timeSec: Float,
        bass: Float,
        mids: Float,
        treble: Float,
        primary: Color,
        secondary: Color,
        accent: Color,
    ): Shader? {
        if (!DesktopGpuSettings.isEnabled() || compileFailed || width <= 1f || height <= 1f) {
            return null
        }

        return try {
            val effect = getEffect(style) ?: return null
            val builder = RuntimeShaderBuilder(effect)
            builder.uniform("u_resolution", width, height)
            builder.uniform("u_time", timeSec)
            builder.uniform("u_bass", bass.coerceIn(0f, 1f))
            builder.uniform("u_mids", mids.coerceIn(0f, 1f))
            builder.uniform("u_treble", treble.coerceIn(0f, 1f))
            builder.uniform("u_primary", primary.red, primary.green, primary.blue, primary.alpha)
            builder.uniform("u_secondary", secondary.red, secondary.green, secondary.blue, secondary.alpha)
            builder.uniform("u_accent", accent.red, accent.green, accent.blue, accent.alpha)
            builder.makeShader()
        } catch (t: Throwable) {
            DesktopTrackLog.log("DesktopGpuVisualizerEngine compile/render fallback: ${t.message}")
            compileFailed = true
            null
        }
    }

    /**
     * Renders directly into a native Skia canvas using the configured GPU shader.
     * Returns true if successfully painted, false if fallback to 2D is needed.
     */
    fun drawGpuVisualizer(
        skiaCanvas: org.jetbrains.skia.Canvas,
        style: GpuVisualizerStyle,
        width: Float,
        height: Float,
        timeSec: Float,
        bass: Float,
        mids: Float,
        treble: Float,
        primary: Color,
        secondary: Color,
        accent: Color,
    ): Boolean {
        val shader = createShader(
            style = style,
            width = width,
            height = height,
            timeSec = timeSec,
            bass = bass,
            mids = mids,
            treble = treble,
            primary = primary,
            secondary = secondary,
            accent = accent,
        ) ?: return false

        return try {
            cachedPaint.shader = shader
            skiaCanvas.drawPaint(cachedPaint)
            cachedPaint.shader = null
            shader.close()
            true
        } catch (t: Throwable) {
            DesktopTrackLog.log("DesktopGpuVisualizerEngine drawPaint failed: ${t.message}")
            cachedPaint.shader = null
            false
        }
    }

    private fun getEffect(style: GpuVisualizerStyle): RuntimeEffect? {
        return when (style) {
            GpuVisualizerStyle.NEON_TUNNEL -> {
                tunnelEffect ?: synchronized(this) {
                    tunnelEffect ?: RuntimeEffect.makeForShader(NEON_TUNNEL_SKSL).also { tunnelEffect = it }
                }
            }
            GpuVisualizerStyle.WATER_SURFACE -> {
                waterEffect ?: synchronized(this) {
                    waterEffect ?: RuntimeEffect.makeForShader(WATER_SURFACE_SKSL).also { waterEffect = it }
                }
            }
            GpuVisualizerStyle.COSMIC_PARTICLES -> {
                cosmicEffect ?: synchronized(this) {
                    cosmicEffect ?: RuntimeEffect.makeForShader(COSMIC_PARTICLES_SKSL).also { cosmicEffect = it }
                }
            }
        }
    }

    /**
     * Completely frees and unloads GPU shader objects, returning GPU memory to 0%.
     */
    fun release() {
        synchronized(this) {
            tunnelEffect?.close()
            tunnelEffect = null
            waterEffect?.close()
            waterEffect = null
            cosmicEffect?.close()
            cosmicEffect = null
            compileFailed = false
            cachedPaint.shader = null
        }
    }
}
