/**
 * The Realtime Database access model, as one table of concrete cases.
 *
 * Both suites run exactly these cases against exactly this fixture:
 *
 *   - test/rules.emulator.test.js through the real rules engine (authoritative), and
 *   - test/offline/rtdb-rules.test.js through the offline evaluator,
 *
 * so the two cannot drift apart: a case added here is checked by both, and if the offline
 * evaluator ever disagrees with the emulator, the emulator run shows it.
 *
 * Every case starts from a fresh copy of FIXTURE. `expect` is whether the rules must allow it.
 *
 * Actors:
 *   uid_a     owner of tenant tnt_a (chats/tnt_a/ownerUid)
 *   uid_b     owner of tenant tnt_b
 *   web_a     a browser paired to tnt_a (listed under chats/tnt_a/sessions)
 *   vis_1     anonymous website visitor who started conv_1 and conv_closed in tnt_a
 *   vis_2     another visitor, who started conv_2 in tnt_a
 *   web_new   a browser with a waiting pairing session
 *   uid_x     any other signed-in account
 *   null      not signed in
 */

const LONG_TEXT = "x".repeat(4001)

const FIXTURE = {
  chats: {
    tnt_a: {
      ownerUid: "uid_a",
      sessions: {
        web_a: { grantedAt: 1, label: "Office laptop" },
      },
      conversations: {
        conv_1: { visitorUid: "vis_1", websiteId: "web_1", status: "open", createdAt: 1 },
        conv_closed: { visitorUid: "vis_1", websiteId: "web_1", status: "closed", createdAt: 1 },
        conv_2: { visitorUid: "vis_2", websiteId: "web_1", status: "open", createdAt: 1 },
      },
      messages: {
        conv_1: { m1: { sender: "visitor", text: "hi", createdAt: 1 } },
        conv_2: { m1: { sender: "visitor", text: "hello", createdAt: 1 } },
      },
    },
    tnt_b: {
      ownerUid: "uid_b",
      conversations: {
        conv_9: { visitorUid: "vis_9", websiteId: "web_9", status: "open", createdAt: 1 },
      },
      messages: {
        conv_9: { m1: { sender: "visitor", text: "other tenant", createdAt: 1 } },
      },
    },
  },
  pairing: {
    sess_waiting: { webUid: "web_new", status: "waiting", createdAt: 1 },
  },
}

const visitorMessage = { sender: "visitor", text: "hello again", createdAt: 2 }
const agentMessage = { sender: "agent", text: "How can I help?", createdAt: 3 }

