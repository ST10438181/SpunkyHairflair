package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.ApiResult
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.logic.BookingCalculator
import com.codingandcurls.spunkyshairflair.logic.BookingDates
import com.codingandcurls.spunkyshairflair.model.Service
import com.codingandcurls.spunkyshairflair.model.Stylist
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Four step-through screens: 1 Service, 2 Stylist, 3 Date & Time, 4 Confirm. */
class BookFragment : Fragment(R.layout.fragment_book) {
    private var step = 1
    private var services: List<Service> = emptyList()
    private var stylists: List<Stylist> = emptyList()
    private var selectedService: Service? = null
    private var selectedStylist: Stylist? = null
    private var selectedDate: LocalDate? = null
    private var selectedTime: String? = null
    private var timesJob: Job? = null
    private var submitting = false

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

        serviceList.layoutManager = LinearLayoutManager(requireContext())
        stylistList.layoutManager = LinearLayoutManager(requireContext())

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

        backButton.setOnClickListener { if (step > 1) showStep(step - 1) }
        nextButton.setOnClickListener { onNext() }
        showStep(1)
        loadCatalog()
    }

    /** Fetches services and stylists from the API, then fills the first two steps. */
    private fun loadCatalog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = Repos.catalog(requireContext())
            val servicesResult = catalog.services()
            val stylistsResult = catalog.stylists()
            val loadedServices = (servicesResult as? ApiResult.Success)?.value
            val loadedStylists = (stylistsResult as? ApiResult.Success)?.value
            if (loadedServices == null || loadedStylists == null) {
                val failure = (servicesResult as? ApiResult.Failure) ?: (stylistsResult as? ApiResult.Failure)
                if (failure != null) toast(failure.message)
                return@launch
            }
            services = loadedServices
            stylists = loadedStylists

            val serviceAdapter = OptionAdapter(services.map {
                Option(it.id, it.name, it.duration, "R${it.price}")
            }) { option -> selectedService = services.firstOrNull { it.id == option.id } }
            serviceList.adapter = serviceAdapter

            stylistList.adapter = OptionAdapter(stylists.map {
                Option(it.id, it.name, it.specialty, "★ ${it.rating}")
            }) { option -> selectedStylist = stylists.firstOrNull { it.id == option.id } }

            // Coming from a service card on Home / Services: pre-select that service.
            val preselected = (activity as? MainActivity)?.consumePreselectedService()
            if (preselected != null) {
                selectedService = services.firstOrNull { it.id == preselected }
                if (selectedService != null) serviceAdapter.select(preselected)
            }
        }
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

    /**
     * Asks the API which slots are free for the chosen stylist and date. Past slots are hidden and
     * slots already booked are disabled, so double-booking is prevented by the server's data.
     */
    private fun renderTimes(keep: String?) {
        selectedTime = null
        timesJob?.cancel()
        timeGroup.removeAllViews()
        val date = selectedDate
        val stylist = selectedStylist
        if (date == null || stylist == null) {
            timeHint.text = "Pick a date to see available times"
            timeHint.visibility = View.VISIBLE
            return
        }
        timeHint.text = "Loading available times..."
        timeHint.visibility = View.VISIBLE

        timesJob = viewLifecycleOwner.lifecycleScope.launch {
            when (val result = Repos.catalog(requireContext()).availability(stylist.id, date.toString())) {
                is ApiResult.Failure -> {
                    timeHint.text = "Couldn't load times - pick the date again to retry"
                    toast(result.message)
                }
                is ApiResult.Success -> {
                    var added = 0
                    for (slot in result.value) {
                        if (slot.reason == "past") continue
                        val taken = !slot.available
                        val chip = layoutInflater.inflate(R.layout.item_chip_choice, timeGroup, false) as Chip
                        chip.id = View.generateViewId()
                        chip.text = if (taken) "${slot.time} (booked)" else slot.time
                        chip.tag = slot.time
                        chip.isEnabled = !taken
                        timeGroup.addView(chip)
                        if (slot.time == keep && !taken) chip.isChecked = true
                        added++
                    }
                    timeHint.text = "No slots left on this day - try another date"
                    timeHint.visibility = if (added == 0) View.VISIBLE else View.GONE
                }
            }
        }
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

    /** Sends the booking to the API. The server re-checks the slot, so a clash comes back as a 409. */
    private fun confirmBooking() {
        if (submitting) return
        val service = selectedService ?: return
        val stylist = selectedStylist ?: return
        val date = selectedDate ?: return
        val time = selectedTime ?: return

        submitting = true
        nextButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = try {
                Repos.bookings(requireContext()).add(service.id, stylist.id, date.toString(), time)
            } finally {
                submitting = false
                nextButton.isEnabled = true
            }
            when (result) {
                is ApiResult.Success -> {
                    toast("Booking confirmed for ${service.name}!")
                    (activity as? MainActivity)?.showBookings()
                }
                is ApiResult.Failure -> {
                    requireContext().handleFailure(result)
                    if (result.code == 409) showStep(3)
                }
            }
        }
    }
}
