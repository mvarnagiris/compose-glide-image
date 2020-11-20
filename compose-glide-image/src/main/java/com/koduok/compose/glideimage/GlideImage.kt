package com.koduok.compose.glideimage

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.WithConstraints
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.unit.IntSize
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GlideImage(
    model: Any,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    onImageReady: (() -> Unit)? = null,
    customize: RequestBuilder<Bitmap>.() -> RequestBuilder<Bitmap> = { this },
) {
    WithConstraints {
        var image by remember { mutableStateOf<ImageAsset?>(null) }
        var drawable by remember { mutableStateOf<Drawable?>(null) }
        val context = ContextAmbient.current

        onCommit(model) {
            val glide = Glide.with(context)
            var target: CustomTarget<Bitmap>? = null
            val job = CoroutineScope(Dispatchers.Main).launch {
                target = object : CustomTarget<Bitmap>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                        image = null
                        drawable = placeholder
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?,
                    ) {
                        FrameManager.ensureStarted()
                        image = resource.asImageAsset()
                        onImageReady?.invoke()
                    }
                }

                val size = constraints.run {
                    IntSize(
                        if (maxWidth in 1 until Int.MAX_VALUE) maxWidth else SIZE_ORIGINAL,
                        if (maxHeight in 1 until Int.MAX_VALUE) maxHeight else SIZE_ORIGINAL
                    )
                }

                glide
                    .asBitmap()
                    .load(model)
                    .override(size.width, size.height)
                    .let(customize)
                    .into(target!!)
            }

            onDispose {
                image = null
                drawable = null
                glide.clear(target)
                job.cancel()
            }
        }

        ActiveImage(
            image = image,
            drawable = drawable,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment,
            alpha = alpha,
            colorFilter = colorFilter
        )
    }
}

@Composable
private fun ActiveImage(
    image: ImageAsset?,
    drawable: Drawable?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    if (image != null) {
        Image(
            asset = image,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment,
            alpha = alpha,
            colorFilter = colorFilter
        )
    } else if (drawable != null) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
        ) {
            drawIntoCanvas { drawable.draw(it.nativeCanvas) }
        }
    }
}