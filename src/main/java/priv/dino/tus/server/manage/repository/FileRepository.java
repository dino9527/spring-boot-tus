package priv.dino.tus.server.manage.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import priv.dino.tus.server.manage.domain.File;

/**
 * 文件 持久层接口
 *
 * @author dino
 * @date 2021/10/29 10:25
 */
public interface FileRepository extends R2dbcRepository<File,Long> {

}
