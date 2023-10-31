package com.afkanerd.deku.DefaultSMS.Models;

import androidx.lifecycle.ViewModel;

public interface RoomViewModel<T> {

    void insert(T entity);
}
