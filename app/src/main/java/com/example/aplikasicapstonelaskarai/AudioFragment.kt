package com.example.aplikasicapstonelaskarai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplikasicapstonelaskarai.databinding.FragmentAudioBinding

class AudioFragment : Fragment() {
    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private lateinit var ttsManager: TtsManager
    private var isPlaying = false
    private var script: String = ""
    private val TAG = "AudioFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        script = arguments?.getString("script") ?: ""
        Log.d(TAG, "Script received: $script")
        if (script.isEmpty()) {
            binding.textViewStatus.text = "Tidak ada skrip untuk diputar"
        } else {
            binding.textViewStatus.text = "Tekan tombol untuk memutar terapi"
        }

        ttsManager = TtsManager(requireContext()) { success, errorMessage ->
            activity?.runOnUiThread {
                if (!success) {
                    binding.textViewStatus.text = errorMessage ?: "Gagal menginisialisasi audio"
                    Log.e(TAG, "TtsManager initialization failed: $errorMessage")
                }
            }
        }

        binding.buttonPlayPause.setOnClickListener {
            if (isPlaying) {
                ttsManager.stop()
                binding.buttonPlayPause.setImageResource(R.drawable.play)
                binding.textViewStatus.text = "Audio dihentikan. Tekan untuk memutar lagi."
                isPlaying = false
            } else {
                if (script.isNotEmpty()) {
                    Log.d(TAG, "Playing script: $script")
                    ttsManager.speak(script) {
                        // Callback saat audio selesai
                        activity?.runOnUiThread {
                            binding.buttonPlayPause.setImageResource(R.drawable.play)
                            binding.textViewStatus.text = "Audio selesai. Tekan untuk memutar lagi."
                            isPlaying = false
                        }
                    }
                    binding.buttonPlayPause.setImageResource(R.drawable.stop)
                    binding.textViewStatus.text = "Memutar terapi..."
                    isPlaying = true
                } else {
                    binding.textViewStatus.text = "Tidak ada skrip untuk diputar"
                }
            }
        }

        binding.buttonFeedback.setOnClickListener {
            ttsManager.stop()
            binding.buttonPlayPause.setImageResource(R.drawable.play)
            isPlaying = false
            binding.textViewStatus.text = "Audio dihentikan."
            findNavController().navigate(R.id.action_audioFragment_to_feedbackFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager.shutdown()
        _binding = null
    }
}