/** op: "read" | "set" | "remove". */
const CASES = [
  // ------------------------------------------------------------------ nothing at the top
  { name: "nobody reads the database root", as: "uid_a", op: "read", path: "/", expect: false },
  { name: "nobody reads the chats root", as: "uid_a", op: "read", path: "chats", expect: false },
  { name: "an owner cannot read the whole tenant node", as: "uid_a", op: "read", path: "chats/tnt_a", expect: false },
  { name: "no write outside the modelled tree", as: "uid_a", op: "set", path: "elsewhere/node", value: 1, expect: false },

  // ------------------------------------------------------------------ ownership mirror
  { name: "a signed-in client reads a tenant's ownerUid", as: "uid_x", op: "read", path: "chats/tnt_a/ownerUid", expect: true },
  { name: "a signed-out client cannot read ownerUid", as: null, op: "read", path: "chats/tnt_a/ownerUid", expect: false },
  { name: "another account cannot take over an owned tenant", as: "uid_x", op: "set", path: "chats/tnt_a/ownerUid", value: "uid_x", expect: false },
  { name: "the owner may rewrite their own ownerUid unchanged", as: "uid_a", op: "set", path: "chats/tnt_a/ownerUid", value: "uid_a", expect: true },
  { name: "ownerUid cannot name someone other than the writer", as: "uid_x", op: "set", path: "chats/tnt_new/ownerUid", value: "uid_a", expect: false },
  // The documented trade-off (README-FIREBASE-ONLY.md): an unclaimed namespace is claimed by
  // the first account that writes its mirror. Kept as a case so a rules change here is noticed.
  { name: "an unclaimed tenant namespace can be claimed once by the writer", as: "uid_x", op: "set", path: "chats/tnt_new/ownerUid", value: "uid_x", expect: true },

  // ------------------------------------------------------------------ inbox reads
  { name: "the owner lists their inbox", as: "uid_a", op: "read", path: "chats/tnt_a/conversations", expect: true },
  { name: "a paired browser lists the inbox it was paired to", as: "web_a", op: "read", path: "chats/tnt_a/conversations", expect: true },
  { name: "a paired browser cannot list another tenant's inbox", as: "web_a", op: "read", path: "chats/tnt_b/conversations", expect: false },
  { name: "another owner cannot list this inbox", as: "uid_b", op: "read", path: "chats/tnt_a/conversations", expect: false },
  { name: "a visitor cannot list the inbox", as: "vis_1", op: "read", path: "chats/tnt_a/conversations", expect: false },
  { name: "a signed-out client cannot list the inbox", as: null, op: "read", path: "chats/tnt_a/conversations", expect: false },
  { name: "another owner cannot read a thread's messages", as: "uid_b", op: "read", path: "chats/tnt_a/messages/conv_1", expect: false },
  { name: "the owner cannot read another tenant's messages", as: "uid_a", op: "read", path: "chats/tnt_b/messages/conv_9", expect: false },

  // ------------------------------------------------------------------ visitor scope
  { name: "a visitor reads their own conversation", as: "vis_1", op: "read", path: "chats/tnt_a/conversations/conv_1", expect: true },
  { name: "a visitor reads their own messages", as: "vis_1", op: "read", path: "chats/tnt_a/messages/conv_1", expect: true },
  { name: "a visitor cannot read someone else's conversation", as: "vis_1", op: "read", path: "chats/tnt_a/conversations/conv_2", expect: false },
  { name: "a visitor cannot read someone else's messages", as: "vis_1", op: "read", path: "chats/tnt_a/messages/conv_2", expect: false },
  { name: "the same conversation id under another tenant stays closed", as: "vis_1", op: "read", path: "chats/tnt_b/conversations/conv_1", expect: false },

  // ------------------------------------------------------------------ starting a conversation
  {
    name: "a visitor starts a conversation under their own uid",
    as: "vis_2", op: "set", path: "chats/tnt_a/conversations/conv_new",
    value: { visitorUid: "vis_2", websiteId: "web_1", status: "pending", createdAt: 5 },
    expect: true,
  },
  {
    name: "a visitor cannot start a conversation in someone else's name",
    as: "vis_2", op: "set", path: "chats/tnt_a/conversations/conv_new",
    value: { visitorUid: "vis_1", websiteId: "web_1", status: "pending", createdAt: 5 },
    expect: false,
  },
  {
    name: "a visitor cannot overwrite an existing conversation",
    as: "vis_2", op: "set", path: "chats/tnt_a/conversations/conv_1",
    value: { visitorUid: "vis_2", websiteId: "web_1", status: "open", createdAt: 5 },
    expect: false,
  },
  {
    name: "a conversation missing required fields is refused",
    as: "vis_2", op: "set", path: "chats/tnt_a/conversations/conv_new",
    value: { visitorUid: "vis_2", status: "pending" },
    expect: false,
  },
  {
    name: "a conversation with an unknown field is refused",
    as: "vis_2", op: "set", path: "chats/tnt_a/conversations/conv_new",
    value: { visitorUid: "vis_2", websiteId: "web_1", status: "pending", createdAt: 5, admin: true },
    expect: false,
  },

  // ------------------------------------------------------------------ conversation state
  { name: "a visitor cannot change the conversation status", as: "vis_1", op: "set", path: "chats/tnt_a/conversations/conv_1/status", value: "closed", expect: false },
  { name: "the owner closes a conversation", as: "uid_a", op: "set", path: "chats/tnt_a/conversations/conv_1/status", value: "closed", expect: true },
  { name: "a paired browser closes a conversation", as: "web_a", op: "set", path: "chats/tnt_a/conversations/conv_1/status", value: "closed", expect: true },
  { name: "another owner cannot close this conversation", as: "uid_b", op: "set", path: "chats/tnt_a/conversations/conv_1/status", value: "closed", expect: false },
  { name: "status only takes the three known values", as: "uid_a", op: "set", path: "chats/tnt_a/conversations/conv_1/status", value: "archived", expect: false },
  { name: "the owner cannot add an unknown field to a conversation", as: "uid_a", op: "set", path: "chats/tnt_a/conversations/conv_1/injected", value: "x", expect: false },
  { name: "a visitor clears their own unread counter", as: "vis_1", op: "set", path: "chats/tnt_a/conversations/conv_1/unread", value: 0, expect: true },
  { name: "a visitor cannot touch another thread's unread counter", as: "vis_2", op: "set", path: "chats/tnt_a/conversations/conv_1/unread", value: 0, expect: false },
  { name: "unread can never be negative", as: "uid_a", op: "set", path: "chats/tnt_a/conversations/conv_1/unread", value: -1, expect: false },

  // ------------------------------------------------------------------ messages
  { name: "a visitor sends a message in their open conversation", as: "vis_1", op: "set", path: "chats/tnt_a/messages/conv_1/m2", value: visitorMessage, expect: true },
  { name: "a visitor cannot send as the agent", as: "vis_1", op: "set", path: "chats/tnt_a/messages/conv_1/m2", value: { ...visitorMessage, sender: "agent" }, expect: false },
  { name: "a visitor cannot post into another visitor's thread", as: "vis_1", op: "set", path: "chats/tnt_a/messages/conv_2/m2", value: visitorMessage, expect: false },
  { name: "a visitor cannot post into a closed conversation", as: "vis_1", op: "set", path: "chats/tnt_a/messages/conv_closed/m2", value: visitorMessage, expect: false },
  { name: "a visitor cannot edit a message already sent", as: "vis_1", op: "set", path: "chats/tnt_a/messages/conv_1/m1", value: visitorMessage, expect: false },
  { name: "a signed-out client cannot post", as: null, op: "set", path: "chats/tnt_a/messages/conv_1/m2", value: visitorMessage, expect: false },
  { name: "the owner replies", as: "uid_a", op: "set", path: "chats/tnt_a/messages/conv_1/m3", value: agentMessage, expect: true },
  { name: "a paired browser replies", as: "web_a", op: "set", path: "chats/tnt_a/messages/conv_1/m3", value: agentMessage, expect: true },
  { name: "another owner cannot reply in this tenant", as: "uid_b", op: "set", path: "chats/tnt_a/messages/conv_1/m3", value: agentMessage, expect: false },
  { name: "messages longer than 4000 characters are refused", as: "uid_a", op: "set", path: "chats/tnt_a/messages/conv_1/m3", value: { ...agentMessage, text: LONG_TEXT }, expect: false },
  { name: "empty messages are refused", as: "uid_a", op: "set", path: "chats/tnt_a/messages/conv_1/m3", value: { ...agentMessage, text: "" }, expect: false },
  { name: "messages with unknown fields are refused", as: "uid_a", op: "set", path: "chats/tnt_a/messages/conv_1/m3", value: { ...agentMessage, html: "<b>" }, expect: false },

  // ------------------------------------------------------------------ satisfaction rating
  { name: "a visitor rates their own conversation", as: "vis_1", op: "set", path: "chats/tnt_a/conversations/conv_1/rating", value: { score: "happy", at: 3 }, expect: true },
  { name: "a visitor cannot rate someone else's conversation", as: "vis_2", op: "set", path: "chats/tnt_a/conversations/conv_1/rating", value: { score: "sad", at: 3 }, expect: false },
  { name: "a rating takes only the three known scores", as: "vis_1", op: "set", path: "chats/tnt_a/conversations/conv_1/rating", value: { score: "great", at: 3 }, expect: false },

  // ------------------------------------------------------------------ paired browsers
  { name: "the owner lists paired browsers", as: "uid_a", op: "read", path: "chats/tnt_a/sessions", expect: true },
  { name: "a paired browser cannot list the other browsers", as: "web_a", op: "read", path: "chats/tnt_a/sessions", expect: false },
  { name: "a paired browser reads its own grant", as: "web_a", op: "read", path: "chats/tnt_a/sessions/web_a", expect: true },
  { name: "another owner cannot list this tenant's browsers", as: "uid_b", op: "read", path: "chats/tnt_a/sessions", expect: false },
  { name: "the owner pairs a new browser", as: "uid_a", op: "set", path: "chats/tnt_a/sessions/web_new", value: { grantedAt: 2 }, expect: true },
  { name: "a browser cannot pair itself", as: "web_new", op: "set", path: "chats/tnt_a/sessions/web_new", value: { grantedAt: 2 }, expect: false },
  { name: "a paired browser cannot pair another browser", as: "web_a", op: "set", path: "chats/tnt_a/sessions/web_new", value: { grantedAt: 2 }, expect: false },
  { name: "a paired browser may unpair itself", as: "web_a", op: "remove", path: "chats/tnt_a/sessions/web_a", expect: true },
  { name: "another owner cannot pair a browser into this tenant", as: "uid_b", op: "set", path: "chats/tnt_a/sessions/web_new", value: { grantedAt: 2 }, expect: false },

  // ------------------------------------------------------------------ pairing handshake
  { name: "a signed-in browser reads a pairing session it knows", as: "uid_x", op: "read", path: "pairing/sess_waiting", expect: true },
  { name: "a signed-out client cannot read pairing sessions", as: null, op: "read", path: "pairing/sess_waiting", expect: false },
  { name: "nobody can list pairing sessions", as: "uid_a", op: "read", path: "pairing", expect: false },
  {
    name: "a browser opens a waiting session for itself",
    as: "web_other", op: "set", path: "pairing/sess_new",
    value: { webUid: "web_other", status: "waiting", createdAt: 1 },
    expect: true,
  },
  {
    name: "a browser cannot open a session in another uid's name",
    as: "web_other", op: "set", path: "pairing/sess_new",
    value: { webUid: "web_new", status: "waiting", createdAt: 1 },
    expect: false,
  },
  {
    name: "a session cannot be born already approved",
    as: "web_other", op: "set", path: "pairing/sess_new",
    value: { webUid: "web_other", status: "approved", createdAt: 1, tenantId: "tnt_a" },
    expect: false,
  },
  {
    name: "a session with an unknown field is refused",
    as: "web_other", op: "set", path: "pairing/sess_new",
    value: { webUid: "web_other", status: "waiting", createdAt: 1, evil: true },
    expect: false,
  },
  {
    name: "the owner approves a waiting session for their own tenant",
    as: "uid_a", op: "set", path: "pairing/sess_waiting",
    value: { webUid: "web_new", status: "approved", createdAt: 1, tenantId: "tnt_a" },
    expect: true,
  },
  {
    name: "an owner cannot approve a session into a tenant they do not own",
    as: "uid_a", op: "set", path: "pairing/sess_waiting",
    value: { webUid: "web_new", status: "approved", createdAt: 1, tenantId: "tnt_b" },
    expect: false,
  },
  {
    name: "an approval cannot swap the browser being approved",
    as: "uid_a", op: "set", path: "pairing/sess_waiting",
    value: { webUid: "uid_x", status: "approved", createdAt: 1, tenantId: "tnt_a" },
    expect: false,
  },
]

module.exports = { FIXTURE, CASES }
