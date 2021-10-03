import android.opengl.GLSurfaceView
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20
import android.opengl.Matrix
import com.example.helloaugmentedworld2.GraphicObject
import java.lang.RuntimeException
import javax.microedition.khronos.egl.EGLConfig
import kotlin.math.cos
import kotlin.math.sin

/**
 * Some parts of this class is based on code from http://www.learnopengles.com/android-lesson-one-getting-started/
 */
class ObjectRenderer(private val objects: List<GraphicObject>) : GLSurfaceView.Renderer {

    private var viewMatrix = FloatArray(16)
    private val frustumMatrix = FloatArray(16)
    private var mMVPMatrixHandle = 0
    private var mPositionHandle = 0
    private var mColorHandle = 0

    fun onPause() {
    }

    fun onResume() {
    }

    /**
     * The code of this function is taken from http://www.learnopengles.com/android-lesson-one-getting-started/
     */
    override fun onSurfaceCreated(glUnused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)

        val vertexShader = """uniform mat4 u_MVPMatrix;      
attribute vec4 a_Position;     
attribute vec4 a_Color;        
varying vec4 v_Color;          
void main()                    
{                              
   v_Color = a_Color;          
   gl_Position = u_MVPMatrix   
               * a_Position;   
}                              
""" // normalized screen coordinates.
        val fragmentShader = """precision mediump float;       
varying vec4 v_Color;          
void main()                    
{                              
   gl_FragColor = v_Color;     
}                              
"""

        // Load in the vertex shader.
        var vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        if (vertexShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vertexShader)

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(vertexShaderHandle)
                vertexShaderHandle = 0
            }
        }
        if (vertexShaderHandle == 0) {
            throw RuntimeException("Error creating vertex shader.")
        }

        // Load in the fragment shader shader.
        var fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        if (fragmentShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader)

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(fragmentShaderHandle)
                fragmentShaderHandle = 0
            }
        }
        if (fragmentShaderHandle == 0) {
            throw RuntimeException("Error creating fragment shader.")
        }

        // Create a program object and store the handle to it.
        var programHandle = GLES20.glCreateProgram()
        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle)

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle)

            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position")
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color")

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle)

            // Get the link status.
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle)
                programHandle = 0
            }
        }
        if (programHandle == 0) {
            throw RuntimeException("Error creating program.")
        }

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix")
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position")
        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color")

        // Tell OpenGL to use this program when rendering.
        GLES20.glUseProgram(programHandle)

    }

    /**
     * Sets the projection matrix according to the wideness of camera view
     *  and the screen ratio.
     * This function is based on code from http://www.learnopengles.com/android-lesson-one-getting-started/
     */
    override fun onSurfaceChanged(glUnused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val nearPlaneDistance = 1f
        val farPlaneDistance = 100f
        val nearPlaneWidth = nearPlaneDistance * 2f / 3f
        val nearPlaneHeight = nearPlaneWidth * height / width
        Matrix.frustumM(frustumMatrix, 0,-nearPlaneWidth / 2f, nearPlaneWidth / 2f, -nearPlaneHeight / 2f, nearPlaneHeight / 2f, nearPlaneDistance, farPlaneDistance)
    }

    /**
     * This function iterates over all to draw them out.
     */
    override fun onDrawFrame(glUnused: GL10) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        for (obj in objects) {
            obj.drawYourself(viewMatrix, frustumMatrix, mPositionHandle, mColorHandle, mMVPMatrixHandle)
        }
    }

    /**
     * This function is based on code from http://www.learnopengles.com/android-lesson-one-getting-started/
     */
    fun setViewMatrix(z: Float, x: Float, angle: Float){
        val eyeX = x
        val eyeY = 0f
        val eyeZ = z

        val lookX = x + 10*cos(angle)
        val lookY = 0f
        val lookZ = z-10*sin(angle)

        val upX = 0.0f
        val upY = 1.0f
        val upZ = 0.0f

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ)
    }

}