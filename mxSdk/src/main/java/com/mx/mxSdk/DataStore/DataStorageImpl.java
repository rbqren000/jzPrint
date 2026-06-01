package com.mx.mxSdk.DataStore;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class DataStorageImpl<E> implements DataStorage<E> {

    private static final String TAG = DataStorageImpl.class.getSimpleName();

    protected List<E> data;

    public DataStorageImpl() {
        this.data = new ArrayList<>();
    }

    @Override
    public void add(E e) {

        if (!this.contains(e)) {
            this.data.add(e);
        }
    }

    public void add(int location,E e) {
        if (!this.contains(e)) {

            this.data.add(location,e);
        }
    }

    public void addFirst(E e) {

        if (!this.contains(e)) {
            this.data.add(0,e);
        }
    }

    public void addLast(E e) {
        if (!this.contains(e)) {

            this.data.add(data.size(),e);
        }
    }

    @Override
    public void add(List<E> e) {
        this.data.addAll(e);
    }

    @Override
    public boolean contains(E e) {
        return this.data.contains(e);
    }

    @Override
    public boolean contains(String attributeName, Object attributeValue) {

        for (E e : this.data) {

            try {
//                Field field = e.getClass().getField(attributeName);
                Field field = e.getClass().getDeclaredField(attributeName);
                Object fieldValue = field.get(e);
                if (fieldValue==null){
                    return false;
                }
                if (fieldValue.equals(attributeValue)){
                    return true;
                }
            } catch (NoSuchFieldException | IllegalAccessException
                    | IllegalArgumentException e1) {

                e1.printStackTrace();

            }

        }
        return false;
    }

    @Override
    public E get(String attributeName, Object attributeValue) {

        for (E e : this.data) {

            try {
//                Field field = e.getClass().getField(attributeName);
                Field field = e.getClass().getDeclaredField(attributeName);

                Object fieldValue = field.get(e);
                if (fieldValue==null){
                    return null;
                }

                if (fieldValue.equals(attributeValue))
                    return e;

            } catch (Exception el) {

                Log.e(TAG, "get发生异常");
            }

        }
        return null;
    }

    @Override
    public List<E> get() {
        return this.data;
    }

    @Override
    public E get(int location) {
        return this.data.get(location);
    }

    @Override
    public void remove(int location) {

        if (location >= 0 && location < this.data.size()) {
            this.data.remove(location);
        }
    }

    @Override
    public void remove(E e) {
        this.data.remove(e);
    }

    @Override
    public int size() {
        return this.data.size();
    }

    @Override
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    @Override
    public void clear() {
        this.data.clear();
    }
}
