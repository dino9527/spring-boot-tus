import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.publisher.PublisherProbe;
import reactor.util.context.Context;

import java.time.Duration;

/**
 * 响应式测试
 *
 * @author dino
 * @date 2021/12/14 15:45
 */
class ReactorTestTests {

    <T> Flux<T> appendBoomError(Flux<T> source) {
        return source.concatWith(Mono.error(new IllegalArgumentException("boom")));
    }

    /**
     1.由于被测试方法需要一个 Flux，定义一个简单的 Flux 用于测试。

     2.创建一个 StepVerifier 构造器来包装和校验一个 Flux。

     3.传进来需要测试的 Flux（即待测方法的返回结果）。

     4.第一个我们期望的信号是 onNext，它的值为 foo。

     5.最后我们期望的是一个终止信号 onError，异常内容应该为 boom。

     6.不要忘了使用 verify() 触发测试。
     */
    @Test
    void testAppendBoomError() {
        Flux<String> source = Flux.just("foo", "bar"); // (1)

        StepVerifier.create( // (2)
                        appendBoomError(source)) // (3)
                .expectNext("foo") // (4)
                .expectNext("bar")
                .expectErrorMessage("boom") // (5)
                .verify(); // (6)
    }

    /**
     1.expectNoEvent 将订阅（subscription）也认作一个事件。假设你用它作为第一步，如果检测 到有订阅信号，也会失败。
     这时候可以使用 expectSubscription().expectNoEvent(duration) 来代替。

     2.期待一天内没有信号发生。

     3.然后期待一个 next 信号为 0。

     4.然后期待完成（同时触发校验）。

     我们也可以使用 thenAwait(Duration.ofDays(1))，但是 expectNoEvent 的好处是 能够验证在此之前不会发生什么。
     */
    @Test
    void testDelay() {
        StepVerifier.withVirtualTime(() -> Mono.delay(Duration.ofDays(1)))
                .expectSubscription() // (1)
                .expectNoEvent(Duration.ofDays(1)) // (2)
                .expectNext(0L) // (3)
                .verifyComplete(); // (4)
    }

    /**
     1.使用 StepVerifierOptions 创建 StepVerifier 并传入初始 Context。

     2.开始对 Context 进行校验，这里只是确保 Context 正常传播了。

     3.对 Context 进行校验的例子：比如验证是否包含一个 "foo" - "bar" 键值对。

     4.使用 then() 切换回对序列的校验。

     5.不要忘了用 verify() 触发整个校验过程。
     */
    @Test
    void testStepVerifierOptions() {
        StepVerifier.create(Mono.just(1).map(i -> i + 10),
                        StepVerifierOptions.create().withInitialContext(Context.of("foo", "bar"))) // (1)
                .expectAccessibleContext() //(2)
                .contains("foo", "bar") // (3)
                .then() // (4)
                .expectNext(11)
                .verifyComplete(); // (5)
    }

    Flux<String> processOrFallback(Mono<String> source, Publisher<String> fallback) {
        return source
                .flatMapMany(phrase -> Flux.fromArray(phrase.split("\\s+")))
                .switchIfEmpty(fallback);
    }

    @Test
    void testSplitPathIsUsed() {
        StepVerifier.create(processOrFallback(Mono.just("just a  phrase with    tabs!"),
                        Mono.just("EMPTY_PHRASE")))
                .expectNext("just", "a", "phrase", "with", "tabs!")
                .verifyComplete();
    }

    @Test
    void testEmptyPathIsUsed() {
        StepVerifier.create(processOrFallback(Mono.empty(), Mono.just("EMPTY_PHRASE")))
                .expectNext("EMPTY_PHRASE")
                .verifyComplete();
    }


    private Mono<String> executeCommand(String command) {
        return Mono.just(command + " DONE");
    }

    /**
     1.then() 方法会忽略 command，它只关心是否结束。

     2.两个都是空序列，这个时候如何区分（哪边执行了）呢？
     */
    Mono<Void> processOrFallback(Mono<String> commandSource, Mono<Void> doWhenEmpty) {
        return commandSource
                .flatMap(command -> executeCommand(command).then()) // (1)
                .switchIfEmpty(doWhenEmpty); // (2)
    }

    /**
     1.创建一个探针（probe），它会转化为一个空序列。

     2.在需要使用 Mono<Void> 的位置调用 probe.mono() 来替换为探针。

     3.序列结束之后，你可以用这个探针来判断序列是如何使用的，你可以检查是它从哪（条路径）被订阅的…​

     4.…对于请求也是一样的…

     5.…以及是否被取消了。

     你也可以在使用 Flux<T> 的位置通过调用 .flux() 方法来放置探针。
     如果你既需要用探针检查执行路径 还需要它能够发出数据，你可以用 PublisherProbe.of(Publisher) 方法包装一个 Publisher<T> 来搞定。
     */
    @Test
    void testCommandEmptyPathIsUsed() {
        PublisherProbe<Void> probe = PublisherProbe.empty(); // (1)

        StepVerifier.create(processOrFallback(Mono.empty(), probe.mono())) // (2)
                .verifyComplete();

        probe.assertWasSubscribed(); //(3)
        probe.assertWasRequested(); //(4)
        probe.assertWasNotCancelled(); //(5)
    }
}
