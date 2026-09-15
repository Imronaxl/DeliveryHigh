import { getSession } from './auth.js';
import { unsubscribeViewSubs } from './ws.js';
import { renderHeader } from './header.js';
import { loginView, registerView } from './views/authViews.js';
import { clientOrdersView, clientNewOrderView } from './views/clientView.js';
import { courierAvailableView, courierActiveView, stopShift } from './views/courierView.js';
import { adminOrdersView, adminCouriersView } from './views/adminView.js';

const routes = {
  '/login': { view: loginView, guest: true },
  '/register': { view: registerView, guest: true },

  '/client/orders': { view: clientOrdersView, role: 'CLIENT' },
  '/client/new': { view: clientNewOrderView, role: 'CLIENT' },

  '/courier/available': { view: courierAvailableView, role: 'COURIER' },
  '/courier/active': { view: courierActiveView, role: 'COURIER', cleanup: stopShift },

  '/admin/orders': { view: adminOrdersView, role: 'ADMIN' },
  '/admin/couriers': { view: adminCouriersView, role: 'ADMIN' },
};

export function homeFor(role) {
  return {
    CLIENT: '/client/orders',
    COURIER: '/courier/available',
    ADMIN: '/admin/orders',
  }[role] || '/login';
}

export function navigate(path) {
  location.hash = '#' + path;
}

let current = null;

export async function handleRoute() {
  const path = location.hash.slice(1) || '/';
  const session = getSession();
  const route = routes[path];
  const app = document.getElementById('app');

  if (!route) {
    location.hash = '#' + (session ? homeFor(session.role) : '/login');
    return;
  }

  if (route.guest && session) {
    location.hash = '#' + homeFor(session.role);
    return;
  }

  if (!route.guest) {
    if (!session) {
      location.hash = '#/login';
      return;
    }
    if (route.role !== session.role) {
      location.hash = '#' + homeFor(session.role);
      return;
    }
  }

  // у предыдущей вьюхи могут остаться таймеры и подписки
  current?.cleanup?.();
  current = route;

  renderHeader();
  unsubscribeViewSubs();
  app.innerHTML = '';

  try {
    await route.view(app);
  } catch (error) {
    app.innerHTML = `
      <div class="empty-state">
        <p><b>Что-то сломалось</b></p>
        <p class="muted">${error.message}</p>
        <p style="margin-top:10px"><a href="#/">Обновить</a></p>
      </div>`;
  }
}

window.addEventListener('hashchange', handleRoute);
