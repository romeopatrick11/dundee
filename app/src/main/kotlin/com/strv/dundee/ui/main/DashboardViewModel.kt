package com.strv.dundee.ui.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import com.strv.dundee.BR
import com.strv.dundee.R
import com.strv.dundee.model.entity.*
import com.strv.dundee.model.repo.BitcoinRepository
import com.strv.dundee.model.repo.ExchangeRateRepository
import com.strv.dundee.model.repo.WalletRepository
import com.strv.ktools.DiffObservableListLiveData
import com.strv.ktools.Resource
import com.strv.ktools.addValueSource
import com.strv.ktools.inject
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList


class DashboardViewModel(mainViewModel: MainViewModel) : ViewModel() {

	private val bitcoinRepository by inject<BitcoinRepository>()
	private val walletRepository by inject<WalletRepository>()
	private val exchangeRateRepository by inject<ExchangeRateRepository>()

	val itemBinding = ItemBinding.of<WalletOverview>(BR.item, R.layout.item_wallet_dashboard).bindExtra(BR.viewModel, this)!!
	var wallets: DiffObservableListLiveData<WalletOverview>
	val tickers = HashMap<String, LiveData<Resource<Ticker>>>()
	val source = mainViewModel.source
	val currency = mainViewModel.currency
	val totalValue = MediatorLiveData<Double>()
	val totalProfit = MediatorLiveData<Double>()
	var exchangeRate: LiveData<Resource<ExchangeRate>>? = null

	init {
		// compose Ticker LiveData (observed by data binding automatically)
		refreshTicker()

		// refresh ticker on input changes
		source.observeForever { refreshTicker() }
		currency.observeForever { currencyChange() }

		if (currency.value != Currency.USD) loadExchangeRate()

		val coinWallets = MediatorLiveData<Resource<List<WalletOverview>>>().addValueSource(walletRepository.getWalletsForCurrentUser(), {
			val result = hashMapOf<String, WalletOverview>()
			Coin.getAll().forEach { result[it] = WalletOverview(it) }
			it?.data?.fold(result, { accumulator, wallet ->
				accumulator[wallet.coin!!]!!.amount += wallet.amount!!
				accumulator[wallet.coin!!]!!.boughtPrice += wallet.boughtPrice!!
				accumulator
			})
			Resource(it?.status ?: Resource.Status.SUCCESS, result.values.toList().sortedByDescending { it.amount })
		})

		wallets = DiffObservableListLiveData(coinWallets, object : DiffObservableList.Callback<WalletOverview> {
			override fun areContentsTheSame(oldItem: WalletOverview?, newItem: WalletOverview?) = oldItem == newItem
			override fun areItemsTheSame(oldItem: WalletOverview?, newItem: WalletOverview?) = oldItem == newItem
		})

		// add total value calculation and attach to ticker and wallets LiveData
		totalValue.addValueSource(wallets, { recalculateTotal() })
		tickers.forEach { totalValue.addValueSource(it.value, { recalculateTotal() }) }
		totalProfit.addValueSource(totalValue, { recalculateTotalProfit() })
	}

	private fun loadExchangeRate() {
		currency.value?.let {
			exchangeRate?.let { totalProfit.removeSource(it) }
			exchangeRate = exchangeRateRepository.getExchangeRate(Currency.USD, it)
			totalProfit.addValueSource(exchangeRate!!, { recalculateTotalProfit() })}
	}

	private fun currencyChange() {
		if (currency.value != Currency.USD) loadExchangeRate()
		refreshTicker()
	}

	private fun refreshTicker() {
		Coin.getAll().forEach { tickers[it] = bitcoinRepository.getTicker(source.value!!, it, currency.value!!, liveDataToReuse = tickers[it]) }
	}

	private fun recalculateTotal(): Double = wallets.value?.data?.sumByDouble { tickers[it.coin]?.value?.data?.getValue(it.amount) ?: 0.toDouble() } ?: 0.toDouble()

	private fun recalculateTotalProfit(): Double = when(currency.value) {
		Currency.USD -> calculateProfit(1.toDouble())
		exchangeRate?.value?.data?.target -> calculateProfit(exchangeRate!!.value!!.data!!.rate)
		else -> 0.toDouble()
	}

	private fun calculateProfit(exchangeRate: Double): Double{
		return totalValue.value?.let { totalValue.value!! - (wallets.value?.data?.sumByDouble { it.boughtPrice } ?: 0.toDouble()) * exchangeRate } ?: 0.toDouble()
	}
}