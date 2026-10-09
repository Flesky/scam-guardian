package ph.scamguardian

/** How protected the user really is: what the main screen's status should say. */
enum class ProtectionLevel {
    /** The button is off, or the accessibility service is not enabled. */
    OFF,

    /** Protection is on and the engine is still loading. */
    STARTING,

    /** The rules and the AI check both work. */
    FULL,

    /** Only the rules work: the model could not be loaded. [ScamEngine.start] tries again. */
    LIMITED,

    /** Nothing is checked: the data files could not be read. */
    UNAVAILABLE,

    ;

    companion object {
        fun of(
            isOn: Boolean,
            serviceEnabled: Boolean,
            engine: EngineState,
        ): ProtectionLevel =
            when {
                !isOn || !serviceEnabled -> OFF
                engine == EngineState.READY -> FULL
                engine == EngineState.LIMITED -> LIMITED
                engine == EngineState.FAILED -> UNAVAILABLE
                else -> STARTING
            }
    }
}
