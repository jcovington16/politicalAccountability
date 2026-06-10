package com.publicrecord.api;

import org.junit.jupiter.api.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicRateLimitFilterTest {

    @Test
    void allowsRequestsUnderLimit() {
        PublicRateLimitFilter filter = new PublicRateLimitFilter(true, 2);
        ContainerRequestContext context = contextFor("search", "203.0.113.10");

        filter.filter(context);
        filter.filter(context);

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any(Response.class));
    }

    @Test
    void rejectsRequestsOverLimit() {
        PublicRateLimitFilter filter = new PublicRateLimitFilter(true, 1);
        ContainerRequestContext context = contextFor("search", "203.0.113.11");

        filter.filter(context);
        filter.filter(context);

        verify(context).abortWith(org.mockito.ArgumentMatchers.argThat(response -> response.getStatus() == 429));
    }

    @Test
    void ignoresNonSearchPaths() {
        PublicRateLimitFilter filter = new PublicRateLimitFilter(true, 1);
        ContainerRequestContext context = contextFor("politicians/abc", "203.0.113.12");

        filter.filter(context);
        filter.filter(context);

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any(Response.class));
    }

    private static ContainerRequestContext contextFor(String path, String forwardedFor) {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);

        ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getHeaderString("X-Forwarded-For")).thenReturn(forwardedFor);
        return context;
    }
}
