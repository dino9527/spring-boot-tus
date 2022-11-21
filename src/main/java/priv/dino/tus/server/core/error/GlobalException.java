package priv.dino.tus.server.core.error;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
/**
 * 全局异常类
 *
 * @author dino
 * @date 2021/11/11 15:58
 */
public class GlobalException extends ResponseStatusException {

    public GlobalException(HttpStatus status) {
        super(status);
    }

    public GlobalException(HttpStatus status, String message) {
        super(status, message);
    }

    public GlobalException(HttpStatus status, String message, Throwable e) {
        super(status, message, e);
    }

    public GlobalException(int rawStatusCode, String message, Throwable e) {
        super(rawStatusCode, message, e);
    }
}
