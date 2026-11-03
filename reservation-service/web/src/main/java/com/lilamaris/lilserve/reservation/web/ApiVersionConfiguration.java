package com.lilamaris.lilserve.reservation.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Turns on API versioning for this web layer, and reads the version from the path segment that
 * carries it: the {@code v1} of {@code /api/v1/rooms} is version {@code 1}. Request mappings
 * declare that version, so this layer answers {@code 1} only.
 *
 * <p>A request whose version no mapping declares, and a request that has no version at the version
 * position of a matching path, is answered with 400 before a controller runs, so no use case is
 * reached. A path with no version position matches no mapping, and is answered with 404.
 */
@Configuration(proxyBeanMethods = false)
public class ApiVersionConfiguration implements WebMvcConfigurer {

    /**
     * The API version every request mapping declares, the version the segment {@code v1} carries.
     */
    public static final String CURRENT = "1";

    private static final int VERSION_SEGMENT_INDEX = 1;
    private static final String VERSION_SEGMENT = "v\\d+";

    /**
     * Resolves the version from the second path segment, the position the versioned paths put it
     * at.
     *
     * @param configurer the API versioning config of this web layer
     */
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.usePathSegment(VERSION_SEGMENT_INDEX, ApiVersionConfiguration::isVersioned);
    }

    /**
     * Decides whether a path carries a version, so that a path with no version at the version
     * position, such as {@code /api/rooms}, is not read as the version {@code rooms}.
     *
     * @param path the request path within the application
     * @return true when the version position holds {@code v} followed by digits
     */
    private static boolean isVersioned(RequestPath path) {
        int segment = 0;
        for (PathContainer.Element element : path.pathWithinApplication().elements()) {
            if (element instanceof PathContainer.PathSegment pathSegment) {
                if (segment == VERSION_SEGMENT_INDEX) {
                    return pathSegment.value().matches(VERSION_SEGMENT);
                }
                segment++;
            }
        }
        return false;
    }
}
