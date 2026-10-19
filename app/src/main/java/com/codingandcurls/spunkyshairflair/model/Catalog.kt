package com.codingandcurls.spunkyshairflair.model

/** Same services, stylists and time slots as the website (js/data.js). */
object Catalog {
    val services = listOf(
        Service(1, "Wash & Blow", 150, "45 min"),
        Service(2, "Trim & Style", 120, "30 min"),
        Service(3, "Box Braids", 450, "3-4 hours"),
        Service(4, "Sew-in Weave", 350, "2-3 hours"),
        Service(5, "Relaxer Treatment", 250, "1.5 hours"),
        Service(6, "Full Style & Blowout", 200, "1 hour")
    )

    val stylists = listOf(
        Stylist(1, "Asenathi Valashiya", "Braiding Specialist", 4.9, 24),
        Stylist(2, "Siphokazi Nofemele", "Cuts & Relaxers", 4.8, 18),
        Stylist(3, "Rethabile Minnaar", "Weaves & Extensions", 4.9, 31),
        Stylist(4, "Luna Summerton", "Barber", 4.8, 22)
    )

    val timeSlots = listOf(
        "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM"
    )
}
