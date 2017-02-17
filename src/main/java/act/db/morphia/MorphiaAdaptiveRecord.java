package act.db.morphia;

import act.Act;
import act.db.AdaptiveRecord;
import act.db.morphia.util.KVStoreConverter;
import act.db.morphia.util.ValueObjectConverter;
import act.inject.param.NoBind;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.utils.IterHelper;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.inject.BeanSpec;
import org.osgl.util.C;
import org.osgl.util.KVStore;
import org.osgl.util.S;
import org.osgl.util.ValueObject;
import relocated.morphia.org.apache.commons.collections.DefaultMapEntry;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Implement {@link AdaptiveRecord} in Morphia
 */
public abstract class MorphiaAdaptiveRecord<MODEL_TYPE extends MorphiaAdaptiveRecord> extends MorphiaModel<MODEL_TYPE> implements AdaptiveRecord<ObjectId, MODEL_TYPE> {

    @Transient
    @NoBind
    private KVStore kv = new KVStore();

    @Transient
    private transient volatile AdaptiveRecord.MetaInfo metaInfo;

    // --- implement KV
    @Override
    public MODEL_TYPE putValue(String key, Object val) {
        $.Func2 setter = metaInfo().fieldSetters.get(key);
        if (null != setter) {
            setter.apply(this, val);
        } else {
            kv.putValue(key, val);
        }
        return _me();
    }

    @Override
    public MODEL_TYPE mergeValue(String key, Object val) {
        $.Func2 merger = metaInfo().fieldMergers.get(key);
        if (null != merger) {
            merger.apply(this, val);
        } else {
            Object v0 = kv.getValue(key);
            kv.putValue(key, AdaptiveRecord.MetaInfo.merge(v0, val));
        }
        return null;
    }

    @Override
    public <T> T getValue(String key) {
        $.Function getter = metaInfo().fieldGetters.get(key);
        if (null != getter) {
            return (T) getter.apply(this);
        }
        return kv.getValue(key);
    }

