package priv.dino.tus.server.core.entitycallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.*;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import priv.dino.tus.server.core.util.SnowflakeIdWorker;
import priv.dino.tus.server.manage.domain.File;
import reactor.core.publisher.Mono;

/**
 * 文件 实体回调
 *
 * @author dino
 * @date 2021/11/18 12:21
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileCallback implements
        BeforeConvertCallback<File>,
        AfterConvertCallback<File>,
        BeforeSaveCallback<File>,
        AfterSaveCallback<File> {

    @Override
    public Publisher<File> onBeforeConvert(File entity, SqlIdentifier table) {
        log.info("[onBeforeConvert]::: entity :{}, table: {}", entity, table);
        if (entity.getId() == null) {
            entity.setId(new SnowflakeIdWorker(0, 0).nextId());
            return Mono.just(entity);
        }
        return Mono.just(entity);
    }

    @Override
    public Publisher<File> onBeforeSave(File entity, OutboundRow row, SqlIdentifier table) {
        log.info("[onBeforeSave]::: entity :{}, outboundRow :{}, table: {}", entity, row, table);
        return Mono.just(entity);
    }

    @Override
    public Publisher<File> onAfterSave(File entity, OutboundRow outboundRow, SqlIdentifier table) {
        log.info("[onAfterSave]::: entity :{}, outboundRow :{}, table: {}", entity, outboundRow, table);
        return Mono.just(entity);
    }

    @Override
    public Publisher<File> onAfterConvert(File entity, SqlIdentifier table) {
        log.info("[onAfterConvert]::: entity :{}, table: {}", entity, table);
        return Mono.just(entity);
    }
}
