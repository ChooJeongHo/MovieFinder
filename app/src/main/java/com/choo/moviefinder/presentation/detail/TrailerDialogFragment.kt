package com.choo.moviefinder.presentation.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.choo.moviefinder.R
import com.choo.moviefinder.databinding.DialogTrailerBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener

class TrailerDialogFragment : DialogFragment() {

    private var _binding: DialogTrailerBinding? = null
    private val binding get() = _binding!!

    private val videoKey: String by lazy {
        requireArguments().getString(ARG_VIDEO_KEY, "")
    }

    override fun getTheme(): Int = R.style.Theme_MovieFinder_TrailerDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTrailerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(binding.youtubePlayerView)

        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadVideo(videoKey, 0f)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_VIDEO_KEY = "video_key"

        fun newInstance(videoKey: String): TrailerDialogFragment {
            return TrailerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_KEY, videoKey)
                }
            }
        }
    }
}
