/**
 * RTDB rules, evaluated offline against the real database.rules.json.
 *
 * The emulator suite (test/rules.emulator.test.js) is authoritative. This file re-checks the same
 * cases (test/rtdb-cases.js) with no emulator, no Java and no network, by evaluating the rule
 * expressions directly. RTDB rule expressions are a subset of JavaScript - `===`, `&&`, `||`, `!`,
 * string `.length`, and the snapshot methods - so they can be run for real rather than
 * approximated, provided the snapshot API is modelled faithfully:
 *
 *   - `.read` and `.write` cascade downward: a grant at the node or any ancestor allows the
 *     operation, and nothing below can take it back. Absent everywhere means denied.
 *   - `.validate` runs on the written node and every node beneath it in the new data, never on a
 *     deletion. A `.validate` of false (the `$other` catch-alls) refuses unknown children.
 *   - `$wildcards` bind path segments and are in scope as variables.
 *   - An expression that throws on a missing value (a null `.length`) evaluates to false, as in
 *     RTDB. An expression that uses an identifier or a snapshot method this evaluator does not
 *     model fails the test loudly instead of quietly evaluating to false.
 *
 * The rules file carries `//` comment lines, which the Firebase CLI accepts and JSON.parse does
 * not; they are stripped before parsing.
 */

const fs = require("fs")
const path = require("path")
const { suite, test, assertEqual, assert } = require("./harness")
const { FIXTURE, CASES } = require("../rtdb-cases")

const RULES_PATH = path.resolve(__dirname, "../../../database.rules.json")
const RULES_SOURCE = fs.readFileSync(RULES_PATH, "utf8")

/** Drops whole-line `//` comments. Rules strings never start a line, so this cannot cut one. */
function stripComments(source) {
  return source
    .split("\n")
    .filter((line) => !line.trim().startsWith("//"))
    .join("\n")
}

let rules = null
let parseError = null
try {
  rules = JSON.parse(stripComments(RULES_SOURCE)).rules
} catch (error) {
  parseError = error
}

// --------------------------------------------------------------------------- data model

const clone = (value) => (value === undefined ? null : JSON.parse(JSON.stringify(value)))

function valueAt(state, segments) {
  let node = state
  for (const segment of segments) {
    if (node === null || typeof node !== "object" || !(segment in node)) return null
    node = node[segment]
  }
  return node === undefined ? null : node
}

/** A new state with `value` written at `segments` (null deletes), pruning emptied parents. */
function writeAt(state, segments, value) {
  const next = clone(state) || {}
  if (segments.length === 0) return clone(value)
  let node = next
  const parents = []
  for (const segment of segments.slice(0, -1)) {
    if (node[segment] === null || typeof node[segment] !== "object") node[segment] = {}
    parents.push([node, segment])
    node = node[segment]
  }
  const leaf = segments[segments.length - 1]
  if (value === null) delete node[leaf]
  else node[leaf] = clone(value)
  for (let i = parents.length - 1; i >= 0; i -= 1) {
    const [parent, key] = parents[i]
    if (Object.keys(parent[key]).length === 0) delete parent[key]
  }
  return next
}

const UNMODELLED = Symbol("unmodelled")

/** The RuleDataSnapshot API, over a plain object state. */
class Snapshot {
  constructor(state, segments) {
    this.state = state
    this.segments = segments
  }

  val() {
    return clone(valueAt(this.state, this.segments))
  }

  exists() {
    return valueAt(this.state, this.segments) !== null
  }

  child(childPath) {
    if (typeof childPath !== "string") throw new TypeError("child() needs a string")
    return new Snapshot(this.state, [...this.segments, ...childPath.split("/").filter(Boolean)])
  }

  hasChild(childPath) {
    return this.child(childPath).exists()
  }

  hasChildren(names) {
    const value = valueAt(this.state, this.segments)
    if (value === null || typeof value !== "object") return false
    if (names === undefined) return Object.keys(value).length > 0
    return names.every((name) => this.hasChild(name))
  }

  isString() {
    return typeof valueAt(this.state, this.segments) === "string"
  }

  isNumber() {
    return typeof valueAt(this.state, this.segments) === "number"
  }

  isBoolean() {
    return typeof valueAt(this.state, this.segments) === "boolean"
  }

  getPriority() {
    return null
  }
}

// --------------------------------------------------------------------------- rule evaluation

function evaluate(expression, scope) {
  if (typeof expression === "boolean") return expression
  if (expression === "true") return true
  if (expression === "false") return false
  const names = Object.keys(scope)
  // eslint-disable-next-line no-new-func
  const fn = new Function(...names, `"use strict"; return (${expression});`)
  try {
    return fn(...names.map((name) => scope[name])) === true
  } catch (error) {
    if (error instanceof ReferenceError || /is not a function/.test(error.message)) {
      const wrapped = new Error(`rule uses something the offline evaluator does not model: ${error.message} in ${expression}`)
      wrapped[UNMODELLED] = true
      throw wrapped
    }
    // A missing value (null.length, child(null)) is an error in RTDB too, and an error denies.
    return false
  }
}

