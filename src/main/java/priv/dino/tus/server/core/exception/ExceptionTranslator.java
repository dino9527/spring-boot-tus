package priv.dino.tus.server.core.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import priv.dino.tus.server.core.payload.ErrorResponse;

/**
 * 异常通知
 *
 * @author dino
 * @date 2021/10/29 12:26
 */
@RestControllerAdvice
public class ExceptionTranslator {

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity handleDuplicateKeyException(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("A File with the same text already exists"));
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity handleTweetNotFoundException(FileNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
}
