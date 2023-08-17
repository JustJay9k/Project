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
package org.linphone.utils

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import org.linphone.R
import org.linphone.databinding.DialogConfirmZrtpSasBinding
import org.linphone.databinding.DialogPickNumberOrAddressBinding
import org.linphone.databinding.DialogRemoveAllCallLogsBinding
import org.linphone.ui.main.calls.model.RemoveAllCallLogsDialogModel
import org.linphone.ui.main.contacts.model.NumberOrAddressPickerDialogModel
import org.linphone.ui.voip.model.ZrtpSasConfirmationDialogModel

class DialogUtils {
    companion object {
        fun getNumberOrAddressPickerDialog(
            context: Context,
            viewModel: NumberOrAddressPickerDialogModel
        ): Dialog {
            val dialog = Dialog(context, R.style.Theme_LinphoneDialog)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val binding: DialogPickNumberOrAddressBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_pick_number_or_address,
                null,
                false
            )
            binding.viewModel = viewModel
            dialog.setContentView(binding.root)

            val d: Drawable = ColorDrawable(
                ContextCompat.getColor(dialog.context, R.color.dialog_background)
            )
            d.alpha = 166
            dialog.window
                ?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            dialog.window?.setBackgroundDrawable(d)
            return dialog
        }
        fun getRemoveAllCallLogsConfirmationDialog(
            context: Context,
            model: RemoveAllCallLogsDialogModel
        ): Dialog {
            val dialog = Dialog(context, R.style.Theme_LinphoneDialog)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val binding: DialogRemoveAllCallLogsBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_remove_all_call_logs,
                null,
                false
            )
            binding.viewModel = model
            dialog.setContentView(binding.root)

            val d: Drawable = ColorDrawable(
                ContextCompat.getColor(dialog.context, R.color.dialog_background)
            )
            d.alpha = 166
            dialog.window
                ?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            dialog.window?.setBackgroundDrawable(d)
            return dialog
        }

        fun getZrtpSasConfirmationDialog(
            context: Context,
            viewModel: ZrtpSasConfirmationDialogModel
        ): Dialog {
            val dialog = Dialog(context, R.style.Theme_LinphoneDialog)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val binding: DialogConfirmZrtpSasBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_confirm_zrtp_sas,
                null,
                false
            )
            binding.viewModel = viewModel
            dialog.setContentView(binding.root)

            val d: Drawable = ColorDrawable(
                ContextCompat.getColor(dialog.context, R.color.dialog_background)
            )
            d.alpha = 166
            dialog.window
                ?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            dialog.window?.setBackgroundDrawable(d)
            return dialog
        }
    }
}
