import request from "@/utils/request.js";

function features() {
  return request.get("/ozon/api/v1/meta/features");
}

export default {
  features
}
