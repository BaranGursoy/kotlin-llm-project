package com.baran.medxr.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured response from the LLM agent.
 *
 * The system prompt instructs the model to return strictly this JSON shape.
 * When parsing fails (e.g., the model returns plain text), the repository
 * falls back to wrapping the raw text into [patientMessage].
 */
@Serializable
data class AgentResponse(
    @SerialName("patient_message")
    val patientMessage: String,

    @SerialName("xr_scene_recommendation")
    val xrSceneRecommendation: String = "General",

    @SerialName("urgency_level")
    val urgencyLevel: String = "Low",

    @SerialName("avatar_emotion_trigger")
    val avatarEmotionTrigger: String = "calming"
)
