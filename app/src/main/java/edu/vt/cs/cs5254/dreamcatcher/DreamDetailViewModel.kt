package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.lang.IllegalArgumentException
import java.util.UUID


class DreamDetailViewModel : ViewModel() {

    private val dreamRepository = DreamRepository.get()
    private val dreamIdLiveData = MutableLiveData<UUID>()
    var dreamWithEntriesLiveData: LiveData<DreamWithEntries> =
        Transformations.switchMap(dreamIdLiveData) { dreamId ->
            dreamRepository.getDreamWithEntries(dreamId)
        }

    fun loadDreamWithEntries(dreamId: UUID) {
        dreamIdLiveData.value = dreamId
    }

    fun saveDreamWithEntries(dreamWithEntries: DreamWithEntries) {
        dreamRepository.updateDreamWithEntries(dreamWithEntries)
    }
//    fun loadDreamWithEntries(dreamId: UUID){
//        dreamWithEntries = dreamRepository.getDreamWithEntries(dreamId)
//            ?: throw IllegalArgumentException("Dream with ID $dreamId not found")
}

//class DreamDetailViewModel : ViewModel() {
//
//    private val dreamRepository = DreamRepository.get()
//    lateinit var dreamWithEntries: DreamWithEntries
//
//    fun loadDreamWithEntries(dreamId: UUID){
//        dreamWithEntries = dreamRepository.getDreamWithEntries(dreamId)
//            ?: throw IllegalArgumentException("Dream with ID $dreamId not found")
//    }
//
//
//}

//class CrimeDetailViewModel() : ViewModel() {
//    private val crimeRepository = CrimeRepository.get()
//    private val crimeIdLiveData = MutableLiveData<UUID>()
//    var crimeLiveData: LiveData<Crime?> = Transformations.switchMap(crimeIdLiveData) { crimeId ->
//        crimeRepository.getCrime(crimeId) }
//    fun loadCrime(crimeId: UUID) { crimeIdLiveData.value = crimeId
//    }
//    fun saveCrime(crime: Crime) {
//        crimeRepository.updateCrime(crime)
//    } }