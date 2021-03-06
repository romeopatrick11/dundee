package com.strv.dundee.common

import android.databinding.BindingAdapter
import android.databinding.InverseBindingAdapter
import android.databinding.InverseBindingListener
import android.graphics.Canvas
import android.graphics.Color
import android.support.annotation.IdRes
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.strv.dundee.R
import com.strv.dundee.model.entity.Currency
import com.strv.dundee.model.entity.ExchangeRates
import com.strv.dundee.model.entity.History
import com.strv.dundee.model.entity.TimeFrame
import com.strv.dundee.ui.charts.MarkerView
import com.strv.ktools.Resource
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter
import java.text.DateFormat
import java.util.Date

@BindingAdapter("hide")
fun View.setHide(hide: Boolean) {
	visibility = if (hide) View.GONE else View.VISIBLE
}

@BindingAdapter("show")
fun View.setShow(show: Boolean) {
	visibility = if (show) View.VISIBLE else View.GONE
}

@BindingAdapter("invisible")
fun View.setInvisible(invisible: Boolean) {
	visibility = if (invisible) View.INVISIBLE else View.VISIBLE
}

@BindingAdapter("onActionDoneCallback")
fun EditText.setOnActionDoneCallback(callback: ActionDoneCallback) {
	setOnEditorActionListener { _, actionId, _ ->
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			callback.onActionDone()
			true
		} else false
	}
}

interface TextChangedCallback {
	fun onTextChanged()
}

@BindingAdapter("onTextChangedCallback")
fun EditText.setOnTextChangedCallback(callback: TextChangedCallback) {
	addTextChangedListener(object : TextWatcher {
		override fun afterTextChanged(editable: Editable?) {
			callback.onTextChanged()
		}

		override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

		}

		override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

		}
	})
}

//Spinner
@Suppress("UNCHECKED_CAST")
@BindingAdapter(value = ["selection", "selectionAttrChanged", "adapter"], requireAll = false)
fun <T> Spinner.setAdapter(newSelection: T?, bindingListener: InverseBindingListener, adapter: ArrayAdapter<T>?) {
	this.adapter = adapter
	onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
		override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, p3: Long) {
			if (newSelection != getItemAtPosition(position))
				bindingListener.onChange()
		}

		override fun onNothingSelected(v: AdapterView<*>?) {}
	}

	if (newSelection != null) {
		val pos = (this.adapter as ArrayAdapter<T>).getPosition(newSelection)
		setSelection(pos)
	}
}

@Suppress("UNCHECKED_CAST")
@InverseBindingAdapter(attribute = "selection", event = "selectionAttrChanged")
fun <T> Spinner.getSelectedValue(): T {
	return selectedItem as T
}

// Bottom Sheet state 2-way binding
@BindingAdapter("bottomSheetOpen", "bottomSheetStateChanged", requireAll = false)
fun View.setBottomSheetOpen(open: Boolean, bindingListener: InverseBindingListener) {
	val behavior = BottomSheetBehavior.from(this)
	behavior.setBottomSheetCallback(null)
	behavior.state = if (open) BottomSheetBehavior.STATE_COLLAPSED else BottomSheetBehavior.STATE_HIDDEN
	behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
		override fun onSlide(bottomSheet: View, slideOffset: Float) {}

		override fun onStateChanged(bottomSheet: View, newState: Int) {
			if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN)
				bindingListener.onChange()
		}
	})
}

@InverseBindingAdapter(attribute = "bottomSheetOpen", event = "bottomSheetStateChanged")
fun View.getBottomSheetOpen(): Boolean {
	val behavior = BottomSheetBehavior.from(this)
	return behavior.state == BottomSheetBehavior.STATE_COLLAPSED
}

@BindingAdapter("touchHelperCallback")
fun <T> RecyclerView.setTouchHelperCallback(touchHelperCallback: TouchHelperCallback<T>) {

	fun getView(viewHolder: RecyclerView.ViewHolder?, @IdRes viewId: Int?): View? {
		if (viewHolder?.itemView != null && viewId != null) {
			return viewHolder.itemView.findViewById(viewId)
				?: throw IllegalArgumentException("Item view doesn't have view with $viewId id")
		}
		return null
	}

	val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
		override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {
			return false
		}

		override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
			val foregroundView = getView(viewHolder, touchHelperCallback.itemForegroundViewId)
			if (foregroundView != null) {
				ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(foregroundView)
			} else {
				super.onSelectedChanged(viewHolder, actionState)
			}
		}

		override fun onChildDrawOver(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
			val foregroundView = getView(viewHolder, touchHelperCallback.itemForegroundViewId)
			if (foregroundView != null) {
				val leftView = getView(viewHolder, touchHelperCallback.itemLeftViewId)
				val rightView = getView(viewHolder, touchHelperCallback.itemRightViewId)
				if (leftView != null && rightView != null) {
					if (dX > 0) {
						leftView.visibility = View.VISIBLE
						rightView.visibility = View.GONE
					} else {
						leftView.visibility = View.GONE
						rightView.visibility = View.VISIBLE
					}
				}
				ItemTouchHelper.Callback.getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive)
			} else {
				super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
			}
		}

		override fun onChildDraw(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
			val foregroundView = getView(viewHolder, touchHelperCallback.itemForegroundViewId)
			if (foregroundView != null) {
				val leftView = getView(viewHolder, touchHelperCallback.itemLeftViewId)
				val rightView = getView(viewHolder, touchHelperCallback.itemRightViewId)
				if (leftView != null && rightView != null) {
					if (dX > 0) {
						leftView.visibility = View.VISIBLE
						rightView.visibility = View.GONE
					} else {
						leftView.visibility = View.GONE
						rightView.visibility = View.VISIBLE
					}
				}
				ItemTouchHelper.Callback.getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive)
			} else {
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
			}
		}

		override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?) {
			val foregroundView = getView(viewHolder, touchHelperCallback.itemForegroundViewId)
			if (foregroundView != null) {
				ItemTouchHelper.Callback.getDefaultUIUtil().clearView(foregroundView)
			} else {
				super.clearView(recyclerView, viewHolder)
			}
		}

		@Suppress("UNCHECKED_CAST")
		override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
			val position = viewHolder?.adapterPosition
				?: -1
			if (adapter is BindingRecyclerViewAdapter<*>) {
				val adapter = adapter as BindingRecyclerViewAdapter<*>
				val item = adapter.getAdapterItem(position)
				item?.let { touchHelperCallback.onItemSwiped(item as T) }
			}
		}
	}

	val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
	itemTouchHelper.attachToRecyclerView(this)
}

