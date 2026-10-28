import { request } from '../api.js';
import { connect, subscribe, topics, observeReconnect, isConnected } from '../ws.js';
import {
  STATUS, statusBadge, stepper, formatDate, shortId, toast, escapeHtml,
} from '../ui.js';
import { createMap, addMarker, drawRoute } from '../map.js';

const PAGE_SIZE = 10;

export async function adminOrdersView(root) {
  let currentMap = null;
  let detailSubs = [];
  let selectedVersion = 0;
  let selectedId = null;
  root.addEventListener('view:dispose', () => { selectedVersion++; currentMap?.remove(); }, { once: true });
  let page = 0;
  let data = await load(page);
  if (!root.isConnected) return;

  root.innerHTML = `
    <div class="split split-wider">
      <section>
        <h2>Все заказы <span class="count">${data.totalElements}</span></h2>
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Маршрут</th>
              <th>Статус</th>
              <th>Обновлён</th>
            </tr>
          </thead>
          <tbody id="orders-body"></tbody>
        </table>
        <div class="pager">
          <button class="btn btn-ghost" id="prev-btn">← Назад</button>
          <span class="muted small" id="page-label"></span>
          <button class="btn btn-ghost" id="next-btn">Вперёд →</button>
        </div>
      </section>
      <section id="admin-detail">
        <div class="empty-state">Выберите заказ, чтобы увидеть историю и карту</div>
      </section>
    </div>`;

  drawTable();
  observeReconnect(root, async () => {
    const fresh = await load(page);
    if (!root.isConnected) return;
    data = fresh;
    drawTable();
  });

  root.querySelector('#prev-btn').addEventListener('click', async () => {
    if (page > 0) {
      page -= 1;
      data = await load(page);
      drawTable();
    }
  });

  root.querySelector('#next-btn').addEventListener('click', async () => {
    if (page < data.totalPages - 1) {
      page += 1;
      data = await load(page);
      drawTable();
    }
  });

  try {
    subscribe(topics.adminOrders(), async () => {
      const fresh = await load(page);
      if (!root.isConnected) return;
      data = fresh;
      drawTable();
      if (data.content.some(order => order.id === selectedId)) await select(selectedId);
    });
    await connect();
  } catch {
  }

  async function load(pageNumber) {
    return request(`/api/v1/admin/orders?page=${pageNumber}&size=${PAGE_SIZE}&sort=createdAt,desc`);
  }

  function drawTable() {
    const body = root.querySelector('#orders-body');
    root.querySelector('#page-label').textContent =
      `стр. ${data.number + 1} из ${Math.max(data.totalPages, 1)}`;

    if (!data.content.length) {
      body.innerHTML = '<tr><td colspan="4" class="muted">Заказов нет</td></tr>';
      return;
    }

    body.innerHTML = data.content.map(order => `
      <tr class="clickable" data-id="${order.id}">
        <td><code>#${shortId(order.id)}</code></td>
        <td>${escapeHtml(order.pickupAddress)} → ${escapeHtml(order.deliveryAddress)}</td>
        <td>${statusBadge(order.status)}</td>
        <td class="muted small">${formatDate(order.updatedAt)}</td>
      </tr>`).join('');

    body.querySelectorAll('tr.clickable').forEach(row =>
      row.addEventListener('click', () => select(row.dataset.id)));
  }

  async function select(orderId) {
    selectedId = orderId;
    const version = ++selectedVersion;
    const order = data.content.find(o => o.id === orderId);
    if (!order) return;

    const detail = root.querySelector('#admin-detail');
    const history = await request(`/api/v1/admin/orders/${orderId}/history`);

    if (version !== selectedVersion || !root.isConnected) return;
    detailSubs.forEach(sub => sub.unsubscribe());
    detailSubs = [];
    currentMap?.remove();
    detail.innerHTML = `
      <div class="card">
        <div class="detail-head">
          <div>
            <h3>Заказ <code>#${shortId(order.id)}</code></h3>
            <p class="muted small">Создан ${formatDate(order.createdAt)}</p>
          </div>
          <span id="detail-badge">${statusBadge(order.status)}</span>
        </div>
        <div id="detail-stepper">${stepper(order.status)}</div>
        <div class="route-points">
          <div class="route-point"><span class="dot dot-a"></span>${escapeHtml(order.pickupAddress)}</div>
          <div class="route-point"><span class="dot dot-b"></span>${escapeHtml(order.deliveryAddress)}</div>
        </div>
        <div class="map" id="detail-map"></div>
        <h3 style="margin-top:18px">История статусов</h3>
        ${history.length
          ? `<div class="timeline">
               ${history.map(item => `
                 <div class="timeline-item">
                   <span class="timeline-dot"></span>
                   <div>
                     <b>${STATUS[item.status]?.label || item.status}</b>
                     <div class="muted small">${formatDate(item.changedAt)}</div>
                   </div>
                 </div>`).join('')}
             </div>`
          : '<p class="muted small">История пуста</p>'}
      </div>`;

    const map = createMap('detail-map', [
      { key: 'pickup', lat: order.pickupLat, lng: order.pickupLng, cls: 'marker-a', label: 'A' },
      { key: 'delivery', lat: order.deliveryLat, lng: order.deliveryLng, cls: 'marker-b', label: 'B' },
    ]).map;
    currentMap = map;
    drawRoute(
      map,
      { lat: order.pickupLat, lng: order.pickupLng },
      { lat: order.deliveryLat, lng: order.deliveryLng }
    );

    let courierMarker = null;
    if (order.courierId) {
      detailSubs.push(subscribe(topics.courierLocation(order.courierId), point => {
        if (!courierMarker) {
          courierMarker = addMarker(map, {
            lat: point.latitude,
            lng: point.longitude,
            cls: 'marker-courier',
          });
        } else {
          courierMarker.setLatLng([point.latitude, point.longitude]);
        }
      }));
    }


  }
}

export async function adminCouriersView(root) {
  const couriers = await request('/api/v1/admin/couriers');
  if (!root.isConnected) return;

  root.innerHTML = `
    <h2>Курьеры <span class="count">${couriers.length}</span></h2>
    <table class="table">
      <thead>
        <tr>
          <th>Имя</th>
          <th>Email</th>
          <th>Телефон</th>
          <th>Зарегистрирован</th>
        </tr>
      </thead>
      <tbody>
        ${couriers.map(courier => `
          <tr>
            <td><b>${escapeHtml(courier.name)}</b></td>
            <td>${escapeHtml(courier.email)}</td>
            <td class="muted">${escapeHtml(courier.phone) || '—'}</td>
            <td class="muted small">${formatDate(courier.createdAt)}</td>
          </tr>`).join('')}
      </tbody>
    </table>
    ${couriers.length ? '' : '<div class="empty-state" style="margin-top:14px">Курьеров пока нет</div>'}`;
}
