package com.synzura.ctools.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.synzura.ctools.databinding.FragmentToolsBinding
import com.synzura.ctools.tools.ToolsAdapter
import com.synzura.ctools.tools.ToolsRepository

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!
    private lateinit var toolsAdapter: ToolsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadTools()
    }
    
    private fun setupRecyclerView() {
        toolsAdapter = ToolsAdapter()
        binding.toolsRecyclerview.apply {
            adapter = toolsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }
    
    private fun loadTools() {
        val tools = ToolsRepository.getAllTools(requireContext())
        toolsAdapter.submitList(tools)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 