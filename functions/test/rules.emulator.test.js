/**
 * Security rules, tested through the real rules engine with @firebase/rules-unit-testing.
 *
 * Run from the repository root (Java 11+ and firebase-tools required, no Firebase project or
 * network login needed - "demo-" project ids are emulator-only):
 *
 *   cd functions && npm ci
 *   npm run test:emulator
 *
 * This suite is authoritative: it runs the same engine production runs. The offline suite
 * (npm test) re-checks the RTDB cases with an evaluator and the Firestore rules structurally, so
 * the important properties can still be verified on a machine with only Node.
 *
 * The access model under test (see firestore.rules, database.rules.json, README-FIREBASE-ONLY.md):
 *
 *   - An owner reaches exactly the tenant whose tenants/{id}.ownerUid is their uid. No claims.
 *   - A website visitor (anonymous auth) reaches exactly the conversation carrying its uid.
 *   - A browser paired by the owner reaches the owner's chats and nothing in Firestore.
 *   - The only signed-out surface is the WordPress plugin's pairing and link-code handshake.
 *   - Everything not named is denied.
 *
 * Every case starts from a freshly seeded database, so a write that succeeds cannot change the
 * outcome of the next case.
 */

const fs = require("fs")
const path = require("path")
const { describe, it, before, after, beforeEach } = require("node:test")
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing")
const {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} = require("firebase/firestore")
const { ref, get, set, remove } = require("firebase/database")
const { FIXTURE, CASES } = require("./rtdb-cases")

const ROOT = path.resolve(__dirname, "..", "..")
const PROJECT_ID = process.env.GCLOUD_PROJECT || "demo-support-chat"

const PAIRING = "pairsession00000000000000000000a"
const NEW_PAIRING = "pairsession00000000000000000000b"

let env

const minutesFromNow = (minutes) => Timestamp.fromMillis(Date.now() + minutes * 60 * 1000)

async function seedFirestore() {
  await env.clearFirestore()
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore()
    const put = (p, data) => setDoc(doc(db, p), data)
    await put("owners/uid_a", { tenantId: "tnt_a" })
    await put("owners/uid_b", { tenantId: "tnt_b" })
    await put("tenants/tnt_a", {
      tenantId: "tnt_a", ownerUid: "uid_a", email: "a@acme.test",
      websiteCount: 0, status: "active", features: ["chat"],
    })
    await put("tenants/tnt_b", {
      tenantId: "tnt_b", ownerUid: "uid_b", email: "b@other.test",
      websiteCount: 1, status: "active", features: ["chat"],
    })
    await put("tenants/tnt_b/websites/web_b", { domain: "other.test", active: true })
    await put("tenants/tnt_a/leads/lead_1", {
      email: "visitor@site.test", websiteId: "web_1", source: "manual",
      conversationCount: 1, conversationIds: ["conv_1"], emailVerified: false,
    })
    await put("tenants/tnt_a/devices/dev_1", { token: "fcm-token" })
    await put("tenants/tnt_a/emailTemplates/tpl_1", { name: "Welcome" })
    await put("publicTenants/tnt_a", { status: "active", features: ["chat"], companyName: "Acme", logoUrl: "" })
    await put("apiKeys/key_a", { tenantId: "tnt_a", websiteId: "web_1", domain: "acme.test", active: true, createdAt: 1 })
    await put(`sitePairings/${PAIRING}`, {
      domain: "acme.test", siteUrl: "https://acme.test", siteName: "Acme",
      status: "waiting", createdAt: 1, expiresAt: minutesFromNow(10),
    })
    const code = (used, expiresAt) => ({
      apiKey: "key_a", tenantId: "tnt_a", websiteId: "web_1", domain: "acme.test",
      createdAt: 1, expiresAt, used,
    })
    await put("linkCodes/FRESH1", code(false, minutesFromNow(10)))
    await put("linkCodes/USED01", code(true, minutesFromNow(10)))
    await put("linkCodes/EXPIRD", code(false, Timestamp.fromMillis(Date.now() - 60 * 1000)))
    await put("coupons/FREE100", { planId: "pro", used: false })
    await put("coupons/SPENT", { planId: "pro", used: true })
    await put("plans/pro", { name: "Pro", tier: 2 })
  })
}

