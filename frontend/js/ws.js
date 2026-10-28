import { WS_URL } from './config.js';
import { getAccessToken, getSession } from './auth.js';
import { refreshSession } from './api.js';

let client = null;
let connecting = null;
let retryTimer = null;
let stopped = false;
let generation = 0;
const subscriptions = new Set();

export const topics = {
  orderStatus: id => `/topic/orders/${id}/status`,
  courierLocation: id => `/topic/courier/${id}/location`,
  availableOrders: () => '/topic/orders/available',
  adminOrders: () => '/topic/admin/orders',
};

function notifyState(connected) {
  document.dispatchEvent(new CustomEvent('ws:state', { detail: { connected } }));
}

function bind(entry) {
  entry.active = client.subscribe(entry.topic, message => {
    try { Promise.resolve(entry.handler(JSON.parse(message.body))).catch(console.error); }
    catch (error) { console.error(error); }
  });
}

function retry() {
  if (stopped || retryTimer || !getSession()) return;
  retryTimer = setTimeout(async () => {
    retryTimer = null;
    if (stopped || !getSession()) return;
    await refreshSession();
    if (!stopped) connect().catch(() => {});
  }, 5000);
}

export function connect() {
  stopped = false;
  if (client?.connected) return Promise.resolve();
  if (connecting) return connecting;
  const version = generation;
  connecting = new Promise((resolve, reject) => {
    const socket = new WebSocket(WS_URL);
    const connection = Stomp.over(socket);
    client = connection;
    connection.debug = () => {};
    let settled = false;
    const timeout = setTimeout(() => socket.close(), 10000);
    socket.addEventListener('close', () => {
      clearTimeout(timeout);
      if (version !== generation) return;
      connecting = null;
      notifyState(false);
      if (!settled) reject(new Error('WebSocket connection failed'));
      retry();
    });
    connection.connect({ Authorization: 'Bearer ' + getAccessToken() }, () => {
      clearTimeout(timeout);
      if (version !== generation) { socket.close(); return; }
      settled = true;
      connecting = null;
      subscriptions.forEach(bind);
      notifyState(true);
      document.dispatchEvent(new CustomEvent('ws:reconnected'));
      resolve();
    }, () => socket.close());
  });
  return connecting;
}

export function subscribe(topic, handler) {
  const entry = { topic, handler, active: null };
  subscriptions.add(entry);
  if (client?.connected) bind(entry);
  return { unsubscribe() {
    if (!subscriptions.delete(entry)) return;
    if (client?.connected) entry.active?.unsubscribe();
  } };
}

export function isConnected() { return !!client?.connected; }

export function unsubscribeViewSubs() {
  if (client?.connected) subscriptions.forEach(entry => entry.active?.unsubscribe());
  subscriptions.clear();
}

export function disconnect() {
  stopped = true;
  generation += 1;
  clearTimeout(retryTimer);
  retryTimer = null;
  unsubscribeViewSubs();
  client?.ws?.close();
  client = null;
  connecting = null;
  notifyState(false);
}

export function observeReconnect(root, handler) {
  const listener = () => Promise.resolve(handler()).catch(console.error);
  document.addEventListener('ws:reconnected', listener);
  root.addEventListener('view:dispose', () => document.removeEventListener('ws:reconnected', listener), { once: true });
}
