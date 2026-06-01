package com.org.jzprinter.database.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.room.TypeConverter;

public class IntegerListConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<Integer> fromString(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        Type type = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(value, type);
    }

    @TypeConverter
    public static String fromList(List<Integer> list) {
        if (list == null || list.isEmpty()) return "[]";
        return gson.toJson(list);
    }
}
