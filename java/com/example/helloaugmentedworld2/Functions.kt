package com.example.helloaugmentedworld2

import kotlin.math.*
import kotlin.system.measureNanoTime

/**
 * A class containing functions used for the AR application.
 */
class Functions{
    companion object {
        fun toRadians(degrees: Float): Float {
            return degrees * PI.toFloat() / 180
        }

        fun toDegrees(radians: Float): Float {
            return radians * 180 / PI.toFloat()
        }

        /**
         * Using the haversine formula, this function calculates the distance from
         * the Null Island (lat=0, long=0) to the given position along the x and z axis.
         * These distances are used to create new Coordinates for the OpenGL graphics.
         */
        fun toGLCoordinates(latitude: Float, longitude: Float): Pair<Float, Float> {
            val zCoord = -toRadians(latitude) * MainActivity.EARTH_RADIUS
            val nullIslandDistance = 2f * MainActivity.EARTH_RADIUS *
                    asin(
                        sqrt(sin(toRadians(latitude)/2).pow(2) +
                                sin(toRadians(longitude)/2).pow(2) *
                                cos(toRadians(latitude))
                        )
                    )
            val xCoord = sign(longitude) * sqrt(nullIslandDistance.pow(2)-zCoord.pow(2))
            return Pair(xCoord, zCoord)
        }

        /**
         * This function calculates the mean of 3 angles
         * The input angles are assumed to be within 180 degrees of each other
         * and each angle should be between 0 and 360 degrees
         *
         * Example for why angle mean can not always be calculated through (x + y + z) / 3:
         * Angle mean of 350, 10 and 0 degrees is 0.
         */
        fun meanAngle(x: Float, y: Float, z: Float): Float {
            val meanXY: Float
            if (abs(x - y) > PI){
                meanXY = (x + y - 2f * PI.toFloat()) / 2f
            } else {
                meanXY = (x + y) / 2f
            }
            val mean: Float
            if (abs(meanXY - z) > PI) {
                if (meanXY > PI) {
                    mean = (2f * meanXY - 4f * PI.toFloat()) / 3f
                } else {
                    mean = (2f * meanXY - 2f * PI.toFloat()) / 3f
                }
            } else {
                mean = (meanXY * 2f + z) / 3f
            }
            return mean
        }

    }
}