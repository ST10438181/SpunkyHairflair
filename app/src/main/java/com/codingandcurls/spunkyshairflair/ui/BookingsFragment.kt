package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.ApiResult
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.model.Booking
import kotlinx.coroutines.launch

class BookingsFragment : Fragment(R.layout.fragment_bookings) {
    private lateinit var list: RecyclerView
    private lateinit var emptyState: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = view.findViewById(R.id.bookingsList)
        emptyState = view.findViewById(R.id.emptyState)
        list.layoutManager = LinearLayoutManager(requireContext())
        view.findViewById<Button>(R.id.emptyBookButton).setOnClickListener {
            (activity as MainActivity).openBook(null)
        }
        refresh()
    }

    /** Loads this customer's bookings from the API (already sorted by date and time). */
    private fun refresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = Repos.bookings(requireContext()).getMine()) {
                is ApiResult.Success -> {
                    val items = result.value
                    list.adapter = BookingAdapter(items) { booking -> confirmCancel(booking) }
                    emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    list.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                }
                is ApiResult.Failure -> requireContext().handleFailure(result)
            }
        }
    }

    private fun confirmCancel(booking: Booking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel appointment?")
            .setMessage("${booking.service} with ${booking.stylistName} on ${booking.date} at ${booking.time}")
            .setPositiveButton("Yes, cancel") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (val result = Repos.bookings(requireContext()).cancel(booking.id)) {
                        is ApiResult.Success -> {
                            toast("Booking cancelled successfully")
                            refresh()
                        }
                        is ApiResult.Failure -> requireContext().handleFailure(result)
                    }
                }
            }
            .setNegativeButton("Keep it", null)
            .show()
    }
}
