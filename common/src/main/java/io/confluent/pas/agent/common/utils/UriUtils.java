package io.confluent.pas.agent.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class UriUtils {

    private static final String URI_TEMPLATE_REGEX = "\\{([^}]+)}";
    private static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile(URI_TEMPLATE_REGEX);

    /**
     * Check if a string is a URI template.
     *
     * @param str The string to check
     * @return true if the string is a URI template, false otherwise
     */
    public static boolean isTemplate(String str) {
        return URI_TEMPLATE_PATTERN.matcher(str).find();
    }

    /**
     * Check if the given URL pattern matches the URL parts.
     *
     * @param urlPattern the URL pattern
     * @param urlParts   the URL parts
     * @return true if the pattern matches the URL parts
     */
    public static boolean isUrlPatternMatch(String urlPattern, List<String> urlParts) {
        final List<String> patternParts = Stream.of(urlPattern.split("/"))
                .filter(StringUtils::isNotEmpty)
                .toList();

        if (urlParts.size() != patternParts.size()) {
            return false;
        }

        return Stream.iterate(0, i -> i + 1)
                .limit(urlParts.size())
                .allMatch(i -> isPartMatch(patternParts.get(i), urlParts.get(i)));
    }

    /**
     * Check if a pattern part matches a URL part.
     *
     * @param patternPart the pattern part
     * @param urlPart     the URL part
     * @return true if the pattern part matches the URL part
     */
    public static boolean isPartMatch(String patternPart, String urlPart) {
        return (patternPart.startsWith("{") && patternPart.endsWith("}")) ||
                patternPart.equals(urlPart);
    }

    /**
     * Extract the URL parts from the given path.
     *
     * @param path the request path
     * @return the list of URL parts
     */
    public static List<String> extractUrlParts(String path) {
        return Stream.of(path.split("/"))
                .filter(StringUtils::isNotEmpty)
                .filter(part -> !part.equalsIgnoreCase("agents"))
                .toList();
    }
}
