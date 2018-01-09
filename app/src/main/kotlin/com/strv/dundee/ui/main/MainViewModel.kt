package com.strv.dundee.ui.main

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.widget.ArrayAdapter
import com.strv.dundee.BR
import com.strv.dundee.R
import com.strv.dundee.model.entity.BitcoinSource
import com.strv.dundee.model.entity.Coin
import com.strv.dundee.model.entity.Currency
import com.strv.dundee.model.entity.Ticker
import com.strv.dundee.model.entity.Wallet
import com.strv.dundee.model.repo.BitcoinRepository
import com.strv.dundee.model.repo.UserRepository
import com.strv.dundee.model.repo.WalletRepository
import com.strv.ktools.DiffObservableListLiveData
import com.strv.ktools.Resource
import com.strv.ktools.addValueSource
import com.strv.ktools.inject
import com.strv.ktools.logMeD
import com.strv.ktools.sharedPrefs
import com.strv.ktools.stringLiveData
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList


class MainViewModel() : ViewModel() {

	private val application by inject<Application>()
	private val bitcoinRepository by inject<BitcoinRepository>()
	private val userRepository by inject<UserRepository>()
	private val walletRepository by inject<WalletRepository>()

	interface OnItemClickListener{
		fun onItemClick(wallet: Wallet)
	}
	val itemBinding = ItemBinding.of<Wallet>(BR.item, R.layout.item_wallet).bindExtra(BR.viewModel, this).bindExtra(BR.listener, object : OnItemClickListener {
		override fun onItemClick(wallet: Wallet) {
			wallet.logMeD()
		}
	})!!

	var wallets: DiffObservableListLiveData<Wallet>
	var user = userRepository.getCurrentUserData()
	val tickers = HashMap<String, LiveData<Resource<Ticker>>>()
	val source by application.sharedPrefs().stringLiveData(BitcoinSource.BITSTAMP)
	val currency by application.sharedPrefs().stringLiveData(Currency.USD)
	val sourceAdapter = ArrayAdapter(application, R.layout.item_spinner_source_currency, BitcoinSource.getAll())
	val currencyAdapter = ArrayAdapter(application, R.layout.item_spinner_source_currency, Currency.getAll())
	val totalValue = MediatorLiveData<Double>()
	val optionsOpen = MutableLiveData<Boolean>().apply { value = false }

	init {
		// compose Ticker LiveData (observed by data binding automatically)
		refreshTicker()

		// refresh ticker on input changes
		source.observeForever { refreshTicker() }
		currency.observeForever { refreshTicker() }

		wallets = DiffObservableListLiveData(walletRepository.getWalletsForCurrentUser(), object : DiffObservableList.Callback<Wallet> {
			override fun areContentsTheSame(oldItem: Wallet?, newItem: Wallet?) = oldItem == newItem
			override fun areItemsTheSame(oldItem: Wallet?, newItem: Wallet?) = oldItem == newItem
		})

		// add total value calculation and attach to ticker and wallets LiveData
		totalValue.addValueSource(wallets, { recalculateTotal() })
		tickers.forEach { totalValue.addValueSource(it.value, { recalculateTotal() }) }
	}

	private fun refreshTicker() {
		Coin.getAll().forEach { tickers[it] = bitcoinRepository.getTicker(source.value!!, it, currency.value!!, liveDataToReuse = tickers[it]) }
	}

	private fun recalculateTotal(): Double = wallets.value?.data?.sumByDouble { tickers[it.coin]?.value?.data?.getValue(it.amount ?: 0.toDouble()) ?: 0.toDouble() } ?: 0.toDouble()

	fun logout() {
		userRepository.signOut()
	}
}