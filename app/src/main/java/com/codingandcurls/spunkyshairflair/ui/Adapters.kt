package com.codingandcurls.spunkyshairflair.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.model.Booking
import com.codingandcurls.spunkyshairflair.model.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A selectable row used for the service and stylist steps of the booking flow. */
data class Option(val id: Int, val title: String, val subtitle: String, val trailing: String)

class OptionAdapter(
    private val options: List<Option>,
    private val onSelected: (Option) -> Unit
) : RecyclerView.Adapter<OptionAdapter.VH>() {

    private var selectedId: Int? = null

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val title: TextView = view.findViewById(R.id.optionTitle)
        val subtitle: TextView = view.findViewById(R.id.optionSubtitle)
        val trailing: TextView = view.findViewById(R.id.optionTrailing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_option, parent, false))

    override fun getItemCount(): Int = options.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val option = options[position]
        val context = holder.itemView.context
        val selected = option.id == selectedId
        holder.title.text = option.title
        holder.subtitle.text = option.subtitle
        holder.trailing.text = option.trailing
        val density = context.resources.displayMetrics.density
        holder.card.strokeWidth = ((if (selected) 3 else 1) * density).toInt()
        holder.card.strokeColor = ContextCompat.getColor(context, if (selected) R.color.primary else R.color.divider)
        holder.card.setCardBackgroundColor(ContextCompat.getColor(context, if (selected) R.color.primary_light else R.color.white))
        holder.itemView.setOnClickListener {
            selectedId = option.id
            notifyDataSetChanged()
            onSelected(option)
        }
    }

    fun select(id: Int) {
        selectedId = id
        notifyDataSetChanged()
    }
}

/** Service cards (name, price, duration, Book button) for Home and Services. */
class ServiceAdapter(
    private val services: List<Service>,
    private val onBook: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.serviceName)
        val price: TextView = view.findViewById(R.id.servicePrice)
        val duration: TextView = view.findViewById(R.id.serviceDuration)
        val book: Button = view.findViewById(R.id.serviceBook)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false))

    override fun getItemCount(): Int = services.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val service = services[position]
        holder.name.text = service.name
        holder.price.text = "R${service.price}"
        holder.duration.text = service.duration
        holder.book.setOnClickListener { onBook(service) }
        holder.itemView.setOnClickListener { onBook(service) }
    }
}

class BookingAdapter(
    private val bookings: List<Booking>,
    private val onCancel: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val service: TextView = view.findViewById(R.id.bookingService)
        val stylist: TextView = view.findViewById(R.id.bookingStylist)
        val dateTime: TextView = view.findViewById(R.id.bookingDateTime)
        val deposit: TextView = view.findViewById(R.id.bookingDeposit)
        val status: TextView = view.findViewById(R.id.bookingStatus)
        val cancel: Button = view.findViewById(R.id.bookingCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false))

    override fun getItemCount(): Int = bookings.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val booking = bookings[position]
        val pretty = LocalDate.parse(booking.date)
            .format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault()))
        holder.service.text = booking.service
        holder.stylist.text = "Stylist: ${booking.stylistName}"
        holder.dateTime.text = "$pretty at ${booking.time}"
        holder.deposit.text = "Deposit (50%): R${booking.deposit}"
        holder.status.text = booking.status.uppercase()
        holder.cancel.setOnClickListener { onCancel(booking) }
    }
}
