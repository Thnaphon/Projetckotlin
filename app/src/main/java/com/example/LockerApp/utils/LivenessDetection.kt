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
        SMILE,
        NONE
    }

    data class LivenessState(
        val currentAction: Action = Action.NONE,
        val isComplete: Boolean = false,
        val message: String = "",
        val progress: List<Action> = listOf(),
        val isActionDetected: Boolean = false
    )

    private val requiredActions = listOf(Action.BLINK, Action.TURN_LEFT, Action.TURN_RIGHT, Action.SMILE)
    private var completedActions = mutableListOf<Action>()
    private var currentActionIndex = 0
    private var lastActionTime = 0L
    private val actionTimeout = 3000L

    // Thresholds // bias
    private val blinkThreshold = 0.2f
    private val turnThreshold = 25f
    private val smileThreshold = 0.8f

    fun reset() {
        completedActions.clear()
        currentActionIndex = 0
        lastActionTime = 0L
    }

    fun processFrame(face: Face): LivenessState {
        if (currentActionIndex >= requiredActions.size) {
            return LivenessState(
                Action.NONE,
                true,
                "Liveness check complete!",
                completedActions,
                false
            )
        }

        val currentAction = requiredActions[currentActionIndex]
        var actionDetected = false
        var actionMessage = ""

        when (currentAction) {
            Action.BLINK -> {
                actionDetected = detectBlink(face)
                actionMessage = "Please blink your eyes"
            }
            Action.TURN_LEFT -> {
                actionDetected = detectTurnLeft(face)
                actionMessage = "Please turn your head left"
            }
            Action.TURN_RIGHT -> {
                actionDetected = detectTurnRight(face)
                actionMessage = "Please turn your head right"
            }
            Action.SMILE -> {
                actionDetected = detectSmile(face)
                actionMessage = "Please smile"
            }
            Action.NONE -> {
                actionMessage = "Processing..."
            }
        }

        val currentTime = System.currentTimeMillis()
        if (actionDetected && (currentTime - lastActionTime > actionTimeout)) {
            if (!completedActions.contains(currentAction)) {
                completedActions.add(currentAction)
                currentActionIndex++
                lastActionTime = currentTime
                Log.d("LivenessDetector", "Action completed: $currentAction")
            }
        }

        return LivenessState(
            currentAction,
            currentActionIndex >= requiredActions.size,
            actionMessage,
            completedActions,
            actionDetected
        )
    }

    private fun detectBlink(face: Face): Boolean {
        val leftEye = face.leftEyeOpenProbability
        val rightEye = face.rightEyeOpenProbability

        return if (leftEye != null && rightEye != null) {
            leftEye < blinkThreshold && rightEye < blinkThreshold
        } else false
    }

    private fun detectTurnLeft(face: Face): Boolean {
        val eulerY = face.headEulerAngleY // Yaw
        return eulerY > turnThreshold
    }

    private fun detectTurnRight(face: Face): Boolean {
        val eulerY = face.headEulerAngleY // Yaw
        return eulerY < -turnThreshold
    }

    private fun detectSmile(face: Face): Boolean {
        return face.smilingProbability?.let { it > smileThreshold } ?: false
    }

    fun getProgress(): Float {
        return completedActions.size.toFloat() / requiredActions.size
    }
}