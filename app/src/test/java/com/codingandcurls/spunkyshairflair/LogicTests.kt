package com.codingandcurls.spunkyshairflair

import com.codingandcurls.spunkyshairflair.logic.BookingCalculator
import com.codingandcurls.spunkyshairflair.logic.BookingDates
import com.codingandcurls.spunkyshairflair.logic.PasswordHasher
import com.codingandcurls.spunkyshairflair.logic.SlotChecker
import com.codingandcurls.spunkyshairflair.logic.Validators
import com.codingandcurls.spunkyshairflair.model.Booking
import com.codingandcurls.spunkyshairflair.model.Catalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ValidatorTests {
    @Test fun validName() = assertNull(Validators.nameError("Luna Summerton"))
    @Test fun shortNameRejected() = assertNotNull(Validators.nameError("A"))
    @Test fun digitsInNameRejected() = assertNotNull(Validators.nameError("Luna123"))
    @Test fun validEmail() = assertNull(Validators.emailError("luna@example.co.za"))
    @Test fun badEmailRejected() = assertNotNull(Validators.emailError("not-an-email"))
    @Test fun validPhone() {
        assertNull(Validators.phoneError("081 234 5678"))
        assertNull(Validators.phoneError("+27812345678"))
    }
    @Test fun badPhoneRejected() = assertNotNull(Validators.phoneError("12345"))
    @Test fun validPassword() = assertNull(Validators.passwordError("Curls2026"))
    @Test fun weakPasswordsRejected() {
        assertNotNull(Validators.passwordError("short1"))
        assertNotNull(Validators.passwordError("onlyletters"))
        assertNotNull(Validators.passwordError("12345678"))
    }
}

class PasswordHasherTests {
    @Test fun hashIsNotThePlainPassword() {
        val salt = PasswordHasher.newSalt()
        assertNotEquals("Curls2026", PasswordHasher.hash("Curls2026", salt))
    }
    @Test fun verifyAcceptsCorrectAndRejectsWrongPassword() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("Curls2026", salt)
        assertTrue(PasswordHasher.verify("Curls2026", salt, hash))
        assertFalse(PasswordHasher.verify("Wrong1234", salt, hash))
    }
    @Test fun sameSaltGivesSameHashDifferentSaltDoesNot() {
        val s1 = PasswordHasher.newSalt()
        val s2 = PasswordHasher.newSalt()
        assertEquals(PasswordHasher.hash("Curls2026", s1), PasswordHasher.hash("Curls2026", s1))
        assertNotEquals(PasswordHasher.hash("Curls2026", s1), PasswordHasher.hash("Curls2026", s2))
    }
}

class BookingLogicTests {
    @Test fun depositIsHalfAndBalanceIsRemainder() {
        assertEquals(225, BookingCalculator.deposit(450))
        assertEquals(225, BookingCalculator.balance(450))
        assertEquals(120, BookingCalculator.deposit(120) + BookingCalculator.balance(120))
    }

    @Test fun openDaysNeverIncludesSunday() {
        val days = BookingDates.openDays(LocalDate.of(2026, 10, 15), 14)
        assertEquals(14, days.size)
        assertTrue(days.none { it.dayOfWeek == DayOfWeek.SUNDAY })
    }

    @Test fun pastSlotsAreDetected() {
        val now = LocalDateTime.of(2026, 10, 15, 11, 30)
        val today = LocalDate.of(2026, 10, 15)
        assertTrue(BookingDates.isPast(today, "9:00 AM", now))
        assertFalse(BookingDates.isPast(today, "12:00 PM", now))
        assertFalse(BookingDates.isPast(today.plusDays(1), "9:00 AM", now))
    }

    @Test fun doubleBookingIsDetectedPerStylist() {
        val existing = listOf(
            Booking(1, 1, "Trim & Style", 120, "Luna Summerton", "2026-10-16", "10:00 AM", 60, "confirmed")
        )
        assertTrue(SlotChecker.isTaken(existing, "Luna Summerton", "2026-10-16", "10:00 AM"))
        assertFalse(SlotChecker.isTaken(existing, "Asenathi Valashiya", "2026-10-16", "10:00 AM"))
        assertFalse(SlotChecker.isTaken(existing, "Luna Summerton", "2026-10-16", "11:00 AM"))
    }
}

class CatalogTests {
    @Test fun matchesWebsiteCatalogue() {
        assertEquals(6, Catalog.services.size)
        assertEquals(8, Catalog.timeSlots.size)
        assertEquals(4, Catalog.stylists.size)
    }
    @Test fun lunaSummertonIsTheBarber() {
        val luna = Catalog.stylists.first { it.name == "Luna Summerton" }
        assertEquals("Barber", luna.specialty)
    }
}
