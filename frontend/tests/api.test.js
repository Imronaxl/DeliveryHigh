import test from 'node:test';
import assert from 'node:assert/strict';
const values = new Map();
globalThis.sessionStorage = { getItem: key => values.get(key) ?? null, setItem: (key,value) => values.set(key,value), removeItem: key => values.delete(key) };
globalThis.localStorage = { removeItem() {} };
globalThis.location = { port: '8000', origin: 'http://localhost:8000', hash: '' };
const { request } = await import('../js/api.js');
test('retries a protected request only once after refresh', async () => {
  values.set('df_refresh_token','refresh');
  let requests = 0, refreshes = 0;
  globalThis.fetch = async url => {
    if (url.endsWith('/auth/refresh')) {
      refreshes++;
      return { ok:true, json:async () => ({ accessToken:'new',refreshToken:'refresh',role:'CLIENT' }) };
    }
    requests++;
    return { status:401,ok:false };
  };
  await assert.rejects(request('/api/v1/orders/me'), error => error.status === 401);
  assert.equal(requests,2);
  assert.equal(refreshes,1);
});
test('concurrent unauthorized requests share one refresh', async () => {
  values.set('df_refresh_token','refresh'); values.set('df_access_token','old');
  let refreshes = 0;
  globalThis.fetch = async (url,options) => {
    if (url.endsWith('/auth/refresh')) {
      refreshes++;
      await new Promise(resolve => setTimeout(resolve,10));
      return { ok:true,json:async () => ({ accessToken:'new',refreshToken:'refresh',role:'CLIENT' }) };
    }
    if (options.headers.Authorization === 'Bearer old') return { status:401,ok:false };
    return { status:200,ok:true,json:async () => [] };
  };
  await Promise.all([request('/api/v1/orders/me'),request('/api/v1/orders/me')]);
  assert.equal(refreshes,1);
});
