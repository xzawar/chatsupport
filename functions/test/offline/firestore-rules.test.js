/**
 * Firestore rules: structural guards, checked offline against the real firestore.rules.
 *
 * Behaviour - who can actually read and write what - is tested through the real rules engine in
 * test/rules.emulator.test.js, which is authoritative. Firestore's rules language (CEL, with
 * get()/exists(), map diffs and timestamps) is not something to re-implement here, so this file
 * instead pins down the SHAPE of the access model, so that a risky edit to the rules fails fast
 * on any machine with Node, before anyone runs the emulator:
 *
 *   - the set of collections is exactly the known inventory (a new one needs its tests written);
 *   - nothing matches recursively, so anything unnamed stays denied by default;
 *   - the unauthenticated surface is exactly the WordPress pairing and link-code handshake;
 *   - collections whose document ids are capabilities can never be listed;
 *   - documents that must survive the client are never deletable by it;
 *   - everything under a tenant needs isOwner(tenantId), apart from the visitor's own lead;
 *   - ownership is derived from tenants/{id}.ownerUid, never from a custom claim;
 *   - the one-tenant, one-website, single-use-coupon and burn-once-link-code locks are present.
 *
 * These are deliberately strict text checks: if one of them fails after an intentional rules
 * change, update the expectation here in the same change, so the review sees both.
 */

const fs = require("fs")
const path = require("path")
const { suite, test, assertEqual, assert } = require("./harness")

const RULES_PATH = path.resolve(__dirname, "../../../firestore.rules")
const source = fs.readFileSync(RULES_PATH, "utf8")

