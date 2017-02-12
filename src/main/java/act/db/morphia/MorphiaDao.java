package act.db.morphia;

import act.inject.param.NoBind;
import act.util.General;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;

/**
 * The morphia dao base implementation use {@link org.bson.types.ObjectId} as the ID type
 */
@General
@NoBind
public class MorphiaDao<MODEL_TYPE>
        extends MorphiaDaoBase<ObjectId, MODEL_TYPE> {

    public MorphiaDao(Class<MODEL_TYPE> modelType, Datastore ds) {
        super(ObjectId.class, modelType, ds);
    }

    public MorphiaDao(Class<MODEL_TYPE> modelType) {
        super(ObjectId.class, modelType);
    }

    public MorphiaDao() {
    }

    public MODEL_TYPE findById(String id) {
        return findById(new ObjectId(id));
    }

    public void deleteById(String id) {
        super.deleteById(new ObjectId(id));
    }

}
