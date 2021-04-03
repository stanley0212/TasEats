package com.luvtas.taseats.Callback;

import android.widget.LinearLayout;

import com.luvtas.taseats.Model.CommentModel;

import java.util.List;

public interface ICommentCallbackListener {
    void onCommentLoadSuccess(List<CommentModel> commentModels);
    void onCommentLoadFailed(String message);
}
