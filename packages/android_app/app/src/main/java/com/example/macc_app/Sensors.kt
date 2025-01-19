package com.example.macc_app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.view.View
import androidx.core.graphics.withMatrix
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
class SensorView(
    context: Context,
    text: String,
    private val pitchEnabled: Boolean,
    private val rollEnabled: Boolean,
    private val yawEnabled: Boolean
) : View(context), SensorEventListener2 {
    private val textToDraw: String = text

    //Camera object for 2D transformation
    private val camera = Camera()

    private var mLastRotationVector = FloatArray(3) //The last value of the rotation vector
    private var mRotationMatrix = FloatArray(9) //3x3 rotation matrix

    /*
        mRotationMatrix:
        [ mRotationMatrix[0], mRotationMatrix[1], mRotationMatrix[2] ]  // X-axis components
        [ mRotationMatrix[3], mRotationMatrix[4], mRotationMatrix[5] ]  // Y-axis components
        [ mRotationMatrix[6], mRotationMatrix[7], mRotationMatrix[8] ]  // Z-axis components
    */

    //Orientation angles
    private var pitch = 0f //rotation of X axis
    private var roll = 0f //rotation of Y axis
    private var yaw = 0f //rotation of Z axis

    private var a = 0.001f //Low-band pass filter

    //Paint for drawing text on the canvas
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK // Set text color to black
        textSize = 32f // Set text size to 32 pixels
    }

    private var containerMatrix = Matrix() //Matrix as change-of-basis
    private var cameraMatrix = Matrix() //Matrix used to rotate head compass

    init {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,  //use this class as the listener
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        mLastRotationVector = event?.values?.clone()!! //Get last rotation vector
        //Convert the rotation vector to a 3x3 rotation matrix
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, mLastRotationVector)

        //Calculate the orientation(Euler) angles with smoothing (low-pass filter applied)
        pitch = a * pitch + (1 - a) * atan2(
            -mRotationMatrix[6],
            sqrt(mRotationMatrix[0] * mRotationMatrix[0] + mRotationMatrix[3] * mRotationMatrix[3]) //Hypotenuse of X and Y
        ) * 180f / PI.toFloat() //Convert to degrees
        roll =
            a * roll + (1 - a) * atan2(mRotationMatrix[3], mRotationMatrix[0]) * 180f / PI.toFloat()
        yaw =
            a * yaw + (1 - a) * atan2(mRotationMatrix[1], mRotationMatrix[4]) * 180f / PI.toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onFlushCompleted(sensor: Sensor?) {
        
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //Set up a transformation matrix to scale and translate the view
        containerMatrix.setScale(1f, -1f)
        containerMatrix.preConcat(Matrix().apply { setTranslate(w / 2f, -h / 2f) })
        // View's width and height are now available in w and h

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        camera.save()
        if (yawEnabled) camera.rotateZ(yaw) //Apply yaw rotation
        if (pitchEnabled) camera.rotateX(pitch) //Apply pitch rotation
        if (rollEnabled) camera.rotateY(roll) //Apply roll rotation
        camera.getMatrix(cameraMatrix) //Get the transformation matrix
        camera.restore()

        with(canvas) {
            if (pitchEnabled) drawText("PITCH: $pitch", 0f, 40f, textPaint)
            if (rollEnabled) drawText("ROLL: $roll", 0f, 80f, textPaint)
            if (yawEnabled) drawText("YAW: $yaw", 0f, 120f, textPaint)
            with(canvas) {
                withMatrix(containerMatrix) { //Apply base transformation
                    withMatrix(cameraMatrix) { //Apply Camera transformation
                        drawText(textToDraw, 0f, width / 3f, textPaint)
                    }
                }
            }
        }

        //Trigger the redraw
        invalidate()
    }
}