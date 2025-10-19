package com.nullpointerexception.cityeye

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.nullpointerexception.cityeye.data.ProblemPreviewViewModel
import com.nullpointerexception.cityeye.databinding.ActivityProblemPreviewBinding
import java.io.File


class ProblemPreview : AppCompatActivity() {

    private lateinit var binding: ActivityProblemPreviewBinding
    private lateinit var viewModel: ProblemPreviewViewModel
    var problemTypeString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProblemPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.loadIndicator.hide()

        viewModel = ViewModelProvider(this)[ProblemPreviewViewModel::class.java]

        viewModel.setCoordinates(intent.getParcelableExtra("coordinates"))
        viewModel.setImage(intent.getSerializableExtra("image") as File)

        viewModel.getAddressFromLocation(
            this,
            LatLng(viewModel.coordinates.value!!.latitude, viewModel.coordinates.value!!.longitude)
        )

        viewModel.address.observe(this) {
            if (it.isNullOrEmpty()) {
                finish()
            } else {
                setAddressText(it)
            }
        }

        setProblemImage()

        viewModel.fetchEvents()
        viewModel.fetchMarkers()

        viewModel.getProblemTypes()
        viewModel.problemType.observe(this) {
            (binding.selector as? MaterialAutoCompleteTextView)?.setAdapter(
                ArrayAdapter(
                    this,
                    R.layout.spinner_item,
                    viewModel.problemType.value?.map { it.type }?.toList() ?: emptyList()
                )
            )
        }

        (binding.selector as? MaterialAutoCompleteTextView)?.setOnItemClickListener { adapterView, view, i, l ->
            val problemType = viewModel.problemType.value?.get(i)?.type ?: ""
            if (problemType == "Event" || problemType == "Marker") {
                binding.spinnerEvent.visibility = View.VISIBLE
                problemTypeString = problemType
                if (problemType == "Event") {
                    binding.spinnerEvent.hint = "Event"
                    (binding.selectorEvent as? MaterialAutoCompleteTextView)?.setAdapter(
                        ArrayAdapter(
                            this,
                            R.layout.spinner_item,
                            viewModel.events.value?.map { it.title }?.toList() ?: emptyList()
                        )
                    )
                } else {
                    binding.spinnerEvent.hint = "Marker"
                    (binding.selectorEvent as? MaterialAutoCompleteTextView)?.setAdapter(
                        ArrayAdapter(
                            this,
                            R.layout.spinner_item,
                            viewModel.markers.value?.map { it.address?.split(",")!![0].trim() }
                                ?.toList() ?: emptyList()
                        )
                    )
                }
            } else {
                binding.spinnerEvent.visibility = View.GONE
                problemTypeString = ""
            }

            binding.fab.setOnClickListener {

                if (!binding.problemTitleEditText.checkIfCharactersExceed(50) && !binding.problemDescriptionEditText.checkIfCharactersExceed(
                        200
                    )
                ) {

                    if (!binding.problemTitleEditText.checkIfHasLessAmountOfCharacters() && !binding.problemDescriptionEditText.checkIfHasLessAmountOfCharacters()) {

                        when (problemTypeString) {
                            "Marker" -> {
                                viewModel.addProblem(
                                    this,
                                    binding.problemTitleEditText.text(),
                                    binding.problemDescriptionEditText.text(),
                                    viewModel.image.value!!,
                                    viewModel.coordinates.value!!,
                                    viewModel.address.value!!,
                                    binding.spinner.text(),
                                    null,
                                    binding.spinnerEvent.text()
                                )
                            }
                            "Event" -> {
                                viewModel.addProblem(
                                    this,
                                    binding.problemTitleEditText.text(),
                                    binding.problemDescriptionEditText.text(),
                                    viewModel.image.value!!,
                                    viewModel.coordinates.value!!,
                                    viewModel.address.value!!,
                                    binding.spinner.text(),
                                    binding.spinnerEvent.text(),
                                    null
                                )
                            }
                            else -> {
                                viewModel.addProblem(
                                    this,
                                    binding.problemTitleEditText.text(),
                                    binding.problemDescriptionEditText.text(),
                                    viewModel.image.value!!,
                                    viewModel.coordinates.value!!,
                                    viewModel.address.value!!,
                                    binding.spinner.text(),
                                    null,
                                    null
                                )
                            }
                        }

                        binding.loadIndicator.show()
                        binding.fab.isEnabled = false

                    } else {
                        Snackbar.make(
                            window.decorView.rootView,
                            resources.getString(R.string.titleOrDescriptionNotLongEnough),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Snackbar.make(
                        window.decorView.rootView,
                        resources.getString(R.string.titleOrDescriptionTooLong),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }


            binding.back.setOnClickListener {
                finish()
            }

            viewModel.response.observe(this) { response ->
                binding.layout.children.iterator().forEach { it.visibility = View.INVISIBLE }
                if (response) {
                    binding.animationDone.visibility = View.VISIBLE
                    binding.animationDone.playAnimation()
                    delay(3000)
                } else {
                    binding.animationError.visibility = View.VISIBLE
                    binding.animationError.playAnimation()
                    delay(4000)
                }
            }
        }
    }

    private fun delay(milis: Long) {
        Handler().postDelayed({
            finish()
        }, milis)
    }

    private fun setAddressText(address: String) {
        binding.address.text = address
    }

    private fun setProblemImage() {
        binding.problemImage.load(viewModel.image.value)
    }

    fun TextInputLayout.text() = this.editText?.text.toString()

    private fun TextInputLayout.checkIfCharactersExceed(amount: Int) = this.text().length > amount

    private fun TextInputLayout.checkIfHasLessAmountOfCharacters() = this.text().length < 5

}