package com.example.macc_app

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
import android.util.Log
import android.view.View
import androidx.core.graphics.withMatrix
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorView(context: Context, text: String): View(context),SensorEventListener2 {
    private val textToDraw: String = text

    private val camera = Camera()

    var mLastRotationVector = FloatArray(3) //The last value of the rotation vector
    var mOrientation = FloatArray(3)
    var mRotationMatrix = FloatArray(9)
    var pitch = 0f
    var roll = 0f
    var yaw = 0f
    var a = 0.001f //Low-band pass filter
    var UPS = 0 //Update Per Second
    var FPS = 0 //Frames Per Second

    var timeFPS = 0L
    var timeUPS = 0L

    var fps =""
    var ups = ""

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK // Set text color to black
        textSize = 32f // Set text size to 32 pixels
    }
    var M = Matrix() //Matrix as change-of-basis
    var M2=Matrix() //Matrix used to rotate head compass

    init {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,  //use this since MyView implements the listener interface
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        mLastRotationVector = event?.values?.clone()!! //Get last rotation vector
        SensorManager.getRotationMatrixFromVector(mRotationMatrix,mLastRotationVector)
        //Calculate the yaw angle, see slides of the lesson——

        pitch = a*yaw+(1-a)* atan2(
            -mRotationMatrix[6],
            sqrt(mRotationMatrix[0] * mRotationMatrix[0] + mRotationMatrix[3] * mRotationMatrix[3])
        ) *180f/ PI.toFloat()
        roll = a*yaw+(1-a)* atan2(mRotationMatrix[3], mRotationMatrix[0]) *180f/ PI.toFloat()
        yaw = a*yaw+(1-a)* atan2(mRotationMatrix[1],mRotationMatrix[4]) *180f/ PI.toFloat()

        //Alternative way using available methods
        //SensorManager.getOrientation(mRotationMatrix,mOrientation)
        //yaw = mOrientation[0]*180f/PI.toFloat()

        if (UPS%10==0 && (System.currentTimeMillis()-timeUPS) != 0L){
            ups=(1000*10/(System.currentTimeMillis()-timeUPS)).toString()
            timeUPS=System.currentTimeMillis()
            UPS=0
        }
        UPS++
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onFlushCompleted(sensor: Sensor?) {
        //TODO("Not yet implemented")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        M.setScale(1f,-1f)
        M.preConcat(Matrix().apply { setTranslate(w/2f,-h/2f) })
        // View's width and height are now available in w and h

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (FPS%10==0){
            fps=(10000/(System.currentTimeMillis()-timeFPS)).toString()
            timeFPS=System.currentTimeMillis()
            FPS=0
        }
        FPS++

        camera.save()
        //camera.translate(width / 2f, height / 2f, 0f)
        camera.rotateX(pitch) // Apply pitch rotation
        camera.rotateY(roll) // Apply roll rotation
        camera.rotateZ(yaw) // Apply yaw rotation
        //camera.translate(-width / 2f, -height / 2f, 0f)
        camera.getMatrix(M2) // Get the transformation matrix
        camera.restore()

        with(canvas) {
            drawText("PITCH: "+pitch.toString(),100f,40f,textPaint)
            drawText("ROLL: "+roll.toString(),100f,80f,textPaint)
            drawText("YAW: "+yaw.toString(),100f,120f,textPaint)
            /*drawText("FPS: "+fps,100f,80f,textPaint)
            drawText("UPS: "+ups,100f,120f,textPaint)*/
            with(canvas) {
                withMatrix(M) { // Apply Camera transformation directly
                    withMatrix(M2) {
                        drawText(textToDraw, 0f, width / 3f, textPaint)
                    }
                }
            }
            /*withMatrix(M.apply {  }) {
                //drawLine(0f,0f,0f,canvas.width/3f,paint)

                withMatrix(M2.apply {
                    setRotate(pitch,0f,0f)
                    setRotate(roll,0f,0f)
                    }) {
                    drawText(textToDraw, 0f,width/3f,textPaint)
                }
                //
            }*/
        }

        /*camera.save()
        camera.rotateX(pitch) // Apply pitch rotation
        camera.rotateY(roll) // Apply roll rotation
        camera.rotateZ(yaw) // Apply yaw rotation
        camera.getMatrix(matrix) // Get the transformation matrix
        camera.restore()

        canvas.concat(matrix) // Apply the transformation to the canvas

        canvas.drawText(textToDraw, x, y, textPaint) // Draw the text*/
        invalidate()
    }
}