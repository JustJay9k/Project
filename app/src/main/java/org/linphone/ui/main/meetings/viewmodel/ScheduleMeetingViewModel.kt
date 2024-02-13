/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.main.meetings.viewmodel

import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.core.Address
import org.linphone.core.ChatRoom
import org.linphone.core.ConferenceInfo
import org.linphone.core.ConferenceScheduler
import org.linphone.core.ConferenceSchedulerListenerStub
import org.linphone.core.Factory
import org.linphone.core.Participant
import org.linphone.core.ParticipantInfo
import org.linphone.core.tools.Log
import org.linphone.ui.main.model.SelectedAddressModel
import org.linphone.utils.AppUtils
import org.linphone.utils.Event
import org.linphone.utils.TimestampUtils

class ScheduleMeetingViewModel @UiThread constructor() : ViewModel() {
    companion object {
        private const val TAG = "[Schedule Meeting ViewModel]"
    }

    val isBroadcastSelected = MutableLiveData<Boolean>()

    val showBroadcastHelp = MutableLiveData<Boolean>()

    val subject = MutableLiveData<String>()

    val description = MutableLiveData<String>()

    val allDayMeeting = MutableLiveData<Boolean>()

    val fromDate = MutableLiveData<String>()

    val toDate = MutableLiveData<String>()

    val fromTime = MutableLiveData<String>()

    val toTime = MutableLiveData<String>()

    val timezone = MutableLiveData<String>()

    val sendInvitations = MutableLiveData<Boolean>()

    val participants = MutableLiveData<ArrayList<SelectedAddressModel>>()

    val operationInProgress = MutableLiveData<Boolean>()

    val conferenceCreatedEvent = MutableLiveData<Event<Boolean>>()

    private var startTimestamp = 0L
    private var endTimestamp = 0L

    internal var startHour = 0
    internal var startMinutes = 0

    internal var endHour = 0
    internal var endMinutes = 0

    private lateinit var conferenceScheduler: ConferenceScheduler

    private lateinit var conferenceInfoToEdit: ConferenceInfo

    private val conferenceSchedulerListener = object : ConferenceSchedulerListenerStub() {
        @WorkerThread
        override fun onStateChanged(
            conferenceScheduler: ConferenceScheduler,
            state: ConferenceScheduler.State?
        ) {
            Log.i("$TAG Conference state changed [$state]")
            when (state) {
                ConferenceScheduler.State.Error -> {
                    operationInProgress.postValue(false)
                    // TODO: show error toast
                }
                ConferenceScheduler.State.Ready -> {
                    val conferenceAddress = conferenceScheduler.info?.uri
                    if (::conferenceInfoToEdit.isInitialized) {
                        Log.i(
                            "$TAG Conference info [${conferenceInfoToEdit.uri?.asStringUriOnly()}] has been updated"
                        )
                    } else {
                        Log.i(
                            "$TAG Conference info created, address will be [${conferenceAddress?.asStringUriOnly()}]"
                        )
                    }

                    if (sendInvitations.value == true) {
                        Log.i("$TAG User asked for invitations to be sent, let's do it")
                        val chatRoomParams = coreContext.core.createDefaultChatRoomParams()
                        chatRoomParams.isGroupEnabled = false
                        chatRoomParams.backend = ChatRoom.Backend.FlexisipChat
                        chatRoomParams.isEncryptionEnabled = true
                        chatRoomParams.subject = "Meeting invitation" // Won't be used
                        conferenceScheduler.sendInvitations(chatRoomParams)
                    } else {
                        Log.i("$TAG User didn't asked for invitations to be sent")
                        operationInProgress.postValue(false)
                        conferenceCreatedEvent.postValue(Event(true))
                    }
                }
                else -> {
                }
            }
        }

        @WorkerThread
        override fun onInvitationsSent(
            conferenceScheduler: ConferenceScheduler,
            failedInvitations: Array<out Address>?
        ) {
            when (val failedCount = failedInvitations?.size) {
                0 -> {
                    Log.i("$TAG All invitations have been sent")
                }
                participants.value.orEmpty().size -> {
                    Log.e("$TAG No invitation sent!")
                    // TODO: show error toast
                }
                else -> {
                    Log.w("$TAG [$failedCount] invitations couldn't have been sent for:")
                    for (failed in failedInvitations.orEmpty()) {
                        Log.w(failed.asStringUriOnly())
                    }
                    // TODO: show error toast
                }
            }

            operationInProgress.postValue(false)
            conferenceCreatedEvent.postValue(Event(true))
        }
    }

