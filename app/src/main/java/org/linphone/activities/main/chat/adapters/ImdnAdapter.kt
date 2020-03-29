/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
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
package org.linphone.activities.main.chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.linphone.R
import org.linphone.activities.main.chat.viewmodels.ImdnParticipantViewModel
import org.linphone.core.ChatMessage
import org.linphone.core.ParticipantImdnState
import org.linphone.databinding.ChatRoomImdnParticipantCellBinding
import org.linphone.databinding.ImdnListHeaderBinding
import org.linphone.utils.HeaderAdapter
import org.linphone.utils.LifecycleViewHolder

class ImdnAdapter : ListAdapter<ParticipantImdnState,
        ImdnAdapter.ViewHolder>(ParticipantImdnStateDiffCallback()), HeaderAdapter {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ChatRoomImdnParticipantCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.chat_room_imdn_participant_cell, parent, false
        )
        val viewHolder = ViewHolder(binding)
        binding.lifecycleOwner = viewHolder
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ChatRoomImdnParticipantCellBinding
    ) : LifecycleViewHolder(binding) {
        fun bind(participantImdnState: ParticipantImdnState) {
            with(binding) {
                val imdnViewModel = ImdnParticipantViewModel(participantImdnState)
                viewModel = imdnViewModel

                executePendingBindings()
            }
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val participantImdnState = getItem(position)
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            getItem(previousPosition).state != participantImdnState.state
        } else true
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val participantImdnState = getItem(position)
        val binding: ImdnListHeaderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.imdn_list_header, null, false
        )
        when (participantImdnState.state) {
            ChatMessage.State.Displayed -> {
                binding.title = R.string.chat_message_imdn_displayed
                binding.textColor = R.color.imdn_read_color
                binding.icon = R.drawable.message_read
            }
            ChatMessage.State.DeliveredToUser -> {
                binding.title = R.string.chat_message_imdn_delivered
                binding.textColor = R.color.grey_color
                binding.icon = R.drawable.message_delivered
            }
            ChatMessage.State.Delivered -> {
                binding.title = R.string.chat_message_imdn_sent
                binding.textColor = R.color.grey_color
                binding.icon = R.drawable.message_delivered
            }
            ChatMessage.State.NotDelivered -> {
                binding.title = R.string.chat_message_imdn_undelivered
                binding.textColor = R.color.red_color
                binding.icon = R.drawable.message_undelivered
            }
        }
        binding.executePendingBindings()
        return binding.root
    }
}

private class ParticipantImdnStateDiffCallback : DiffUtil.ItemCallback<ParticipantImdnState>() {
    override fun areItemsTheSame(
        oldItem: ParticipantImdnState,
        newItem: ParticipantImdnState
    ): Boolean {
        return oldItem.participant.address.weakEqual(newItem.participant.address)
    }

    override fun areContentsTheSame(
        oldItem: ParticipantImdnState,
        newItem: ParticipantImdnState
    ): Boolean {
        return false
    }
}
