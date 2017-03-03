package act.db.morphia;

import act.db.AdaptiveRecord;
import act.inject.param.NoBind;
import com.alibaba.fastjson.JSONObject;
import org.mongodb.morphia.annotations.Transient;
import org.osgl.Osgl;
import org.osgl.inject.BeanSpec;

import java.util.Map;
import java.util.Set;

/**
 * Implement {@link AdaptiveRecord} in Morphia
 */
public abstract class MorphiaAdaptiveRecordWithLongId<MODEL_TYPE extends MorphiaAdaptiveRecordWithLongId> extends MorphiaModelWithLongId<MODEL_TYPE> implements AdaptiveRecord<Long, MODEL_TYPE> {

    @Transient
    @NoBind
    private JSONObject kv = new JSONObject();

    @Transient
    private transient volatile AdaptiveRecord.MetaInfo metaInfo;

    @Override
    public Map<String, Object> internalMap() {
        return kv;
    }

    // --- implement KV
    @Override
    public MODEL_TYPE putValue(String key, Object val) {
        Util.putValue(this, key, val);
        return _me();
    }

    @Override
    public MODEL_TYPE mergeValue(String key, Object val) {
        Util.mergeValue(this, key, val);
        return _me();
    }

    @Override
    public <T> T getValue(String key) {
        return Util.getValue(this, key);
    }

    @Override
    public MODEL_TYPE putValues(Map<String, Object> map) {
        Util.putValues(this, map);
        return _me();
    }

    @Override
    public MODEL_TYPE mergeValues(Map<String, Object> map) {
        Util.mergeValues(this, map);
        return _me();
    }

    @Override
    public boolean containsKey(String key) {
        return Util.containsKey(this, key);
    }

    @Override
    public Map<String, Object> toMap() {
        return Util.toMap(this);
    }

    @Override
    public int size() {
        return Util.size(this);
    }

    @Override
    public Set<String> keySet() {
        return Util.keySet(this);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return entrySet(null);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet(Osgl.Function<BeanSpec, Boolean> function) {
        return Util.entrySet(this, function);
    }

    public Map<String, Object> asMap() {
        return Util.asMap(this);
    }

    @Override
    @java.beans.Transient
    public AdaptiveRecord.MetaInfo metaInfo() {
        if (null == metaInfo) {
            synchronized (this) {
                if (null == metaInfo) {
                    metaInfo = Util.generateMetaInfo(this);
                }
            }
        }
        return metaInfo;
    }

}