    init {
        isBroadcastSelected.value = false
        showBroadcastHelp.value = false
        allDayMeeting.value = false
        sendInvitations.value = true

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.add(Calendar.HOUR, 1)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val nextFullHour = cal.timeInMillis
        startHour = cal.get(Calendar.HOUR_OF_DAY)
        startMinutes = 0

        cal.add(Calendar.HOUR, 1)
        val twoHoursLater = cal.timeInMillis
        endHour = cal.get(Calendar.HOUR_OF_DAY)
        endMinutes = 0

        startTimestamp = nextFullHour
        endTimestamp = twoHoursLater

        Log.i(
            "$TAG Default start time is [$startHour:$startMinutes], default end time is [$startHour:$startMinutes]"
        )
        Log.i("$TAG Default start date is [$startTimestamp], default end date is [$endTimestamp]")

        computeDateLabels()
        computeTimeLabels()
        updateTimezone()
    }

    override fun onCleared() {
        super.onCleared()

        coreContext.postOnCoreThread {
            if (::conferenceScheduler.isInitialized) {
                conferenceScheduler.removeListener(conferenceSchedulerListener)
            }
        }
    }

    @UiThread
    fun loadExistingConferenceInfoFromUri(conferenceUri: String) {
        coreContext.postOnCoreThread { core ->
            val conferenceAddress = core.interpretUrl(conferenceUri, false)
            if (conferenceAddress == null) {
                Log.e("$TAG Failed to parse conference URI [$conferenceUri], abort")
                return@postOnCoreThread
            }

            val conferenceInfo = core.findConferenceInformationFromUri(conferenceAddress)
            if (conferenceInfo == null) {
                Log.e(
                    "$TAG Failed to find a conference info matching URI [${conferenceAddress.asString()}], abort"
                )
                return@postOnCoreThread
            }

            conferenceInfoToEdit = conferenceInfo
            Log.i(
                "$TAG Found conference info matching URI [${conferenceInfo.uri?.asString()}] with subject [${conferenceInfo.subject}]"
            )
            subject.postValue(conferenceInfo.subject)
            description.postValue(conferenceInfo.description)

            isBroadcastSelected.postValue(false) // TODO FIXME

            startHour = 0
            startMinutes = 0
            endHour = 0
            endMinutes = 0
            startTimestamp = conferenceInfo.dateTime * 1000 /* Linphone timestamps are in seconds */
            endTimestamp = (conferenceInfo.dateTime + conferenceInfo.duration) * 1000 /* Linphone timestamps are in seconds */
            Log.i(
                "$TAG Loaded start date is [$startTimestamp], loaded end date is [$endTimestamp]"
            )
            computeDateLabels()
            computeTimeLabels()
            updateTimezone()

            val list = arrayListOf<SelectedAddressModel>()
            for (participant in conferenceInfo.participantInfos) {
                val address = participant.address
                val avatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(
                    address
                )
                val model = SelectedAddressModel(address, avatarModel) { model ->
                    // onRemoveFromSelection
                    removeModelFromSelection(model)
                }
                list.add(model)
                Log.i("$TAG Loaded participant [${address.asStringUriOnly()}]")
            }
            Log.i(
                "$TAG [${list.size}] participants loaded from found conference info"
            )
            participants.postValue(list)
        }
    }

    @UiThread
    fun getCurrentlySelectedStartDate(): Long {
        return startTimestamp
    }

    @UiThread
    fun setStartDate(timestamp: Long) {
        startTimestamp = timestamp
        endTimestamp = timestamp
        computeDateLabels()
    }

    @UiThread
    fun getCurrentlySelectedEndDate(): Long {
        return endTimestamp
    }

