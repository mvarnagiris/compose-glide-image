package com.koduok.compose.glideimage

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FrameManager
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.stateFor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.WithConstraints
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ContextAmbient
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
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    onImageReady: (() -> Unit)? = null,
    customize: RequestBuilder<Bitmap>.() -> RequestBuilder<Bitmap> = { this }
) {
    WithConstraints {
        val image = stateFor<ImageAsset?>(null) { null }
        val drawable = stateFor<Drawable?>(null) { null }
        val context = ContextAmbient.current

        onCommit(model) {
            val glide = Glide.with(context)
            var target: CustomTarget<Bitmap>? = null
            val job = CoroutineScope(Dispatchers.Main).launch {
                target = object : CustomTarget<Bitmap>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                        image.value = null
                        drawable.value = placeholder
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        FrameManager.ensureStarted()
                        image.value = resource.asImageAsset()
                        onImageReady?.invoke()
                    }
                }

                val width =
                    if (constraints.maxWidth > 0 && constraints.maxWidth < Int.MAX_VALUE) {
                        constraints.maxWidth
                    } else {
                        SIZE_ORIGINAL
                    }

                val height =
                    if (constraints.maxHeight > 0 && constraints.maxHeight < Int.MAX_VALUE) {
                        constraints.maxHeight
                    } else {
                        SIZE_ORIGINAL
                    }

                glide
                    .asBitmap()
                    .load(model)
                    .override(width, height)
                    .let(customize)
                    .into(target!!)
            }

            onDispose {
                image.value = null
                drawable.value = null
                glide.clear(target)
                job.cancel()
            }
        }

        val theImage = image.value
        val theDrawable = drawable.value
        if (theImage != null) {
            Image(
                asset = theImage,
                modifier = modifier,
                contentScale = contentScale,
                alignment = alignment,
                alpha = alpha,
                colorFilter = colorFilter
            )
        } else if (theDrawable != null) {
            Canvas(modifier = Modifier.fillMaxSize().then(modifier)) {
                drawIntoCanvas { theDrawable.draw(it.nativeCanvas) }
            }
        }
    }
}
