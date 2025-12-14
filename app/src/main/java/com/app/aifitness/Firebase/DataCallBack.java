package com.app.aifitness.Firebase;

public interface DataCallBack<T> {
    void onSuccess(T data);
    void onError(String msg);
}
