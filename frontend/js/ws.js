import { WS_URL } from './config.js';

// один STOMP-клиент на всё приложение; вьюхи подписываются на топики,
// а роутер при переходе отписывает подписки конкретной вьюхи

let client = null;
let connecting = null;
let viewSubs = [];

export const topics = {
  orderStatus: orderId => `/topic/orders/${orderId}/status`,
  courierLocation: courierId => `/topic/courier/${courierId}/location`,
  availableOrders: () => '/topic/orders/available',
};

export function connect() {
  if (client && client.connected) return Promise.resolve();
  if (connecting) return connecting;

  connecting = new Promise((resolve, reject) => {
    client = Stomp.over(new SockJS(WS_URL));
    client.reconnect_delay = 5000;

    client.connect({}, () => resolve(), () => {
      connecting = null;
      reject(new Error('WebSocket connection failed'));
    });
  });

  return connecting;
}

export function subscribe(topic, handler) {
  if (!client) throw new Error('WebSocket is not connected yet');
  const sub = client.subscribe(topic, message => handler(JSON.parse(message.body)));
  viewSubs.push(sub);
  return sub;
}

export function isConnected() {
  return !!(client && client.connected);
}

export function unsubscribeViewSubs() {
  viewSubs.forEach(sub => sub.unsubscribe());
  viewSubs = [];
}

export function disconnect() {
  if (client) client.disconnect(() => {});
  client = null;
  connecting = null;
  viewSubs = [];
}
