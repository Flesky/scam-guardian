package ph.scamguardian.ui.testmessage

/** A sample message for the quick-fill buttons. Separate chat bubbles are separate lines. */
data class SampleMessage(
    val label: String,
    val text: String,
)

val sampleMessages =
    listOf(
        SampleMessage("Fake link", "BDO reminds you that 6,552 points will expire today. Visit https://bdo-bd0.cc/ph"),
        SampleMessage("OTP request", "Hi po, may na-send akong 6-digit code sa number mo, paki-send naman po."),
        SampleMessage("Money request", "Bes emergency lang\nPa-GCash muna ng 3k ngayon na."),
        SampleMessage("Real OTP", "Your BDO OTP is 482913. Do not share this code with anyone."),
        SampleMessage("Padala", "anak padalhan mo naman ako\n5kyaw babalik ko rin bukas\nsensya na."),
    )
