package com.codingandcurls.spunkyshairflair.logic

import com.codingandcurls.spunkyshairflair.model.Booking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pricing: a 50% deposit secures the slot, the balance is paid at the salon. */
object BookingCalculator {
    fun deposit(price: Int): Int = Math.round(price * 0.5).toInt()
    fun balance(price: Int): Int = price - deposit(price)
}

/** The salon is open Monday to Saturday, so Sundays are skipped. */
object BookingDates {
    fun openDays(from: LocalDate, count: Int): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var day = from
        while (result.size < count) {
            if (day.dayOfWeek != DayOfWeek.SUNDAY) result.add(day)
            day = day.plusDays(1)
        }
        return result
    }

    private val SLOT_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    fun isPast(date: LocalDate, slot: String, now: LocalDateTime): Boolean {
        val time = LocalTime.parse(slot, SLOT_FORMAT)
        return !LocalDateTime.of(date, time).isAfter(now)
    }
}

object SlotChecker {
    /** True when this stylist already has a booking at that date and time (prevents double-booking). */
    fun isTaken(bookings: List<Booking>, stylistName: String, date: String, time: String): Boolean =
        bookings.any { it.stylistName == stylistName && it.date == date && it.time == time && it.status != "cancelled" }
}
