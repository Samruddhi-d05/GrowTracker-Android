package me.anon.grow3.ui.main.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import me.anon.grow3.R
import me.anon.grow3.databinding.FragmentMainHostBinding
import me.anon.grow3.ui.base.BaseFragment
import me.anon.grow3.ui.base.BaseHostFragment
import me.anon.grow3.ui.diaries.fragment.EmptyFragment
import me.anon.grow3.ui.diaries.fragment.LogListFragment
import me.anon.grow3.ui.diaries.fragment.ViewDiaryFragment
import me.anon.grow3.ui.main.activity.MainActivity
import me.anon.grow3.ui.main.activity.MainActivity.Companion.EXTRA_DIARY_ID
import me.anon.grow3.ui.main.activity.MainActivity.Companion.EXTRA_NAVIGATE
import me.anon.grow3.util.name

/**
 * Main navigator fragment for the application. [MainActivity] controls the UI and distribution
 * of navigation actions from this class.
 */
class MainNavigationFragment : BaseHostFragment(FragmentMainHostBinding::class.java)
{
	private val pendingActions = ArrayList<Bundle>(1)

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)

		if (activity !is MainActivity) throw IllegalStateException("Main host not attached to Main activity")

		if (pendingActions.isNotEmpty())
		{
			executePendingActions()
		}
	}

	override fun setArguments(args: Bundle?)
	{
		super.setArguments(args)

		args?.let {
			pendingActions += it
			if (isAdded && !isDetached) executePendingActions()
		}
	}

	/**
	 * The main navigation controller method. All routings happen here
	 */
	private fun executePendingActions()
	{
		with (pendingActions.asReversed())
		{
			while (size > 0)
			{
				val item = this.removeAt(0)
				val route = item.getString(EXTRA_NAVIGATE) ?: throw IllegalArgumentException("No route set")
//				val origin = item.getString(EXTRA_ORIGINATOR)

				when (route)
				{
					name<EmptyFragment>() -> {
						beginStack(EmptyFragment())
					}

					name<ViewDiaryFragment>() -> {
						val id = item.getString(EXTRA_DIARY_ID) ?: throw IllegalArgumentException("No diary ID set")
						val fragment = ViewDiaryFragment().apply {
							arguments = Bundle().apply {
								putString(ViewDiaryFragment.EXTRA_DIARY_ID, id)
							}
						}
						beginStack(fragment)
					}

					name<LogListFragment>() -> {
						addToStack(LogListFragment().apply {
							arguments = item
						})
					}
				}
			}
		}
	}

	override fun onBackPressed(): Boolean
		= (childFragmentManager.findFragmentByTag("fragment") as? BaseFragment)?.onBackPressed() ?: false

	private fun clearStack()
	{
		activity().clearStack()
	}

	private fun beginStack(fragment: Fragment)
	{
		clearStack()
		childFragmentManager.commitNow {
			setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
			replace(R.id.fragment_container, fragment, "fragment")
			activity().notifyPagerChange(this@MainNavigationFragment)
		}
	}

	private fun addToStack(fragment: Fragment)
	{
		activity().addToStack(fragment)
	}
}
