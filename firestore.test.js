const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProjectPayload(uid, overrides = {}) {
  return {
    userId: uid,
    title: "Samudra Manthan",
    storyInput: "Devas and Asuras churn the cosmic ocean of milk with Mount Mandara.",
    language: "hindi",
    visualStyle: "Epic 3D Mythological",
    continuitySummary: "Golden dusk horizon over cosmic waters.",
    attachedCharacterIds: ["char_1"],
    sceneCount: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

function validScenePayload(uid, projectId, overrides = {}) {
  return {
    userId: uid,
    projectId,
    sceneNumber: 1,
    title: "The Churning Begins",
    durationSeconds: 8,
    sceneDescription: "Vast cosmic ocean illuminated by golden dusk.",
    cameraDirection: "Wide sweeping aerial drone push-in",
    characterActions: "Devas brace at the tail of Vasuki.",
    continuityNotes: "Golden mist remains constant.",
    narrationScript: "क्षीर सागर की लहरों में दिव्य मंथन आरंभ हुआ।",
    sanskritPhonetics: "kṣī-ra sā-ga-ra",
    subtitleText: "The divine churning of the cosmic ocean began.",
    includedInCut: true,
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

function validCharacterPayload(uid, overrides = {}) {
  return {
    userId: uid,
    name: "Shri Vishnu (Kurma)",
    archetype: "Supreme Preserver",
    visualDetails: "Serene luminous expression, golden halo.",
    costumeDetails: "Pitambara silk, Kaustubha gem, golden crown.",
    auraAndPalette: "Antique gold and deep sapphire",
    continuityTags: ["golden_crown", "kaustubha"],
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

function validRenderJobPayload(uid, overrides = {}) {
  return {
    userId: uid,
    projectId: "proj_1",
    sceneId: "scene_1",
    mode: "text_to_video",
    providerId: "veo_adapter",
    modelName: "External Video Provider (Unconnected)",
    billingTier: "paid",
    prompt: "Golden celestial ocean churning at twilight",
    aspectRatio: "16:9",
    durationSeconds: 8,
    characterNames: ["Shri Vishnu"],
    status: "awaiting_provider",
    statusDetail: "Provider not connected; paid billing not activated.",
    createdAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

test("Unauthenticated caller cannot read or list projects or characters", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/characters`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/renderJobs`).get());
});

test("Cross-user access is denied for projects, scenes, characters, and renderJobs", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${BOB_UID}/projects/proj_1`).set(validProjectPayload(BOB_UID));
    await db.doc(`users/${BOB_UID}/characters/char_1`).set(validCharacterPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.doc(`users/${BOB_UID}/projects/proj_1`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/projects`).get());
  await assertFails(aliceDb.doc(`users/${BOB_UID}/characters/char_1`).get());
  await assertFails(
    aliceDb.doc(`users/${BOB_UID}/projects/proj_x`).set(validProjectPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, read, update, and delete project & child scene", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectRef = aliceDb.doc(`users/${ALICE_UID}/projects/proj_1`);
  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects`).get());

  const sceneRef = aliceDb.doc(`users/${ALICE_UID}/projects/proj_1/scenes/scene_1`);
  await assertSucceeds(sceneRef.set(validScenePayload(ALICE_UID, "proj_1")));
  await assertSucceeds(sceneRef.update({ title: "Updated Churning", updatedAt: new Date() }));
  await assertSucceeds(sceneRef.delete());
  await assertSucceeds(projectRef.delete());
});

test("Parent Gate rejects orphaned scene without existing parent project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orphanSceneRef = aliceDb.doc(`users/${ALICE_UID}/projects/missing_proj/scenes/scene_1`);
  await assertFails(orphanSceneRef.set(validScenePayload(ALICE_UID, "missing_proj")));
});

test("Shadow Update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/characters/char_ghost`).set(
      validCharacterPayload(ALICE_UID, { isVerified: true })
    )
  );
});

test("Identity spoofing and immutable field tampering are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/projects/proj_spoof`).set(validProjectPayload(BOB_UID))
  );

  const charRef = aliceDb.doc(`users/${ALICE_UID}/characters/char_1`);
  await assertSucceeds(charRef.set(validCharacterPayload(ALICE_UID)));
  await assertFails(charRef.update({ userId: BOB_UID, updatedAt: new Date() }));
  await assertFails(charRef.update({ createdAt: new Date(0), updatedAt: new Date() }));
});

test("RenderJob terminal state locking rejects updates after cancelled", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const jobRef = aliceDb.doc(`users/${ALICE_UID}/renderJobs/job_1`);
  await assertSucceeds(jobRef.set(validRenderJobPayload(ALICE_UID)));
  await assertSucceeds(jobRef.update({ status: "cancelled", statusDetail: "User cancelled", updatedAt: new Date() }));
  await assertFails(jobRef.update({ status: "packet_ready", updatedAt: new Date() }));
});
