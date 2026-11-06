package org.udaan.collab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.udaan.TestApi;
import org.udaan.TestApi.Response;

/** Status tracking, resolution evidence, community confirmation and notifications, end to end. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"udaan.environment=ci"})
class LifecycleNotificationsTest {

    @Value("${local.server.port}")
    int port;

    TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(port);
    }

    String raise(String token) {
        Response r = api.post("/api/v1/problems", """
                {"title":"Streetlight out at the bus stop","description":"The only streetlight at the Kapoorthala bus stop has been dark for a month.",
                 "categoryId":"0193f1a0-0000-7000-8000-000000000001","cityId":1,"localityText":"Kapoorthala",
                 "urgency":"MEDIUM","desiredOutcome":"A working light at the stop","assistanceTypes":["GOVERNMENT_SUPPORT"]}
                """, token);
        assertThat(r.status()).as(r.toString()).isEqualTo(201);
        return r.field("publicCode");
    }

    String photo(String token) {
        return api.upload("/api/v1/media", "evidence.png", "image/png", org.udaan.problem.TestImagesAccess.png(), token).field("id");
    }

    String transition(String code, String token, String to, String note, String photoId) {
        String evidence = photoId == null ? "" : ",\"evidenceMediaIds\":[\"" + photoId + "\"]";
        String noteJson = note == null ? "" : ",\"note\":\"" + note + "\"";
        Response r = api.post("/api/v1/problems/" + code + "/status-transitions", "{\"toStatus\":\"" + to + "\"" + noteJson + evidence + "}", token);
        // "200 IN_PROGRESS" on success, "403 NOT_ALLOWED" style on failure.
        return r.status() + " " + (r.status() >= 400 ? r.field("code") : r.field("status"));
    }

    @Test
    void problemsMoveFromAcceptedToResolvedWithEvidenceAndPeopleAreNotified() {
        String owner = api.registerNewUser("Owner");
        String proposer = api.registerNewUser("Proposer");
        String follower = api.registerNewUser("Follower");
        String stranger = api.registerNewUser("Stranger");
        String code = raise(owner);

        Response ownerInbox = api.get("/api/v1/notifications", owner);
        assertThat(ownerInbox.status()).as(ownerInbox.toString()).isEqualTo(200);
        assertThat(ownerInbox.body()).contains("PROBLEM_PUBLISHED");

        assertThat(api.put("/api/v1/problems/" + code + "/follow", follower).status()).isEqualTo(200);
        String solutionId = api.post("/api/v1/problems/" + code + "/solutions", """
                {"title":"Ask the ward office to replace the bulb","summary":"File a request with the ward office and follow up weekly until it is fixed.",
                 "approach":"Write to the ward office, attach photos, and escalate to the councillor after one week."}
                """, proposer).field("id");
        assertThat(api.get("/api/v1/notifications", owner).body()).contains("SOLUTION_PROPOSED").contains("Proposer proposed a solution");
        assertThat(api.get("/api/v1/notifications", follower).body()).contains("SOLUTION_PROPOSED");
        assertThat(api.get("/api/v1/notifications", proposer).body()).as("never notified about your own action").doesNotContain("SOLUTION_PROPOSED");

        assertThat(api.post("/api/v1/solutions/" + solutionId + "/accept", "{}", owner).status()).isEqualTo(200);
        assertThat(api.get("/api/v1/notifications", proposer).body()).contains("Your solution was accepted");

        Response asProposer = api.get("/api/v1/problems/" + code, proposer);
        assertThat(asProposer.body()).contains("\"canPostUpdates\":true").contains("\"allowedTransitions\":[\"IN_PROGRESS\"]");

        assertThat(transition(code, stranger, "IN_PROGRESS", null, null)).startsWith("403");
        assertThat(transition(code, proposer, "IN_PROGRESS", null, null)).isEqualTo("200 IN_PROGRESS");
        assertThat(api.get("/api/v1/notifications", follower).body()).contains("is now in progress");

        String progressPhoto = photo(proposer);
        Response update = api.post("/api/v1/problems/" + code + "/updates",
                "{\"body\":\"Ward office says the new pole arrives Friday.\",\"mediaIds\":[\"" + progressPhoto + "\"]}", proposer);
        assertThat(update.status()).as(update.toString()).isEqualTo(201);
        assertThat(update.body()).contains("Ward office says the new pole arrives Friday.").contains(progressPhoto).contains("ACCEPTED_PROPOSER");
        assertThat(api.get("/api/v1/notifications", owner).body()).contains("PROGRESS_UPDATE");
        assertThat(api.post("/api/v1/problems/" + code + "/updates", "{\"body\":\"I am not involved\"}", stranger).status()).isEqualTo(403);

        assertThat(transition(code, proposer, "RESOLVED", "Light replaced and working", photo(proposer))).as("only owners resolve").startsWith("409");
        assertThat(transition(code, owner, "RESOLVED", "Light replaced and working", null)).startsWith("400 VALIDATION_FAILED");
        assertThat(transition(code, owner, "RESOLVED", "short", photo(owner))).startsWith("400 VALIDATION_FAILED");
        assertThat(transition(code, owner, "RESOLVED", "New light installed on Friday evening", photo(owner))).isEqualTo("200 RESOLVED");

        Response resolved = api.get("/api/v1/problems/" + code, follower);
        assertThat(resolved.body()).contains("\"votingOpen\":true").contains("New light installed on Friday evening");
        assertThat(api.get("/api/v1/notifications", follower).body()).contains("is now resolved");
    }

    @Test
    void enoughDisputesReopenAResolvedProblem() {
        String owner = api.registerNewUser("Owner");
        String code = raise(owner);
        assertThat(transition(code, owner, "RESOLVED", "Fixed by the electricity board", photo(owner))).isEqualTo("200 RESOLVED");

        Response ownVote = api.post("/api/v1/problems/" + code + "/resolution-votes", "{\"vote\":\"CONFIRM\"}", owner);
        assertThat(ownVote.status()).isEqualTo(400);

        String confirmer = api.registerNewUser("Confirmer");
        assertThat(api.post("/api/v1/problems/" + code + "/resolution-votes", "{\"vote\":\"CONFIRM\"}", confirmer).body())
                .contains("\"confirms\":1").contains("\"viewerVote\":\"CONFIRM\"");

        for (int i = 0; i < 2; i++) {
            Response r = api.post("/api/v1/problems/" + code + "/resolution-votes", "{\"vote\":\"DISPUTE\"}", api.registerNewUser("Neighbour"));
            assertThat(r.field("status")).isEqualTo("RESOLVED");
        }
        Response third = api.post("/api/v1/problems/" + code + "/resolution-votes", "{\"vote\":\"DISPUTE\"}", api.registerNewUser("Neighbour"));
        assertThat(third.status()).as(third.toString()).isEqualTo(200);
        assertThat(third.field("status")).isEqualTo("IN_PROGRESS");
        assertThat(third.body()).contains("Reopened: 3 people said the problem is not resolved");
        assertThat(api.get("/api/v1/notifications", owner).body()).contains("is now in progress");

        assertThat(api.post("/api/v1/problems/" + code + "/resolution-votes", "{\"vote\":\"DISPUTE\"}", confirmer).status())
                .as("voting closes once reopened").isEqualTo(409);
    }

    @Test
    void commentsRepliesAndOffersNotifyTheRightPeopleAndCanBeMarkedRead() {
        String owner = api.registerNewUser("Owner");
        String alice = api.registerNewUser("Alice");
        String bob = api.registerNewUser("Bob");
        String code = raise(owner);

        String top = api.post("/api/v1/problems/" + code + "/comments", "{\"body\":\"I saw the same last night.\"}", alice).field("id");
        api.post("/api/v1/problems/" + code + "/comments", "{\"body\":\"Me too.\",\"parentId\":\"" + top + "\"}", bob);
        assertThat(api.get("/api/v1/notifications", alice).body()).contains("COMMENT_REPLY").contains("Bob replied to your comment");
        assertThat(api.get("/api/v1/notifications", owner).body()).contains("Alice commented on").contains("Bob commented on");

        api.post("/api/v1/problems/" + code + "/contributions", "{\"types\":[\"VOLUNTEERING\"]}", bob);
        api.post("/api/v1/problems/" + code + "/contributions", "{\"types\":[\"EXPERTISE\"]}", bob);
        Response inbox = api.get("/api/v1/notifications", owner);
        assertThat(inbox.body().split("HELP_OFFERED", -1).length - 1).as("updating an offer doesn't notify again").isEqualTo(1);

        long unread = api.get("/api/v1/notifications/unread-count", owner).number("unreadCount");
        assertThat(unread).isGreaterThanOrEqualTo(4);
        String oneId = inbox.field("id");
        assertThat(api.post("/api/v1/notifications/read", "{\"ids\":[\"" + oneId + "\"]}", owner).number("unreadCount")).isEqualTo(unread - 1);
        assertThat(api.post("/api/v1/notifications/read", "{}", owner).number("unreadCount")).isZero();
        assertThat(api.get("/api/v1/notifications", alice).body()).as("other users unaffected").contains("\"read\":false");
        assertThat(api.get("/api/v1/notifications").status()).isEqualTo(401);
    }
}
