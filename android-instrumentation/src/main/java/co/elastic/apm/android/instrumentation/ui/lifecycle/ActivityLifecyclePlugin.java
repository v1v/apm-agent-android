package co.elastic.apm.android.instrumentation.ui.lifecycle;

import android.app.Activity;
import android.os.Bundle;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.build.AndroidDescriptor;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;

public class ActivityLifecyclePlugin implements Plugin {
    private final AndroidDescriptor androidDescriptor;

    public ActivityLifecyclePlugin(AndroidDescriptor androidDescriptor) {
        this.androidDescriptor = androidDescriptor;
    }

    @Override
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                        TypeDescription typeDescription,
                                        ClassFileLocator classFileLocator) {
        return builder.visit(Advice.to(ActivityLifecycleAdvice.class).on(ElementMatchers.named("onCreate").and(ElementMatchers.takesArguments(Bundle.class))));
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean matches(TypeDescription target) {
        if (androidDescriptor.getTypeScope(target) == AndroidDescriptor.TypeScope.EXTERNAL) {
            return false;
        }
        return !target.getSimpleName().startsWith("Hilt_") && target.isAssignableTo(Activity.class);
    }
}