    @UiThread
    fun setEndDate(timestamp: Long) {
        endTimestamp = timestamp
        computeDateLabels()
    }

    @UiThread
    fun setStartTime(hours: Int, minutes: Int) {
        startHour = hours
        startMinutes = minutes

        endHour = hours + 1
        endMinutes = minutes

        computeTimeLabels()
    }

    @UiThread
    fun setEndTime(hours: Int, minutes: Int) {
        endHour = hours
        endMinutes = minutes

        computeTimeLabels()
    }

    @UiThread
    fun selectMeeting() {
        isBroadcastSelected.value = false
        showBroadcastHelp.value = false
    }

    @UiThread
    fun selectBroadcast() {
        isBroadcastSelected.value = true
        showBroadcastHelp.value = true
    }

    @UiThread
    fun closeBroadcastHelp() {
        showBroadcastHelp.value = false
    }

    @UiThread
    fun addParticipants(toAdd: List<String>) {
        coreContext.postOnCoreThread {
            val list = arrayListOf<SelectedAddressModel>()
            list.addAll(participants.value.orEmpty())

            for (participant in toAdd) {
                val address = Factory.instance().createAddress(participant)
                if (address == null) {
                    Log.e("$TAG Failed to parse [$participant] as address!")
                } else {
                    val found = list.find { it.address.weakEqual(address) }
                    if (found != null) {
                        Log.i(
                            "$TAG Participant [${found.address.asStringUriOnly()}] already in list, skipping"
                        )
                        continue
                    }

                    val avatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(
                        address
                    )
                    val model = SelectedAddressModel(address, avatarModel) { model ->
                        // onRemoveFromSelection
                        removeModelFromSelection(model)
                    }
                    list.add(model)
                    Log.i("$TAG Added participant [${address.asStringUriOnly()}]")
                }
            }

            Log.i(
                "$TAG [${toAdd.size}] participants added, now there are [${list.size}] participants in list"
            )
            participants.postValue(list)
        }
    }

    // TODO FIXME handle speakers when in broadcast mode

    @UiThread
    fun schedule() {
        if (subject.value.orEmpty().isEmpty() || participants.value.orEmpty().isEmpty()) {
            Log.e(
                "$TAG Either no subject was set or no participant was selected, can't schedule meeting."
            )
            // TODO: show red toast
            return
        }

        coreContext.postOnCoreThread { core ->
            Log.i(
                "$TAG Scheduling ${if (isBroadcastSelected.value == true) "broadcast" else "meeting"}"
            )
            operationInProgress.postValue(true)

            val localAccount = core.defaultAccount
            val localAddress = localAccount?.params?.identityAddress

            val conferenceInfo = Factory.instance().createConferenceInfo()
            conferenceInfo.organizer = localAddress
            conferenceInfo.subject = subject.value
            conferenceInfo.description = description.value

            val startTime = startTimestamp / 1000 // Linphone expects timestamp in seconds
            conferenceInfo.dateTime = startTime
            val duration = ((endTimestamp - startTimestamp) / 1000).toInt() // Linphone expects duration in seconds
            conferenceInfo.duration = duration

            val participantsList = participants.value.orEmpty()
            val participantsInfoList = arrayListOf<ParticipantInfo>()
            for (participant in participantsList) {
                val info = Factory.instance().createParticipantInfo(participant.address)
                if (info == null) {
                    Log.e(
                        "$TAG Failed to create Participant Info from address [${participant.address.asStringUriOnly()}]"
                    )
                    continue
                }

                // For meetings, all participants must have Speaker role
                info.role = Participant.Role.Speaker
                participantsInfoList.add(info)
            }

            val participantsInfoArray = arrayOfNulls<ParticipantInfo>(participantsInfoList.size)
            participantsInfoList.toArray(participantsInfoArray)
            conferenceInfo.setParticipantInfos(participantsInfoArray)

            if (!::conferenceScheduler.isInitialized) {
                conferenceScheduler = core.createConferenceScheduler()
                conferenceScheduler.addListener(conferenceSchedulerListener)
            }

            conferenceScheduler.account = localAccount
            // Will trigger the conference creation automatically
            conferenceScheduler.info = conferenceInfo
        }
    }

