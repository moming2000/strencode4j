package com.free.strencode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * @author
 */
public class JsonUtil {

    public static ObjectMapper createDefaultMapper(){
        val obj = createObjectMapper();
        return obj;
    }

    public static ObjectMapper createObjectMapper(){
       val obj =  new ObjectMapper()
                .enable(MapperFeature.USE_ANNOTATIONS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES,true)
               //允许不带引号的字段
               .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
               //允许单引号
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .setTimeZone(TimeZone.getTimeZone("GMT+8"))
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
                obj.setSerializationInclusion(JsonInclude.Include.NON_NULL);
       return obj;
    }


    @Getter @Setter
    private static ObjectMapper objectMapper = createDefaultMapper();;

    public static String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("序列化失败，对象：" + object, e);
        }
    }

    public static <T> T deserialize(String str, Type type) {
        try {
            return objectMapper.readValue(str, new TypeReference<T>(){
                @Override
                public Type getType() {
                    return type;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("反序列化失败，类型：" + type + "，JSON：" + str, e);
        }
    }

}