async function seedDatabase() {
  await env.withSecurityRulesDisabled(async (context) => {
    await set(ref(context.database(), "/"), FIXTURE)
  })
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.join(ROOT, "firestore.rules"), "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
    database: {
      rules: fs.readFileSync(path.join(ROOT, "database.rules.json"), "utf8"),
      host: "127.0.0.1",
      port: 9000,
    },
  })
})

after(async () => {
  if (env) await env.cleanup()
})

// Identities. Owners and visitors carry no custom claims: there is no server to mint any.
const as = (uid, claims) => env.authenticatedContext(uid, claims).firestore()
const signedOut = () => env.unauthenticatedContext().firestore()
const ownerA = () => as("uid_a")
const ownerB = () => as("uid_b")
const newUser = () => as("uid_new")
const visitor = () => as("vis_1")
// A visitor who signed in with Google: the provider itself vouches for the address.
const googleVisitor = () => as("vis_g", { email: "visitor@site.test", email_verified: true })

const ok = (promise) => assertSucceeds(promise)
const denied = (promise) => assertFails(promise)

describe("Firestore", () => {
  beforeEach(seedFirestore)

  describe("owners/{uid}: the one-tenant-per-owner lock", () => {
    it("an owner reads their own lock", () => ok(getDoc(doc(ownerA(), "owners/uid_a"))))
    it("an owner cannot read another owner's lock", () => denied(getDoc(doc(ownerA(), "owners/uid_b"))))
    it("nobody lists the locks", () => denied(getDocs(collection(ownerA(), "owners"))))
    it("a new account takes its own lock", () => ok(setDoc(doc(newUser(), "owners/uid_new"), { tenantId: "tnt_new" })))
    it("the lock carries only tenantId", () =>
      denied(setDoc(doc(newUser(), "owners/uid_new"), { tenantId: "tnt_new", plan: "pro" })))
    it("nobody takes a lock in another uid's name", () =>
      denied(setDoc(doc(newUser(), "owners/uid_other"), { tenantId: "tnt_new" })))
    it("a lock can never be rewritten", () => denied(updateDoc(doc(ownerA(), "owners/uid_a"), { tenantId: "tnt_b" })))
    it("a lock can never be deleted", () => denied(deleteDoc(doc(ownerA(), "owners/uid_a"))))
  })

  describe("tenants/{tenantId}", () => {
    const fresh = {
      tenantId: "tnt_new", ownerUid: "uid_new", email: "new@site.test",
      websiteCount: 0, status: "active", features: [],
    }

    it("an owner reads their own tenant", () => ok(getDoc(doc(ownerA(), "tenants/tnt_a"))))
    it("another owner cannot read it", () => denied(getDoc(doc(ownerB(), "tenants/tnt_a"))))
    it("a visitor cannot read it", () => denied(getDoc(doc(visitor(), "tenants/tnt_a"))))
    it("a signed-out client cannot read it", () => denied(getDoc(doc(signedOut(), "tenants/tnt_a"))))
    it("an owner finds their tenant by ownerUid", () =>
      ok(getDocs(query(collection(ownerA(), "tenants"), where("ownerUid", "==", "uid_a")))))
    it("an unfiltered tenant listing is refused", () => denied(getDocs(collection(ownerA(), "tenants"))))

    it("a new account creates its tenant together with its lock", async () => {
      const db = newUser()
      const batch = writeBatch(db)
      batch.set(doc(db, "tenants/tnt_new"), fresh)
      batch.set(doc(db, "owners/uid_new"), { tenantId: "tnt_new" })
      await ok(batch.commit())
    })
    it("an owner who already has a tenant cannot create a second", () =>
      denied(setDoc(doc(ownerA(), "tenants/tnt_second"), { ...fresh, tenantId: "tnt_second", ownerUid: "uid_a" })))
    it("nobody creates a tenant owned by someone else", () =>
      denied(setDoc(doc(newUser(), "tenants/tnt_new"), { ...fresh, ownerUid: "uid_a" })))
    it("a tenant cannot be born with websites already counted", () =>
      denied(setDoc(doc(newUser(), "tenants/tnt_new"), { ...fresh, websiteCount: 5 })))

    it("an owner updates their profile", () => ok(updateDoc(doc(ownerA(), "tenants/tnt_a"), { companyName: "Acme Ltd" })))
    it("ownership can never change hands", () => denied(updateDoc(doc(ownerA(), "tenants/tnt_a"), { ownerUid: "uid_b" })))
    it("the website counter moves one step at a time", () =>
      denied(updateDoc(doc(ownerA(), "tenants/tnt_a"), { websiteCount: 2 })))
    it("another owner cannot update it", () => denied(updateDoc(doc(ownerB(), "tenants/tnt_a"), { companyName: "x" })))
    it("a tenant document is never deleted by a client", () => denied(deleteDoc(doc(ownerA(), "tenants/tnt_a"))))
  })

  describe("tenants/{id}/websites: one per tenant", () => {
    const site = { domain: "acme.test", active: true }
    it("an owner lists their websites", () => ok(getDocs(collection(ownerA(), "tenants/tnt_a/websites"))))
    it("another owner cannot", () => denied(getDocs(collection(ownerB(), "tenants/tnt_a/websites"))))
    it("an owner adds their first website", () => ok(setDoc(doc(ownerA(), "tenants/tnt_a/websites/web_1"), site)))
    it("a second website is refused", () => denied(setDoc(doc(ownerB(), "tenants/tnt_b/websites/web_2"), site)))
    it("another owner cannot add a website here", () => denied(setDoc(doc(ownerB(), "tenants/tnt_a/websites/web_1"), site)))
  })

  describe("tenants/{id}/leads: written by the visitor, read by the owner", () => {
    const lead = {
      email: "new@site.test", websiteId: "web_1", source: "manual",
      conversationCount: 1, conversationIds: ["conv_2"], emailVerified: false,
    }
    it("an owner lists their leads", () => ok(getDocs(collection(ownerA(), "tenants/tnt_a/leads"))))
    it("another owner cannot", () => denied(getDocs(collection(ownerB(), "tenants/tnt_a/leads"))))
    it("a visitor cannot read leads", () => denied(getDoc(doc(visitor(), "tenants/tnt_a/leads/lead_1"))))
    it("a visitor records their own unverified lead", () => ok(setDoc(doc(visitor(), "tenants/tnt_a/leads/lead_2"), lead)))
    it("a signed-out client cannot record a lead", () => denied(setDoc(doc(signedOut(), "tenants/tnt_a/leads/lead_2"), lead)))
    it("a typed email cannot claim to be verified", () =>
      denied(setDoc(doc(visitor(), "tenants/tnt_a/leads/lead_2"), { ...lead, emailVerified: true })))
    it("a provider-verified email may be recorded as verified", () =>
      ok(setDoc(doc(googleVisitor(), "tenants/tnt_a/leads/lead_g"), { ...lead, email: "visitor@site.test", emailVerified: true })))
    it("a verified flag must match the signed-in address", () =>
      denied(setDoc(doc(googleVisitor(), "tenants/tnt_a/leads/lead_g"), { ...lead, email: "someone@else.test", emailVerified: true })))
    it("a returning visitor adds one conversation", () =>
      ok(updateDoc(doc(visitor(), "tenants/tnt_a/leads/lead_1"), { conversationCount: 2, conversationIds: ["conv_1", "conv_2"] })))
    it("the conversation count cannot jump", () =>
      denied(updateDoc(doc(visitor(), "tenants/tnt_a/leads/lead_1"), { conversationCount: 5 })))
    it("an update cannot grant verification", () =>
      denied(updateDoc(doc(visitor(), "tenants/tnt_a/leads/lead_1"), { emailVerified: true })))
    it("an update cannot change the email", () =>
      denied(updateDoc(doc(visitor(), "tenants/tnt_a/leads/lead_1"), { email: "other@site.test" })))
    it("an owner deletes a lead", () => ok(deleteDoc(doc(ownerA(), "tenants/tnt_a/leads/lead_1"))))
  })

  describe("tenants/{id}/devices and emailTemplates: owner only", () => {
    it("an owner registers a device", () => ok(setDoc(doc(ownerA(), "tenants/tnt_a/devices/dev_2"), { token: "t" })))
    it("another owner cannot read devices", () => denied(getDocs(collection(ownerB(), "tenants/tnt_a/devices"))))
    it("a visitor cannot read devices", () => denied(getDoc(doc(visitor(), "tenants/tnt_a/devices/dev_1"))))
    it("an owner writes a template", () => ok(setDoc(doc(ownerA(), "tenants/tnt_a/emailTemplates/tpl_2"), { name: "Bye" })))
    it("another owner cannot write templates", () =>
      denied(setDoc(doc(ownerB(), "tenants/tnt_a/emailTemplates/tpl_2"), { name: "Bye" })))
  })

  describe("publicTenants/{id}: what a visitor may see", () => {
    const pub = { status: "active", features: ["chat"], companyName: "Acme", logoUrl: "" }
    it("a signed-in visitor reads it", () => ok(getDoc(doc(visitor(), "publicTenants/tnt_a"))))
    it("a signed-out client cannot", () => denied(getDoc(doc(signedOut(), "publicTenants/tnt_a"))))
    it("nobody lists them", () => denied(getDocs(collection(visitor(), "publicTenants"))))
    it("an owner publishes their public fields", () => ok(setDoc(doc(ownerA(), "publicTenants/tnt_a"), pub)))
    it("private fields cannot be published", () =>
      denied(setDoc(doc(ownerA(), "publicTenants/tnt_a"), { ...pub, ownerEmail: "a@acme.test" })))
    it("another owner cannot publish for this tenant", () => denied(setDoc(doc(ownerB(), "publicTenants/tnt_a"), pub)))
  })

  describe("apiKeys/{key}: resolvable by id, never enumerable", () => {
    const key = { tenantId: "tnt_a", websiteId: "web_1", domain: "acme.test", active: true, createdAt: 2 }
    it("a signed-in widget resolves a key it holds", () => ok(getDoc(doc(visitor(), "apiKeys/key_a"))))
    it("a signed-out client cannot", () => denied(getDoc(doc(signedOut(), "apiKeys/key_a"))))
    it("nobody lists keys", () => denied(getDocs(collection(ownerA(), "apiKeys"))))
    it("an owner creates a key for their tenant", () => ok(setDoc(doc(ownerA(), "apiKeys/key_new"), key)))
    it("nobody creates a key for someone else's tenant", () =>
      denied(setDoc(doc(ownerB(), "apiKeys/key_new"), key)))
    it("a key is never edited in place", () => denied(updateDoc(doc(ownerA(), "apiKeys/key_a"), { active: false })))
    it("an owner revokes their key", () => ok(deleteDoc(doc(ownerA(), "apiKeys/key_a"))))
    it("another owner cannot revoke it", () => denied(deleteDoc(doc(ownerB(), "apiKeys/key_a"))))
  })

  describe("sitePairings/{session}: the plugin's scan-to-link handshake", () => {
    const session = (overrides = {}) => ({
      domain: "acme.test", siteUrl: "https://acme.test", siteName: "Acme",
      status: "waiting", createdAt: 1, expiresAt: minutesFromNow(10), ...overrides,
    })
    it("a signed-out site reads its session", () => ok(getDoc(doc(signedOut(), `sitePairings/${PAIRING}`))))
    it("nobody lists sessions", () => denied(getDocs(collection(ownerA(), "sitePairings"))))
    it("a signed-out site opens a waiting session", () => ok(setDoc(doc(signedOut(), `sitePairings/${NEW_PAIRING}`), session())))
    it("a session cannot be born answered", () =>
      denied(setDoc(doc(signedOut(), `sitePairings/${NEW_PAIRING}`), session({ status: "ready" }))))
    it("a guessable short session id is refused", () => denied(setDoc(doc(signedOut(), "sitePairings/short"), session())))
    it("a session cannot outlive fifteen minutes", () =>
      denied(setDoc(doc(signedOut(), `sitePairings/${NEW_PAIRING}`), session({ expiresAt: minutesFromNow(60) }))))
    it("the app answers with a link code", () =>
      ok(updateDoc(doc(ownerA(), `sitePairings/${PAIRING}`), { status: "ready", linkCode: "FRESH1", ownerEmail: "a@acme.test", answeredAt: 2 })))
    it("an answer cannot rewrite the domain", () =>
      denied(updateDoc(doc(ownerA(), `sitePairings/${PAIRING}`), { status: "ready", linkCode: "FRESH1", domain: "evil.test" })))
    it("a signed-out client cannot answer", () =>
      denied(updateDoc(doc(signedOut(), `sitePairings/${PAIRING}`), { status: "ready", linkCode: "FRESH1" })))
    it("the site closes its session", () => ok(updateDoc(doc(signedOut(), `sitePairings/${PAIRING}`), { status: "done" })))
    it("closing cannot touch other fields", () =>
      denied(updateDoc(doc(signedOut(), `sitePairings/${PAIRING}`), { status: "done", linkCode: "X" })))
    it("a session is never deleted by a client", () => denied(deleteDoc(doc(ownerA(), `sitePairings/${PAIRING}`))))
  })

  describe("linkCodes/{code}: single use, ten minutes", () => {
    const fresh = {
      apiKey: "key_a", tenantId: "tnt_a", websiteId: "web_1", domain: "acme.test",
      createdAt: 2, expiresAt: minutesFromNow(10), used: false,
    }
    it("a signed-out plugin reads a code", () => ok(getDoc(doc(signedOut(), "linkCodes/FRESH1"))))
    it("nobody lists codes", () => denied(getDocs(collection(signedOut(), "linkCodes"))))
    it("an owner issues a code for their tenant", () => ok(setDoc(doc(ownerA(), "linkCodes/NEWONE"), fresh)))
    it("nobody issues a code for someone else's tenant", () => denied(setDoc(doc(ownerB(), "linkCodes/NEWONE"), fresh)))
    it("the plugin burns a fresh code", () => ok(updateDoc(doc(signedOut(), "linkCodes/FRESH1"), { used: true })))
    it("a used code cannot be burned again", () => denied(updateDoc(doc(signedOut(), "linkCodes/USED01"), { used: true })))
    it("an expired code cannot be burned", () => denied(updateDoc(doc(signedOut(), "linkCodes/EXPIRD"), { used: true })))
    it("burning cannot rewrite the key it points at", () =>
      denied(updateDoc(doc(signedOut(), "linkCodes/FRESH1"), { used: true, apiKey: "stolen" })))
    it("a code is never deleted by a client", () => denied(deleteDoc(doc(ownerA(), "linkCodes/FRESH1"))))
  })

  describe("coupons/{code}: single-use activation", () => {
    it("a signed-in owner reads a coupon", () => ok(getDoc(doc(ownerA(), "coupons/FREE100"))))
    it("a signed-out client cannot", () => denied(getDoc(doc(signedOut(), "coupons/FREE100"))))
    it("nobody lists coupons", () => denied(getDocs(collection(ownerA(), "coupons"))))
    it("an unused coupon is redeemed once", () => ok(updateDoc(doc(ownerA(), "coupons/FREE100"), { used: true })))
    it("a spent coupon cannot be redeemed again", () => denied(updateDoc(doc(ownerA(), "coupons/SPENT"), { used: true })))
    it("redeeming cannot switch the plan", () =>
      denied(updateDoc(doc(ownerA(), "coupons/FREE100"), { used: true, planId: "enterprise" })))
    it("clients cannot mint coupons", () => denied(setDoc(doc(ownerA(), "coupons/MINE"), { planId: "pro", used: false })))
    it("clients cannot delete coupons", () => denied(deleteDoc(doc(ownerA(), "coupons/FREE100"))))
  })

  describe("plans and everything else", () => {
    it("a signed-in client lists plans", () => ok(getDocs(collection(visitor(), "plans"))))
    it("a signed-out client cannot read plans", () => denied(getDoc(doc(signedOut(), "plans/pro"))))
    it("plans are read-only", () => denied(setDoc(doc(ownerA(), "plans/pro"), { name: "Free" })))
    it("an unnamed collection cannot be read", () => denied(getDoc(doc(ownerA(), "anything/else"))))
    it("an unnamed collection cannot be written", () => denied(setDoc(doc(ownerA(), "anything/else"), { x: 1 })))
  })
})

describe("Realtime Database (shared cases, test/rtdb-cases.js)", () => {
  beforeEach(seedDatabase)

  for (const testCase of CASES) {
    it(testCase.name, async () => {
      const context = testCase.as ? env.authenticatedContext(testCase.as) : env.unauthenticatedContext()
      const target = ref(context.database(), testCase.path)
      let action
      if (testCase.op === "read") action = get(target)
      else if (testCase.op === "remove") action = remove(target)
      else action = set(target, testCase.value)
      await (testCase.expect ? assertSucceeds(action) : assertFails(action))
    })
  }
})
