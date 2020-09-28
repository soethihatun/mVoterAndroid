package com.popstack.mvoter2015.feature.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.RouterTransaction
import com.popstack.mvoter2015.R
import com.popstack.mvoter2015.core.mvp.MvvmController
import com.popstack.mvoter2015.databinding.ControllerNewsBinding
import com.popstack.mvoter2015.domain.news.model.NewsId
import com.popstack.mvoter2015.exception.GlobalExceptionHandler
import com.popstack.mvoter2015.feature.analytics.screen.CanTrackScreen
import com.popstack.mvoter2015.feature.browser.OpenBrowserDelegate
import com.popstack.mvoter2015.feature.home.BottomNavigationHostViewModelStore
import com.popstack.mvoter2015.feature.news.search.NewsSearchController
import com.popstack.mvoter2015.helper.RecyclerViewMarginDecoration
import com.popstack.mvoter2015.helper.conductor.requireActivity
import com.popstack.mvoter2015.helper.conductor.requireContext
import com.popstack.mvoter2015.helper.conductor.setSupportActionBar
import com.popstack.mvoter2015.helper.conductor.supportActionBar
import com.popstack.mvoter2015.logging.HasTag
import com.popstack.mvoter2015.paging.CommonLoadStateAdapter
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewsController : MvvmController<ControllerNewsBinding>(), HasTag, CanTrackScreen {

  override val tag: String = "NewsController"

  override val screenName: String = "NewsController"

  override val bindingInflater: (LayoutInflater) -> ControllerNewsBinding =
    ControllerNewsBinding::inflate

  private val viewModel: NewsViewModel by viewModels(
    store = BottomNavigationHostViewModelStore.viewModelStore ?: viewModelStore
  )

  private val newsPagingAdapter by lazy {
    NewsRecyclerViewAdapter(this::onNewsItemClick)
  }

  private val globalExceptionHandler by lazy {
    GlobalExceptionHandler(requireContext())
  }

  @Inject
  lateinit var openBrowserDelegate: OpenBrowserDelegate

  override fun onBindView(savedViewState: Bundle?) {
    super.onBindView(savedViewState)
    setSupportActionBar(binding.toolBar)
    supportActionBar()?.title = requireContext().getString(R.string.navigation_news)

    setHasOptionsMenu(R.menu.menu_news, this::handleMenuItemClick)

    binding.rvNews.apply {
      adapter = newsPagingAdapter.withLoadStateFooter(
        footer = CommonLoadStateAdapter(newsPagingAdapter::retry)
      )
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      val dimen =
        context.resources.getDimensionPixelSize(R.dimen.recycler_view_item_margin)
      addItemDecoration(RecyclerViewMarginDecoration(dimen, 0))
    }

    binding.btnRetry.setOnClickListener {
      newsPagingAdapter.retry()
    }

    newsPagingAdapter.addLoadStateListener { loadStates ->
      val refreshLoadState = loadStates.refresh
      binding.rvNews.isVisible = refreshLoadState is LoadState.NotLoading
      binding.progressBar.isVisible = refreshLoadState is LoadState.Loading
      binding.tvErrorMessage.isVisible = refreshLoadState is LoadState.Error
      binding.btnRetry.isVisible = refreshLoadState is LoadState.Error

      if (refreshLoadState is LoadState.Error) {
        binding.tvErrorMessage.text =
          globalExceptionHandler.getMessageForUser(refreshLoadState.error)
      }
    }

    lifecycleScope.launch {
      viewModel.getNewsPagingFlow()
        .collectLatest { pagingData ->
          newsPagingAdapter.submitData(lifecycle, pagingData)
        }
    }
  }

  private fun onNewsItemClick(
    id: NewsId,
    url: String
  ) {
    lifecycleScope.launch {
      openBrowserDelegate.browserHandler()
        .launchNewsInBrowser(requireActivity(), url)
    }
  }

  private fun handleMenuItemClick(menuItem: MenuItem): Boolean {
    return when (menuItem.itemId) {
      R.id.action_search -> {
        router.pushController(RouterTransaction.with(NewsSearchController()))
        true
      }
      else -> false
    }
  }

}