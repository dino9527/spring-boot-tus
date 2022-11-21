import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * 响应式核心
 *
 * @author dino
 * @date 2021/12/13 14:40
 */
class ReactorCoreTests {

    @Test
    void justTest() {
        Flux<String> seq1 = Flux.just("foo", "bar", "foobar");
        seq1.subscribe(System.out::println);

        List<String> iterable = Arrays.asList("foo", "bar", "foobar");
        Flux<String> seq2 = Flux.fromIterable(iterable);
        seq2.subscribe(System.out::println);
    }

    @Test
    void monoTest() {
        Mono<String> noData = Mono.empty();
        noData.subscribe(System.out::println);

        Mono<String> data = Mono.just("foo");
        data.subscribe(System.out::println);

        Flux<Integer> numbersFromFiveToSeven = Flux.range(5, 3);
        numbersFromFiveToSeven.subscribe(System.out::println);
    }

    /**
     1.订阅并触发序列。

     2.对每一个生成的元素进行消费。

     3.对正常元素进行消费，也对错误进行响应。

     4.对正常元素和错误均有响应，还定义了序列正常完成后的回调。

     5.对正常元素、错误和完成信号均有响应， 同时也定义了对该 subscribe 方法返回的 Subscription 执行的回调。

     以上方法会返回一个 Subscription 的引用，如果不再需要更多元素你可以通过它来取消订阅。
     取消订阅时， 源头会停止生成新的数据，并清理相关资源。取消和清理的操作在 Reactor 中是在 接口 Disposable 中定义的。
     */
    @Test
    void justTest2() {
        Flux<Integer> ints = Flux.range(1, 3);
        ints.subscribe();

        Flux<Integer> ints2 = Flux.range(1, 3);
        ints2.subscribe(System.out::println);

        Flux<Integer> ints3 = Flux.range(1, 4)
                .map(i -> {
                    if (i <= 3) return i;
                    throw new RuntimeException("Got to 4");
                });
        ints3.subscribe(System.out::println,
                error -> System.err.println("Error: " + error));

        Flux<Integer> ints4 = Flux.range(1, 4);
        ints4.subscribe(System.out::println,
                error -> System.err.println("Error " + error),
                () -> System.out.println("Done"));

        SampleSubscriber<Integer> ss = new SampleSubscriber<>();
        Flux<Integer> ints5 = Flux.range(1, 4);
        ints5.subscribe(System.out::println,
                error -> System.err.println("Error " + error),
                () -> System.out.println("Done"),
                s -> ss.request(10));
        ints5.subscribe(ss);


    }

    @Test
    void justTest3() {
        SampleSubscriber<Integer> ss = new SampleSubscriber<>();
        Flux<Integer> ints5 = Flux.range(1, 4);
        ints5.subscribe(ss);
    }

    @Test
    void justTest4() {
        Flux.range(1, 10)
                .doOnRequest(r -> System.out.println("request of " + r))
                .subscribe(new BaseSubscriber<Integer>() {

                    @Override
                    public void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    public void hookOnNext(Integer integer) {
                        System.out.println("Cancelling after having received " + integer);
                        cancel();
                    }
                });
    }

