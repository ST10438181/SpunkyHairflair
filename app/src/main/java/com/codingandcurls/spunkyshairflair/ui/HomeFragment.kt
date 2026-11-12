package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.ApiResult
import com.codingandcurls.spunkyshairflair.data.Repos
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var greeting: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        greeting = view.findViewById(R.id.greeting)
        val main = activity as MainActivity

        view.findViewById<Button>(R.id.heroBookButton).setOnClickListener { main.openBook(null) }

        val grid = view.findViewById<RecyclerView>(R.id.homeServices)
        grid.layoutManager = GridLayoutManager(requireContext(), 2)
        val team = view.findViewById<RecyclerView>(R.id.homeStylists)
        team.layoutManager = LinearLayoutManager(requireContext())

        // Services and stylists come from the API (database), not from hard-coded lists.
        viewLifecycleOwner.lifecycleScope.launch {
            val catalog = Repos.catalog(requireContext())
            when (val services = catalog.services()) {
                is ApiResult.Success ->
                    grid.adapter = ServiceAdapter(services.value) { service -> main.openBook(service.id) }
                is ApiResult.Failure -> toast(services.message)
            }
            when (val stylists = catalog.stylists()) {
                is ApiResult.Success ->
                    team.adapter = OptionAdapter(stylists.value.map {
                        Option(it.id, it.name, it.specialty, "★ ${it.rating}")
                    }) { }
                is ApiResult.Failure -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val user = Repos.users(requireContext()).getCurrentUser()
        greeting.text = if (user != null) "Welcome back, ${user.name.substringBefore(' ')}!" else ""
    }
}
