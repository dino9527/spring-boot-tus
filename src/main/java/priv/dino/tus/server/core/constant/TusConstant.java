package priv.dino.tus.server.core.constant;

/**
 * TUS HEADER 环境变量
 *
 * @author dino
 * @date 2021/10/29 14:03
 */
public interface TusConstant {

    String TUS_RESUMABLE_HEADER = "Tus-Resumable";
    String TUS_RESUMABLE_VALUE = "1.0.0";

    String TUS_VERSION_HEADER = "Tus-Version";
    String TUS_VERSION_VALUE = "1.0.0,0.2.2,0.2.1";

    String TUS_EXTENTION_HEADER = "Tus-Extension";
    String TUS_EXTENTION_VALUE = "creation,expiration,termination,concatenation";

    String TUS_CHECKSUM_ALGORITHM_HEADER = "Tus-Checksum-Algorithm";

    String TUS_MAX_SIZE_HEADER = "Tus-Max-Size";

    String UPLOAD_OFFSET_HEADER = "Upload-Offset";

    String UPLOAD_LENGTH_HEADER = "Upload-Length";

    String LOCATION_HEADER = "Location";

    String UPLOAD_EXPIRES_HEADER = "Upload-Expires";

    String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    String ACCESS_CONTROL_ALLOW_ORIGIN_VALUE = "*";

    String ACCESS_CONTROL_ALLOW_METHIDS_HEADER = "Access-Control-Allow-Methods";
    String ACCESS_CONTROL_ALLOW_METHIDS_VALUE = "GET,PUT,PATCH,POST,DELETE";

    String ACCESS_CONTROL_EXPOSE_HEADER = "Access-Control-Expose-Headers";
    String ACCESS_CONTROL_EXPOSE_OPTIONS_VALUE = "Tus-Resumable, Tus-Version, Tus-Max-Size, Tus-Extension";
    String ACCESS_CONTROL_EXPOSE_POST_VALUE = "Location, Tus-Resumable";
    String ACCESS_CONTROL_EXPOSE_HEAD_VALUE = "Upload-Offset, Upload-Length, Tus-Resumable";
    String ACCESS_CONTROL_EXPOSE_PATCH_VALUE = "Upload-Offset, Tus-Resumable";

    String CACHE_CONTROL_HEADER = "Cache-Control";
    String CACHE_CONTROL_VALUE = "no-store";

    String URL_PREFIX = "/upload";

    String CONTENT_TYPE = "application/offset+octet-stream";
}
