# How to get
```
implementation 'com.github.mvarnagiris:compose-glide-image:{version}'
```
[![](https://jitpack.io/v/mvarnagiris/compose-glide-image.svg)](https://jitpack.io/#mvarnagiris/compose-glide-image)

# How to use
```
GlideImage("url")
```

or if you want more control
```
GlideImage("url") {
    // this RequestBuilder<Bitmap>
    centerCrop().error(R.drawable.error) 
}
```
