package priv.dino.tus.server.core.util;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Operations to simplify common {@link java.security.MessageDigest} tasks.
 * This class is immutable and thread-safe.
 * However the MessageDigest instances it creates generally won't be.
 *
 * @author dino
 * @date 2021/11/30 15:49
 */
public class DigestUtils {

    private DigestUtils() {}

    private static final String MD5 = "md5";

    public static String digestAsHex(String algorithm, String value) {
        if (MD5.equals(algorithm)) {
            return md5DigestAsHex(value.getBytes(UTF_8));
        }
        return sha1DigestAsHex(value.getBytes(UTF_8));
    }

    public static String md5DigestAsHex(byte[] bytes) {
        return org.springframework.util.DigestUtils.md5DigestAsHex(bytes);
    }

    public static String sha1DigestAsHex(byte[] bytes) {
        return Base64Utils.encodeToString(sha1(bytes));
    }

    public static byte[] sha1(byte[] data) {
        return getSha1Digest().digest(data);
    }

    public static MessageDigest getSha1Digest() {
        return getDigest("SHA-1");
    }

    public static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException var2) {
            throw new IllegalArgumentException(var2);
        }
    }

    public static String getSha1(String str){
        if (null == str || 0 == str.length()){
            return null;
        }
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};
        byte[] md = sha1(str.getBytes(UTF_8));
        int j = md.length;
        char[] buf = new char[j * 2];
        int k = 0;
        for (byte byte0 : md) {
            buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
            buf[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(buf);
    }

    public static String readBase64Content(DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        DataBufferUtils.release(buffer);
        return Base64Utils.encodeToString(sha1(bytes));
    }

    public static Mono<String> readBase64Content(Flux<DataBuffer> parts) {
        return parts.flatMap(buffer -> {
            String encode = readBase64Content(buffer);
            return Mono.just(encode);
        }).last();
    }
}
