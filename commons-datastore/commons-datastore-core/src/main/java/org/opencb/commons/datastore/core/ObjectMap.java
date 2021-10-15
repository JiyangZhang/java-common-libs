/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.commons.datastore.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by imedina on 20/03/14.
 */
public class ObjectMap implements Map<String, Object>, Serializable {

    private Map<String, Object> objectMap;
    protected ObjectMapper jsonObjectMapper = new ObjectMapper();

    public ObjectMap() {
        objectMap = new LinkedHashMap<>();
    }

    public ObjectMap(int size) {
        objectMap = new LinkedHashMap<>(size);
    }

    public ObjectMap(final String key, final Object value) {
        objectMap = new LinkedHashMap<>();
        objectMap.put(key, value);
    }

    public ObjectMap(final Map<String, ?> map) {
        objectMap = new LinkedHashMap<>(map);
    }

    public ObjectMap(String json) {
        try {
            objectMap = new LinkedHashMap<>();
            objectMap.putAll(jsonObjectMapper.readValue(json, objectMap.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toJson() {
        try {
            return jsonObjectMapper.writeValueAsString(objectMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Deprecated
    public String safeToString() {
        Iterator<String> iter = objectMap.keySet().iterator();
        String key;
        StringBuilder sb = new StringBuilder("{\n");
        while (iter.hasNext()) {
            key = iter.next();
            if (!key.equals("result")) {
                sb.append("\t" + key + ": " + objectMap.get(key) + ",\n");
            }
        }
        sb.append("}");
        return sb.toString();
    }


    public String getString(String field) {
        return getString(field, "");
    }

    public String getString(String field, String defaultValue) {
        if (field != null) {
            Object o = get(field);
            if (o != null) {
                if (o instanceof Collection) {
                    //Join manually to avoid surrounding brackets
                    return ((Collection<?>) o).stream().map(Objects::toString).collect(Collectors.joining(","));
                } else {
                    return o.toString();
                }
            } else {
                return defaultValue;
            }
        }
        return defaultValue;
    }


    public int getInt(String field) {
        return getInt(field, 0);
    }

    public int getInt(String field, int defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            } else if (obj instanceof String) {
                try {
                    return Integer.parseInt((String) obj);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    public long getLong(String field) {
        return getLong(field, 0L);
    }

    public long getLong(String field, long defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj instanceof Number) {
                return ((Number) obj).longValue();
            } else if (obj instanceof String) {
                try {
                    return Long.parseLong((String) obj);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }


    public float getFloat(String field) {
        return getFloat(field, 0.0f);
    }

    public float getFloat(String field, float defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj instanceof Number) {
                return ((Number) obj).floatValue();
            } else if (obj instanceof String) {
                try {
                    return Float.parseFloat((String) obj);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }


    public double getDouble(String field) {
        return getDouble(field, 0.0);
    }

    public double getDouble(String field, double defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            } else if (obj instanceof String) {
                try {
                    return Double.parseDouble((String) obj);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }


    public boolean getBoolean(String field) {
        return getBoolean(field, false);
    }

    public boolean getBoolean(String field, boolean defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj instanceof Boolean) {
                return ((Boolean) obj);
            } else if (obj instanceof String) {
                return Boolean.parseBoolean((String) obj);
            }
        }
        return defaultValue;
    }


    /**
     * Gets the value of the given key, casting it to the given {@code Class<T>}.  This is useful to avoid having casts
     * in client code, though the effect is the same.  So to get the value of a key that is of type String, you would write
     * {@code String name = doc.get("name", String.class)} instead of {@code String name = (String) doc.get("x") }.
     *
     * @param field the field
     * @param clazz the class to cast the value to
     * @param <T>   the type of the class
     * @return the value of the given key, or null if the instance does not contain this key, or defaultValue if the object
     * is not of type T.
     */
    public <T> T get(final String field, final Class<T> clazz) {
        return get(field, clazz, null);
    }

    public <T> T get(final String field, final Class<T> clazz, T defaultValue) {
        Object obj = get(field);
        if (obj != null) {
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            }
        }
        return defaultValue;
    }

    /**
     * Some fields can be a List, this method cast the Object to List of generic Objects.
     *
     * @param field List field name
     * @return A List representation of the field
     */
    public List<Object> getList(String field) {
        return getList(field, null);
    }

    public List<Object> getList(String field, final List<Object> defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj != null) {
                return (List<Object>) obj;
            }
        }
        return defaultValue;
    }

    public List<String> getAsStringList(String field) {
        return getAsStringList(field, ",");
    }

    public List<String> getAsStringList(String field, String separator) {
        List list = getAsList(field, separator);
        if (!list.isEmpty() && list.get(0) instanceof String) {
            return ((List<String>) list);
        } else {
            List<String> stringList = new ArrayList<>(list.size());
            for (Object o : list) {
                stringList.add(o == null ? null : o.toString());
            }
            return stringList;
        }
    }
//
//    public List<Integer> getAsIntegerList(String field) {
//        return getAsIntegerList(field, ",");
//    }
//
//    public List<Integer> getAsIntegerList(String field, String separator) {
//        List list = getAsList(field, separator);
//        if (!list.isEmpty() && list.get(0) instanceof Integer) {
//            return ((List<Integer>) list);
//        } else {
//            List<Integer> integerList = new ArrayList<>(list.size());
//            for (Object o : list) {
//                int i;
//                if (o instanceof Integer) {
//                    i = (int) o;
//                } else {
//                    i = Integer.parseInt(o.toString());
//                }
//                integerList.add(i);
//            }
//            return integerList;
//        }
//    }

    public List<Integer> getAsIntegerList(String field) {
        return getAsNumberList(field, Integer.class, Integer::parseInt, ",");
    }

    public List<Integer> getAsIntegerList(String field, String separator) {
        return getAsNumberList(field, Integer.class, Integer::parseInt, separator);
    }

    public List<Long> getAsLongList(String field) {
        return getAsNumberList(field, Long.class, Long::parseLong, ",");
    }

    public List<Long> getAsLongList(String field, String separator) {
        return getAsNumberList(field, Long.class, Long::parseLong, separator);
    }

    public List<Double> getAsDoubleList(String field) {
        return getAsNumberList(field, Double.class, Double::parseDouble, ",");
    }

    public List<Double> getAsDoubleList(String field, String separator) {
        return getAsNumberList(field, Double.class, Double::parseDouble, separator);
    }

    public List<Short> getAsShortList(String field) {
        return getAsNumberList(field, Short.class, Short::parseShort, ",");
    }

    public List<Short> getAsShortList(String field, String separator) {
        return getAsNumberList(field, Short.class, Short::parseShort, separator);
    }

    public List<Byte> getAsByteList(String field) {
        return getAsNumberList(field, Byte.class, Byte::parseByte, ",");
    }

    public List<Byte> getAsByteList(String field, String separator) {
        return getAsNumberList(field, Byte.class, Byte::parseByte, separator);
    }

    protected <N extends Number> List<N> getAsNumberList(String field, Class<N> clazz, Function<String, N> parser, String separator) {
        List list = getAsList(field, separator);

        if (list.isEmpty()) {
            return Collections.<N>emptyList();
        } else {
            boolean valid = true;
            for (Object o : list) {
                if (!clazz.isInstance(o)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return list;
            } else {
                List<N> numericList = new ArrayList<>(list.size());
                for (Object o : list) {
                    if (clazz.isInstance(o)) {
                        numericList.add(clazz.cast(o));
                    } else {
                        numericList.add(parser.apply(o.toString()));
                    }
                }
                return numericList;
            }
        }
    }

    /**
     * Some fields can be a List, this method cast the Object to aList of type T. Gets the value of the given key, casting it to the given
     * {@code Class<T>}.  This is useful to avoid having casts
     * in client code, though the effect is the same.  So to get the value of a key that is of type String, you would write
     * {@code String name = doc.get("name", String.class)} instead of {@code String name = (String) doc.get("x") }.
     *
     * @param field List field name
     * @param clazz Class to be returned
     * @param <T> Element class
     * @return A List representation of the field
     */
    @Deprecated
    public <T> List<T> getAsList(String field, final Class<T> clazz) {
        return getAsList(field, clazz, null);
    }

    @Deprecated
    public <T> List<T> getAsList(String field, final Class<T> clazz, List<T> defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj != null) {
                return (List<T>) get(field);
            }
        }
        return defaultValue;
    }

    public List<Object> getAsList(String field) {
        return getAsList(field, ",");
    }

    /**
     * Get the field as List. If field was not a list, returns an UnmodifiableList.
     * Do not modify the ObjectMap content.
     *
     * @param field List field name
     * @param separator Item separator
     * @return A list of objects
     */
    public List<Object> getAsList(String field, String separator) {
        Object value = get(field);
        if (value == null) {
            return Collections.emptyList();
        } else {
            if (value instanceof List) {
                return (List) value;
            } else if (value instanceof Collection) {
                Collection x = (Collection) value;
                return new ArrayList<Object>(x);
            } else {
                return Arrays.<Object>asList(value.toString().split(separator));
            }
        }
    }


    /**
     * Some fields can be also a Map, this method cast the Object to Map.
     *
     * @param field List field name
     * @return A Map representation of the field
     */
    public Map<String, Object> getMap(String field) {
        return getMap(field, null);
    }


    public Map<String, Object> getMap(String field, Map<String, Object> defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj != null) {
                return (Map<String, Object>) obj;
            }
        }
        return defaultValue;
    }

    /**
     * Some fields can be a Map, this method cast the Object to Map of type T. Gets the value of the given key, casting it to the given
     * {@code Class<T>}.  This is useful to avoid having casts
     * in client code, though the effect is the same.  So to get the value of a key that is of type String, you would write
     * {@code String name = doc.get("name", String.class)} instead of {@code String name = (String) doc.get("x") }.
     *
     * @param field List field name
     * @param clazz Class to be returned
     * @param <T> Element class
     * @return A Map representation of the field
     */
    @Deprecated
    public <T> Map<String, T> getMapAs(String field, final Class<T> clazz) {
        return getMapAs(field, clazz, null);
    }

    @Deprecated
    public <T> Map<String, T> getMapAs(String field, final Class<T> clazz, Map<String, T> defaultValue) {
        if (field != null) {
            Object obj = get(field);
            if (obj != null) {
                return (Map<String, T>) obj;
            }
        }
        return defaultValue;
    }


    public ObjectMap append(String key, Object value) {
        put(key, value);
        return this;
    }

    public ObjectMap appendAll(Map<String, ?> m) {
        putAll(m);
        return this;
    }

    public Object putIfNotNull(String key, Object value) {
        if (key != null && value != null) {
            return objectMap.put(key, value);
        }
        return value;
    }

    public Object putIfNotEmpty(String key, String value) {
        if (key != null && value != null && !value.isEmpty()) {
            return objectMap.put(key, value);
        }
        return value;
    }


    /**
     * Map methods implementation. Side effect of composition.
     */
    @Override
    public int size() {
        return objectMap.size();
    }

    @Override
    public boolean isEmpty() {
        return objectMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (objectMap.containsKey(key)) {
            return true;
        } else if (key instanceof String && ((String) key).contains(".")) {
            return getNested(((String) key)) != null;
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return objectMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        Object value = objectMap.get(key);
        if (value == null && key instanceof String && ((String) key).contains(".")) {
            return getNested(((String) key));
        }
        return value;
    }

    @Override
    public Object put(String key, Object value) {
        return put(key, value, false);
    }

    public Object put(String key, Object value, boolean nestedKey) {
        return put(key, value, nestedKey, false);
    }

    public Object put(String key, Object value, boolean nestedKey, boolean parents) {
        if (nestedKey) {
            return putNested(key, value, parents);
        } else {
            return objectMap.put(key, value);
        }
    }

    /**
     * Get nested attributes.
     *
     * @param key     Key that will be looked for.
     * @param listId  In case of finding an array of objects, the identifier that needs to match. See example below:
     *        Example:
     *         List<ObjectMap> values = Arrays.asList(
     *                 new ObjectMap("id", "abc").append("name", "ABC").append("nested", new ObjectMap("value", "A")),
     *                 new ObjectMap("id", "def").append("name", "DEF").append("nested", new ObjectMap("value", "D")),
     *                 new ObjectMap("id", "ghi").append("name", "GHI").append("nested", new ObjectMap("value", "G"))
     *         );
     *         objectMap.put("objectList", values);
     *
     *         Let's say we send key="objectList.abc.nested.value" and listId="id"
     *         What that means is that we will look for objectList (map) first. In that scenario, the value is not a map anymore but a list,
     *              which means that the next key 'abc' will correspond to one unique value that we need to find within the list. This value
     *              should correspond to the key passed in `listId`, so we will need to iterate and find which of the elements matches
     *              with 'id=abc'. Once found, we will proceed as usual, taking 'nested.value' as the final result. -> "A"
     *
     * @return Object value.
     */
    public Object getNested(String key, String listId) {
        int idx = key.lastIndexOf(".");
        if (idx < 0) {
            return get(key);
        }
        String mapKey = key.substring(0, idx);
        String valueKey = key.substring(idx + 1);
        Map<String, Object> subMap = getNestedMap(mapKey, listId, objectMap, jsonObjectMapper, false, false);
        if (subMap != null) {
            return subMap.get(valueKey);
        } else {
            return null;
        }
    }

    public Object getNested(String key) {
        // By default, we will assume we need to filter by id in case of finding lists
        return getNested(key, "id");
    }

    public Object putNested(String key, String listId, Object value, boolean parents) {
        int idx = key.lastIndexOf(".");
        if (idx < 0) {
            return put(key, value, false);
        }
        String mapKey = key.substring(0, idx);
        String valueKey = key.substring(idx + 1);
        Map<String, Object> subMap = getNestedMap(mapKey, listId, objectMap, jsonObjectMapper, true, parents);
        if (subMap != null) {
            return subMap.put(valueKey, value);
        } else {
            throw new IllegalArgumentException("Key '" + key + "' not found!");
        }
    }

    public Object putNested(String key, Object value, boolean parents) {
        return putNested(key, "id", value, parents);
    }

    public ObjectMap getNestedMap(String key) {
        Map<String, Object> subMap = getNestedMap(key, "id", objectMap, jsonObjectMapper, false, false);
        return subMap == null ? null : (subMap instanceof ObjectMap ? ((ObjectMap) subMap) : new ObjectMap(subMap));
    }

    private static Map<String, Object> getNestedMap(String key, String listId, Map<String, Object> map, ObjectMapper jsonObjectMapper,
                                                    boolean convert, boolean parents) {
        if (map == null) {
            return map;
        }
        int idx = key.indexOf(".");
        String firstKey;
        String nextKey;
        if (idx < 0) {
            firstKey = key;
            nextKey = null;
        } else {
            firstKey = key.substring(0, idx);
            nextKey = key.substring(idx + 1);
        }

        Object value = map.get(firstKey);
        Map<String, Object> subMap = null;

        if (value instanceof Map) {
            subMap = (Map) value;
        } else if (value instanceof Collection && nextKey != null) {
            // Recalculate keys. Next key is not supposed to be an object key but the value of the key 'idList'
            idx = nextKey.indexOf(".");
            if (idx < 0) {
                firstKey = nextKey;
                nextKey = null;
            } else {
                firstKey = nextKey.substring(0, idx);
                nextKey = nextKey.substring(idx + 1);
            }

            for (Object o : ((Collection) value)) {
                if (o instanceof Map) {
                    String tmpValue = String.valueOf(((Map) o).get(listId));
                    if (StringUtils.isNotEmpty(tmpValue) && tmpValue.equals(firstKey)) {
                        subMap = (Map) o;
                        break;
                    }
                } else {
                    break;
                }
            }
        } else if (value == null
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value.getClass().isArray()
                || value.getClass().isEnum()) {
            // Expected a Map or an Object.
            subMap = null;
        } else {
            subMap = jsonObjectMapper.convertValue(value, Map.class);
            if (convert) {
                map.put(firstKey, subMap);
            }
        }
        if (parents && subMap == null) {
            subMap = new HashMap<>();
            map.put(firstKey, subMap);
        }

        if (nextKey == null) {
            return subMap;
        } else {
            return getNestedMap(nextKey, listId, subMap, jsonObjectMapper, convert, parents);
        }

    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return objectMap.putIfAbsent(key, value);
    }

    @Override
    public Object remove(Object key) {
        return objectMap.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return objectMap.remove(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        objectMap.putAll(m);
    }

    @Override
    public void clear() {
        objectMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return objectMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return objectMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return objectMap.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ObjectMap)) {
            return false;
        }
        ObjectMap objectMap1 = (ObjectMap) o;
        return Objects.equals(objectMap, objectMap1.objectMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectMap);
    }
}
