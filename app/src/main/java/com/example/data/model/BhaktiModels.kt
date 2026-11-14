package com.example.data.model

import com.example.BuildConfig

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MovieProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val storyInput: String = "",
    val language: String = "hindi",
    val visualStyle: String = "Epic 3D Mythological",
    val continuitySummary: String = "",
    val attachedCharacterIds: List<String> = emptyList(),
    val sceneCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MovieScene(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val sceneNumber: Long = 1L,
    val title: String = "",
    val durationSeconds: Long = 8L,
    val sceneDescription: String = "",
    val cameraDirection: String = "",
    val characterActions: String = "",
    val continuityNotes: String = "",
    val narrationScript: String = "",
    val sanskritPhonetics: String = "",
    val subtitleText: String = "",
    val includedInCut: Boolean = true,
    val audioFileName: String = "",
    val audioLocalUri: String = "",
    val audioDurationMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MythCharacter(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val archetype: String = "",
    val visualDetails: String = "",
    val costumeDetails: String = "",
    val auraAndPalette: String = "",
    val continuityTags: List<String> = emptyList(),
    val localImagePath: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ModularRenderJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val sceneId: String = "",
    val mode: String = "text_to_video",
    val providerId: String = "storyboard_packet",
    val modelName: String = "BhaktiMotion Shot Packet",
    val billingTier: String = "free",
    val prompt: String = "",
    val aspectRatio: String = "16:9",
    val durationSeconds: Long = 8L,
    val characterNames: List<String> = emptyList(),
    val referenceImagePath: String = "",
    val status: String = "packet_ready",
    val statusDetail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class VideoProviderSpec(
    val id: String,
    val displayName: String,
    val modelLabel: String,
    val billingTier: String,
    val isConnected: Boolean,
    val supportsImageToVideo: Boolean,
    val supportsTextToVideo: Boolean,
    val description: String
)

object VideoProviderRegistry {
    val providers: List<VideoProviderSpec> = listOf(
        VideoProviderSpec(
            id = "storyboard_packet",
            displayName = "Production Shot Packet",
            modelLabel = "Gemini Storyboard Engine (Built-in)",
            billingTier = "free",
            isConnected = true,
            supportsImageToVideo = true,
            supportsTextToVideo = true,
            description = "Prepares camera-ready shot specifications, continuity notes, and exportable prompts at zero cost."
        ),
        VideoProviderSpec(
            id = "veo_adapter",
            displayName = "Vertex AI Veo Adapter",
            modelLabel = "External Video API (Not Connected)",
            billingTier = "paid",
            isConnected = false,
            supportsImageToVideo = true,
            supportsTextToVideo = true,
            description = "Modular slot for Google Cloud Veo video rendering. Requires paid Cloud Billing; disabled by default."
        ),
        VideoProviderSpec(
            id = "runway_kling_adapter",
            displayName = "External Studio Adapter",
            modelLabel = "Runway / Kling / Luma Webhook (Not Connected)",
            billingTier = "paid",
            isConnected = false,
            supportsImageToVideo = true,
            supportsTextToVideo = true,
            description = "Modular adapter slot for third-party video APIs. Queues render payloads without activating billing."
        ),
        VideoProviderSpec(
            id = "gemini_omni_adapter",
            displayName = "Gemini Omni Flash (Direct)",
            modelLabel = "gemini-omni-1.1-flash",
            billingTier = "paid",
            isConnected = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            supportsImageToVideo = true,
            supportsTextToVideo = true,
            description = "Renders a real MP4 on this device's request through the Gemini API. Billed by Google per clip; only runs when you press the render button. Needs GEMINI_API_KEY in .env."
        )
    )
}

data class SanskritCoachResult(
    val devanagari: String = "",
    val iastTransliteration: String = "",
    val syllableBreakdown: String = "",
    val deliveryNotes: String = ""
)

data class StoryStarter(
    val title: String,
    val language: String,
    val visualStyle: String,
    val storyText: String
)

object StoryStarters {
    val presets = listOf(
        StoryStarter(
            title = "Samudra Manthan",
            language = "hindi",
            visualStyle = "Epic 3D Mythological",
            storyText = "At twilight over the primordial Milk Ocean, the Devas and Asuras gather around Mount Mandara with Vasuki coiled as the churning rope. Lord Vishnu manifests as the giant Kurma tortoise beneath the mountain to steady the cosmos. Golden mist rises as celestial treasures emerge from the glowing waves."
        ),
        StoryStarter(
            title = "Ganga Avataran",
            language = "sanskrit",
            visualStyle = "Sacred Watercolor Cinema",
            storyText = "King Bhagiratha stands in deep austerity amid Himalayan snow peaks, praying for the descent of sacred Ganga. Answering his devotion, Ganga cascades from the celestial realm with thunderous grace, while Lord Shiva stands serenely on Mount Kailash to receive her torrent within his matted locks."
        ),
        StoryStarter(
            title = "Hanuman's Leap",
            language = "bilingual",
            visualStyle = "Traditional Pattachitra-3D",
            storyText = "On the southern rocky shore of Mahendragiri, Hanuman awakens to his boundless strength. Expanding to a towering golden form against a fiery saffron sky, he bows to Shri Rama and leaps across the vast ocean toward Lanka, leaving trails of luminous embers."
        )
    )
}

object Cinematic3DPresets {
    val renderEngines = listOf(
        "Unreal 5 Photorealistic 3D",
        "IMAX Mythic 3D CGI",
        "Sacred Bronze & Marble 3D",
        "Volumetric Painterly 3D"
    )

    val cameraRigs = listOf(
        "Orbital 3D Arc (35mm)",
        "Anamorphic Dolly Push (50mm)",
        "Aerial Crane Sweep (24mm)",
        "Low Hero Steadicam (85mm)"
    )

    val lightingMoods = listOf(
        "Volumetric God Rays",
        "Golden Aarti Rim Light",
        "Cosmic Nebula & Embers",
        "Himalayan Mist & Sunbeams"
    )
}
