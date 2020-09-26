package com.a99Spicy.a99spicy.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.a99Spicy.a99spicy.MyApplication
import com.a99Spicy.a99spicy.R
import com.a99Spicy.a99spicy.database.DatabaseCart
import com.a99Spicy.a99spicy.database.asLineItems
import com.a99Spicy.a99spicy.databinding.CartFragmentBinding
import com.a99Spicy.a99spicy.network.*
import com.a99Spicy.a99spicy.ui.HomeActivity
import com.a99Spicy.a99spicy.ui.order.OrderFragment
import com.a99Spicy.a99spicy.ui.payment.PaymentActivity
import com.a99Spicy.a99spicy.ui.payment.PaymentMethodFragment
import com.a99Spicy.a99spicy.ui.profile.Loading
import com.a99Spicy.a99spicy.utils.AppUtils
import com.a99Spicy.a99spicy.utils.Constants
import timber.log.Timber


class CartFragment : Fragment(), PaymentMethodFragment.OnPaymentMethodClickListener,
    OrderFragment.OnOrderCompleteListener {

    private lateinit var viewModel: CartViewModel
    private lateinit var cartFragmentBinding: CartFragmentBinding
    private lateinit var cartListAdapter: CartListAdapter
    private var totalAmount = 0.00
    private var cartList: MutableSet<DatabaseCart> = mutableSetOf()
    private lateinit var placeOrder: PlaceOrder
    private lateinit var userId: String
    private lateinit var shipping: Shipping
    private lateinit var walletBalance:String

    private lateinit var profile:Profile
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //Inflating layout
        cartFragmentBinding = CartFragmentBinding.inflate(inflater, container, false)

        //Initializing ViewModel class
        val application = requireNotNull(this.activity).application as MyApplication
        val viewModelFactory = CartViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory).get(CartViewModel::class.java)

        //Getting values from activity
        val activity = activity as HomeActivity
        activity.setAppBarElevation(0F)
        activity.setToolbarTitle(getString(R.string.title_cart))
        activity.setToolbarLogo(null)
        userId = activity.getUserId()

        //Getting arguments
        val arguments = CartFragmentArgs.fromBundle(requireArguments())
        profile = arguments.profile
        shipping = profile.shipping
        cartFragmentBinding.deliveryLocationTextView.text =
            "${shipping.postcode} ${shipping.city}"

        //Setting up cart RecyclcerView
        cartFragmentBinding.cartRecyclerView.itemAnimator = null
        //Setting up cart recyclerView
        cartListAdapter = CartListAdapter(CartListItemClickListener {
        }, this, viewLifecycleOwner)
        cartFragmentBinding.cartRecyclerView.adapter = cartListAdapter

        //Observing Cart Items
        viewModel.cartItemsLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it.isNotEmpty()) {
                    cartFragmentBinding.cartEmptyTextView.visibility = View.GONE
                    cartList.addAll(it)
                    for (item in it) {
                        Timber.e("name ${item.name}, quantity: ${item.quantity}")
                        item.salePrice?.let { itemPrice ->
                            if (itemPrice.isNotEmpty()) {
                                totalAmount += itemPrice.toDouble().times(item.quantity)
                            }
                        }
                    }
                    cartFragmentBinding.cartTotalAmountTextView.text =
                        totalAmount.toString() + " Rs/-"
                    cartListAdapter.submitList(it)
                } else {
                    cartListAdapter.submitList(it)
                    cartFragmentBinding.cartEmptyTextView.visibility = View.VISIBLE
                }
            }
        })

        //Set onClickListener to placeOrder button
        cartFragmentBinding.placeOrderButton.setOnClickListener {

            val paymentMethodBottomSheetFragment = PaymentMethodFragment(this)
            paymentMethodBottomSheetFragment.show(
                requireActivity().supportFragmentManager,
                paymentMethodBottomSheetFragment.tag
            )
        }

        //Observe wallet balance
        viewModel.walletBalanceLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                walletBalance = it
                if (walletBalance.toDouble() > 1.00) {
                    viewModel.cdWallet(userId, WalletRequest("debit", 1.00, "checkout"))
                    viewModel.resetWalletBalance()
                } else {
                    Toast.makeText(requireContext(), "Add money to Wallet", Toast.LENGTH_LONG)
                        .show()
                }
            }
        })

        viewModel.walletResponseLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                goToOrder()
                viewModel.resetWallet()
            }
        })
        return cartFragmentBinding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.PLACE_ORDER_REQUEST_CODE) {
            val message = data?.getStringExtra(Constants.MESSAGE)
            message?.let {
                if (it == getString(R.string.success)) {
                    goToOrder()
                } else {
                    Toast.makeText(requireContext(), "Payment Failed, Try Again", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    override fun onPaymentMethodClick(name: String) {

        placeOrder = PlaceOrder(
            "INR",
            userId.toInt(),
            shipping,
            name,
            "Checkout",
            AppUtils.generatePaytmOrderId(),
            cartList.toList().asLineItems(),
            true
        )

        if (name == getString(R.string.credit_card_debit_card_upi) || name == getString(R.string.paytm)) {
            val intent = Intent(activity, PaymentActivity::class.java)
            intent.putExtra(Constants.AMOUNT, totalAmount.toString())
            intent.putExtra(Constants.TRANSACTION_MODE, name)
            intent.putExtra(Constants.ORDER, placeOrder)
            intent.putExtra(Constants.TRANSACTION_TYPE, getString(R.string.place_order))
            startActivityForResult(intent, Constants.PLACE_ORDER_REQUEST_CODE)
        }else{
            viewModel.getWalletBalance(userId)
        }
    }

    private fun goToOrder() {
        val orderFragment = OrderFragment(placeOrder, this)
        orderFragment.show(requireActivity().supportFragmentManager, orderFragment.tag)
    }

    override fun onOrderComplete(orderResponse: OrderResponse) {
        findNavController().navigate(
            CartFragmentDirections.actionCartFragmentToOrderDetailsFragment(
                orderResponse
            )
        )
    }
}