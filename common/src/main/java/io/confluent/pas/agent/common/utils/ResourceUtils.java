package io.confluent.pas.agent.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for reading resources from the classpath.
 * Provides methods to read resources as streams, strings, or byte arrays.
 */
@Slf4j
public class ResourceUtils {

    /**
     * Reads a resource from the classpath as an InputStream.
     *
     * @param resourcePath the path to the resource in the classpath
     * @return InputStream of the resource, or null if the resource is not found
     */
    public static InputStream readResource(String resourcePath) {
        try {
            return ResourceUtils.class.getClassLoader().getResourceAsStream(resourcePath);
        } catch (Exception e) {
            log.error("Error reading resource: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * Reads a resource from the classpath and returns its content as a String.
     *
     * @param resourcePath the path to the resource in the classpath
     * @return String content of the resource
     */
    public static String readResourceAsString(String resourcePath) {
        try (InputStream is = readResource(resourcePath)) {
            try {
                if (is == null) {
                    log.error("Resource not found: {}", resourcePath);
                    return null;
                }
                return new String(readResourceAsBytes(is), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Error reading resource as string: {}", resourcePath, e);
            }
        } catch (IOException e) {
            log.error("Error closing InputStream for resource: {}", resourcePath, e);
        }

        return null;
    }

    /**
     * Reads all bytes from an input stream into a byte array.
     *
     * @param is the input stream to read from
     * @return byte array containing all the bytes from the input stream
     */
    public static byte[] readResourceAsBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }


}
