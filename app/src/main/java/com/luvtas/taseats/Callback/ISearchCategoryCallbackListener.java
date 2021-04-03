package com.luvtas.taseats.Callback;

import com.luvtas.taseats.Database.CartItem;
import com.luvtas.taseats.Model.CategoryModel;
import com.luvtas.taseats.Model.FoodModel;

public interface ISearchCategoryCallbackListener {
    void onSearchCategoryFound(CategoryModel categoryModel, CartItem cartItem);
    void onSearchCategoryNotFound(String message);
}
