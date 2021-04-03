package com.luvtas.taseats.ui.cart;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.luvtas.taseats.Common.Common;
import com.luvtas.taseats.Database.CartDataSource;
import com.luvtas.taseats.Database.CartDatabase;
import com.luvtas.taseats.Database.CartItem;
import com.luvtas.taseats.Database.LocalCartDataSource;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class CartViewModel extends ViewModel {
    private MutableLiveData<List<CartItem>> mutableLiveDataCartItem;
    private CompositeDisposable compositeDisposable;
    private CartDataSource cartDataSource;

    public CartViewModel(){
        compositeDisposable = new CompositeDisposable();
    }

    public void initCartDataSource(Context context){
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(context).cartDAO());
    }

    public void onStop(){
        compositeDisposable.clear();
    }

    public MutableLiveData<List<CartItem>> getMutableLiveDataCartItem() {
        if(mutableLiveDataCartItem == null)
            mutableLiveDataCartItem = new MutableLiveData<>();
        getAllCartItems();
        return mutableLiveDataCartItem;
    }

    private void getAllCartItems() {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(cartItems -> {
            mutableLiveDataCartItem.setValue(cartItems);
        }, throwable -> {
            mutableLiveDataCartItem.setValue(null);
        }));
    }
}