    @UiThread
    fun update() {
        coreContext.postOnCoreThread { core ->
            Log.i(
                "$TAG Updating ${if (isBroadcastSelected.value == true) "broadcast" else "meeting"}"
            )
            if (!::conferenceInfoToEdit.isInitialized) {
                Log.e("No conference info to edit found!")
                return@postOnCoreThread
            }

            operationInProgress.postValue(true)

            val conferenceInfo = conferenceInfoToEdit
            conferenceInfo.subject = subject.value
            conferenceInfo.description = description.value

            val startTime = startTimestamp / 1000 // Linphone expects timestamp in seconds
            conferenceInfo.dateTime = startTime
            val duration = ((endTimestamp - startTimestamp) / 1000).toInt() // Linphone expects duration in seconds
            conferenceInfo.duration = duration

            val participantsList = participants.value.orEmpty()
            val participantsInfoList = arrayListOf<ParticipantInfo>()
            for (participant in participantsList) {
                val info = Factory.instance().createParticipantInfo(participant.address)
                if (info == null) {
                    Log.e(
                        "$TAG Failed to create Participant Info from address [${participant.address.asStringUriOnly()}]"
                    )
                    continue
                }

                // For meetings, all participants must have Speaker role
                info.role = Participant.Role.Speaker
                participantsInfoList.add(info)
            }

            val participantsInfoArray = arrayOfNulls<ParticipantInfo>(participantsInfoList.size)
            participantsInfoList.toArray(participantsInfoArray)
            conferenceInfo.setParticipantInfos(participantsInfoArray)

            if (!::conferenceScheduler.isInitialized) {
                conferenceScheduler = core.createConferenceScheduler()
                conferenceScheduler.addListener(conferenceSchedulerListener)
            }

            // Will trigger the conference update automatically
            conferenceScheduler.info = conferenceInfo
        }
    }

    @UiThread
    private fun removeModelFromSelection(model: SelectedAddressModel) {
        val newList = arrayListOf<SelectedAddressModel>()
        newList.addAll(participants.value.orEmpty())
        newList.remove(model)
        Log.i("$TAG Removed participant [${model.address.asStringUriOnly()}]")
        participants.postValue(newList)
    }

    @AnyThread
    private fun computeDateLabels() {
        val start = TimestampUtils.toString(
            startTimestamp,
            onlyDate = true,
            timestampInSecs = false,
            shortDate = false,
            hideYear = false
        )
        fromDate.postValue(start)
        Log.i("$TAG Computed start date for timestamp [$startTimestamp] is [$start]")

        val end = TimestampUtils.toString(
            endTimestamp,
            onlyDate = true,
            timestampInSecs = false,
            shortDate = false,
            hideYear = false
        )
        toDate.postValue(end)
        Log.i("$TAG Computed end date for timestamp [$endTimestamp] is [$end]")
    }

    @AnyThread
    private fun computeTimeLabels() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startTimestamp
        if (startHour != 0 && startMinutes != 0) {
            cal.set(Calendar.HOUR_OF_DAY, startHour)
            cal.set(Calendar.MINUTE, startMinutes)
        }
        val start = TimestampUtils.timeToString(cal.timeInMillis, timestampInSecs = false)
        Log.i("$TAG Computed start time for timestamp [$startTimestamp] is [$start]")
        fromTime.postValue(start)

        cal.timeInMillis = endTimestamp
        if (endHour != 0 && endMinutes != 0) {
            cal.set(Calendar.HOUR_OF_DAY, endHour)
            cal.set(Calendar.MINUTE, endMinutes)
        }
        val end = TimestampUtils.timeToString(cal.timeInMillis, timestampInSecs = false)
        Log.i("$TAG Computed end time for timestamp [$endTimestamp] is [$end]")
        toTime.postValue(end)
    }

    @AnyThread
    private fun updateTimezone() {
        timezone.postValue(
            AppUtils.getFormattedString(
                R.string.meeting_schedule_timezone_title,
                TimeZone.getDefault().displayName.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(
                            Locale.getDefault()
                        )
                    } else {
                        it.toString()
                    }
                }
            )
        )
    }
}