/** Rule nodes from the root down to `segments`, each with the $wildcards bound so far. */
function walk(segments) {
  let node = rules
  let bindings = {}
  const chain = [{ node, bindings, depth: 0 }]
  for (let i = 0; i < segments.length; i += 1) {
    const segment = segments[i]
    if (!node || typeof node !== "object") break
    let next
    if (Object.prototype.hasOwnProperty.call(node, segment) && !segment.startsWith(".")) {
      next = node[segment]
    } else {
      const wildcard = Object.keys(node).find((key) => key.startsWith("$"))
      if (!wildcard) break
      next = node[wildcard]
      bindings = { ...bindings, [wildcard]: segment }
    }
    node = next
    chain.push({ node, bindings, depth: i + 1 })
  }
  return chain
}

const split = (p) => p.split("/").filter(Boolean)

function canRead(state, pathString, auth) {
  const segments = split(pathString)
  for (const { node, bindings, depth } of walk(segments)) {
    if (node && typeof node === "object" && ".read" in node) {
      const scope = {
        auth,
        now: Date.now(),
        root: new Snapshot(state, []),
        data: new Snapshot(state, segments.slice(0, depth)),
        ...bindings,
      }
      if (evaluate(node[".read"], scope)) return true
    }
  }
  return false
}

/** Every path in the new value's subtree, as segment arrays rooted at `base`. */
function subtreePaths(value, base) {
  const out = [base]
  if (value !== null && typeof value === "object") {
    for (const [key, child] of Object.entries(value)) out.push(...subtreePaths(child, [...base, key]))
  }
  return out
}

function canWrite(state, pathString, value, auth) {
  const segments = split(pathString)
  const next = writeAt(state, segments, value)
  const root = new Snapshot(state, [])

  let granted = false
  for (const { node, bindings, depth } of walk(segments)) {
    if (node && typeof node === "object" && ".write" in node) {
      const at = segments.slice(0, depth)
      const scope = {
        auth,
        now: Date.now(),
        root,
        data: new Snapshot(state, at),
        newData: new Snapshot(next, at),
        ...bindings,
      }
      if (evaluate(node[".write"], scope)) {
        granted = true
        break
      }
    }
  }
  if (!granted) return false
  if (value === null) return true

  for (const at of subtreePaths(value, segments)) {
    const chain = walk(at)
    const last = chain[chain.length - 1]
    if (last.depth !== at.length) continue // no rule node this deep: nothing to validate
    const { node, bindings } = last
    if (node === false) return false
    if (node && typeof node === "object" && ".validate" in node) {
      const scope = {
        auth,
        now: Date.now(),
        root,
        data: new Snapshot(state, at),
        newData: new Snapshot(next, at),
        ...bindings,
      }
      if (!evaluate(node[".validate"], scope)) return false
    }
  }
  return true
}

const authFor = (uid) => (uid ? { uid, provider: "anonymous", token: {} } : null)

function allowed(testCase) {
  const state = clone(FIXTURE)
  const auth = authFor(testCase.as)
  if (testCase.op === "read") return canRead(state, testCase.path, auth)
  if (testCase.op === "remove") return canWrite(state, testCase.path, null, auth)
  return canWrite(state, testCase.path, testCase.value, auth)
}

// --------------------------------------------------------------------------- suite

module.exports = async function run() {
  suite("RTDB rules (database.rules.json)")

  await test("the rules file parses once comment lines are stripped", () => {
    assert(parseError === null, `parse failed: ${parseError && parseError.message}`)
    assert(rules && typeof rules === "object", "no top-level rules object")
  })
  if (!rules) return

  await test("the root denies by default", () => {
    assertEqual(rules[".read"], false, "root .read")
    assertEqual(rules[".write"], false, "root .write")
  })

  await test("no rule relies on a custom claim or a role", () => {
    const expressions = RULES_SOURCE.match(/"\.(read|write|validate)":\s*"[^"]*"/g) || []
    assert(expressions.length > 0, "no rule expressions found")
    for (const expression of expressions) {
      // Nothing can mint claims without a server, so a rule reading one would be unreachable
      // at best, and a hole the day someone adds a claim for another purpose at worst.
      assert(!/auth\.token\./.test(expression), `custom claim used in ${expression}`)
      assert(!/\brole\b/.test(expression), `role used in ${expression}`)
    }
  })

  for (const testCase of CASES) {
    await test(testCase.name, () => {
      assertEqual(
        allowed(testCase),
        testCase.expect,
        `${testCase.op} ${testCase.path} as ${testCase.as || "signed-out"}`,
      )
    })
  }
}

module.exports.allowed = allowed
