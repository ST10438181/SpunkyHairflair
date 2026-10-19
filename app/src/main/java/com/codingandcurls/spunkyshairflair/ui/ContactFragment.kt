package com.codingandcurls.spunkyshairflair.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.codingandcurls.spunkyshairflair.R

class ContactFragment : Fragment(R.layout.fragment_contact) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        view.findViewById<View>(R.id.cardVisit).setOnClickListener {
            context.openExternal(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Mandalay, Mitchell's Plain, Cape Town")))
        }
        view.findViewById<View>(R.id.cardCall).setOnClickListener {
            context.openExternal(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+27812345678")))
        }
        view.findViewById<View>(R.id.cardEmail).setOnClickListener {
            context.openExternal(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hello@spunkyshairflair.co.za")))
        }
        view.findViewById<View>(R.id.cardFollow).setOnClickListener {
            context.openExternal(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/spunkyshairflair")))
        }
    }
}
