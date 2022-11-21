DROP TABLE IF EXISTS `file`;

CREATE TABLE `file` (
                        `id` bigint(20) unsigned NOT NULL COMMENT '主键',
                        `mime_type` varchar(255) DEFAULT NULL COMMENT 'MIME类型',
                        `content_length` bigint(20) unsigned NOT NULL COMMENT '内容长度',
                        `content_offset` bigint(20) unsigned NOT NULL COMMENT '内容偏移',
                        `last_uploaded_chunk_number` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '最后上传的块号',
                        `original_name` varchar(500) NOT NULL COMMENT '文件名称',
                        `fingerprint` varchar(500) NOT NULL COMMENT '指纹',
                        `is_partial` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否部分上传:1是0否',
                        `created_at` timestamp NULL DEFAULT NULL,
                        `updated_at` timestamp NULL DEFAULT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件'