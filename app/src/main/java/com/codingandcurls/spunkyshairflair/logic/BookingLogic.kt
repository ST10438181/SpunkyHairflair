package com.codingandcurls.spunkyshairflair.logic

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pricing: a 50% deposit secures the slot, the balance is paid at the salon.
 * The API applies the same rule (half rounded up) when it stores the booking.
 */
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
}
