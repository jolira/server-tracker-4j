package com.jolira.st4j.impl;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("javadoc")
public class NaturalDeserializerTest {

    @Test
    public void testDeserializeArray() {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Object.class, new NaturalDeserializer());

        final Gson parser = gsonBuilder.create();
        @SuppressWarnings("unchecked")
        final Collection<Object> events = (Collection<Object>) parser.fromJson("[{\"a\" : \"b\" }]", Object.class);

        assertEquals(1, events.size());

        final Iterator<Object> it = events.iterator();
        @SuppressWarnings("unchecked")
        final Map<String, Object> event = (Map<String, Object>) it.next();

        assertEquals(1, event.size());
        assertEquals("b", event.get("a"));
    }

    @Test
    public void testDeserializeBoolean() {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Object.class, new NaturalDeserializer());

        final Gson parser = gsonBuilder.create();
        final Object event = parser.fromJson("true", Object.class);

        assertEquals(Boolean.TRUE, event);
    }

    @Test
    public void testDeserializeMap() {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Object.class, new NaturalDeserializer());

        final Gson parser = gsonBuilder.create();
        @SuppressWarnings("unchecked")
        final Map<String, Object> event = (Map<String, Object>) parser.fromJson("{\"a\" : \"b\" }", Object.class);

        assertEquals(1, event.size());
        assertEquals("b", event.get("a"));
    }
}
