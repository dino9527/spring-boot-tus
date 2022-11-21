import org.junit.jupiter.api.Test;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

/**
 * 高级特性
 *
 * @author dino
 * @date 2021/12/15 13:57
 */
class ReactorAdvancedFeaturesTests {

    @Test
    void testTransform() {
        Function<Flux<String>, Flux<String>> filterAndMap =
                f -> f.filter(color -> !color.equals("orange"))
                        .map(String::toUpperCase);

        Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                .doOnNext(System.out::println)
                .transform(filterAndMap)
                .subscribe(d -> System.out.println("Subscriber to Transformed MapAndFilter: "+d));
    }

    @Test
    void testTransformDeferred() {
        AtomicInteger ai = new AtomicInteger();
        Function<Flux<String>, Flux<String>> filterAndMap = f -> {
            if (ai.incrementAndGet() == 1) {
                return f.filter(color -> !color.equals("orange"))
                        .map(String::toUpperCase);
            }
            return f.filter(color -> !color.equals("purple"))
                    .map(String::toUpperCase);
        };

        Flux<String> composedFlux =
                Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                        .doOnNext(System.out::println)
                        .transformDeferred(filterAndMap);

        composedFlux.subscribe(d -> System.out.println("Subscriber 1 to Composed MapAndFilter :"+d));
        composedFlux.subscribe(d -> System.out.println("Subscriber 2 to Composed MapAndFilter: "+d));
    }

    @Test
    void testCold() {
        Flux<String> source = Flux.fromIterable(Arrays.asList("blue", "green", "orange", "purple"))
                .map(String::toUpperCase);

        source.subscribe(d -> System.out.println("Subscriber 1: "+d));
        source.subscribe(d -> System.out.println("Subscriber 2: "+d));
    }

    @Test
    void testHot() {
        Sinks.Many<String> hotSource = Sinks.unsafe().many().multicast().directBestEffort();

        Flux<String> hotFlux = hotSource.asFlux().map(String::toUpperCase);

        hotFlux.subscribe(d -> System.out.println("Subscriber 1 to Hot Source: "+d));

        hotSource.emitNext("blue", FAIL_FAST);
        hotSource.tryEmitNext("green").orThrow();

        hotFlux.subscribe(d -> System.out.println("Subscriber 2 to Hot Source: "+d));

        hotSource.emitNext("orange", FAIL_FAST);
        hotSource.emitNext("purple", FAIL_FAST);
        hotSource.emitComplete(FAIL_FAST);
    }

    @Test
    void testConnectableFlux() throws InterruptedException {
        Flux<Integer> source = Flux.range(1, 3)
                .doOnSubscribe(s -> System.out.println("subscribed to source"));

        ConnectableFlux<Integer> co = source.publish();

        co.subscribe(System.out::println, e -> {}, () -> {});
        co.subscribe(System.out::println, e -> {}, () -> {});

        System.out.println("done subscribing");
        Thread.sleep(500);
        System.out.println("will now connect");

        co.connect();


        Flux<Integer> autoCo = source.publish().autoConnect(2);

        autoCo.subscribe(System.out::println, e -> {}, () -> {});
        System.out.println("subscribed first");
        Thread.sleep(500);
        System.out.println("subscribing second");
        autoCo.subscribe(System.out::println, e -> {}, () -> {});
    }

    @Test
    void testGroupedFlux() {
        StepVerifier.create(
                        Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
                                .groupBy(i -> i % 2 == 0 ? "even" : "odd")
                                .concatMap(g -> g.defaultIfEmpty(-1) //如果组为空，显示为 -1
                                        .map(String::valueOf) //转换为字符串
                                        .startWith(g.key())) //以该组的 key 开头
                )
                .expectNext("odd", "1", "3", "5", "11", "13")
                .expectNext("even", "2", "4", "6", "12")
                .verifyComplete();
    }

    @Test
    void testWindow () {
        StepVerifier.create(
                        Flux.range(1, 10)
                                .window(5, 3) //overlapping windows
                                .concatMap(g -> g.defaultIfEmpty(-1)) //将 windows 显示为 -1
                )
                .expectNext(1, 2, 3, 4, 5)
                .expectNext(4, 5, 6, 7, 8)
                .expectNext(7, 8, 9, 10)
                .expectNext(10)
                .verifyComplete();
    }

    @Test
    void testWindowWhile() {
        StepVerifier.create(
                        Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
                                .windowWhile(i -> i % 2 == 0)
                                .concatMap(g -> g.defaultIfEmpty(-1))
                )
                .expectNext(-1, -1, -1) //分别被奇数 1 3 5 触发
                .expectNext(2, 4, 6) // 被 11 触发
                .expectNext(12) // 被 13 触发
//                .expectNext(-1) // 空的 completion window，如果 onComplete 前的元素能够匹配上的话就没有这个了
                .verifyComplete();
    }

