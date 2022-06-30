/*
 * Copyright (c) 2022. TremorSoft
 * All Rights Reserved. This work is protected by copyright laws and international treaties.
 */

package com.tremorsoft.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.tremorsoft.R
import com.tremorsoft.databinding.FragmentPatientBinding
import com.tremorsoft.viewmodels.DataViewModel

class PatientDataFragment : BaseFragment() {

    private lateinit var dataViewModel: DataViewModel
    private var _binding: FragmentPatientBinding? = null

    private val binding get() = _binding!!

    private var familyHistory: String? = null
    private var gender: String? = null
    private var patientID: String? = null
    private var age: String? = null
    private var diagnosis: String? = null
    private var method: String? = null
    private var onsetAge: String? = null
    private var bilaterality: String? = null
    private var treatments: String? = null
    private var medication: String? = null
    private var concomitances: String? = null
    private var comorbidities: String? = null
    private var tremor: Boolean? = null

    private var validForm: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataViewModel = ViewModelProvider(this)[DataViewModel::class.java]

        val viewModel: DataViewModel by activityViewModels()

        viewModel.patientData.observe(viewLifecycleOwner,
            Observer { newPatientForm ->
                if (newPatientForm == null) {
                    return@Observer
                }
                if (newPatientForm.isPatientDataCompleted) {
                    if (diagnosis == "PD" || diagnosis == "ET") {
                        validForm = onsetAge!!.toInt() <= age!!.toInt()
                    } else if (diagnosis == "HS") {
                        validForm = true
                    }
                }
            })

        with(binding) {
            lyFamilyTremorHistory.isVisible = false
            lyPreviousDiagnosis.isVisible = false
            setDiagnosisData(false)

            rgGender.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rbMale -> gender = "Male"
                    R.id.rbFemale -> gender = "Female"
                }
            }

            rgFamilyHistory.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.tfYes -> lyFamilyTremorHistory.isVisible = true
                    R.id.tfNo -> {
                        lyFamilyTremorHistory.isVisible = false
                        rgFamilyTremor.clearCheck()
                        familyHistory = "No"
                    }
                }
            }
            rgFamilyTremor.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rbPD -> familyHistory = "PD"
                    R.id.rbET -> familyHistory = "ET"
                }
            }

            rgPatientType.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rbTP -> {
                        lyPreviousDiagnosis.isVisible = true
                    }
                    R.id.rbHS -> {
                        rgPreviousDiagnosis.clearCheck()
                        setDiagnosisData(false)
                        lyPreviousDiagnosis.isVisible = false
                        diagnosis = "HS"
                        method = "NA"
                        onsetAge = "NA"
                        bilaterality = "NA"
                        treatments = "NA"
                        medication = "NA"
                        concomitances = "NA"
                        comorbidities = "NA"
                    }
                }
            }

            rgPreviousDiagnosis.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.pdYes -> {
                        tremor = true
                        setDiagnosisData(tremor)
                    }

                    R.id.pdNo -> {
                        tremor = false
                        setDiagnosisData(tremor)
                    }
                }
            }

            rgDiagnosis.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rb_pd -> diagnosis = "PD"
                    R.id.rb_et -> diagnosis = "ET"
                }
            }

            rgMethod.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rb_spect -> method = "SPECT"
                    R.id.rb_clinical -> method = "Clinical"
                }
            }

            rgBilaterality.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rb_bil_yes -> bilaterality = "Yes"
                    R.id.rb_bil_no -> bilaterality = "No"
                }
            }

            rgTreatment.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rb_tre_yes -> treatments = "Yes"
                    R.id.rb_tre_no -> treatments = "No"
                }
            }

            rgMedication.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rb_med_yes -> medication = "Yes"
                    R.id.rb_med_no -> medication = "No"
                }
            }

            rgConcomitances.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rb_con_yes -> concomitances = "Yes"
                    R.id.rb_con_no -> concomitances = "No"
                }
            }

            rgComorbidities.setOnCheckedChangeListener { _, answer ->
                when (answer) {
                    R.id.rb_com_yes -> comorbidities = "Yes"
                    R.id.rb_com_no -> comorbidities = "No"
                }
            }

            btnConfirm.setOnClickListener {

                if (!etAge.text?.isEmpty()!! && !etPatientID.text?.isEmpty()!!) {
                    patientID = etPatientID.text.toString()
                    age = etAge.text.toString()

                    if (diagnosis != "HS" && !diagnosis.isNullOrEmpty()) {
                        if (etOnsetAge.text.isNullOrEmpty()) {
                            Toast.makeText(
                                context, "Make sure you have provided all required data",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            onsetAge = etOnsetAge.text.toString()
                            if (onsetAge!!.toInt() > age!!.toInt()) {
                                Toast.makeText(
                                    context,
                                    "The onset age of the tremor must be equal to or lower than the patient's current age.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        context, "Make sure you have provided all required data",
                        Toast.LENGTH_LONG
                    ).show()
                }

                println("$patientID, $age, $gender, $familyHistory, $diagnosis, $method, $onsetAge, $bilaterality, $treatments, $medication, $concomitances, $comorbidities")

                viewModel.patientDataChanged(
                    patientID, age, gender, familyHistory, diagnosis, method, onsetAge,
                    bilaterality, treatments, medication, concomitances, comorbidities
                )

                if (validForm) {
                    findNavController().navigate(R.id.action_nav_patient_to_nav_sensor)
                }
            }
        }
    }

    private fun setDiagnosisData(tremor: Boolean?) {
        with(binding) {
            if (tremor != null) {
                cvDiagnosis.isVisible = tremor
                cvMethod.isVisible = tremor
                cvOnsetAge.isVisible = tremor
                cvBilaterality.isVisible = tremor
                cvTreatment.isVisible = tremor
                cvMedication.isVisible = tremor
                cvConcomitances.isVisible = tremor
                cvComorbidities.isVisible = tremor
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}