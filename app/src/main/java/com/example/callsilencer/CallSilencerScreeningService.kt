package com.example.callsilencer.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.callsilencer.data.repository.CallSilencerRepository

class CallSilencerScreeningService : CallScreeningService() {

    private lateinit var repository: CallSilencerRepository

    override fun onCreate() {
        super.onCreate()
        repository = CallSilencerRepository(this)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""

        // If silencer is off, let all calls through
        if (!repository.isSilencerActive()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // If number is in allowed list, ring normally
        if (repository.isAllowedContact(phoneNumber)) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Otherwise silence + log it
        repository.addToRecentSilenced(phoneNumber)

        val response = CallResponse.Builder()
            .setSilenceCall(true)
            .setSkipCallLog(false)     // still shows in phone's call log
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }
}