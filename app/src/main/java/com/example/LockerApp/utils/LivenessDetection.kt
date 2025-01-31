package com.example.LockerApp.utils

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

class LivenessDetector {
    enum class Action {
        BLINK,
        TURN_LEFT,
        TURN_RIGHT,
        NONE
    }

    data class LivenessState(
        val currentAction: Action = Action.NONE,
        val isComplete: Boolean = false,
        val message: String = "",
        val progress: List<Action> = listOf()
    )

    private val requiredActions = listOf(Action.BLINK, Action.TURN_LEFT, Action.TURN_RIGHT)
    private var completedActions = mutableListOf<Action>()
    private var currentActionIndex = 0

    private val blinkThreshold = 0.1f
    private val turnThreshold = 30f

    fun reset() {
        completedActions.clear()
        currentActionIndex = 0
    }

    fun processFrame(face: Face): LivenessState {
        if (currentActionIndex >= requiredActions.size) {
            return LivenessState(
                Action.NONE,
                true,
                "Liveness check complete!",
                completedActions
            )
        }

        val currentAction = requiredActions[currentActionIndex]
        val actionDetected = when (currentAction) {
            Action.BLINK -> detectBlink(face)
            Action.TURN_LEFT -> detectTurnLeft(face)
            Action.TURN_RIGHT -> detectTurnRight(face)
            Action.NONE -> false
        }

        if (actionDetected && !completedActions.contains(currentAction)) {
            completedActions.add(currentAction)
            currentActionIndex++
        }

        val message = when (currentAction) {
            Action.BLINK -> "Please blink"
            Action.TURN_LEFT -> "Please turn your head left"
            Action.TURN_RIGHT -> "Please turn your head right"
            Action.NONE -> "Processing..."
        }

        return LivenessState(
            currentAction,
            currentActionIndex >= requiredActions.size,
            message,
            completedActions
        )
    }

    private fun detectBlink(face: Face): Boolean {
        // Get eye landmarks
        val leftEye = face.leftEyeOpenProbability
        val rightEye = face.rightEyeOpenProbability

        // Check if both eyes are closed (low probability of being open)
        return if (leftEye != null && rightEye != null) {
            leftEye < blinkThreshold && rightEye < blinkThreshold
        } else false
    }

    private fun detectTurnLeft(face: Face): Boolean {
        val eulerX = face.headEulerAngleX // Pitch
        val eulerY = face.headEulerAngleY // Yaw
        val eulerZ = face.headEulerAngleZ // Roll

        // Check if head is turned left (positive yaw angle)
        return eulerY > turnThreshold
    }

    private fun detectTurnRight(face: Face): Boolean {
        val eulerX = face.headEulerAngleX // Pitch
        val eulerY = face.headEulerAngleY // Yaw
        val eulerZ = face.headEulerAngleZ // Roll

        // Check if head is turned right (negative yaw angle)
        return eulerY < -turnThreshold
    }
}