package app.VBeta.performance;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WallSectionsPerformanceTest {

    @LocalServerPort
    private int port;

    @Test
    void wallSections_shouldMeetLatencyAndErrorBudgets() throws Exception {
        int totalRequests = Integer.getInteger("perf.totalRequests", 500);
        int concurrency = Integer.getInteger("perf.concurrency", 20);
        int warmupRequests = Integer.getInteger("perf.warmupRequests", 50);
        long p95BudgetMs = Long.getLong("perf.p95BudgetMs", 300L);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        URI uri = URI.create("http://localhost:" + port + "/home/wall-sections");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        for (int i = 0; i < warmupRequests; i++) {
            client.send(request, HttpResponse.BodyHandlers.discarding());
        }

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        List<Integer> statuses = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            futures.add(pool.submit(() -> {
                long started = System.nanoTime();
                try {
                    HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                    statuses.add(response.statusCode());
                } catch (Exception ex) {
                    // Use synthetic status to account for client-side failures in assertions.
                    statuses.add(599);
                } finally {
                    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    latenciesMs.add(elapsedMs);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Worker pool did not terminate");

        long non200Count = statuses.stream().filter(status -> status != 200).count();
        assertEquals(0, non200Count, "Expected all responses to return HTTP 200");

        Collections.sort(latenciesMs);
        int p95Index = (int) Math.ceil(latenciesMs.size() * 0.95) - 1;
        long p95 = latenciesMs.get(Math.max(0, p95Index));

        assertTrue(
                p95 <= p95BudgetMs,
                "P95 latency budget exceeded. Expected <= " + p95BudgetMs + "ms, got " + p95 + "ms");
    }
}
