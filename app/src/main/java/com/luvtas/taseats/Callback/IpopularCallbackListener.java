package com.luvtas.taseats.Callback;

import com.luvtas.taseats.Model.PopularCategoryModel;

import java.util.List;

public interface IpopularCallbackListener {
    void onPopularLoadSuccess(List<PopularCategoryModel> popularCategoryModels);
    void onPopularLoadFailed(String message);
}
