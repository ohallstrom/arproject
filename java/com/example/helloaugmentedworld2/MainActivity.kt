package com.example.helloaugmentedworld2

import ObjectRenderer
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import kotlin.math.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var square: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private var currentLocation: Location = createLocation(46.524555f,6.575858f)
    private var currentXCoord: Float = 642235.0f
    private var currentZCoord: Float = - 5179070.5f
    private var compassAngle: Float = 0f
    private var trigonometricAngle: Float = 0f
    private var oldTrigonometricAngle: Float = 0f
    private lateinit var mySurfaceView: TransparentSurfaceView
    private lateinit var myRenderer: ObjectRenderer
    private lateinit var objects: List<GraphicObject>
    private var visibleObjects: MutableList<GraphicObject> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createObjects()
        startSensors()

        setContentView(R.layout.activity_main)
        square = findViewById(R.id.tv_square)
        val frame: FrameLayout = findViewById(R.id.framee)
        mySurfaceView = TransparentSurfaceView(this)
        myRenderer = mySurfaceView.getRenderer()
        frame.addView(mySurfaceView)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Request camera and location permissions
        if (allPermissionsGranted()) {
            startCamera()
            startLocation()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    /**
     * Functions that checks if all permissions are granted fot camera and location services.
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = object: LocationListener{
            override fun onLocationChanged(location: Location) {
                currentLocation.latitude = location.latitude
                currentLocation.longitude = location.longitude
                val (x, z) = Functions.toGLCoordinates(location.latitude.toFloat(), location.longitude.toFloat())
                currentXCoord = ceil(x*2f)/2f
                currentZCoord = ceil(z*2f)/2f
            }
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0,
            0f,
            locationListener)
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(p0.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (p0?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(p0.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        val sink = rotationMatrix[1] - rotationMatrix[3]
        val cosk = rotationMatrix[0] + rotationMatrix[4]
        val atanAngle: Float = atan(sink/cosk)
        compassAngle = if (cosk < 0) (atanAngle + PI).toFloat() else if (sink < 0) (atanAngle + 2 * PI).toFloat() else atanAngle
        var newTrigonometricAngle = PI.toFloat()/2f - compassAngle
        newTrigonometricAngle = if (newTrigonometricAngle < 0f) newTrigonometricAngle + 2f*PI.toFloat() else newTrigonometricAngle

        // We take the mean of the three latest angles and to decrease fluctuations,
        val angleMean = Functions.meanAngle(newTrigonometricAngle, trigonometricAngle, oldTrigonometricAngle) * 10f
        oldTrigonometricAngle = trigonometricAngle
        trigonometricAngle = newTrigonometricAngle
        detection()
        myRenderer.setViewMatrix(currentZCoord, currentXCoord, angleMean)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        cameraExecutor.shutdown()
        locationManager.removeUpdates(locationListener)
        super.onDestroy()
    }

    /**
     * This function is based on a guide on https://developer.android.com/
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
                startLocation()
            } else {
                Toast.makeText(this,
                    "Necessary permissions was not given.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * This function is taken from a guide on https://developer.android.com/
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun createLocation(latitude: Float, longitude: Float): Location {
        val location = Location("")
        location.latitude = latitude.toDouble()
        location.longitude = longitude.toDouble()
        return location
    }

    private fun createObjects() {
        objects = listOf(
           GraphicObject(
               46.524946f,
               6.576108f,
               "Garbage station",
               "RED"
           ),
            GraphicObject(
                46.524529f,
                6.576240f,
                "Lamp post 1",
                "BLUE"
            ),
            GraphicObject(
                46.524860f,
                6.576310f,
                "Lamp post 2",
                "GREEN"
            )
        )
    }

    /**
     * Function that iterates over all object
     * to see which of them that are
     * visible at the given moment. If
     * visible, it adds the name of the
     * object to the screen.
     */
    private fun detection(){
        var text = ""
        for (obj in objects) {
            checkCurrent(obj)
            obj.setDistance(currentXCoord, currentZCoord)
            obj.setAngleToUser(currentXCoord, currentZCoord)
        }
        for (obj in visibleObjects) {
            text += obj.getDisplay() + "\n"
        }
        square.text = text
        visibleObjects.clear()
    }

    /**
     * Uses the haversine formula, divided into latitude and longitude parts,
     * in order to calculate the angle between the viewed point and the object (from the user).
     * @return the angle in radians
     */
    fun getAngleTo(obj: GraphicObject): Float {
        val dLatitude: Float = obj.getLatitude() - currentLocation.latitude.toFloat()
        val oppositeLeg: Float = asin(
            abs(
                sin(Functions.toRadians(dLatitude) / 2)
            )
        )
        val dLongitude: Float = obj.getLongitude() - currentLocation.longitude.toFloat()
        val adjacentLeg: Float = asin(
            sqrt(
                cos(Functions.toRadians(currentLocation.latitude.toFloat())) *
                        cos(Functions.toRadians(obj.getLatitude())) *
                        sin(Functions.toRadians(dLongitude) / 2).pow(2)
            )
        )
        val atanAngle = atan(oppositeLeg/adjacentLeg) // note that atanAngle takes values between 0 and PI/2
        return if (dLongitude < 0f && dLatitude < 0f) atanAngle + PI.toFloat() else if (dLatitude < 0f) 2 * PI.toFloat() - atanAngle else if (dLongitude < 0f) PI.toFloat() - atanAngle else atanAngle
    }

    /**
     * Function that adds an object to visibleObjects
     * if it is visible.
     */
    fun checkCurrent(obj: GraphicObject) {
        val angleTo = getAngleTo(obj)
        val angleDifference = (angleTo - trigonometricAngle)
        val adjustedAngleDifference: Float = if (angleDifference > PI) 2 * PI.toFloat() - angleDifference else angleDifference

        if (abs(adjustedAngleDifference) < atan(1f/3f)) {
            visibleObjects.add(obj)
        }
    }

    fun getObjects(): List<GraphicObject> {
        return objects
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)
        const val EARTH_RADIUS: Int = 6378100
    }
}