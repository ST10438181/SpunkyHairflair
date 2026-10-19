package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos
import com.codingandcurls.spunkyshairflair.model.Catalog

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var greeting: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        greeting = view.findViewById(R.id.greeting)
        val main = activity as MainActivity

        view.findViewById<Button>(R.id.heroBookButton).setOnClickListener { main.openBook(null) }

        val grid = view.findViewById<RecyclerView>(R.id.homeServices)
        grid.layoutManager = GridLayoutManager(requireContext(), 2)
        grid.adapter = ServiceAdapter(Catalog.services) { service -> main.openBook(service.id) }

        val team = view.findViewById<RecyclerView>(R.id.homeStylists)
        team.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        team.adapter = OptionAdapter(Catalog.stylists.map {
            Option(it.id, it.name, it.specialty, "★ ${it.rating}")
        }) { }
    }

    override fun onResume() {
        super.onResume()
        val user = Repos.users(requireContext()).getCurrentUser()
        greeting.text = if (user != null) "Welcome back, ${user.name.substringBefore(' ')}!" else ""
    }
}
