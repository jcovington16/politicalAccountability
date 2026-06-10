package com.publicrecord.api;

import org.junit.jupiter.api.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuthorizationFilterTest {

    @Test
    void allowsPublicPathsWithoutToken() {
        AdminAuthorizationFilter filter = new AdminAuthorizationFilter("secret");
        ContainerRequestContext context = contextFor("bills/search", null);

        filter.filter(context);

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any(Response.class));
    }

    @Test
    void rejectsProtectedPathsWithoutToken() {
        AdminAuthorizationFilter filter = new AdminAuthorizationFilter("secret");
        ContainerRequestContext context = contextFor("imports", null);

        filter.filter(context);

        verify(context).abortWith(org.mockito.ArgumentMatchers.argThat(response ->
                response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
        ));
    }

    @Test
    void allowsProtectedPathsWithToken() {
        AdminAuthorizationFilter filter = new AdminAuthorizationFilter("secret");
        ContainerRequestContext context = contextFor("audit-log", "secret");

        filter.filter(context);

        verify(context, never()).abortWith(org.mockito.ArgumentMatchers.any(Response.class));
    }

    @Test
    void rejectsReviewPathsWithoutToken() {
        AdminAuthorizationFilter filter = new AdminAuthorizationFilter("secret");
        ContainerRequestContext context = contextFor("review/queue", null);

        filter.filter(context);

        verify(context).abortWith(org.mockito.ArgumentMatchers.argThat(response ->
                response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
        ));
    }

    @Test
    void rejectsClassificationPathsWithoutToken() {
        AdminAuthorizationFilter filter = new AdminAuthorizationFilter("secret");
        ContainerRequestContext context = contextFor("classification/civic", null);

        filter.filter(context);

        verify(context).abortWith(org.mockito.ArgumentMatchers.argThat(response ->
                response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
        ));
    }

    private static ContainerRequestContext contextFor(String path, String token) {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);

        ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getHeaderString(AdminAuthorizationFilter.ADMIN_TOKEN_HEADER)).thenReturn(token);
        return context;
    }
}
