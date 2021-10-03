package com.example.helloaugmentedworld2

import ObjectRenderer
import android.opengl.GLSurfaceView
import android.content.Context
import android.graphics.PixelFormat

/**
 * This class is partly based on code from http://www.learnopengles.com/android-lesson-one-getting-started/
 */

class TransparentSurfaceView(context: Context?) : GLSurfaceView(context) {
    private var myRenderer: ObjectRenderer

    fun getRenderer() : ObjectRenderer {
        return myRenderer
    }

    override fun onPause() {
        super.onPause()
        myRenderer.onPause()
    }

    override fun onResume() {
        super.onResume()
        myRenderer.onResume()
    }

    init {
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2)
        super.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        setZOrderMediaOverlay(true)
        // Set the Renderer for drawing on the GLSurfaceView
        val main = context as MainActivity
        myRenderer = ObjectRenderer(main.getObjects())
        setRenderer(myRenderer)

        // Make the background transparent
        getHolder().setFormat(PixelFormat.TRANSLUCENT)

        // Render the view only when there is a change in the drawing data
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}