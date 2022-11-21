package priv.dino.tus.server.core.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;

/**
 * Request Handling utility functions.
 *
 * @author dino
 * @date 2021/11/15 14:20
 */
@Slf4j
public final class Utils {

    private Utils() {
    }

    public static Map<String, String> parseMetadata(String metadata) {
        HashMap<String, String> map = new HashMap<>();
        if (metadata == null) {
            return map;
        }
        String[] pairs = metadata.split(",");
        for (String pair : pairs) {
            String[] element = pair.trim().split(" ");
            if (element.length != 2) {
                log.warn("Ignoring metadata element: {}.", pair);
                continue;
            }
            String key = element[0];
            byte[] value;
            try {
                value = Base64.getUrlDecoder().decode(element[1]);
            } catch (IllegalArgumentException iae) {
                log.warn("Invalid encoding of metadata element: {}.", pair);
                continue;
            }
            map.put(key, new String(value));
        }
        return map;
    }

    public static Optional<Tuple2<String,String>> getHeaderAsChecksumInfo(final String key, ServerRequest ctx) {
        Optional<String> headerAsString = getHeaderAsString(key, ctx);
        if (headerAsString.isPresent()) {
            String[] pair = headerAsString.get().split(" ");
            if (pair.length == 2) {
                return Optional.of(Tuples.of(pair[0], pair[1]));
            }
            return Optional.empty();
        }
        return Optional.empty();

    }

    public static Optional<Long> getHeaderAsLong(final String key, final ServerRequest ctx) {
        String value = getHeader(key, ctx);
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }

    public static Optional<String> getHeaderAsString(final String key, final ServerRequest ctx) {
        String value = getHeader(key, ctx);
        return Optional.ofNullable(value);
    }

    public static String getHeader(final String key, final ServerRequest ctx) {
        return ctx.headers().firstHeader(key);
    }


    public static Long[] extractPartialUploadIds(String[] fullParts) {
        return Arrays.stream(fullParts).map(Utils::getLastBitFromUrl).toArray(Long[]::new);
    }

    private static Long getLastBitFromUrl(final String url) {
        return Long.valueOf(url.replaceFirst(".*/([^/?]+).*", "$1"));
    }

}
