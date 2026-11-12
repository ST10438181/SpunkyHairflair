package com.codingandcurls.spunkyshairflair.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.ApiResult
import com.codingandcurls.spunkyshairflair.data.Repos
import kotlinx.coroutines.launch

class ServicesFragment : Fragment(R.layout.fragment_services) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = view.findViewById<RecyclerView>(R.id.servicesList)
        list.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = Repos.catalog(requireContext()).services()) {
                is ApiResult.Success ->
                    list.adapter = ServiceAdapter(result.value) { service ->
                        (activity as MainActivity).openBook(service.id)
                    }
                is ApiResult.Failure -> toast(result.message)
            }
        }
    }
}
