import request from "@/utils/request.js";

function importMessages(data) {
  return request.post("/ozon/api/v1/chat/import", data);
}

function listSessions(params) {
  return request.get("/ozon/api/v1/chat/session/list", { params });
}

function listMessages(params) {
  return request.get("/ozon/api/v1/chat/message/list", { params });
}

function recordReply(data) {
  return request.post("/ozon/api/v1/chat/reply/record", data);
}

function sendReply(data) {
  return request.post("/ozon/api/v1/chat/reply/send", data);
}

function listReplyAudits(params) {
  return request.get("/ozon/api/v1/chat/reply/audit/list", { params });
}

export default {
  importMessages,
  listSessions,
  listMessages,
  recordReply,
  sendReply,
  listReplyAudits
}
