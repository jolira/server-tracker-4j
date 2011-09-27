package com.jolira.st4j.impl;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class NaturalDeserializer implements JsonDeserializer<Object> {
    @Override
    public Object deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
        if (json.isJsonNull()) {
            return null;
        }

        if (json.isJsonPrimitive()) {
            final JsonPrimitive primitive = json.getAsJsonPrimitive();

            return handlePrimitive(primitive);
        }

        if (json.isJsonArray()) {
            final JsonArray array = json.getAsJsonArray();

            return handleArray(array, context);
        }

        final JsonObject obj = json.getAsJsonObject();

        return handleObject(obj, context);
    }

    private Object handleArray(final JsonArray json, final JsonDeserializationContext context) {
        final int size = json.size();
        final Collection<Object> collection = new ArrayList<Object>(size);

        for (int i = 0; i < size; i++) {
            final JsonElement value = json.get(i);
            final Object deserialized = context.deserialize(value, Object.class);

            collection.add(deserialized);
        }

        return collection;
    }

    private Object handleObject(final JsonObject json, final JsonDeserializationContext context) {
        final Map<String, Object> map = new HashMap<String, Object>();

        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), context.deserialize(entry.getValue(), Object.class));
        }

        return map;
    }

    private Object handlePrimitive(final JsonPrimitive json) {
        if (json.isBoolean()) {
            final boolean asBoolean = json.getAsBoolean();

            return Boolean.valueOf(asBoolean);
        }

        if (json.isString()) {
            return json.getAsString();
        }

        final BigDecimal bigDec = json.getAsBigDecimal();

        // Find out if it is an int type
        try {
            bigDec.toBigIntegerExact();

            try {
                final int val = bigDec.intValueExact();

                return Integer.valueOf(val);
            } catch (final ArithmeticException e) {
                // do nothing
            }
            final long val = bigDec.longValue();

            return Long.valueOf(val);
        } catch (final ArithmeticException e) {
            // do nothing
        }

        // Just return it as a double
        final double val = bigDec.doubleValue();

        return Double.valueOf(val);
    }
}