    /**
     1.初始化状态值（state）为0。

     2.我们基于状态值 state 来生成下一个值（state 乘以 3）。

     3.我们也可以用状态值来决定什么时候终止序列。

     4.返回一个新的状态值 state，用于下一次调用。

     上面的代码生成了“3 x”的乘法表：
     */
    @Test
    void justTest5() {
        Flux<String> flux = Flux.generate(
                () -> 0, // (1)
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3*state); // (2)
                    if (state == 10) sink.complete(); // (3)
                    return state + 1; // (4)
                });
        flux.subscribe(System.out::println);
    }

    /**
     1.这次我们初始化一个可变类型的状态值。

     2.改变状态值。

     3.返回 同一个 实例作为新的状态值。

     Tip: 如果状态对象需要清理资源，可以使用 generate(Supplier<S>, BiFunction, Consumer<S>) 这个签名方法来清理状态对象
     （译者注：Comsumer 在序列终止才被调用）。
     */
    @Test
    void justTest6() {
        Flux<String> flux = Flux.generate(
                AtomicLong::new, // (1)
                (state, sink) -> {
                    long i = state.getAndIncrement(); // (2)
                    sink.next("3 x " + i + " = " + 3*i);
                    if (i == 10) sink.complete();
                    return state; // (3)
                });
        flux.subscribe(System.out::println);
    }
    /**
     1.同样，初始化一个可变对象作为状态变量。

     2.改变状态。

     3.返回 同一个 实例作为新的状态。

     4.我们会看到最后一个状态值（11）会被这个 Consumer lambda 输出。

     如果 state 使用了数据库连接或者其他需要最终进行清理的资源，这个 Consumer lambda 可以用来在最后关闭连接或完成相关的其他清理任务。
     */
    @Test
    void justTest7() {
        Flux<String> flux = Flux.generate(
                AtomicLong::new,
                (state, sink) -> { // (1)
                    long i = state.getAndIncrement(); // (2)
                    sink.next("3 x " + i + " = " + 3*i);
                    if (i == 10) sink.complete();
                    return state; // (3)
                }, (state) -> System.out.println("state: " + state)); // (4)

        flux.subscribe(System.out::println);
    }

    /**
     可以把它当做 map 与 filter 的组合

     将 handle 用于一个 "映射 + 过滤 null" 的场景
     */
    @Test
    void justTest8() {
        Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
                .handle((i, sink) -> {
                    String letter = alphabet(i); // (1)
                    if (letter != null) // (2)
                        sink.next(letter); // (3)
                });

        alphabet.subscribe(System.out::println);
    }

    public String alphabet(int letterNumber) {
        if (letterNumber < 1 || letterNumber > 26) {
            return null;
        }
        int letterIndexAscii = 'A' + letterNumber - 1;
        return "" + (char) letterIndexAscii;
    }

    @Test
    void publishOnTest() {
        Scheduler s = Schedulers.newParallel("parallel-scheduler", 4);

        final Flux<String> flux = Flux
                .range(1, 2)
                .map(i -> 10 + i)
                .publishOn(s)
                .map(i -> "value " + i);

        new Thread(() -> flux.subscribe(System.out::println));
    }

    @Test
    void subscribeOnTest() {
        Scheduler s = Schedulers.newParallel("parallel-scheduler", 4);

        final Flux<String> flux = Flux
                .range(1, 2)
                .map(i -> 10 + i)
                .subscribeOn(s)
                .map(i -> "value " + i);

        new Thread(() -> flux.subscribe(System.out::println));
    }

    @Test
    void onErrorReturnTest() {
        Flux.just(1, 2, 0)
                .map(i -> "100 / " + i + " = " + (100 / i))
                .onErrorReturn("Divided by zero :(").subscribe(System.out::println);
    }

    /**
     1.第一个 lambda 生成资源，这里我们返回模拟的（mock） Disposable。

     2.第二个 lambda 处理资源，返回一个 Flux<T>。

     3.第三个 lambda 在 2) 中的资源 Flux 终止或取消的时候，用于清理资源。

     4.在订阅或执行流序列之后， isDisposed 会置为 true。

     另一方面， doFinally 在序列终止（无论是 onComplete、`onError`还是取消）的时候被执行， 并且能够判断是什么类型的终止事件（完成、错误还是取消？）。
     */
    @Test
    void java7TryWithResourcesTest() {
        AtomicBoolean isDisposed = new AtomicBoolean();
        Disposable disposableInstance = new Disposable() {
            @Override
            public void dispose() {
                isDisposed.set(true); // (4)
            }

            @Override
            public String toString() {
                return "DISPOSABLE";
            }
        };

        Flux<String> flux =
                Flux.using(
                        () -> disposableInstance, // (1)
                        disposable -> Flux.just(disposable.toString()), // (2)
                        Disposable::dispose // (3)
                );
        flux.subscribe(System.out::println);
    }

    /**
     1.我们想进行统计，所以用到了 LongAdder。

     2.doFinally 用 SignalType 检查了终止信号的类型。

     3.如果只是取消，那么统计数据自增。

     4.take(1) 能够在发出 1 个元素后取消流。
     */
    @Test
    void doFinallyTest() {
        LongAdder statsCancel = new LongAdder(); // (1)

        Flux<String> flux =
                Flux.just("foo", "bar")
                        .doFinally(type -> {
                            if (type == SignalType.CANCEL) // (2)
                                statsCancel.increment(); // (3)
                        })
                        .take(1); // (4)
        flux.subscribe(System.out::println);
    }

    /**
     注意 interval 默认基于一个 timer Scheduler 来执行。 如果我们想在 main 方法中运行， 我们需要调用 sleep，这样程序就可以避免在还没有产生任何值的时候就退出了。
     */
    @Test
    void doErrorTest() throws InterruptedException {
        Flux<String> flux =
                Flux.interval(Duration.ofMillis(250))
                        .map(input -> {
                            if (input < 3) return "tick " + input;
                            throw new RuntimeException("boom");
                        })
                        .onErrorReturn("Uh oh");

        flux.subscribe(System.out::println);
        Thread.sleep(2100);
    }

    /**
     1.elapsed 会关联从当前值与上个值发出的时间间隔。

     2.我们还是要看一下 onError 时的内容。

     3.确保我们有足够的时间可以进行 4x2 次 tick。
     */
    @Test
    void retryTest() throws InterruptedException {
        Flux.interval(Duration.ofMillis(250))
                .map(input -> {
                    if (input < 3) return "tick " + input;
                    throw new RuntimeException("boom");
                })
                .elapsed() // (1)
                .retry(1)
                .subscribe(System.out::println, System.err::println); // (2)

        Thread.sleep(2100); // (3)
    }

    /**
     1.持续产生错误。

     2.在 retry 之前 的 doOnError 可以让我们看到错误。

     3.这里，我们认为前 3 个错误是可以重试的（take(3)），再有错误就放弃。

     事实上，下边例子最终得到的是一个 空的 Flux，但是却 成功 完成了。反观对同一个 Flux 调用 retry(3) 的话，最终是以最后一个 error 终止 Flux，故而 retryWhen 与之不同。
     */
    @Test
    void retryWhenTest() {
        Flux<String> flux = Flux
                .<String>error(new IllegalArgumentException()) // (1)
                .doOnError(System.out::println) // (2)
                .retryWhen(Retry.from(companion ->
                        companion.take(3))); // (3)
        flux.subscribe(System.out::println);
    }

    /**
     1.技巧一：使用 zip 和一个“重试个数 + 1”的 range。

     2.zip 方法让你可以在对重试次数计数的同时，仍掌握着原始的错误（error）。

     3.允许三次重试，小于 4 的时候发出一个值。

     4.为了使序列以错误结束。我们将原始异常在三次重试之后抛出。
     */
    @Test
    void retryWhenTest2() {
        Flux<String> flux =
                Flux.<String>error(new IllegalArgumentException())
                        .retryWhen(Retry.from(companion -> companion
                                .zipWith(Flux.range(1, 4), // (1)
                                        (error, index) -> { // (2)
                                            if (index < 4) return index; // (3)
                                            else throw Exceptions.propagate(error.failure()); // (4)
                                        }))
                        );
        flux.subscribe(System.out::println);
    }

    @Test
    void retryWhenTest3() {
        AtomicInteger errorCount = new AtomicInteger();
        Flux<String> flux =
                Flux.<String>error(new IllegalArgumentException())
                        .doOnError(e -> errorCount.incrementAndGet())
                        .retryWhen(Retry.from(companion ->
                                companion.map(rs -> {
                                    if (rs.totalRetries() < 3) return rs.totalRetries();
                                    else throw Exceptions.propagate(rs.failure());
                                })
                        ));
        flux.subscribe(System.out::println);
    }

    @Test
    void retryWhenTest4() {
        AtomicInteger errorCount = new AtomicInteger();
        AtomicInteger transientHelper = new AtomicInteger();
        Flux<Integer> transientFlux = Flux.<Integer>generate(sink -> {
                    int i = transientHelper.getAndIncrement();
                    if (i == 10) {
                        sink.next(i);
                        sink.complete();
                    }
                    else if (i % 3 == 0) {
                        sink.next(i);
                    }
                    else {
                        sink.error(new IllegalStateException("Transient error at " + i));
                    }
                })
                .doOnError(e -> errorCount.incrementAndGet());

        transientFlux.retryWhen(Retry.max(2).transientErrors(true))
                .blockLast();
        assertThat(errorCount).hasValue(6);
    }
}

class SampleSubscriber<T> extends BaseSubscriber<T> {

    public void hookOnSubscribe(Subscription subscription) {
        System.out.println("Subscribed");
        request(1);
    }

    public void hookOnNext(T value) {
        System.out.println(value);
        request(1);
    }
}

interface MyEventListener<T> {
    void onDataChunk(List<T> chunk);
    void processComplete();
}