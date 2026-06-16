function jsonResult(data) {
  return {
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200,
      data
    })
  };
}

export function buildFeatureFlags() {
  const enabled = { enabled: true, reason: null };
  return {
    auth: enabled,
    product: enabled,
    productWrite: enabled,
    task: enabled,
    error: enabled,
    finance: enabled,
    chat: enabled,
    ads: enabled,
    stockWrite: enabled,
    priceWrite: enabled,
    postingWrite: enabled,
    chatSend: enabled,
    adsSync: enabled
  };
}

export async function installCommonAppMocks(page, extraHandlers = {}) {
  await page.addInitScript(() => {
    localStorage.setItem('jsessionid', 'e2e-session');
    localStorage.setItem('jsessiontime', 'e2e-jsessiontime');
  });

  const commonHandlers = {
    'GET /api/admin/api/v1/menus/route': () => jsonResult([
      {
        name: 'ozon_task',
        sort: 1,
        meta: {
          title: 'Ozon',
          icon: 'Workbench',
          permissions: []
        },
        children: []
      }
    ]),
    'GET /admin/api/v1/users/info': () => jsonResult({ id: 'tester', name: 'E2E User' }),
    'GET /admin/api/v1/users/findbindlist': () => jsonResult([]),
    'GET /admin/api/v1/users/getEnable': () => jsonResult(true),
    'GET /admin/api/v1/notify/pullMessage': () => jsonResult([]),
    'GET /admin/api/v1/notify/findNitofyNums': () => jsonResult(0),
    'GET /admin/api/v1/notify/findNoReadByUserAll': () => jsonResult([]),
    'GET /amazon/api/v1/amzauthority/getAmazonGroup': () => jsonResult([]),
    'GET /ozon/api/v1/meta/features': () => jsonResult(buildFeatureFlags()),
    'GET /ozon/api/v1/auth/list': () => jsonResult([
      {
        id: 'auth-1',
        name: 'Ozon E2E Shop',
        status: 'ACTIVE',
        lastSyncStatus: 'SUCCESS'
      }
    ])
  };

  await page.route('**/*', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const key = `${request.method()} ${url.pathname}`;
    const handler = extraHandlers[key] || commonHandlers[key];
    if (!handler) {
      return route.continue();
    }
    const response = await handler({ route, request, url });
    return route.fulfill(response);
  });
}
