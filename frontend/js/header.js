import { getSession, logout } from './auth.js';
import { disconnect } from './ws.js';
import { logoHtml } from './ui.js';

// навигация в шапке зависит от роли
const NAV = {
  CLIENT: [
    { href: '#/client/orders', label: 'Мои заказы' },
    { href: '#/client/new', label: 'Новый заказ' },
  ],
  COURIER: [
    { href: '#/courier/available', label: 'Доступные' },
    { href: '#/courier/active', label: 'В работе' },
  ],
  ADMIN: [
    { href: '#/admin/orders', label: 'Заказы' },
    { href: '#/admin/couriers', label: 'Курьеры' },
  ],
};

const ROLE_LABEL = { CLIENT: 'клиент', COURIER: 'курьер', ADMIN: 'админ' };

export function renderHeader() {
  const root = document.getElementById('topbar');
  const user = getSession();

  const logo = `<a class="logo" href="#/">${logoHtml()} DeliveryFlow</a>`;

  if (!user) {
    root.innerHTML = `${logo}<div class="topbar-user"></div>`;
    return;
  }

  const links = (NAV[user.role] || [])
    .map(link => `
      <a href="${link.href}" class="${location.hash === link.href ? 'active' : ''}">
        ${link.label}
      </a>`).join('');

  root.innerHTML = `
    ${logo}
    <nav class="topbar-nav">${links}</nav>
    <div class="topbar-user">
      <div class="user-chip">
        <b>${user.name}</b>
        <span>${ROLE_LABEL[user.role] || user.role}</span>
      </div>
      <button class="btn btn-ghost" id="logout-btn" style="color:#cbd5e1">Выйти</button>
    </div>`;

  root.querySelector('#logout-btn').addEventListener('click', () => {
    logout();
    disconnect();
    location.hash = '#/login';
  });
}
