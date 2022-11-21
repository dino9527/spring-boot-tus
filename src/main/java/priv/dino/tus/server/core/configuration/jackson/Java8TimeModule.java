package priv.dino.tus.server.core.configuration.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * java 8 时间默认序列化
 *
 * @author dino
 * @date 2021/11/22 15:32
 */
public class Java8TimeModule extends SimpleModule {

    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME;
    static {
        ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(ISO_LOCAL_TIME)
                .toFormatter(Locale.CHINA);
    }


    public Java8TimeModule() {
        super(PackageVersion.VERSION);
        // ======================= 时间序列化规则 ===============================
        // yyyy-MM-dd HH:mm:ss
        this.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(ISO_LOCAL_DATE_TIME));
        // yyyy-MM-dd
        this.addSerializer(LocalDate.class, new LocalDateSerializer(ISO_LOCAL_DATE));
        // HH:mm:ss
        this.addSerializer(LocalTime.class, new LocalTimeSerializer(ISO_LOCAL_TIME));
        // Instant 类型序列化
        this.addSerializer(Instant.class, InstantSerializer.INSTANCE);

        // ======================= 时间反序列化规则 ==============================
        // yyyy-MM-dd HH:mm:ss
        this.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(ISO_LOCAL_DATE_TIME));
        // yyyy-MM-dd
        this.addDeserializer(LocalDate.class, new LocalDateDeserializer(ISO_LOCAL_DATE));
        // HH:mm:ss
        this.addDeserializer(LocalTime.class, new LocalTimeDeserializer(ISO_LOCAL_TIME));
        // Instant 反序列化
        this.addDeserializer(Instant.class, InstantDeserializer.INSTANT);
    }
}
