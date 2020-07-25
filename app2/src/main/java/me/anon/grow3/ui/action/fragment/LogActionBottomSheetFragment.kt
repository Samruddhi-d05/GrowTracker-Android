package me.anon.grow3.ui.action.fragment

import android.view.View
import androidx.core.graphics.plus
import androidx.core.view.plusAssign
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.google.android.material.bottomsheet.BottomSheetBehavior
import me.anon.grow3.R
import me.anon.grow3.data.model.Diary
import me.anon.grow3.data.model.Log
import me.anon.grow3.data.model.Water
import me.anon.grow3.databinding.FragmentActionLogBinding
import me.anon.grow3.ui.action.view.LogView
import me.anon.grow3.ui.action.view.WaterLogView
import me.anon.grow3.ui.action.viewmodel.LogActionViewModel
import me.anon.grow3.ui.base.BaseFragment
import me.anon.grow3.util.*
import me.anon.grow3.util.states.asSuccess
import me.anon.grow3.view.CropSelectView
import javax.inject.Inject
import kotlin.math.abs

class LogActionBottomSheetFragment : BaseFragment(FragmentActionLogBinding::class)
{
	override val injector: Injector = { it.inject(this) }

	@Inject internal lateinit var viewModelFactory: LogActionViewModel.Factory
	private val viewModel: LogActionViewModel by viewModels { ViewModelProvider(viewModelFactory, this) }
	private val viewBindings by viewBinding<FragmentActionLogBinding>()
	private var logView: LogView<*>? = null
	private var isFinishing = false

	private val layoutSheetBehavior by lazy { BottomSheetBehavior.from(requireView().parentViewById<View>(R.id.bottom_sheet)) }
	private val sheetListener = object : BottomSheetBehavior.BottomSheetCallback()
	{
		override fun onSlide(bottomSheet: View, slideOffset: Float)
		{
			if (bottomSheet.top < insets.value?.top ?: 0)
			{
				val elevation = slideOffset * 8f
				viewBindings.toolbarLayout.elevation = elevation
				viewBindings.toolbarLayout.setBackgroundColor(R.attr.colorSurface.resColor(requireContext()))

				viewBindings.toolbarLayout.updateMargin(top = abs(bottomSheet.top - (insets.value?.top ?: 0)))
			}
			else
			{
				viewBindings.toolbarLayout.updateMargin(top = 12.dp(requireContext()))
				viewBindings.toolbarLayout.elevation = 0f
				viewBindings.toolbarLayout.setBackgroundColor(android.R.color.transparent.color(requireContext()))
			}
		}

		override fun onStateChanged(bottomSheet: View, newState: Int)
		{
			when (newState)
			{
				BottomSheetBehavior.STATE_COLLAPSED -> {
					viewBindings.logContent.visibility = View.INVISIBLE
				}
				else -> viewBindings.logContent.visibility = View.VISIBLE
			}
		}
	}

	override fun bindUi()
	{
		setToolbar(viewBindings.toolbar)
		viewBindings.toolbar.setNavigationOnClickListener {
			requireActivity().promptExit {
				layoutSheetBehavior.isHideable = true
				layoutSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
			}
		}

		insets.observe(viewLifecycleOwner) { insets ->
			viewBindings.actionDone.updateMargin(insets + 16.dp(this))
			viewBindings.logContent.updatePadding(bottom = insets.bottom)
		}

		layoutSheetBehavior.isGestureInsetBottomIgnored = false
		layoutSheetBehavior.isFitToContents = false
		layoutSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

		requireView().parentViewById<View>(R.id.bottom_sheet).post {
			layoutSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
			layoutSheetBehavior.isHideable = false

			viewBindings.toolbarLayout.afterMeasured {
				layoutSheetBehavior.setPeekHeight(measuredHeight + 12.dp(context) + (insets.value?.bottom ?: 0), false)
			}
		}

		layoutSheetBehavior.addBottomSheetCallback(sheetListener)

		viewBindings.actionDone.onClick {
			logView?.let {
				it.saveView()
				viewModel.saveLog()
				finish()
			}
		}
	}

	private fun finish()
	{
		requireView().hideKeyboard()

		isFinishing = true
		layoutSheetBehavior.isHideable = true
		layoutSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
	}

	override fun bindVm()
	{
		viewModel.log.observe(viewLifecycleOwner) { log ->
			val diary = viewModel.diary.value!!.asSuccess()
			renderLogView(diary, log)
		}
	}

	override fun onDestroyView()
	{
		if (!isFinishing)
		{
			logView?.let {
				it.saveView()
				viewModel.saveLog(draft = true)
			}
		}

		super.onDestroyView()
	}

	private fun renderLogView(diary: Diary, log: Log)
	{
		when (log)
		{
			is Water -> {
				logView = WaterLogView(log)
			}
		}

		logView?.let { logView ->
			viewBindings.toolbar.title = logView.provideTitle() ?: R.string.log_action_new_title.string()

			viewBindings.logContent.removeAllViews()
			val view = logView.createView(layoutInflater, viewBindings.logContent)
			logView.bindView(view)
			viewBindings.logContent += view

			view.findViewById<CropSelectView>(R.id.crop_select_view)?.let {
				it.setDiary(diary)
			}
		}
	}

	override fun onBackPressed(): Boolean
	{
		when (layoutSheetBehavior.state)
		{
			BottomSheetBehavior.STATE_COLLAPSED -> {
				requireActivity().promptExit {
					layoutSheetBehavior.isHideable = true
					layoutSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
				}
				return true
			}

			BottomSheetBehavior.STATE_EXPANDED, BottomSheetBehavior.STATE_HALF_EXPANDED -> {
				layoutSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
				return true
			}
		}

		return false
	}

	override fun onDestroy()
	{
		super.onDestroy()
		layoutSheetBehavior.removeBottomSheetCallback(sheetListener)
	}
}