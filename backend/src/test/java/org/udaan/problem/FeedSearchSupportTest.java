package org.udaan.problem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.udaan.TestApi;
import org.udaan.TestApi.Response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"udaan.environment=ci"})
class FeedSearchSupportTest {

    static final String DRAINAGE = "0193f1a0-0000-7000-8000-000000000002";
    static final String WASTE = "0193f1a0-0000-7000-8000-000000000003";

    @Value("${local.server.port}")
    int port;

    TestApi api;

    /** A word unique to this test run, so results from other tests sharing the database don't interfere. */
    String tag;

    @BeforeEach
    void setUp() {
        api = new TestApi(port);
        tag = "tag" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    String raise(String token, String title, String categoryId, String urgency, int peopleAffected) {
        String json = """
                {"title":"%s","description":"Residents report this every week and nothing changes. %s",
                 "categoryId":"%s","cityId":1,"localityText":"Test Nagar","urgency":"%s","peopleAffected":%d,
                 "desiredOutcome":"Fix it properly this year","assistanceTypes":["VOLUNTEERING"],"anonymous":false}
                """.formatted(title, tag, categoryId, urgency, peopleAffected);
        Response r = api.post("/api/v1/problems", json, token);
        assertThat(r.status()).as(r.toString()).isEqualTo(201);
        return r.field("publicCode");
    }

    @Test
    void supportsAreCountedOncePerPersonAndRankTheFeed() {
        String owner = api.registerNewUser("Owner");
        String quiet = raise(owner, "Broken bench in the park " + tag, DRAINAGE, "LOW", 10);
        String popular = raise(owner, "Garbage dump near school " + tag, WASTE, "HIGH", 500);

        String fan1 = api.registerNewUser("Fan One");
        String fan2 = api.registerNewUser("Fan Two");
        Response first = api.put("/api/v1/problems/" + popular + "/support", fan1);
        assertThat(first.status()).as(first.toString()).isEqualTo(200);
        assertThat(first.number("count")).isEqualTo(1);
        assertThat(api.put("/api/v1/problems/" + popular + "/support", fan1).number("count")).as("idempotent").isEqualTo(1);
        assertThat(api.put("/api/v1/problems/" + popular + "/support", fan2).number("count")).isEqualTo(2);

        Response own = api.put("/api/v1/problems/" + popular + "/support", owner);
        assertThat(own.status()).isEqualTo(400);
        assertThat(own.field("code")).isEqualTo("CANNOT_SUPPORT_OWN");
        assertThat(api.put("/api/v1/problems/" + popular + "/support", null).status()).isEqualTo(401);

        Response bySupport = api.get("/api/v1/problems?q=" + tag + "&sort=SUPPORTED");
        assertThat(bySupport.status()).as(bySupport.toString()).isEqualTo(200);
        assertThat(bySupport.body().indexOf(popular)).isLessThan(bySupport.body().indexOf(quiet)).isNotNegative();

        Response asFan = api.get("/api/v1/problems?q=" + tag + "&sort=SUPPORTED", fan1);
        assertThat(asFan.body()).contains("\"supportedByMe\":true");

        Response detail = api.get("/api/v1/problems/" + popular, fan1);
        assertThat(detail.body()).contains("\"viewerSupports\":true").contains("\"supporters\":2");

        Response removed = api.delete("/api/v1/problems/" + popular + "/support", fan1);
        assertThat(removed.number("count")).isEqualTo(1);
        assertThat(api.delete("/api/v1/problems/" + popular + "/support", fan1).number("count")).isEqualTo(1);
    }

    @Test
    void filtersSortsAndSearches() {
        String owner = api.registerNewUser("Owner");
        String lights = raise(owner, "Streetlights not working on Kursi Road " + tag, DRAINAGE, "MEDIUM", 50);
        String garbage = raise(owner, "Garbage not collected for weeks " + tag, WASTE, "CRITICAL", 5000);

        Response byCategory = api.get("/api/v1/problems?q=" + tag + "&categoryId=" + WASTE);
        assertThat(byCategory.body()).contains(garbage).doesNotContain(lights);

        Response byUrgency = api.get("/api/v1/problems?q=" + tag + "&urgency=MEDIUM");
        assertThat(byUrgency.body()).contains(lights).doesNotContain(garbage);

        Response byImpact = api.get("/api/v1/problems?q=" + tag + "&sort=IMPACT");
        assertThat(byImpact.body().indexOf(garbage)).isLessThan(byImpact.body().indexOf(lights)).isNotNegative();

        Response typo = api.get("/api/v1/problems?q=stretlights");
        assertThat(typo.body()).as("typo-tolerant search").contains(lights);

        Response words = api.get("/api/v1/problems?q=garbage%20collected");
        assertThat(words.body()).contains(garbage);

        Response paged = api.get("/api/v1/problems?q=" + tag + "&size=1");
        assertThat(paged.body()).contains("\"hasMore\":true");
        Response trending = api.get("/api/v1/problems?q=" + tag + "&sort=TRENDING");
        assertThat(trending.status()).as(trending.toString()).isEqualTo(200);
        assertThat(trending.body()).contains(lights).contains(garbage);
        Response badSort = api.get("/api/v1/problems?sort=nonsense");
        assertThat(badSort.status()).as(badSort.toString()).isEqualTo(400);
        assertThat(badSort.field("code")).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void suggestsSimilarProblemsWhileWriting() {
        String owner = api.registerNewUser("Owner");
        String existing = raise(owner, "Open manhole on Faizabad Road " + tag, DRAINAGE, "HIGH", 300);

        Response similar = api.get("/api/v1/problems/similar?cityId=1&title=open%20manhole%20faizabad%20road");
        assertThat(similar.status()).as(similar.toString()).isEqualTo(200);
        assertThat(similar.body()).contains(existing);

        Response unrelated = api.get("/api/v1/problems/similar?cityId=1&title=zzqq%20yyww%20" + UUID.randomUUID());
        assertThat(unrelated.body()).doesNotContain(existing);
    }

    @Test
    void followingIsToggled() {
        String owner = api.registerNewUser("Owner");
        String code = raise(owner, "Bus stop has no shelter " + tag, DRAINAGE, "LOW", 100);
        String follower = api.registerNewUser("Follower");

        assertThat(api.put("/api/v1/problems/" + code + "/follow", follower).number("count")).isEqualTo(1);
        assertThat(api.get("/api/v1/problems/" + code, follower).body()).contains("\"viewerFollows\":true");
        assertThat(api.delete("/api/v1/problems/" + code + "/follow", follower).number("count")).isEqualTo(0);
        assertThat(api.put("/api/v1/problems/" + code + "/follow", owner).number("count")).as("owners may follow").isEqualTo(1);
    }
}