@BindingAdapter("profitState")
fun TextView.setProfitState(profit: Double?) {
	profit?.let {
		setTextColor(ContextCompat.getColor(context, when {
			profit > 0 -> R.color.currency_profit
			profit < 0 -> R.color.currency_loss
			else -> R.color.currency_none
		}))
	}
}

@BindingAdapter("init")
fun LineChart.setupChart(boolean: Boolean) {
	axisLeft.isEnabled = false
	description.isEnabled = false

	axisRight.apply {
		setDrawAxisLine(false)
		labelCount = 2
	}

	xAxis.apply {
		setValueFormatter { value, axis ->
			val timestamp = value.toLong()
			val date = Date(timestamp)
			DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
		}
		labelCount = 2
		setDrawAxisLine(false)
	}

	isScaleYEnabled = false
	invalidate()
}

@BindingAdapter("historyPrizes", "currency", "exchangeRates")
fun LineChart.setCandles(historyPrizes: Resource<History>, currency: String, exchangeRates: ExchangeRates) {
	var entries = historyPrizes.data?.prices?.map { Entry(it.timestamp.toFloat(), it.price.toFloat()) }?.sortedBy { it.x }
	if (historyPrizes.data?.timeFrame == TimeFrame.DAY || historyPrizes.data?.timeFrame == TimeFrame.WEEK) entries = entries?.dropLast(1)        // drop last item for day and week time frame, because of wrong API data

	if (entries != null && entries.isNotEmpty() && historyPrizes.data != null) {
		marker = MarkerView(context, historyPrizes.data.currency, currency, exchangeRates)

		val btcDataSet = LineDataSet(entries, "$currency/${historyPrizes.data.coin}").apply {
			setDrawCircles(false)
			color = ContextCompat.getColor(context, R.color.primary)
			lineWidth = 1.5f
		}

		axisRight.setValueFormatter { value, axis -> Currency.formatValue(currency, exchangeRates.calculate(historyPrizes.data.currency, currency, value.toDouble())) }

		data = LineData(btcDataSet)
		data.setValueFormatter { value, entry, dataSetIndex, viewPortHandler -> Currency.formatValue(currency, exchangeRates.calculate(historyPrizes.data.currency, currency, value.toDouble())) }
		data.setDrawValues(false)
		invalidate()
	} else {
		clear()
	}
}

@BindingAdapter("historicalProfit", "currency", "exchangeRates")
fun LineChart.setHistoricalProfit(historicalProfit: List<Entry>, currency: String, exchangeRates: ExchangeRates) {
	if (historicalProfit.isNotEmpty()) {
		marker = MarkerView(context, Currency.USD, currency, exchangeRates)

		val dataSet = LineDataSet(historicalProfit, "${resources.getString(R.string.wallet_detail_profit_loss)}/$currency").apply {
			setDrawCircles(false)
			color = ContextCompat.getColor(context, R.color.primary)
			lineWidth = 1.5f
			colors = historicalProfit.mapIndexed { index, entry ->
				if (index < historicalProfit.size-1 && entry.y >= 0 && historicalProfit[index + 1].y >= 0) Color.GREEN else if(index >= historicalProfit.size-1 && entry.y >= 0) Color.RED else Color.RED }
		}

		axisRight.setValueFormatter { value, axis -> Currency.formatValue(currency, exchangeRates.calculate(Currency.USD, currency, value.toDouble())) }
		legend.isEnabled = false

		data = LineData(dataSet)
		data.setValueFormatter { value, entry, dataSetIndex, viewPortHandler -> Currency.formatValue(currency, exchangeRates.calculate(Currency.USD, currency, value.toDouble())) }
		data.setDrawValues(false)
		invalidate()
	} else {
		clear()
	}
}

@BindingAdapter("app:htmlText")
fun TextView.setHtmlText(html: String) {
	setText(Html.fromHtml(html))
}