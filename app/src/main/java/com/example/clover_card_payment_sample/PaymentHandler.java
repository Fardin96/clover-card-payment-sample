package com.example.clover_card_payment_sample;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
//import com.clover.sdk.v1.account.CloverAccount;
import com.clover.connector.sdk.v3.PaymentConnector;
import com.clover.sdk.v3.connector.IPaymentConnector;
import com.clover.sdk.v3.connector.IPaymentConnectorListener;
//import com.clover.sdk.v3.connector.PaymentConnector;
//import com.clover.sdk.v3.order.SaleResponse;
import com.clover.sdk.v3.payments.Payment;
import com.clover.sdk.v3.remotepay.AuthResponse;
import com.clover.sdk.v3.remotepay.PreAuthResponse;
import com.clover.sdk.v3.remotepay.SaleResponse;
import com.clover.sdk.v3.remotepay.TipAdjustAuthResponse;
//import com.clover.sdk.v3.payments.VoidPaymentResponse;

public class PaymentHandler {


    private PaymentConnector initializePaymentConnector() {
        // Get the Clover account that will be used with the service; uses the GET_ACCOUNTS permission
        Account cloverAccount = CloverAccount.getAccount(this);
        // Set your RAID as the remoteApplicationId
        String remoteApplicationId = "SWDEFOTWBD7XT.6W3D67YDX8GN3";

        //Implement the interface
        IPaymentConnectorListener paymentConnectorListener = new IPaymentConnectorListener() {

            @Override
            public void onPreAuthResponse(PreAuthResponse response) {
                Log.d(TAG, "onPreAuthResponse: " + response.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.getSuccess()) {
                            Payment _payment = response.getPayment();
                            CardTransaction cardTransaction = _payment.getCardTransaction();
                            String cardDetails = cardTransaction.getCardType().toString() + " " + cardTransaction.getLast4();
                            long cashback = _payment.getCashbackAmount() == null ? 0 : _payment.getCashbackAmount();
                            long tip = _payment.getTipAmount() == null ? 0 : _payment.getTipAmount();
                            POSPayment payment = new POSPayment(_payment.getAmount(), cardDetails, cardTransaction.getCardType(), new Date(_payment.getCreatedTime()), _payment.getId(), _payment.getTender().getLabel(),
                                    "PreAuth", cardTransaction.getType(), false, cardTransaction.getEntryType(), cardTransaction.getState(), cashback, _payment.getOrder().getId(), _payment.getExternalPaymentId(), _payment.getTaxAmount(), tip);
                            setPaymentStatus(payment, response);
                            payment.setResult(_payment.getResult());
                            store.getCurrentOrder().setPreAuth(payment);
                            store.addTransaction(payment);
                            showMessage("PreAuth successfully processed.", Toast.LENGTH_SHORT);
                            preAuthSuccess(_payment.getCardTransaction());
                        } else {
                            showMessage("PreAuth: " + response.getResult(), Toast.LENGTH_LONG);
                        }
                    }
                });
                displayConnector.showWelcomeScreen();
            }

            @Override
            public void onAuthResponse(AuthResponse response) {
                Log.d(TAG, "onAuthResponse: "+ response.toString());
                if (response.getSuccess()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Payment _payment = response.getPayment();
                            CardTransaction cardTransaction = _payment.getCardTransaction();
                            long cashback = _payment.getCashbackAmount() == null ? 0 : _payment.getCashbackAmount();
                            long tip = _payment.getTipAmount() == null ? 0 : _payment.getTipAmount();//
                            String cardDetails = cardTransaction.getCardType().toString() + " " + cardTransaction.getLast4();
                            POSPayment payment = new POSPayment(_payment.getAmount(), cardDetails, cardTransaction.getCardType(), new Date(_payment.getCreatedTime()), _payment.getId(), _payment.getTender().getLabel(),
                                    "Auth", cardTransaction.getType(), false, cardTransaction.getEntryType(), cardTransaction.getState(), cashback, _payment.getOrder().getId(), _payment.getExternalPaymentId(), _payment.getTaxAmount(), tip);
                            setPaymentStatus(payment, response);
                            payment.setResult(_payment.getResult());
                            store.addPaymentToOrder(payment, store.getCurrentOrder());
                            store.addTransaction(payment);
                            showMessage("Auth successfully processed.", Toast.LENGTH_SHORT);

                            store.createOrder(false);
                            CurrentOrderFragment currentOrderFragment = (CurrentOrderFragment) getFragmentManager().findFragmentById(R.id.PendingOrder);
                            currentOrderFragment.setOrder(store.getCurrentOrder());
                            hidePreAuth();
                            showRegister(null);
                            displayConnector.showWelcomeScreen();
                        }
                    });
                } else {
                    showMessage("Auth error:" + response.getResult(), Toast.LENGTH_LONG);
                    displayConnector.showMessage("There was a problem processing the transaction");
                }
            }

            @Override
            public void onTipAdjustAuthResponse(TipAdjustAuthResponse response) {
                Log.d(TAG, "onTipAdjustAuthResponse: " + response.toString());
                if (response.getSuccess()) {
                    boolean updatedTip = false;
                    for (POSOrder order : store.getOrders()) {
                        for (POSTransaction exchange : order.getPayments()) {
                            if (exchange instanceof POSPayment) {
                                POSPayment posPayment = (POSPayment) exchange;
                                if (exchange.getId().equals(response.getPaymentId())) {
                                    posPayment.setTipAmount(response.getTipAmount());
                                    updatePaymentDetailsTip(posPayment);
                                    // TODO: should the stats be updated?
                                    updatedTip = true;
                                    break;
                                }
                            }
                        }
                        if (updatedTip) {
                            showMessage("Tip successfully adjusted", Toast.LENGTH_LONG);
                            break;
                        }
                    }
                } else {
                    showMessage("Tip adjust failed", Toast.LENGTH_LONG);
                }
            }



            public void onSaleResponse(SaleResponse response) {
                String result;
                if(response.getSuccess()) {
                    result = "Sale was successful";
                } else {
                    result = "Sale was unsuccessful" + response.getReason() + ":" + response.getMessage();
                }
                Toast.makeText(getApplication().getApplicationContext(), result, Toast.LENGTH_LONG).show();
            }
        };

        // Implement the other IPaymentConnector listener methods

        // Create the PaymentConnector with the context, account, listener, and RAID
        return new PaymentConnector(this, cloverAccount, paymentConnectorListener, remoteApplicationId);
    }

}
