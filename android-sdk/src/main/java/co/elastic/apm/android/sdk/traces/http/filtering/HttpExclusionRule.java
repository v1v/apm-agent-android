package co.elastic.apm.android.sdk.traces.http.filtering;

import androidx.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URL;

import co.elastic.apm.android.sdk.traces.http.data.HttpRequest;
import co.elastic.apm.android.sdk.traces.otel.processor.ElasticSpanProcessor;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

abstract public class HttpExclusionRule implements ElasticSpanProcessor.ExclusionRule {

    @Override
    public boolean exclude(ReadableSpan span) {
        String httpMethod = span.getAttribute(SemanticAttributes.HTTP_METHOD);
        if (httpMethod == null) {
            // Not an http-related Span.
            return false;
        }

        return exclude(new HttpRequest(httpMethod, getUrl(span)));
    }

    private URL getUrl(ReadableSpan span) {
        String urlString = span.getAttribute(SemanticAttributes.HTTP_URL);
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract boolean exclude(@NonNull HttpRequest request);
}