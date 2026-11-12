package com.codingandcurls.spunkyshairflair

import com.codingandcurls.spunkyshairflair.logic.BookingCalculator
import com.codingandcurls.spunkyshairflair.logic.BookingDates
import com.codingandcurls.spunkyshairflair.logic.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

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

class BookingLogicTests {
    @Test fun depositIsHalfAndBalanceIsRemainder() {
        assertEquals(225, BookingCalculator.deposit(450))
        assertEquals(225, BookingCalculator.balance(450))
        assertEquals(120, BookingCalculator.deposit(120) + BookingCalculator.balance(120))
    }

    @Test fun oddPricesRoundTheDepositUpLikeTheApi() {
        assertEquals(88, BookingCalculator.deposit(175))
        assertEquals(87, BookingCalculator.balance(175))
    }

    @Test fun openDaysNeverIncludesSunday() {
        val days = BookingDates.openDays(LocalDate.of(2026, 10, 15), 14)
        assertEquals(14, days.size)
        assertTrue(days.none { it.dayOfWeek == DayOfWeek.SUNDAY })
    }
}
