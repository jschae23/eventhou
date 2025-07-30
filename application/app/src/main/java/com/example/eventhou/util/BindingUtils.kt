package com.example.eventhou.util

import androidx.databinding.BindingAdapter
import com.facebook.drawee.view.SimpleDraweeView

/**
 * Binding adapter to bind image in SimpleDraweeView
 */
@BindingAdapter("image")
fun loadImage(view: SimpleDraweeView, url: String) {
    view.setImageURI(url)
}
