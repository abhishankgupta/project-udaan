package org.udaan.problem;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.udaan.TestApi;
import org.udaan.TestApi.Response;

/** Raising a problem end to end, in a non-production environment where new problems publish immediately. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"udaan.environment=ci"})
class ProblemFlowTest {

    static final String DRAINAGE = "0193f1a0-0000-7000-8000-000000000002";

    @Value("${local.server.port}")
    int port;

    TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(port);
    }

    static String problemJson(String localityPart, String mediaIds, boolean anonymous) {
        return """
                {"title":"Waterlogging on main road after every rain",
                 "description":"During monsoon the main road floods knee-deep. Residents cannot reach the market and children miss school.",
                 "categoryId":"%s","cityId":1,%s,"addressText":"Near the post office",
                 "latitude":26.88,"longitude":80.94,"urgency":"HIGH","peopleAffected":2000,
                 "desiredOutcome":"Working drains before the next monsoon",
                 "assistanceTypes":["GOVERNMENT_SUPPORT","EXPERTISE"],"anonymous":%s,"mediaIds":[%s]}
                """.formatted(DRAINAGE, localityPart, anonymous, mediaIds);
    }

    long firstLocalityId() {
        Response cities = api.get("/api/v1/geo/cities");
        assertThat(cities.body()).contains("Lucknow").contains("Uttar Pradesh");
        Response localities = api.get("/api/v1/geo/cities/1/localities");
        assertThat(localities.body()).contains("Hazratganj").contains("Gomti Nagar");
        return localities.number("id");
    }

    @Test
    void uploadsAPhotoAndRaisesAProblemThatAnyoneCanView() {
        String token = api.registerNewUser("Asha");

        Response upload = api.upload("/api/v1/media", "street.png", "image/png", TestImages.png(2400, 1200), token);
        assertThat(upload.status()).as(upload.toString()).isEqualTo(201);
        assertThat(upload.number("width")).isEqualTo(1600);
        assertThat(upload.number("height")).isEqualTo(800);
        String mediaId = upload.field("id");

        HttpResponse<byte[]> photo = api.getBytes(upload.field("url"));
        assertThat(photo.statusCode()).isEqualTo(200);
        assertThat(photo.headers().firstValue("Content-Type")).hasValue("image/jpeg");
        assertThat(photo.body()[0]).isEqualTo((byte) 0xFF); // JPEG magic number
        assertThat(photo.body()[1]).isEqualTo((byte) 0xD8);

        long localityId = firstLocalityId();
        Response created = api.post("/api/v1/problems",
                problemJson("\"localityId\":" + localityId, "\"" + mediaId + "\"", false), token);
        assertThat(created.status()).as(created.toString()).isEqualTo(201);
        String code = created.field("publicCode");
        assertThat(code).matches("UDN-LKO-\\d{6}");
        assertThat(created.field("status")).isEqualTo("OPEN_FOR_SOLUTIONS");
        assertThat(created.body()).contains("Submitted for review").contains("Published automatically")
                .contains(mediaId).contains("\"viewerIsOwner\":true");

        Response publicView = api.get("/api/v1/problems/" + code);
        assertThat(publicView.status()).isEqualTo(200);
        assertThat(publicView.field("displayName")).isEqualTo("Asha");
        assertThat(publicView.field("cityName")).isEqualTo("Lucknow");
        assertThat(publicView.body()).contains("\"viewerIsOwner\":false").contains("Drainage and water");

        Response mine = api.get("/api/v1/me/problems", token);
        assertThat(mine.status()).isEqualTo(200);
        assertThat(mine.body()).contains(code).contains("/api/v1/media/" + mediaId);
    }

    @Test
    void anonymousSubmissionsHideTheSubmitterFromOthers() {
        String token = api.registerNewUser("Hidden Person");
        Response created = api.post("/api/v1/problems", problemJson("\"localityText\":\"New Colony\"", "", true), token);
        assertThat(created.status()).as(created.toString()).isEqualTo(201);
        assertThat(created.field("localityName")).isEqualTo("New Colony");

        Response asOwner = api.get("/api/v1/problems/" + created.field("publicCode"), token);
        assertThat(asOwner.field("displayName")).isEqualTo("Hidden Person");

        Response asOther = api.get("/api/v1/problems/" + created.field("publicCode"));
        assertThat(asOther.body()).as("not in submittedBy nor the timeline").doesNotContain("Hidden Person")
                .contains("\"anonymous\":true").contains("Anonymous resident");
    }

    @Test
    void rejectsInvalidSubmissions() {
        String token = api.registerNewUser("Asha");

        Response noLocality = api.post("/api/v1/problems", problemJson("\"localityText\":\"\"", "", false), token);
        assertThat(noLocality.status()).isEqualTo(400);
        assertThat(noLocality.body()).contains("localityId");

        Response shortTitle = api.post("/api/v1/problems",
                problemJson("\"localityText\":\"X\"", "", false).replace("Waterlogging on main road after every rain", "Short"), token);
        assertThat(shortTitle.status()).isEqualTo(400);
        assertThat(shortTitle.body()).contains("title");

        String someoneElsesPhoto = api.upload("/api/v1/media", "a.png", "image/png", TestImages.png(10, 10),
                api.registerNewUser("Other")).field("id");
        Response stolenPhoto = api.post("/api/v1/problems",
                problemJson("\"localityText\":\"X\"", "\"" + someoneElsesPhoto + "\"", false), token);
        assertThat(stolenPhoto.status()).isEqualTo(400);
        assertThat(stolenPhoto.field("code")).isEqualTo("INVALID_MEDIA");

        assertThat(api.post("/api/v1/problems", problemJson("\"localityText\":\"X\"", "", false)).status())
                .as("must be signed in").isEqualTo(401);
    }

    @Test
    void rejectsFilesThatAreNotPhotos() {
        String token = api.registerNewUser("Asha");
        Response notImage = api.upload("/api/v1/media", "x.png", "image/png", "not really a png".getBytes(), token);
        assertThat(notImage.status()).isEqualTo(415);
        Response pdf = api.upload("/api/v1/media", "x.pdf", "application/pdf", TestImages.png(10, 10), token);
        assertThat(pdf.status()).isEqualTo(415);
        assertThat(api.upload("/api/v1/media", "x.png", "image/png", TestImages.png(10, 10), null).status()).isEqualTo(401);
    }

    @Test
    void limitsSubmissionsPerDay() {
        String token = api.registerNewUser("Busy");
        for (int i = 0; i < 5; i++) {
            assertThat(api.post("/api/v1/problems", problemJson("\"localityText\":\"X\"", "", false), token).status()).isEqualTo(201);
        }
        Response limited = api.post("/api/v1/problems", problemJson("\"localityText\":\"X\"", "", false), token);
        assertThat(limited.status()).isEqualTo(429);
        assertThat(limited.field("code")).isEqualTo("PROBLEM_RATE_LIMITED");
    }
}
