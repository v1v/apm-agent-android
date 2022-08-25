package co.elastic.apm.android.sdk.traces.common.attributes;

import android.os.Build;

import co.elastic.apm.android.sdk.attributes.AttributesBuilderVisitor;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class DeviceInfoVisitor implements AttributesBuilderVisitor {

    @Override
    public void visit(AttributesBuilder builder) {
        builder.put(ResourceAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                .put(ResourceAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER);
    }
}
