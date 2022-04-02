package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.ViewModel

class DreamListViewModel : ViewModel() {

    private val dreamRepository = DreamRepository.get()
    val dreamListLiveData = dreamRepository.getDreams()
//    val dreams = mutableListOf<Dream>()
//    init {
//        for (i in 0 until 100) {
//            val dream = Dream()
//            dream.title = "Dream #$i"
//            dream.isFulfilled = i % 2 == 0
//            dreams += dream
//        }
//    }
}