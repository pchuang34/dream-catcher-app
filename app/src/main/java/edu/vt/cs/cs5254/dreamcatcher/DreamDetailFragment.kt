package edu.vt.cs.cs5254.dreamcatcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.widget.Button
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import android.text.format.DateFormat
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import java.io.File
import java.util.UUID

private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"

private val REFLECTION_BUTTON_COLOR = "#1aadf0"
private val CONCEIVED_BUTTON_COLOR = "#00942a"
private val DEFERRED_BUTTON_COLOR = "#454545"
private val FULFILLED_BUTTON_COLOR = "#99004d"

const val REQUEST_KEY_ADD_REFLECTION = "request_key"
const val BUNDLE_KEY_REFLECTION_TEXT = "reflection_text"

class DreamDetailFragment : Fragment() {

    private lateinit var dreamWithEntries: DreamWithEntries

    private var _binding: FragmentDreamDetailBinding? = null
    private val binding get() = _binding!! // !!guarantee that this is not null
    private var adapter: DreamEntryAdapter? = null
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var photoLauncher: ActivityResultLauncher<Uri>

    private val viewModel: DreamDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        dreamWithEntries = DreamWithEntries(Dream(), emptyList())
        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        viewModel.loadDreamWithEntries(dreamId)
        Log.d(TAG, "Dream detail fragment for dream with ID $dreamId")
        setHasOptionsMenu(true)
        photoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                updatePhotoView()
            }
            requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)
        val cameraAvailable = PictureUtils.isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            Log.d(TAG, "Camera available: $cameraAvailable")
            isEnabled = cameraAvailable
            isVisible = cameraAvailable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.share_dream -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getSharedDream())
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.dream_share_subject)
                    )
                }.also { intent ->
                    val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.send_share))
                    startActivity(chooserIntent)
                }
                true
            }
            R.id.take_dream_photo -> {
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                requireActivity().packageManager
                    .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    .forEach { cameraActivity ->
                        requireActivity().grantUriPermission(
                            cameraActivity.activityInfo.packageName,
                            photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                photoLauncher.launch(photoUri)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun getSharedDream(): String {
        val stateString = when {
            dreamWithEntries.dream.isFulfilled -> {
                getString(R.string.share_dream_fulfilled)
            }
            dreamWithEntries.dream.isDeferred -> {
                getString(R.string.share_dream_deferred)
            }
            else -> {
                ""
            }
        }
        val dreamTitleString = dreamWithEntries.dream.title
        val conceivedDate = dreamWithEntries.dreamEntries.toMutableList().elementAt(0).date
        val dateString = DateFormat.format("MMM dd, yyyy", conceivedDate)
        var entriesTextList = mutableListOf<String>()
        dreamWithEntries.dreamEntries.filter { dreamEntry -> dreamEntry.kind == DreamEntryKind.REFLECTION }
            .forEach { dreamEntry -> entriesTextList += ("- " + dreamEntry.text) }
        val reflectionTextString = entriesTextList.joinToString(separator = "\n")
        return getString(
            R.string.dream_share,
            dreamTitleString,
            dateString,
            reflectionTextString,
            stateString
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)

        refreshView()
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dreamWithEntriesLiveData.observe(
            viewLifecycleOwner, Observer { dreamWithEntries ->
                dreamWithEntries?.let {
                    this.dreamWithEntries = dreamWithEntries
                    photoFile = viewModel.getPhotoFile(dreamWithEntries.dream)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        "edu.vt.cs.cs5254.dreamcatcher.fileprovider",
                        photoFile
                    )
                    refreshView()
                    updatePhotoView()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val itemTouchHelper = ItemTouchHelper(
            SwipeToDeleteCallback(
                binding.dreamEntryRecyclerView.adapter!! as DreamEntryAdapter,
                0,
                ItemTouchHelper.LEFT
            )
        )
        itemTouchHelper.attachToRecyclerView(binding.dreamEntryRecyclerView)

        binding.dreamTitleText.doOnTextChanged { text, start, before, count ->
            dreamWithEntries.dream.title = text.toString()
        }

        binding.dreamFulfilledCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isFulfilled = isChecked
                if (isChecked) {
                    //binding.addReflectionButton.isEnabled = false
                    val dreamId = dreamWithEntries.dream.id
                    if (!dreamWithEntries.dreamEntries.any { it.kind == DreamEntryKind.FULFILLED }) {
                        dreamWithEntries.dreamEntries += DreamEntry(
                            kind = DreamEntryKind.FULFILLED,
                            dreamId = dreamId
                        )
                    }

                } else {
                    val updatedEntries = dreamWithEntries.dreamEntries.toMutableList()
                        .filterNot { entry -> entry.kind == DreamEntryKind.FULFILLED }
                    dreamWithEntries.dreamEntries = updatedEntries
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
                    val updatedEntries = dreamWithEntries.dreamEntries.toMutableList()
                        .filterNot { entry -> entry.kind == DreamEntryKind.DEFERRED }
                    dreamWithEntries.dreamEntries = updatedEntries
                }
                refreshView()
            }
        }

        binding.addReflectionButton.setOnClickListener {
            AddReflectionDialog().show(parentFragmentManager, REQUEST_KEY_ADD_REFLECTION)
        }

        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_ADD_REFLECTION,
            viewLifecycleOwner
        )
        { _, bundle ->
            val reflectionText = bundle.getString(BUNDLE_KEY_REFLECTION_TEXT, "")

            val newDreamEntry = DreamEntry(
                dreamId = dreamWithEntries.dream.id,
                text = reflectionText,
                kind = DreamEntryKind.REFLECTION
            )

            dreamWithEntries.dreamEntries += newDreamEntry

            refreshView()
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
        adapter = DreamEntryAdapter(dreamWithEntries.dreamEntries)
        binding.dreamEntryRecyclerView.adapter = adapter

        when {
            dreamWithEntries.dream.isFulfilled -> {
                binding.dreamFulfilledCheckbox.isChecked = true
                binding.dreamDeferredCheckbox.isEnabled = false
                binding.addReflectionButton.isEnabled = false
            }
            dreamWithEntries.dream.isDeferred -> {
                binding.dreamDeferredCheckbox.isChecked = true
                binding.dreamFulfilledCheckbox.isEnabled = false
                binding.addReflectionButton.isEnabled = true
            }
            else -> {
                binding.dreamFulfilledCheckbox.isChecked = false
                binding.dreamDeferredCheckbox.isChecked = false
                binding.dreamFulfilledCheckbox.isEnabled = true
                binding.dreamDeferredCheckbox.isEnabled = true
                binding.addReflectionButton.isEnabled = true
            }
        }

        //updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = PictureUtils.getScaledBitmap(photoFile.path, 120, 120)
            binding.dreamPhoto.setImageBitmap(bitmap)
        } else {
            binding.dreamPhoto.setImageDrawable(null)
        }
    }

    private fun setButtonColor(button: Button, colorString: String, textColor: Int) {
        button.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorString))
        button.setTextColor(textColor)
    }

    private fun updateEntryButton(entryButton: Button, dreamEntry: DreamEntry) {
        when (dreamEntry.kind) {
            DreamEntryKind.CONCEIVED -> {
                entryButton.text = "CONCEIVED"
                setButtonColor(entryButton, CONCEIVED_BUTTON_COLOR, Color.WHITE)
            }
            DreamEntryKind.REFLECTION -> {
                val time = DateFormat.format("MMM dd, yyyy", dreamEntry.date)
                entryButton.text = time.toString() + ": " + dreamEntry.text
                setButtonColor(entryButton, REFLECTION_BUTTON_COLOR, Color.BLACK)
            }
            DreamEntryKind.FULFILLED -> {
                entryButton.text = "FULFILLED"
                setButtonColor(entryButton, FULFILLED_BUTTON_COLOR, Color.WHITE)
            }
            DreamEntryKind.DEFERRED -> {
                entryButton.text = "DEFERRED"
                setButtonColor(entryButton, DEFERRED_BUTTON_COLOR, Color.WHITE)
            }
        }
    }

    // DreamEntryHolder && DreamEntryAdapter
    inner class DreamEntryHolder(val itemBinding: ListItemDreamEntryBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
        private lateinit var dreamEntry: DreamEntry

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(dreamEntry: DreamEntry) {
            this.dreamEntry = dreamEntry
            updateEntryButton(itemBinding.dreamEntryButton, dreamEntry)
        }

        override fun onClick(v: View) {
//            callbacks?.onDreamSelected(dream.id)
        }
    }

    private inner class DreamEntryAdapter(var dreamEntries: List<DreamEntry>) :
        RecyclerView.Adapter<DreamDetailFragment.DreamEntryHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DreamDetailFragment.DreamEntryHolder {
            val itemBinding = ListItemDreamEntryBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamEntryHolder(itemBinding)
        }

        override fun getItemCount() = dreamEntries.size
        override fun onBindViewHolder(holder: DreamDetailFragment.DreamEntryHolder, position: Int) {
            val dream = dreamEntries[position]
            holder.bind(dream)
        }

        fun deleteItem(position: Int) {
            val recentlyDeletedItem = dreamEntries[position];
            val recentlyDeletedItemPosition = position;
            if (recentlyDeletedItem.kind == DreamEntryKind.REFLECTION) {
                dreamWithEntries.dreamEntries =
                    dreamWithEntries.dreamEntries.filter { it.id != recentlyDeletedItem.id }
                //notifyItemRemoved(position)
            }
            refreshView()
        }
    }

    private inner class SwipeToDeleteCallback(
        adapter: DreamEntryAdapter,
        dragDirs: Int,
        swipeDirs: Int
    ) :
        ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position: Int = viewHolder.adapterPosition
            adapter?.deleteItem(position)
        }

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