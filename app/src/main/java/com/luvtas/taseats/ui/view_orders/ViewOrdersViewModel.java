package com.luvtas.taseats.ui.view_orders;



import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.luvtas.taseats.Model.Order;

import java.util.List;

public class ViewOrdersViewModel extends ViewModel {
    private MutableLiveData<List<Order>> mutableLiveDataOrderList;

    public ViewOrdersViewModel(){
        mutableLiveDataOrderList = new MutableLiveData<>();
    }

    public MutableLiveData<List<Order>> getMutableLiveDataOrderList() {
        return mutableLiveDataOrderList;
    }

    public void setMutableLiveDataOrderList(List<Order> orderList) {
        mutableLiveDataOrderList.setValue(orderList);
    }
}
