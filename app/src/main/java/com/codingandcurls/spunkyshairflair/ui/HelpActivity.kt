package com.codingandcurls.spunkyshairflair.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.codingandcurls.spunkyshairflair.R

/** In-app user support: frequently asked questions plus call / email buttons. */
class HelpActivity : AppCompatActivity() {
    private val faqs = listOf(
        "How do I book an appointment?" to "Open the Book tab, then choose a service, a stylist, a date and a time. Review the summary and tap Confirm Booking.",
        "How much is the deposit?" to "A 50% deposit of the service price secures your slot. The balance is paid at the salon. The summary shows both amounts before you confirm.",
        "Can I cancel an appointment?" to "Yes. Open My Bookings and tap Cancel on the appointment. The slot becomes available to other customers again.",
        "Why can't I pick a certain time?" to "Times already booked with your chosen stylist are greyed out and times that have passed are hidden. Choose another time or stylist.",
        "When is the salon open?" to "Monday to Saturday, 9am to 6pm. We are closed on Sundays, so Sundays are not offered when booking.",
        "How do I change my details or password?" to "Tap the menu (three dots) at the top right and choose Settings.",
        "Who are the stylists?" to "Asenathi (braiding), Siphokazi (cuts and relaxers), Rethabile (weaves and extensions) and Luna (barber).",
        "I forgot my password." to "Please call the salon on +27 81 234 5678 or email hello@spunkyshairflair.co.za and we will help you regain access."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_help, hasToolbar = true)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Help & Support"

        val container = findViewById<LinearLayout>(R.id.faqContainer)
        faqs.forEach { (question, answer) ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_faq, container, false)
            row.findViewById<TextView>(R.id.faqQuestion).text = question
            row.findViewById<TextView>(R.id.faqAnswer).text = answer
            container.addView(row)
        }

        findViewById<Button>(R.id.helpCall).setOnClickListener {
            openExternal(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+27812345678")))
        }
        findViewById<Button>(R.id.helpEmail).setOnClickListener {
            openExternal(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hello@spunkyshairflair.co.za")))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
