package com.koduok.compose.glideimage

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.state
import androidx.ui.core.ContextAmbient
import androidx.ui.core.WithConstraints
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Image
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.ImageAssetConfig
import androidx.ui.graphics.NativeImageAsset
import androidx.ui.graphics.colorspace.ColorSpace
import androidx.ui.graphics.colorspace.ColorSpaces
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.IntPx
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.request.transition.Transition

@Composable
fun GlideImage(
    model: Any,
    customize: RequestBuilder<Bitmap>.() -> RequestBuilder<Bitmap> = { this }
) {
    WithConstraints { constraints, _ ->
        var image by state<ImageAsset?> { null }
        var drawable by state<Drawable?> { null }
        val context = ContextAmbient.current

        onCommit(model) {
            val target = object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    image = null
                    drawable = placeholder
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    image = AndroidImage(resource)
                }
            }

            val width =
                if (constraints.maxWidth > IntPx.Zero && constraints.maxWidth < IntPx.Infinity) {
                    constraints.maxWidth.value
                } else {
                    SIZE_ORIGINAL
                }

            val height =
                if (constraints.maxHeight > IntPx.Zero && constraints.maxHeight < IntPx.Infinity) {
                    constraints.maxHeight.value
                } else {
                    SIZE_ORIGINAL
                }

            val glide = Glide.with(context)
            glide
                .asBitmap()
                .load(model)
                .override(width, height)
                .let(customize)
                .into(target)

            onDispose {
                image = null
                drawable = null
                glide.clear(target)
            }
        }

        val theImage = image
        val theDrawable = drawable
        if (theImage != null) {
            Image(image = theImage)
        } else if (theDrawable != null) {
            Canvas(modifier = LayoutSize.Fill) { theDrawable.draw(nativeCanvas) }
        }
    }
}

class AndroidImage(private val bitmap: Bitmap) : ImageAsset {
    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override val config: ImageAssetConfig
        get() = bitmap.config.toImageConfig()

    override val colorSpace: ColorSpace
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmap.colorSpace?.toComposeColorSpace() ?: ColorSpaces.Srgb
        } else {
            ColorSpaces.Srgb
        }

    override val hasAlpha: Boolean
        get() = bitmap.hasAlpha()

    override val nativeImage: NativeImageAsset
        get() = bitmap

    override fun prepareToDraw() {
        bitmap.prepareToDraw()
    }
}

internal fun Bitmap.Config.toImageConfig(): ImageAssetConfig {
    // Cannot utilize when statements with enums that may have different sets of supported
    // values between the compiled SDK and the platform version of the device.
    // As a workaround use if/else statements
    // See https://youtrack.jetbrains.com/issue/KT-30473 for details
    @Suppress("DEPRECATION")
    return if (this == Bitmap.Config.ALPHA_8) {
        ImageAssetConfig.Alpha8
    } else if (this == Bitmap.Config.RGB_565) {
        ImageAssetConfig.Rgb565
    } else if (this == Bitmap.Config.ARGB_4444) {
        ImageAssetConfig.Argb8888 // Always upgrade to Argb_8888
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.RGBA_F16) {
        ImageAssetConfig.F16
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.HARDWARE) {
        ImageAssetConfig.Gpu
    } else {
        ImageAssetConfig.Argb8888
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun android.graphics.ColorSpace.toComposeColorSpace(): ColorSpace {
    return when (this) {
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB)
        -> ColorSpaces.Srgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACES)
        -> ColorSpaces.Aces
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACESCG)
        -> ColorSpaces.Acescg
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ADOBE_RGB)
        -> ColorSpaces.AdobeRgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT2020)
        -> ColorSpaces.Bt2020
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT709)
        -> ColorSpaces.Bt709
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_LAB)
        -> ColorSpaces.CieLab
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_XYZ)
        -> ColorSpaces.CieXyz
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DCI_P3)
        -> ColorSpaces.DciP3
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DISPLAY_P3)
        -> ColorSpaces.DisplayP3
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.EXTENDED_SRGB)
        -> ColorSpaces.ExtendedSrgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB)
        -> ColorSpaces.LinearExtendedSrgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_SRGB)
        -> ColorSpaces.LinearSrgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.NTSC_1953)
        -> ColorSpaces.Ntsc1953
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.PRO_PHOTO_RGB)
        -> ColorSpaces.ProPhotoRgb
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SMPTE_C)
        -> ColorSpaces.SmpteC
        else -> ColorSpaces.Srgb
    }
}