    @Test
    void testBuffer() {
        StepVerifier.create(
                        Flux.range(1, 10)
                                .buffer(5, 3) // 缓存重叠
                )
                .expectNext(Arrays.asList(1, 2, 3, 4, 5))
                .expectNext(Arrays.asList(4, 5, 6, 7, 8))
                .expectNext(Arrays.asList(7, 8, 9, 10))
                .expectNext(Collections.singletonList(10))
                .verifyComplete();
    }

    @Test
    void testBufferWhile() {
        StepVerifier.create(
                        Flux.just(1, 3, 5, 2, 4, 6, 11, 12, 13)
                                .bufferWhile(i -> i % 2 == 0)
                )
                .expectNext(Arrays.asList(2, 4, 6)) // 被 11 触发
                .expectNext(Collections.singletonList(12)) // 被 13 触发
                .verifyComplete();
    }

    @Test
    void testParallelFlux() {
        Flux.range(1, 10)
                .parallel(2)
                .runOn(Schedulers.parallel())
                .subscribe(i -> System.out.println(Thread.currentThread().getName() + " -> " + i));
    }

    /**
     * context 下游至上游
     */
    @Test
    void testContextWrite() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                .flatMap(s -> Mono.deferContextual(ctx ->
                        Mono.just(s + " " + ctx.get(key))))
                .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier.create(r)
                .expectNext("Hello World")
                .verifyComplete();

    }

    /**
     * context 上游至下游
     */
    @Test
    void testContextWrite2() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                .contextWrite(ctx -> ctx.put(key, "World"))
                .flatMap( s -> Mono.deferContextual(ctx ->
                        Mono.just(s + " " + ctx.getOrDefault(key, "Stranger"))));

        StepVerifier.create(r)
                .expectNext("Hello Stranger")
                .verifyComplete();

    }

    /**
     * context 最近的
     */
    @Test
    void testContextWrite3() {
        String key = "message";
        Mono<String> r = Mono
                .deferContextual(ctx -> Mono.just("Hello " + ctx.get(key)))
                .contextWrite(ctx -> ctx.put(key, "Reactor"))
                .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier.create(r)
                .expectNext("Hello Reactor")
                .verifyComplete();

    }

    @Test
    void testContextWrite4() {
        String key = "message";
        Mono<String> r = Mono
                .deferContextual(ctx -> Mono.just("Hello " + ctx.get(key)))
                .contextWrite(ctx -> ctx.put(key, "Reactor"))
                .flatMap( s -> Mono.deferContextual(ctx ->
                        Mono.just(s + " " + ctx.get(key))))
                .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier.create(r)
                .expectNext("Hello Reactor World")
                .verifyComplete();

    }

    @Test
    void testContextWrite5() {
        String key = "message";
        Mono<String> r = Mono.just("Hello")
                .flatMap( s -> Mono
                        .deferContextual(ctxView -> Mono.just(s + " " + ctxView.get(key)))
                )
                .flatMap( s -> Mono
                        .deferContextual(ctxView -> Mono.just(s + " " + ctxView.get(key)))
                        .contextWrite(ctx -> ctx.put(key, "Reactor"))
                )
                .contextWrite(ctx -> ctx.put(key, "World"));

        StepVerifier.create(r)
                .expectNext("Hello World Reactor")
                .verifyComplete();

    }

    static final String HTTP_CORRELATION_ID = "reactive.http.library.correlationId";

    Mono<Tuple2<Integer, String>> doPut(String url, Mono<String> data) {
        Mono<Tuple2<String, Optional<Object>>> dataAndContext =
                data.zipWith(Mono.deferContextual(c ->
                        Mono.just(c.getOrEmpty(HTTP_CORRELATION_ID)))
                );

        return dataAndContext.<String>handle((dac, sink) -> {
                    if (dac.getT2().isPresent()) {
                        sink.next("PUT <" + dac.getT1() + "> sent to " + url +
                                " with header X-Correlation-ID = " + dac.getT2().get());
                    } else {
                        sink.next("PUT <" + dac.getT1() + "> sent to " + url);
                    }
                    sink.complete();
                })
                .map(msg -> Tuples.of(200, msg));

    }
    @Test
    void contextForLibraryReactivePut() {
        Mono<String> put = doPut("www.example.com", Mono.just("Walter"))
                .contextWrite(Context.of(HTTP_CORRELATION_ID, "2-j3r9afaf92j-afkaf"))
                .filter(t -> t.getT1() < 300)
                .map(Tuple2::getT2);

        StepVerifier.create(put)
                .expectNext("PUT <Walter> sent to www.example.com" +
                        " with header X-Correlation-ID = 2-j3r9afaf92j-afkaf")
                .verifyComplete();
    }
}


