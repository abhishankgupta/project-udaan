package org.udaan.collab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.udaan.TestApi;
import org.udaan.TestApi.Response;

/** Solutions, discussions and offers of help, end to end. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"udaan.environment=ci"})
class CollaborationFlowTest {

    @Value("${local.server.port}")
    int port;

    TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(port);
    }

    String raise(String token) {
        Response r = api.post("/api/v1/problems", """
                {"title":"Broken drain cover near the market","description":"An open drain near the vegetable market is a hazard for children and the elderly.",
                 "categoryId":"0193f1a0-0000-7000-8000-000000000002","cityId":1,"localityText":"Chowk",
                 "urgency":"HIGH","desiredOutcome":"A strong drain cover installed","assistanceTypes":["FUNDING"],"anonymous":false}
                """, token);
        assertThat(r.status()).as(r.toString()).isEqualTo(201);
        return r.field("publicCode");
    }

    String solutionJson(String title) {
        return """
                {"title":"%s","summary":"Install a cast-iron cover and a warning sign, funded by local shops.",
                 "approach":"Collect quotes from two fabricators, pool money from shop owners, and install within a week.",
                 "estimatedCostInr":15000,"timelineText":"2 weeks","resourcesRequired":"Welder, cover, sign","skills":["Civil work","Fundraising"]}
                """.formatted(title);
    }

    @Test
    void solutionsCanBeProposedEndorsedAcceptedAndWithdrawn() {
        String owner = api.registerNewUser("Owner");
        String proposer = api.registerNewUser("Proposer");
        String other = api.registerNewUser("Other");
        String code = raise(owner);

        Response invalid = api.post("/api/v1/problems/" + code + "/solutions", solutionJson("Short"), proposer);
        assertThat(invalid.status()).isEqualTo(400);

        Response proposed = api.post("/api/v1/problems/" + code + "/solutions", solutionJson("Cast-iron cover funded by shops"), proposer);
        assertThat(proposed.status()).as(proposed.toString()).isEqualTo(201);
        String solutionId = proposed.field("id");
        assertThat(proposed.body()).contains("\"status\":\"PUBLISHED\"").contains("Civil work").contains("\"viewerIsProposer\":true");

        Response problem = api.get("/api/v1/problems/" + code);
        assertThat(problem.field("status")).isEqualTo("SOLUTION_PROPOSED");
        assertThat(problem.body()).contains("\"solutions\":1").contains("A solution has been proposed");

        assertThat(api.put("/api/v1/solutions/" + solutionId + "/endorsement", other).number("count")).isEqualTo(1);
        assertThat(api.put("/api/v1/solutions/" + solutionId + "/endorsement", proposer).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/solutions/" + solutionId, other).body()).contains("\"viewerEndorsed\":true");

        String alternative = api.post("/api/v1/problems/" + code + "/solutions", solutionJson("Plastic grill as a quick fix"), other).field("id");

        assertThat(api.post("/api/v1/solutions/" + solutionId + "/accept", "{}", proposer).status()).as("only the owner accepts").isEqualTo(403);
        Response asOwner = api.get("/api/v1/problems/" + code + "/solutions", owner);
        assertThat(asOwner.body()).contains("\"canAccept\":true");

        Response accepted = api.post("/api/v1/solutions/" + solutionId + "/accept", "{}", owner);
        assertThat(accepted.status()).as(accepted.toString()).isEqualTo(200);
        assertThat(accepted.body()).contains("\"status\":\"ACCEPTED\"");
        Response after = api.get("/api/v1/problems/" + code);
        assertThat(after.field("status")).isEqualTo("SOLUTION_ACCEPTED");
        assertThat(after.body()).contains("accepted the solution: Cast-iron cover funded by shops");

        assertThat(api.post("/api/v1/solutions/" + alternative + "/accept", "{}", owner).status()).isEqualTo(409);
        assertThat(api.post("/api/v1/solutions/" + solutionId + "/withdraw", "{}", proposer).status()).isEqualTo(409);

        Response withdrawn = api.post("/api/v1/solutions/" + alternative + "/withdraw", "{}", other);
        assertThat(withdrawn.status()).as(withdrawn.toString()).isEqualTo(200);
        Response list = api.get("/api/v1/problems/" + code + "/solutions");
        assertThat(list.body()).contains(solutionId).doesNotContain(alternative);
        assertThat(api.get("/api/v1/solutions/" + alternative).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/problems/" + code).body()).contains("\"solutions\":1");

        assertThat(api.get("/api/v1/me/solutions", proposer).body()).contains(solutionId);
        assertThat(api.get("/api/v1/solutions?size=50").body()).contains(solutionId);
    }

    @Test
    void discussionsThreadRepliesAndLetAuthorsDeleteTheirComments() {
        String owner = api.registerNewUser("Owner");
        String alice = api.registerNewUser("Alice");
        String bob = api.registerNewUser("Bob");
        String code = raise(owner);

        Response top = api.post("/api/v1/problems/" + code + "/comments", "{\"body\":\"I can help measure the drain.\"}", alice);
        assertThat(top.status()).as(top.toString()).isEqualTo(201);
        String topId = top.field("id");

        Response reply = api.post("/api/v1/problems/" + code + "/comments",
                "{\"body\":\"Great, Saturday works?\",\"parentId\":\"" + topId + "\"}", bob);
        assertThat(reply.status()).isEqualTo(201);
        Response nested = api.post("/api/v1/problems/" + code + "/comments",
                "{\"body\":\"Yes, 10am.\",\"parentId\":\"" + reply.field("id") + "\"}", alice);
        assertThat(nested.status()).isEqualTo(201);

        Response thread = api.get("/api/v1/problems/" + code + "/comments", bob);
        assertThat(thread.body()).contains("I can help measure the drain.").contains("Great, Saturday works?").contains("Yes, 10am.");
        assertThat(thread.body().indexOf("Yes, 10am.")).as("replies to replies stay one level deep").isGreaterThan(thread.body().indexOf(topId));
        assertThat(api.get("/api/v1/problems/" + code).body()).contains("\"comments\":3");

        assertThat(api.delete("/api/v1/comments/" + topId, bob).status()).isEqualTo(403);
        assertThat(api.delete("/api/v1/comments/" + topId, alice).status()).isEqualTo(204);
        Response afterDelete = api.get("/api/v1/problems/" + code + "/comments");
        assertThat(afterDelete.body()).doesNotContain("I can help measure the drain.").contains("\"removed\":true").contains("Great, Saturday works?");
        assertThat(api.get("/api/v1/problems/" + code).body()).contains("\"comments\":2");

        assertThat(api.post("/api/v1/problems/" + code + "/comments", "{\"body\":\"hi\"}").status()).isEqualTo(401);
        assertThat(api.post("/api/v1/problems/" + code + "/comments", "{\"body\":\"  \"}", bob).status()).isEqualTo(400);

        String solutionId = api.post("/api/v1/problems/" + code + "/solutions", solutionJson("Cast-iron cover funded by shops"), bob).field("id");
        assertThat(api.post("/api/v1/solutions/" + solutionId + "/comments", "{\"body\":\"How much will the welder cost?\"}", alice).status()).isEqualTo(201);
        assertThat(api.get("/api/v1/solutions/" + solutionId + "/comments").body()).contains("How much will the welder cost?");
        assertThat(api.get("/api/v1/problems/" + code + "/comments").body()).doesNotContain("How much will the welder cost?");
    }

    @Test
    void offersOfHelpKeepMessagesAndContactsPrivate() {
        String owner = api.registerNewUser("Owner");
        String proposer = api.registerNewUser("Proposer");
        String helper = api.registerNewUser("Helper");
        String code = raise(owner);
        String solutionId = api.post("/api/v1/problems/" + code + "/solutions", solutionJson("Cast-iron cover funded by shops"), proposer).field("id");

        Response offer = api.post("/api/v1/problems/" + code + "/contributions",
                "{\"types\":[\"VOLUNTEERING\",\"FUNDING\"],\"message\":\"I can give 2000 rupees and a Sunday.\",\"shareContact\":true}", helper);
        assertThat(offer.status()).as(offer.toString()).isEqualTo(201);
        String offerId = offer.field("id");

        Response publicView = api.get("/api/v1/problems/" + code + "/contributions");
        assertThat(publicView.body()).contains("Helper").contains("VOLUNTEERING")
                .doesNotContain("2000 rupees").doesNotContain("@example.org");

        Response ownerView = api.get("/api/v1/problems/" + code + "/contributions", owner);
        assertThat(ownerView.body()).contains("I can give 2000 rupees and a Sunday.").contains("@example.org");

        Response updated = api.post("/api/v1/problems/" + code + "/contributions",
                "{\"types\":[\"EXPERTISE\"],\"message\":\"Changed my mind: advice only.\",\"shareContact\":false}", helper);
        assertThat(updated.field("id")).as("same offer updated, not duplicated").isEqualTo(offerId);
        Response ownerAgain = api.get("/api/v1/problems/" + code + "/contributions", owner);
        assertThat(ownerAgain.body()).contains("advice only").doesNotContain("@example.org").doesNotContain("VOLUNTEERING");

        Response toSolution = api.post("/api/v1/problems/" + code + "/contributions",
                "{\"types\":[\"RESOURCES\"],\"message\":\"I have a spare cover.\",\"solutionId\":\"" + solutionId + "\",\"shareContact\":true}", helper);
        assertThat(toSolution.status()).isEqualTo(201);
        assertThat(api.get("/api/v1/problems/" + code + "/contributions", proposer).body())
                .contains("I have a spare cover.").doesNotContain("advice only");

        assertThat(api.get("/api/v1/me/contributions", helper).body()).contains(code).contains("advice only");
        assertThat(api.delete("/api/v1/contributions/" + offerId, owner).status()).isEqualTo(403);
        assertThat(api.delete("/api/v1/contributions/" + offerId, helper).status()).isEqualTo(204);
        assertThat(api.get("/api/v1/problems/" + code + "/contributions", owner).body()).doesNotContain("advice only");

        assertThat(api.post("/api/v1/problems/" + code + "/contributions", "{\"types\":[]}", helper).status()).isEqualTo(400);
        assertThat(api.post("/api/v1/problems/" + code + "/contributions",
                "{\"types\":[\"FUNDING\"],\"solutionId\":\"" + UUID.randomUUID() + "\"}", helper).status()).isEqualTo(404);
    }
}
