package com.synzura.ctools.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.synzura.ctools.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 处理状态栏高度
        binding.toolbar?.let { toolbar ->
            com.synzura.ctools.utils.StatusBarUtils.addStatusBarPadding(toolbar, requireContext())
        }
        
        // Initialize the profile screen components
        setupUI()
    }
    
    private fun setupUI() {
        // Setup profile screen components
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}