    @Override
    public MODEL_TYPE putValues(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry: map.entrySet()) {
            putValue(entry.getKey(), entry.getValue());
        }
        return _me();
    }

    @Override
    public MODEL_TYPE mergeValues(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry: map.entrySet()) {
            mergeValue(entry.getKey(), entry.getValue());
        }
        return _me();
    }

    @Override
    public boolean containsKey(String key) {
        return kv.containsKey(key) || metaInfo().fieldTypes.containsKey(key);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = kv.toMap();
        for (Map.Entry<String, $.Function> entry : metaInfo().fieldGetters.entrySet()) {
            map.put(entry.getKey(), entry.getValue().apply(this));
        }
        return map;
    }

    @Override
    public int size() {
        return kv.size() + fieldsSize();
    }

    @Override
    public Set<String> keySet() {
        if (!hasFields()) {
            return kv.keySet();
        }
        Set<String> set = new HashSet<String>(metaInfo().fieldTypes.keySet());
        set.addAll(kv.keySet());
        return set;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return entrySet(null);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet(Osgl.Function<BeanSpec, Boolean> function) {
        if (!hasFields()) {
            return kv.toMap().entrySet();
        }
        Set<Map.Entry<String, Object>> set = new HashSet<Map.Entry<String, Object>>(kv.toMap().entrySet());
        AdaptiveRecord.MetaInfo metaInfo = metaInfo();
        boolean filter = null != function;
        for (Map.Entry<String, $.Function> entry: metaInfo.fieldGetters.entrySet()) {
            String fieldName = entry.getKey();
            if ("kv".equals(fieldName)) {
                continue;
            }
            if (filter) {
                BeanSpec field = metaInfo.fieldSpecs.get(fieldName);
                if (!function.apply(field)) {
                    continue;
                }
            }
            $.Function getter = entry.getValue();
            set.add(new DefaultMapEntry(fieldName, getter.apply(this)));
        }
        return set;
    }

    public Map<String, Object> asMap() {
        final AdaptiveRecord ar = this;
        // TODO: should we check the field value on size, remove, containsXxx etc methods?
        return new Map<String, Object>() {
            @Override
            public int size() {
                return ar.size();
            }

            @Override
            public boolean isEmpty() {
                return ar.size() == 0;
            }

            @Override
            public boolean containsKey(Object key) {
                return kv.containsKey(key) || metaInfo().fieldTypes.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return kv.containsValue(value);
            }

            @Override
            public Object get(Object key) {
                $.Function getter = metaInfo().fieldGetters.get(key);
                return null != getter ? getter.apply(this) : kv.getValue((String)key);
            }

            @Override
            public Object put(String key, Object value) {
                $.Func2 setter = metaInfo().fieldSetters.get(key);
                if (null != setter) {
                    Object o = get(key);
                    setter.apply(this, value);
                    return o;
                }
                return kv.putValue(key, value);
            }

            @Override
            public Object remove(Object key) {
                $.Function getter = metaInfo().fieldGetters.get(key);
                if (null != getter) {
                    return null;
                } else {
                    return kv.remove(key).value();
                }
            }

            @Override
            public void putAll(Map<? extends String, ?> m) {
                putValues((Map)m);
            }

            @Override
            public void clear() {
                kv.clear();
                // TODO: should we clear field values?
            }

            @Override
            public Set<String> keySet() {
                return ar.keySet();
            }

            @Override
            public Collection<Object> values() {
                List<Object> list = new ArrayList<Object>();
                for (ValueObject vo : kv.values()) {
                    list.add(vo.value());
                }
                for ($.Function getter : metaInfo().fieldGetters.values()) {
                    list.add(getter.apply(this));
                }
                return list;
            }

            @Override
            public Set<Entry<String, Object>> entrySet() {
                return ar.entrySet();
            }
        };
    }

    private int fieldsSize() {
        return metaInfo().fieldSpecs.size();
    }

    private boolean hasFields() {
        return !metaInfo().fieldSpecs.isEmpty();
    }

    @Override
    @java.beans.Transient
    public AdaptiveRecord.MetaInfo metaInfo() {
        if (null == metaInfo) {
            synchronized (this) {
                if (null == metaInfo) {
                    AdaptiveRecord.MetaInfo.Repository r = Act.appServicePluginManager().get(AdaptiveRecord.MetaInfo.Repository.class);
                    metaInfo = r.get(getClass(), new $.Transformer<Class<? extends AdaptiveRecord>, AdaptiveRecord.MetaInfo>() {
                        @Override
                        public AdaptiveRecord.MetaInfo transform(Class<? extends AdaptiveRecord> aClass) {
                            return new AdaptiveRecord.MetaInfo(aClass, Transient.class);
                        }
                    });
                }
            }
        }
        return metaInfo;
    }

    public static class AdaptiveRecordMappingInterceptor extends AbstractEntityInterceptor {

        @Override
        public void prePersist(Object ent, DBObject dbObj, Mapper mapper) {
            if (null == ent) {
                return;
            }
            Class<?> c = ent.getClass();
            if (MorphiaAdaptiveRecord.class.isAssignableFrom(c)) {
                MorphiaAdaptiveRecord ar = $.cast(ent);
                KVStore kv = ar.kv;
                for (Map.Entry<String, ValueObject> entry : kv.entrySet()) {
                    dbObj.put(entry.getKey(), entry.getValue().value());
                }
            }
        }

        private static final Set<String> BUILT_IN_PROPS = C.setOf("_id,className,_created,_modified,v".split(","));

        @Override
        public void postLoad(Object ent, DBObject dbObj, Mapper mapper) {
            Class<?> c = ent.getClass();
            if (MorphiaAdaptiveRecord.class.isAssignableFrom(c)) {
                MorphiaAdaptiveRecord ar = $.cast(ent);
                final KVStore kv = ar.kv;
                final AdaptiveRecord.MetaInfo metaInfo = ar.metaInfo();
                new IterHelper<>().loopMap(dbObj, new IterHelper.MapIterCallback<Object, Object>() {
                    @Override
                    public void eval(final Object k, final Object val) {
                        final String key = S.string(k);
                        if (BUILT_IN_PROPS.contains(key) || metaInfo.fieldTypes.containsKey(key)) {
                            return;
                        }
                        if (!metaInfo.fieldTypes.containsKey(key)) {
                            kv.putValue(key, JSONObject.toJSON(val));
                        }
                    }
                });
            }
        }
    }
}
