package priv.dino.tus.server.core.exception;

/**
 * 自定义异常
 *
 * @author dino
 * @date 2021/10/29 12:23
 */
public class FileNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;


    public FileNotFoundException(Long fileId) {
        super("File not found with id " + fileId);
    }
}
