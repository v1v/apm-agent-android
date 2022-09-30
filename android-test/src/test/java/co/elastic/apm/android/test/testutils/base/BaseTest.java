package co.elastic.apm.android.test.testutils.base;

import static org.junit.Assert.assertEquals;

import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import co.elastic.apm.android.test.testutils.spans.SpanExporterProvider;
import io.opentelemetry.sdk.trace.data.SpanData;

public class BaseTest {

    protected List<SpanData> getRecordedSpans(int amountExpected) {
        List<SpanData> spans = getCapturedSpansOrderedByCreation(getSpanExporter(), amountExpected);
        assertEquals(amountExpected, spans.size());

        return spans;
    }

    @SuppressWarnings("unchecked")
    private List<SpanData> getCapturedSpansOrderedByCreation(DummySpanExporter spanExporter, int amountExpected) {
        List<SpanData> spans = new ArrayList<>();
        for (List<SpanData> list : spanExporter.getCapturedSpans()) {
            if (list.size() > 1) {
                // Since we're using SimpleSpanProcessor, each call to SpanExporter.export must contain
                // only one span.
                throw new IllegalStateException();
            }
            spans.add(list.get(0));
        }

        spans.sort(Comparator.comparing(SpanData::getStartEpochNanos));
        return spans;
    }

    protected DummySpanExporter getSpanExporter() {
        SpanExporterProvider spanExporterProvider = (SpanExporterProvider) RuntimeEnvironment.getApplication();
        return spanExporterProvider.getSpanExporter();
    }

    protected String getClassSpanName(Class<?> theClass, String suffix) {
        return theClass.getName() + suffix;
    }

    protected SpanData getRecordedSpan() {
        return getRecordedSpans(1).get(0);
    }
}
