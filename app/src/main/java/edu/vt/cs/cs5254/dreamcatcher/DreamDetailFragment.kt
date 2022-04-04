package edu.vt.cs.cs5254.dreamcatcher

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import android.text.format.DateFormat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import java.util.UUID

private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"

private val REFLECTION_BUTTON_COLOR = "#1aadf0"
private val CONCEIVED_BUTTON_COLOR = "#00942a"
private val DEFERRED_BUTTON_COLOR = "#454545"
private val FULFILLED_BUTTON_COLOR = "#99004d"

class DreamDetailFragment : Fragment() {

    //TODO
    private lateinit var dreamWithEntries: DreamWithEntries

    private var _binding: FragmentDreamDetailBinding? = null
    private val binding get() = _binding!! // !!guarantee that this is not null

    lateinit var entriesButtonList: List<Button>

//    private val viewModel: DreamDetailViewModel by lazy {
//        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
//    }

    private val viewModel: DreamDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        //TODO we no longer have a crime property in the view-model
        super.onCreate(savedInstanceState)
        dreamWithEntries = DreamWithEntries(Dream(), emptyList())
        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        viewModel.loadDreamWithEntries(dreamId)
        Log.d(TAG, "Dream detail fragment for dream with ID $dreamId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root

//        binding.dreamTitleText.setText(dreamWithEntries.dream.title)

//        if (dreamWithEntries.dream.isFulfilled) {
//            binding.dreamFulfilledCheckbox.isChecked = dreamWithEntries.dream.isFulfilled
//            binding.dreamDeferredCheckbox.isEnabled = false
//        }
//
//        if (dreamWithEntries.dream.isDeferred) {
//            binding.dreamDeferredCheckbox.isChecked = dreamWithEntries.dream.isDeferred
//            binding.dreamFulfilledCheckbox.isEnabled = false
//        }

        // ------------------------------------------------------
        // Initialize entries-button list
        // ------------------------------------------------------

        entriesButtonList = binding.root
            .children
            .toList()
            .filterIsInstance<Button>()

//        entriesButtonList.zip(dreamWithEntries.dreamEntries) { btn, ent ->
//            btn.visibility = View.VISIBLE
//            when (ent.kind) {
//                DreamEntryKind.CONCEIVED -> {
//                    btn.text = "CONCEIVED"
//                    setButtonColor(btn, CONCEIVED_BUTTON_COLOR, Color.WHITE)
//                }
//                DreamEntryKind.REFLECTION -> {
//                    val time = DateFormat.format("MMM dd, yyyy", ent.date)
//                    btn.text = time.toString() + ": " + ent.text
//                    setButtonColor(btn, REFLECTION_BUTTON_COLOR, Color.BLACK)
//                }
//                DreamEntryKind.FULFILLED -> {
//                    btn.text = "FULFILLED"
//                    setButtonColor(btn, FULFILLED_BUTTON_COLOR, Color.WHITE)
//                }
//                DreamEntryKind.DEFERRED -> {
//                    btn.text = "DEFERRED"
//                    setButtonColor(btn, DEFERRED_BUTTON_COLOR, Color.WHITE)
//                }
//            }
//
//        }

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dreamWithEntriesLiveData.observe(
            viewLifecycleOwner, Observer { dreamWithEntries ->
                dreamWithEntries?.let {
                }
                this.dreamWithEntries = dreamWithEntries
                refreshView()
            })
    }

    override fun onStart() {
        super.onStart()
//        val titleWatcher = object : TextWatcher {
//            override fun beforeTextChanged(
//                sequence: CharSequence?, start: Int, count: Int, after: Int
//            ) {
//            }
//
//            override fun onTextChanged(
//                sequence: CharSequence?,
//                start: Int, before: Int, count: Int
//            ) {
//                dreamWithEntries.dream.title = sequence.toString()
//            }
//
//            override fun afterTextChanged(sequence: Editable?) {}
//        }
//        binding.dreamTitleText.addTextChangedListener(titleWatcher)

        binding.dreamTitleText.doOnTextChanged { text, start, before, count ->
            dreamWithEntries.dream.title = text.toString()
        }

        binding.dreamFulfilledCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isFulfilled = isChecked
                if (isChecked) {
                    val dreamId = dreamWithEntries.dream.id
                    if (!dreamWithEntries.dreamEntries.any { it.kind == DreamEntryKind.FULFILLED }) {
                        dreamWithEntries.dreamEntries += DreamEntry(
                            kind = DreamEntryKind.FULFILLED,
                            dreamId = dreamId
                        )
                    }

                } else {
                    val newEntries = dreamWithEntries.dreamEntries.toMutableList()
                    newEntries.removeLast()
                    dreamWithEntries.dreamEntries = newEntries
                }
                refreshView()
            }
        }
        binding.dreamDeferredCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isDeferred = isChecked
                if (isChecked) {
                    val dreamId = dreamWithEntries.dream.id
                    if (!dreamWithEntries.dreamEntries.any { it.kind == DreamEntryKind.DEFERRED }) {
                        dreamWithEntries.dreamEntries += DreamEntry(
                            kind = DreamEntryKind.DEFERRED,
                            dreamId = dreamId
                        )
                    }
                } else {
                    val newEntries = dreamWithEntries.dreamEntries.toMutableList()
                    newEntries.removeLast()
                    dreamWithEntries.dreamEntries = newEntries
                }
                refreshView()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDreamWithEntries(dreamWithEntries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshView() {

        binding.dreamTitleText.setText(dreamWithEntries.dream.title)

        when {
            dreamWithEntries.dream.isFulfilled -> {
                binding.dreamFulfilledCheckbox.isChecked = true
                binding.dreamDeferredCheckbox.isEnabled = false
            }
            dreamWithEntries.dream.isDeferred -> {
                binding.dreamDeferredCheckbox.isChecked = true
                binding.dreamFulfilledCheckbox.isEnabled = false
            }
            else -> {
                binding.dreamFulfilledCheckbox.isChecked = false
                binding.dreamDeferredCheckbox.isChecked = false
                binding.dreamFulfilledCheckbox.isEnabled = true
                binding.dreamDeferredCheckbox.isEnabled = true
            }
        }

        entriesButtonList.forEach { it.visibility = View.INVISIBLE }

        entriesButtonList.zip(dreamWithEntries.dreamEntries) { btn, ent ->
            btn.visibility = View.VISIBLE
            when (ent.kind) {
                DreamEntryKind.CONCEIVED -> {
                    btn.text = "CONCEIVED"
                    setButtonColor(btn, CONCEIVED_BUTTON_COLOR, Color.WHITE)
                }
                DreamEntryKind.REFLECTION -> {
                    val time = DateFormat.format("MMM dd, yyyy", ent.date)
                    btn.text = time.toString() + ": " + ent.text
                    setButtonColor(btn, REFLECTION_BUTTON_COLOR, Color.BLACK)
                }
                DreamEntryKind.FULFILLED -> {
                    btn.text = "FULFILLED"
                    setButtonColor(btn, FULFILLED_BUTTON_COLOR, Color.WHITE)
                }
                DreamEntryKind.DEFERRED -> {
                    btn.text = "DEFERRED"
                    setButtonColor(btn, DEFERRED_BUTTON_COLOR, Color.WHITE)
                }
            }

        }

    }

    private fun setButtonColor(button: Button, colorString: String, textColor: Int) {
        button.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorString))
        button.setTextColor(textColor)
    }

    companion object {
        fun newInstance(dreamId: UUID): DreamDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DREAM_ID, dreamId)
            }
            return DreamDetailFragment().apply {
                arguments = args
            }
        }
    }

}