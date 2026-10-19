package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.logic.BookingCalculator
import com.codingandcurls.spunkyshairflair.logic.BookingDates
import com.codingandcurls.spunkyshairflair.logic.SlotChecker
import com.codingandcurls.spunkyshairflair.model.Booking
import com.codingandcurls.spunkyshairflair.model.Catalog
import com.codingandcurls.spunkyshairflair.model.Service
import com.codingandcurls.spunkyshairflair.model.Stylist
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Four step-through screens: 1 Service, 2 Stylist, 3 Date & Time, 4 Confirm. */
class BookFragment : Fragment(R.layout.fragment_book) {
    private var step = 1
    private var selectedService: Service? = null
    private var selectedStylist: Stylist? = null
    private var selectedDate: LocalDate? = null
    private var selectedTime: String? = null

    private lateinit var progress: LinearProgressIndicator
    private lateinit var stepLabel: TextView
    private lateinit var serviceList: RecyclerView
    private lateinit var stylistList: RecyclerView
    private lateinit var dateTimeStep: View
    private lateinit var summaryStep: View
    private lateinit var dateGroup: ChipGroup
    private lateinit var timeGroup: ChipGroup
    private lateinit var timeHint: TextView
    private lateinit var summaryText: TextView
    private lateinit var backButton: Button
    private lateinit var nextButton: Button
    private lateinit var serviceAdapter: OptionAdapter

    private val titles = listOf("Choose Your Service", "Choose Your Stylist", "Select Date & Time", "Booking Summary")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress = view.findViewById(R.id.stepProgress)
        stepLabel = view.findViewById(R.id.stepLabel)
        serviceList = view.findViewById(R.id.serviceList)
        stylistList = view.findViewById(R.id.stylistList)
        dateTimeStep = view.findViewById(R.id.dateTimeStep)
        summaryStep = view.findViewById(R.id.summaryStep)
        dateGroup = view.findViewById(R.id.dateGroup)
        timeGroup = view.findViewById(R.id.timeGroup)
        timeHint = view.findViewById(R.id.timeHint)
        summaryText = view.findViewById(R.id.summaryText)
        backButton = view.findViewById(R.id.backButton)
        nextButton = view.findViewById(R.id.nextButton)

        serviceAdapter = OptionAdapter(Catalog.services.map {
            Option(it.id, it.name, it.duration, "R${it.price}")
        }) { option -> selectedService = Catalog.services.first { it.id == option.id } }
        serviceList.layoutManager = LinearLayoutManager(requireContext())
        serviceList.adapter = serviceAdapter

        stylistList.layoutManager = LinearLayoutManager(requireContext())
        stylistList.adapter = OptionAdapter(Catalog.stylists.map {
            Option(it.id, it.name, it.specialty, "★ ${it.rating}")
        }) { option -> selectedStylist = Catalog.stylists.first { it.id == option.id } }

