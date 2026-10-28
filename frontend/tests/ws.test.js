import test from 'node:test';
import assert from 'node:assert/strict';
const values = new Map([['df_user','{"role":"CLIENT"}'],['df_access_token','access']]);
globalThis.sessionStorage = { getItem:key => values.get(key) ?? null, removeItem:key => values.delete(key), setItem:(key,value) => values.set(key,value) };
globalThis.localStorage = { removeItem() {} };
globalThis.location = { port:'8000',origin:'http://localhost:8000' };
globalThis.document = new EventTarget();
globalThis.CustomEvent = class extends Event { constructor(type,options) { super(type); this.detail=options?.detail; } };
const sockets=[],connections=[];
globalThis.WebSocket = class extends EventTarget {
  constructor(url) { super(); this.url=url; sockets.push(this); }
  close() { connections.find(c => c.ws===this).connected=false; this.dispatchEvent(new Event('close')); }
};
globalThis.Stomp = { over(socket) {
  const connection={ ws:socket,connected:false,bound:[],connect(headers,success) { this.headers=headers; this.success=() => { this.connected=true; success(); }; },subscribe(topic,handler) { const sub={topic,handler,removed:false,unsubscribe() { this.removed=true; }};this.bound.push(sub);return sub; } };
  connections.push(connection); return connection;
} };
const ws = await import('../js/ws.js');
test('authenticates STOMP and restores subscriptions after disconnect', async () => {
  const originalSetTimeout=globalThis.setTimeout;
  let retry;
  globalThis.setTimeout=(callback,delay) => delay===5000 ? (retry=callback,42) : originalSetTimeout(callback,delay);
  try {
    const sub=ws.subscribe('/topic/orders/id/status',()=>{});
    const ready=ws.connect();
    assert.equal(sockets[0].url,'ws://localhost:8080/ws');
    assert.equal(connections[0].headers.Authorization,'Bearer access');
    connections[0].success(); await ready;
    assert.equal(connections[0].bound.length,1);
    sockets[0].close();
    assert.equal(ws.isConnected(),false);
    await retry();
    connections[1].success();
    assert.equal(connections[1].bound.length,1);
    sub.unsubscribe(); sub.unsubscribe();
    assert.equal(connections[1].bound[0].removed,true);
  } finally { ws.disconnect();globalThis.setTimeout=originalSetTimeout; }
});
