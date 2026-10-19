package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.model.Catalog

class ServicesFragment : Fragment(R.layout.fragment_services) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = view.findViewById<RecyclerView>(R.id.servicesList)
        list.layoutManager = GridLayoutManager(requireContext(), 2)
        list.adapter = ServiceAdapter(Catalog.services) { service ->
            (activity as MainActivity).openBook(service.id)
        }
    }
}
