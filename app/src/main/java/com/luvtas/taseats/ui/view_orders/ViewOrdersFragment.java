package com.luvtas.taseats.ui.view_orders;

import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.androidwidgets.formatedittext.widgets.FormatEditText;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.luvtas.taseats.Adapter.MyOrdersAdapter;
import com.luvtas.taseats.Callback.ILoadOrderCallbackListener;
import com.luvtas.taseats.Common.Common;
import com.luvtas.taseats.Common.MySwipeHelper;
import com.luvtas.taseats.Database.CartDataSource;
import com.luvtas.taseats.Database.CartDatabase;
import com.luvtas.taseats.Database.CartItem;
import com.luvtas.taseats.Database.LocalCartDataSource;
import com.luvtas.taseats.EventBus.CounterCartEvent;
import com.luvtas.taseats.EventBus.MenuItemBack;
import com.luvtas.taseats.Model.Order;
import com.luvtas.taseats.Model.RefundRequestModel;
import com.luvtas.taseats.Model.ShippingOrderModel;
import com.luvtas.taseats.R;
import com.luvtas.taseats.TrackingOrderActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ViewOrdersFragment extends Fragment implements ILoadOrderCallbackListener {

    private ViewOrdersViewModel viewOrdersViewModel;
    private ILoadOrderCallbackListener listener;
    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;
    private Unbinder unbinder;
    AlertDialog dialog;

    CartDataSource cartDataSource;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static ViewOrdersFragment newInstance() {
        return new ViewOrdersFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
       viewOrdersViewModel = ViewModelProviders.of(this).get(ViewOrdersViewModel.class);
       View root = inflater.inflate(R.layout.fragment_view_orders, container,false);

       unbinder = ButterKnife.bind(this,root);
       initViews(root);
       loadOrdersFromFirebase();
       viewOrdersViewModel.getMutableLiveDataOrderList().observe(getViewLifecycleOwner(), orderList -> {
           MyOrdersAdapter adapter = new MyOrdersAdapter(getContext(), orderList);
           // order by desc
           Collections.reverse(orderList);
           recycler_orders.setAdapter(adapter);
       });
       return root;
    }

    private void loadOrdersFromFirebase() {
        List<Order> orderList = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.ORDER_REF)
                .orderByChild("userId")
                .equalTo(Common.currentUser.getUid())
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot orderSnapShot : dataSnapshot.getChildren())
                        {
                            Order order = orderSnapShot.getValue(Order.class);
                            order.setOrderNumber(orderSnapShot.getKey());
                            orderList.add(order);
                        }
                        listener.onLoadOrderSuccess(orderList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadOrderFailed(databaseError.getMessage());
                    }
                });
    }

    private void initViews(View root) {

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        listener = this;

        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).build();

        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));


        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_orders,250) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Cancel Order", 30, 0 , Color.parseColor("#FF3C30"),
                        pos -> {

                    Order orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);
                            Log.d("status", String.valueOf(orderModel.getOrderStatus()));
                    if(orderModel.getOrderStatus() == 0)
                    {
                        if(orderModel.isCod())
                        {
                            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                            builder.setTitle("Cancel Order")
                                    .setMessage("Do you really want to cancel this order?")
                                    .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                                    .setPositiveButton("YES", (dialogInterface, i) -> {

                                        Map<String, Object> update_data = new HashMap<>();
                                        update_data.put("orderStatus", -1);
                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.RESTAURANT_REF)
                                                .child(Common.currentRestaurant.getUid())
                                                .child(Common.ORDER_REF)
                                                .child(orderModel.getOrderNumber())
                                                .updateChildren(update_data)
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        orderModel.setOrderStatus(-1);
                                                        ((MyOrdersAdapter)recycler_orders.getAdapter()).setItemAtPosition(pos,orderModel);
                                                        recycler_orders.getAdapter().notifyItemChanged(pos);
                                                        Toast.makeText(getContext(),"Cancel order successfully!", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    });

                            androidx.appcompat.app.AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        else  // Not Cash of delivery
                        {
                            View layout_refund_request = LayoutInflater.from(getContext())
                                    .inflate(R.layout.layout_refund_request, null);

                            EditText edt_name = (EditText) layout_refund_request.findViewById(R.id.edt_card_name);
                            FormatEditText edt_card_number = (FormatEditText)layout_refund_request.findViewById(R.id.edt_card_number);
                            FormatEditText edt_card_exp = (FormatEditText)layout_refund_request.findViewById(R.id.edt_exp);

                            // Format credit card
                            edt_card_number.setFormat("---- ---- ---- ----");
                            edt_card_exp.setFormat("--/--");

                            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                            builder.setTitle("Cancel Order")
                                    .setMessage("Do you really want to cancel this order?")
                                    .setView(layout_refund_request)
                                    .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                                    .setPositiveButton("YES", (dialogInterface, i) -> {

                                        RefundRequestModel refundRequestModel = new RefundRequestModel();
                                        refundRequestModel.setName(Common.currentUser.getName());
                                        refundRequestModel.setPhone(Common.currentUser.getPhone());
                                        refundRequestModel.setCardName(edt_name.getText().toString());
                                        refundRequestModel.setCardNumber(edt_card_number.getText().toString());
                                        refundRequestModel.setCardExp(edt_card_exp.getText().toString());
                                        refundRequestModel.setAmount(orderModel.getFinalPayment());

                                        String key = new StringBuilder()
                                                .append(Common.currentUser.getPhone())
                                                .append("_")
                                                .append(orderModel.getOrderNumber())
                                                .toString();

                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.RESTAURANT_REF)
                                                .child(Common.currentRestaurant.getUid())
                                                .child(Common.REQUEST_REFUND_REF)
                                                .child(key)
                                                .setValue(refundRequestModel)
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
//                                                        orderModel.setOrderStatus(-1);
//                                                        ((MyOrdersAdapter)recycler_orders.getAdapter()).setItemAtPosition(pos,orderModel);
//                                                        recycler_orders.getAdapter().notifyItemChanged(pos);
//                                                        Toast.makeText(getContext(),"Cancel order successfully!", Toast.LENGTH_SHORT).show();

                                                        // update Firebase
                                                        Map<String, Object> update_data = new HashMap<>();
                                                        update_data.put("orderStatus", -1);
                                                        FirebaseDatabase.getInstance()
                                                                .getReference(Common.RESTAURANT_REF)
                                                                .child(Common.currentRestaurant.getUid())
                                                                .child(Common.ORDER_REF)
                                                                .child(orderModel.getOrderNumber())
                                                                .updateChildren(update_data)
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Toast.makeText(getContext(),""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                })
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        orderModel.setOrderStatus(-1);
                                                                        ((MyOrdersAdapter)recycler_orders.getAdapter()).setItemAtPosition(pos,orderModel);
                                                                        recycler_orders.getAdapter().notifyItemChanged(pos);
                                                                        Toast.makeText(getContext(),"Cancel order successfully!", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });

                                                    }
                                                });
                                    });

                            androidx.appcompat.app.AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                    else
                    {
                        Toast.makeText(getContext(), new StringBuilder("Your order was changed to ")
                        .append(Common.convertStatusToText(orderModel.getOrderStatus()))
                        .append(" , so you can't cancel it"), Toast.LENGTH_SHORT).show();
                    }

                        }));


                buf.add(new MyButton(getContext(), "Tracking Order", 30, 0 , Color.parseColor("#001970"),
                        pos -> {

                            Order orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);

                            // Fetch from Firebase
                            FirebaseDatabase.getInstance()
                                    .getReference(Common.RESTAURANT_REF)
                                    .child(Common.currentRestaurant.getUid())
                                    .child(Common.SHIPPING_ORDER_REF)
                                    .child(orderModel.getOrderNumber())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            //Log.d("test222", String.valueOf(dataSnapshot.exists()));

                                            if(dataSnapshot.exists())
                                            {
                                                Common.currentShippingOrder = dataSnapshot.getValue(ShippingOrderModel.class);
                                                Common.currentShippingOrder.setKey(dataSnapshot.getKey());
                                                if(Common.currentShippingOrder.getCurrentLat() != -1 &&
                                                Common.currentShippingOrder.getCurrentLng() != -1)
                                                {
                                                    startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                                                }
                                                else
                                                {
                                                    Toast.makeText(getContext(),"Shipper not start delivery your order, just wait a moment", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            else
                                            {
                                                Toast.makeText(getContext(),"Your order just placed, must be wait it shipping", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            Toast.makeText(getContext(),""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }));

                buf.add(new MyButton(getContext(), "Repeat Order", 30, 0 , Color.parseColor("#5d4e37"),
                        pos -> {

                            Order orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);

                            //Toast.makeText(getContext(), "Repeat Order", Toast.LENGTH_SHORT).show();
                            dialog.show();
                            cartDataSource.cleanCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {

                                            CartItem[] cartItems = orderModel
                                                    .getCartItemList().toArray(new CartItem[orderModel.getCartItemList().size()]);

                                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItems)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(() -> {
                                                dialog.dismiss();
                                                Toast.makeText(getContext(),"Add all items in order cart success", Toast.LENGTH_SHORT).show();
                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                            }, throwable -> {
                                                Toast.makeText(getContext(), ""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
                                            })
                                            );
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            //Log.d("test2", "OK");
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "[Error]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }));

            }
        };


    }

    @Override
    public void onLoadOrderSuccess(List<Order> orderList) {
        dialog.dismiss();
        viewOrdersViewModel.setMutableLiveDataOrderList(orderList);
    }

    @Override
    public void onLoadOrderFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(),message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }
}