/** Comments stripped, so prose such as "allow list is refused" cannot be read as a rule. */
const code = source
  .replace(/\/\*[\s\S]*?\*\//g, "")
  .split("\n")
  .map((line) => line.replace(/\/\/.*$/, ""))
  .join("\n")

/**
 * Walks the file once, tracking which match block each allow statement belongs to.
 * Returns { matches: [fullPath], statements: [{ path, operations, condition }], functions }.
 */
function parse(text) {
  const stack = []
  const matches = []
  const statements = []
  const functions = {}
  let pending = null
  let currentFunction = null

  for (const rawLine of text.split("\n")) {
    const line = rawLine.trim()
    if (!line) continue

    if (pending) {
      pending.text += ` ${line}`
      if (line.includes(";")) {
        statements.push(finish(pending))
        pending = null
      }
      continue
    }

    const matchBlock = line.match(/^match\s+(\S+)\s*\{$/)
    if (matchBlock) {
      const parent = stack.filter((frame) => frame.type === "match").map((frame) => frame.path).join("")
      const full = parent + matchBlock[1]
      stack.push({ type: "match", path: matchBlock[1] })
      matches.push(full)
      continue
    }

    const fn = line.match(/^function\s+(\w+)\s*\(([^)]*)\)\s*\{$/)
    if (fn) {
      stack.push({ type: "function" })
      currentFunction = fn[1]
      functions[currentFunction] = ""
      continue
    }

    if (/^service\s+.*\{$/.test(line)) {
      stack.push({ type: "service" })
      continue
    }

    if (line === "}") {
      const frame = stack.pop()
      if (frame && frame.type === "function") currentFunction = null
      continue
    }

    if (currentFunction) {
      functions[currentFunction] += ` ${line}`
      continue
    }

    if (line.startsWith("allow ")) {
      const at = stack.filter((frame) => frame.type === "match").map((frame) => frame.path).join("")
      pending = { path: at, text: line }
      if (line.includes(";")) {
        statements.push(finish(pending))
        pending = null
      }
    }
  }
  return { matches, statements, functions }
}

function finish({ path: at, text }) {
  const m = text.match(/^allow\s+([a-z,\s]+):\s*if\s+([\s\S]+);\s*$/)
  if (!m) throw new Error(`could not parse rule: ${text}`)
  return {
    path: at.replace(/^\/databases\/\{database\}\/documents/, ""),
    operations: m[1].split(",").map((op) => op.trim()).filter(Boolean),
    condition: m[2].replace(/\s+/g, " ").trim(),
  }
}

const parsed = parse(code)

/** The concrete operations a statement's operation list covers. */
function expand(operations) {
  const out = new Set()
  for (const op of operations) {
    if (op === "read") ["get", "list"].forEach((o) => out.add(o))
    else if (op === "write") ["create", "update", "delete"].forEach((o) => out.add(o))
    else out.add(op)
  }
  return out
}

/** Statements on exactly this match path that could ever grant `operation`. */
function grants(matchPath, operation) {
  return parsed.statements.filter(
    (s) => s.path === matchPath && expand(s.operations).has(operation) && s.condition !== "false",
  )
}

const AUTH_GUARDS = ["signedIn()", "isOwner(", "providerVerified("]
const requiresAuth = (condition) => AUTH_GUARDS.some((guard) => condition.includes(guard))

const INVENTORY = [
  "/databases/{database}/documents",
  "/databases/{database}/documents/owners/{ownerUid}",
  "/databases/{database}/documents/tenants/{tenantId}",
  "/databases/{database}/documents/tenants/{tenantId}/websites/{websiteId}",
  "/databases/{database}/documents/tenants/{tenantId}/leads/{leadId}",
  "/databases/{database}/documents/tenants/{tenantId}/devices/{deviceId}",
  "/databases/{database}/documents/tenants/{tenantId}/emailTemplates/{templateId}",
  "/databases/{database}/documents/publicTenants/{tenantId}",
  "/databases/{database}/documents/apiKeys/{apiKey}",
  "/databases/{database}/documents/sitePairings/{session}",
  "/databases/{database}/documents/linkCodes/{code}",
  "/databases/{database}/documents/coupons/{code}",
  "/databases/{database}/documents/plans/{planId}",
]

/** The whole signed-out surface: the WordPress plugin has no Firebase account at all. */
const UNAUTHENTICATED_SURFACE = [
  "/linkCodes/{code}:get",
  "/linkCodes/{code}:update",
  "/sitePairings/{session}:create",
  "/sitePairings/{session}:get",
  "/sitePairings/{session}:update",
]

module.exports = async function run() {
  suite("Firestore rules (firestore.rules)")

  await test("the rules parse into match blocks and allow statements", () => {
    assert(parsed.matches.length > 0, "no match blocks parsed")
    assert(parsed.statements.length > 0, "no allow statements parsed")
    assert(source.includes("rules_version = '2'"), "rules_version 2 is required for list/get split")
  })

  await test("the collections are exactly the known inventory", () => {
    assertEqual(
      JSON.stringify([...parsed.matches].sort()),
      JSON.stringify([...INVENTORY].sort()),
      "match paths (a new collection needs its access tests written first)",
    )
  })

  await test("nothing matches recursively, so unnamed paths stay denied", () => {
    for (const target of parsed.matches) {
      assert(!/=\*\*/.test(target), `recursive wildcard in ${target}`)
    }
  })

  await test("the signed-out surface is exactly the pairing and link-code handshake", () => {
    const surface = []
    for (const s of parsed.statements) {
      if (s.condition === "false" || requiresAuth(s.condition)) continue
      for (const op of expand(s.operations)) surface.push(`${s.path}:${op}`)
    }
    assertEqual(
      JSON.stringify([...new Set(surface)].sort()),
      JSON.stringify(UNAUTHENTICATED_SURFACE),
      "operations allowed without signing in",
    )
  })

  await test("signed-out writes are pinned to a single field flip or a fresh waiting session", () => {
    const burn = grants("/linkCodes/{code}", "update").find((s) => !requiresAuth(s.condition))
    assert(burn, "no link-code burn rule")
    for (const part of [
      "resource.data.used == false",
      "request.time < resource.data.expiresAt",
      "request.resource.data.used == true",
      "affectedKeys().hasOnly(['used'])",
    ]) {
      assert(burn.condition.includes(part), `link-code burn is missing: ${part}`)
    }

    const open = grants("/sitePairings/{session}", "create")[0]
    assert(open, "no pairing create rule")
    for (const part of [
      "session.size() >= 24",
      "request.resource.data.status == 'waiting'",
      "request.resource.data.expiresAt < request.time + duration.value(15, 'm')",
      "keys().hasOnly(",
    ]) {
      assert(open.condition.includes(part), `pairing create is missing: ${part}`)
    }

    const close = grants("/sitePairings/{session}", "update").find((s) => !requiresAuth(s.condition))
    assert(close, "no pairing close rule")
    assert(close.condition.includes("affectedKeys().hasOnly(['status'])"), "close may touch more than status")
    assert(close.condition.includes("['done', 'cancelled']"), "close may move status anywhere")
  })

  await test("capability collections can never be listed", () => {
    for (const collection of [
      "/owners/{ownerUid}",
      "/publicTenants/{tenantId}",
      "/apiKeys/{apiKey}",
      "/sitePairings/{session}",
      "/linkCodes/{code}",
      "/coupons/{code}",
    ]) {
      assertEqual(grants(collection, "list").length, 0, `list granted on ${collection}`)
    }
  })

  await test("records the client must not destroy are never deletable by it", () => {
    for (const collection of [
      "/owners/{ownerUid}",
      "/tenants/{tenantId}",
      "/sitePairings/{session}",
      "/linkCodes/{code}",
      "/coupons/{code}",
    ]) {
      assertEqual(grants(collection, "delete").length, 0, `delete granted on ${collection}`)
    }
    assertEqual(grants("/owners/{ownerUid}", "update").length, 0, "owners lock can be rewritten")
    assertEqual(grants("/apiKeys/{apiKey}", "update").length, 0, "api keys can be edited in place")
    assertEqual(grants("/coupons/{code}", "create").length, 0, "clients can mint coupons")
    for (const op of ["create", "update", "delete"]) {
      assertEqual(grants("/plans/{planId}", op).length, 0, `plans ${op} is client-writable`)
    }
  })

  await test("everything under a tenant needs isOwner(tenantId), except the visitor's own lead", () => {
    const visitorLeadWrites = new Set(["/tenants/{tenantId}/leads/{leadId}:create", "/tenants/{tenantId}/leads/{leadId}:update"])
    for (const s of parsed.statements) {
      if (!s.path.startsWith("/tenants/{tenantId}/") || s.condition === "false") continue
      for (const op of expand(s.operations)) {
        if (visitorLeadWrites.has(`${s.path}:${op}`)) continue
        assert(s.condition.includes("isOwner(tenantId)"), `${s.path} ${op} is not owner-only: ${s.condition}`)
      }
    }
  })

  await test("a visitor's lead can never claim a verified email it was not given", () => {
    for (const op of ["create", "update"]) {
      const [rule] = grants("/tenants/{tenantId}/leads/{leadId}", op)
      assert(rule, `no lead ${op} rule`)
      assert(rule.condition.includes("providerVerified(request.resource.data.email)"), `lead ${op} lacks the provider check`)
    }
    const body = parsed.functions.providerVerified || ""
    assert(body.includes("request.auth.token.email_verified == true"), "providerVerified ignores email_verified")
    assert(body.includes("request.auth.token.email.lower() == email.lower()"), "providerVerified ignores the address")
  })

  await test("ownership comes from tenants/{id}.ownerUid, never a custom claim", () => {
    const body = parsed.functions.isOwner || ""
    assert(body.includes("tenantDoc(tenantId).ownerUid == request.auth.uid"), "isOwner does not read ownerUid")
    assert(body.includes("exists("), "isOwner does not require the tenant to exist")

    const claims = new Set([
      ...[...code.matchAll(/request\.auth\.token\.(\w+)/g)].map((m) => m[1]),
      ...[...code.matchAll(/'(\w+)' in request\.auth\.token/g)].map((m) => m[1]),
    ])
    assertEqual(
      JSON.stringify([...claims].sort()),
      JSON.stringify(["email", "email_verified"]),
      "token fields read by the rules (only the provider's own email fields are allowed)",
    )
    assert(!/\brole\b/.test(code), "rules mention a role")
  })

  await test("tenant identity is immutable and one tenant per owner holds", () => {
    const [update] = grants("/tenants/{tenantId}", "update")
    assert(update, "no tenant update rule")
    assert(update.condition.includes("request.resource.data.ownerUid == resource.data.ownerUid"), "ownerUid can change")
    assert(update.condition.includes("request.resource.data.tenantId == resource.data.tenantId"), "tenantId can change")
    assert(update.condition.includes("websiteCount <= resource.data.websiteCount + 1"), "websiteCount can jump")

    const [create] = grants("/tenants/{tenantId}", "create")
    assert(create, "no tenant create rule")
    assert(create.condition.includes("request.resource.data.ownerUid == request.auth.uid"), "tenant can be created for someone else")
    assert(
      create.condition.includes("!exists(/databases/$(database)/documents/owners/$(request.auth.uid))"),
      "a second tenant per owner is not refused",
    )

    const [ownerLock] = grants("/owners/{ownerUid}", "create")
    assert(ownerLock && ownerLock.condition.includes("request.auth.uid == ownerUid"), "owner lock can be taken for another uid")
  })

  await test("one website per tenant and single-use coupons hold", () => {
    const [website] = grants("/tenants/{tenantId}/websites/{websiteId}", "create")
    assert(website && website.condition.includes("tenantDoc(tenantId).websiteCount == 0"), "second website not refused")

    const [redeem] = grants("/coupons/{code}", "update")
    assert(redeem, "no coupon redeem rule")
    for (const part of [
      "resource.data.used == false",
      "request.resource.data.used == true",
      "request.resource.data.planId == resource.data.planId",
    ]) {
      assert(redeem.condition.includes(part), `coupon redeem is missing: ${part}`)
    }
  })
}