        buildDateChips()
        dateGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedDate = if (checkedIds.isEmpty()) null
            else group.findViewById<Chip>(checkedIds[0]).tag as LocalDate
            renderTimes(keep = null)
        }
        timeGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedTime = if (checkedIds.isEmpty()) null
            else group.findViewById<Chip>(checkedIds[0]).tag as String
        }

        (activity as? MainActivity)?.consumePreselectedService()?.let { id ->
            selectedService = Catalog.services.firstOrNull { it.id == id }
            serviceAdapter.select(id)
        }

        backButton.setOnClickListener { if (step > 1) showStep(step - 1) }
        nextButton.setOnClickListener { onNext() }
        showStep(1)
    }

    private fun buildDateChips() {
        val format = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
        BookingDates.openDays(LocalDate.now(), 14).forEach { date ->
            val chip = layoutInflater.inflate(R.layout.item_chip_choice, dateGroup, false) as Chip
            chip.id = View.generateViewId()
            chip.text = date.format(format)
            chip.tag = date
            dateGroup.addView(chip)
        }
    }

    /** Rebuilds the time chips: past slots are hidden, slots already booked with this stylist are disabled. */
    private fun renderTimes(keep: String?) {
        selectedTime = null
        timeGroup.removeAllViews()
        val date = selectedDate
        if (date == null) {
            timeHint.text = "Pick a date to see available times"
            timeHint.visibility = View.VISIBLE
            return
        }
        val bookings = Repos.bookings(requireContext()).getAll()
        val now = LocalDateTime.now()
        var added = 0
        Catalog.timeSlots.forEach { slot ->
            if (BookingDates.isPast(date, slot, now)) return@forEach
            val taken = selectedStylist?.let { SlotChecker.isTaken(bookings, it.name, date.toString(), slot) } ?: false
            val chip = layoutInflater.inflate(R.layout.item_chip_choice, timeGroup, false) as Chip
            chip.id = View.generateViewId()
            chip.text = if (taken) "$slot (booked)" else slot
            chip.tag = slot
            chip.isEnabled = !taken
            if (slot == keep && !taken) chip.isChecked = true
            timeGroup.addView(chip)
            added++
        }
        timeHint.text = "No slots left on this day - try another date"
        timeHint.visibility = if (added == 0) View.VISIBLE else View.GONE
    }

    private fun onNext() {
        when (step) {
            1 -> if (selectedService == null) toast("Please choose a service") else showStep(2)
            2 -> if (selectedStylist == null) toast("Please choose a stylist") else showStep(3)
            3 -> when {
                selectedDate == null -> toast("Please select a date")
                selectedTime == null -> toast("Please select a time")
                else -> showStep(4)
            }
            else -> confirmBooking()
        }
    }

    private fun showStep(newStep: Int) {
        val previousTime = selectedTime
        step = newStep
        serviceList.visibility = if (step == 1) View.VISIBLE else View.GONE
        stylistList.visibility = if (step == 2) View.VISIBLE else View.GONE
        dateTimeStep.visibility = if (step == 3) View.VISIBLE else View.GONE
        summaryStep.visibility = if (step == 4) View.VISIBLE else View.GONE
        stepLabel.text = "Step $step of 4 - ${titles[step - 1]}"
        progress.setProgressCompat(step, true)
        backButton.visibility = if (step > 1) View.VISIBLE else View.INVISIBLE
        nextButton.text = if (step == 4) "Confirm Booking" else "Next"
        if (step == 3) renderTimes(keep = previousTime)   // availability depends on the chosen stylist
        if (step == 4) renderSummary()
    }

    private fun renderSummary() {
        val service = selectedService ?: return
        val stylist = selectedStylist ?: return
        val date = selectedDate ?: return
        val prettyDate = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
        summaryText.text = buildString {
            appendLine("Service:  ${service.name}")
            appendLine("Stylist:  ${stylist.name}")
            appendLine("Date:  $prettyDate")
            appendLine("Time:  $selectedTime")
            appendLine()
            appendLine("Total:  R${service.price}")
            appendLine("Deposit (50%):  R${BookingCalculator.deposit(service.price)}")
            append("Balance at salon:  R${BookingCalculator.balance(service.price)}")
        }
    }

    private fun confirmBooking() {
        val context = requireContext()
        val user = Repos.users(context).getCurrentUser() ?: return
        val service = selectedService ?: return
        val stylist = selectedStylist ?: return
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val repo = Repos.bookings(context)

        if (SlotChecker.isTaken(repo.getAll(), stylist.name, date.toString(), time)) {
            toast("That time was just taken - please choose another")
            showStep(3)
            return
        }
        repo.add(Booking(
            id = repo.nextId(),
            userId = user.id,
            service = service.name,
            price = service.price,
            stylistName = stylist.name,
            date = date.toString(),
            time = time,
            deposit = BookingCalculator.deposit(service.price),
            status = "confirmed"
        ))
        toast("Booking confirmed for ${service.name}!")
        (activity as MainActivity).showBookings()
    }
}
