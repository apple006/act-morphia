package act.db.morphia;

import act.Act;
import act.ActComponent;
import act.app.App;
import act.app.event.AppEventId;
import act.util.SubTypeFinder;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.converters.TypeConverter;

public class TypeConverterFinder extends SubTypeFinder<TypeConverter> {

    private static final String SYS_CONVERTER_PKG = Morphia.class.getPackage().getName();

    public TypeConverterFinder() {
        super(TypeConverter.class);
    }

    @Override
    protected void found(final Class<? extends TypeConverter> target, final App app) {
        if (target.getName().startsWith(SYS_CONVERTER_PKG)) {
            return;
        }
        app.jobManager().on(AppEventId.DEPENDENCY_INJECTOR_PROVISIONED, new Runnable() {
            @Override
            public void run() {
                Act.LOGGER.error("Adding converter: %s", target.getName());
                MorphiaService.mapper().getConverters().addConverter(app.getInstance(target));
            }
        });
    }

}
