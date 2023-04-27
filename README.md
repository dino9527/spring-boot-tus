# TUS Server Java Implementation
>An implementation of the Tus Resumable Upload protocol [https://tus.io/protocols/resumable-upload.html] in java. 
> The HTTP stack uses Webflux and MySQL as the backend of upload information management.
> Supported extensions include creation, checksum, expiration, termination, and concatenation. 
> It provides an implementation of tus v1.0.0 protocol for local storage and allows implementers to develop their own specific requirements.

# Guide
* Run tus-server from your favorite IDE: priv.dino.tus.server.TusServerApplication
* Mysql is started
* Browser access：http://127.0.0.1:8080/swagger-ui.html <br>
