package com.example.helloaugmentedworld2

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

class GraphicObject(private var latitude: Float, private var longitude: Float, private val name: String, private var color: String) {

    private var translationMatrix = FloatArray(16)
    private var MVPMatrix = FloatArray(16)
    private var modelMatrix = FloatArray(16)
    private var model: FloatBuffer
    private var glXCoord: Float = 0f
    private var glZCoord: Float = 0f
    private var distance: Float = 0f
    private var angleToUser: Float= 0f

    /**
     * Set distance between user and object using the
     * pythagorean theorem, rounded up to nearest 10.
     */
    fun setDistance(x: Float, z: Float) {
        distance = ceil(
            sqrt(
            (x-glXCoord).pow(2) +
                    (z-glZCoord).pow(2)
        ) / 10f
        ) * 10f
    }

    /**
     * Set angle from object to user using arctan.
     * Note that the range of atanAngle goes from -PI/2 to PI/2
     */
    fun setAngleToUser(x: Float, z: Float) {
        val dX: Float = x - glXCoord
        val dZ: Float = z - glZCoord
        val atanAngle = atan(-dZ/dX)
        angleToUser = if (dX < 0f) atanAngle + PI.toFloat()
        else if (-dZ < 0f) 2 * PI.toFloat() + atanAngle
        else atanAngle
    }

    fun getDisplay(): String{
        return name + ": " + color + "\nwithin " + distance.toInt() + " m"
    }

    fun getLatitude(): Float {
        return latitude
    }

    fun getLongitude(): Float {
        return longitude
    }

    /**
     * This function calculates and sends necessary
     * information to the shaders for the drawing of the object.
     *
     * It is partly based on code from
     * http://www.learnopengles.com/android-lesson-one-getting-started/
     */
    fun drawYourself(viewMatrix: FloatArray, projectionMatrix: FloatArray, positionHandle: Int, colorHandle: Int, MVPMatrixHandle: Int) {
        // Passes position information to shader
        model.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT, false,
            28, model
        )
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Pass color information to shader
        model.position(3)
        GLES20.glVertexAttribPointer(
            colorHandle, 4, GLES20.GL_FLOAT, false,
            28, model
        )
        GLES20.glEnableVertexAttribArray(colorHandle)

        // The following code calculates modelM = translateM * rotationMatrix * scaleMatrix
        val scaleMatrix = FloatArray(16)
        Matrix.setIdentityM(scaleMatrix, 0)
        Matrix.scaleM(scaleMatrix, 0, distance, distance, distance)
        val rotationMatrix = FloatArray(16)
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, Functions.toDegrees(angleToUser), 0f, 1f, 0f)
        Matrix.multiplyMM(modelMatrix, 0, rotationMatrix, 0, scaleMatrix, 0)
        Matrix.multiplyMM(modelMatrix, 0, translationMatrix, 0, modelMatrix, 0)

        // Calculating MVPM = projectionM * viewM * modelM and passes it to the shader.
        Matrix.multiplyMM(MVPMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVPMatrix, 0)
        GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, MVPMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
    }

    init {
        // Set the correct rgb-parameters based on the color string
        var r = 0f
        var g = 0f
        var b = 0f
        if (color == "RED"){
            r = 1f
        } else if (color == "GREEN"){
            g = 1f
        } else if (color == "BLUE"){
            b = 1f
        } else {
            r = 1f
            g = 1f
            b = 1f
            color = "WHITE"
        }

        // creating the vertices data based on the color and a given triangular shape
        val verticesData = floatArrayOf(
            // X, Y, Z,
            // R, G, B, A
            0f, -10f, -0.01f,
            r, g, b, 1.0f,
            0f, -10f, 0.01f,
            r, g, b, 1.0f,
            0f, 10f, 0f,
            r, g, b, 1.0f
        )

        // creating the model based on the vertices data, based on http://www.learnopengles.com/android-lesson-one-getting-started/
        model = ByteBuffer.allocateDirect(verticesData.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        model.put(verticesData).position(0)

        // setting the matrix translation
        val (x, z) = Functions.toGLCoordinates(latitude, longitude)
        glXCoord = x
        glZCoord = z
        Matrix.setIdentityM(translationMatrix, 0)
        Matrix.translateM(translationMatrix, 0, x, 0f, z)
    }
}