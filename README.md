# TUS 服务 Java 实现
>Tus 可恢复上传协议 [https://tus.io/protocols/resumable-upload.html] 在 java 中的实现。 http栈部分使用Webflux，Mysql作为上传信息管理的后端。
支持的扩展包括creation(创建),checksum(校验),expiration(过期),termination(终止),concatenation(续传)。存储库目前提供本地存储的 Tus 协议实现，允许实现者研发自己的特定需求。

# 指南
* 从您最喜欢的 IDE 中运行 tus-server：priv.dino.tus.server.TusServerApplication
* Mysql处于启动状态
* 浏览器访问：http://127.0.0.1:8080/swagger-ui.html <br>
* 存储库目前提供了两种web api方式：
    * Functional endpoint（函数-推荐） 
    * Annotated controller（注解-校验等功能未完善）

# 最后
维护者对于响应式编程只是初步了解，代码若有Bug望体谅。
