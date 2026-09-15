// обёртка над Leaflet, чтобы вьюхи не возились с деталями карты

export function createMap(containerId, points = []) {
  const map = L.map(containerId);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map);

  const markers = {};
  points.forEach(point => {
    markers[point.key] = addMarker(map, point);
  });

  if (points.length === 1) {
    map.setView([points[0].lat, points[0].lng], 14);
  } else if (points.length > 1) {
    map.fitBounds(points.map(p => [p.lat, p.lng]), { padding: [40, 40] });
  } else {
    // центр Москвы как дефолтная песочница
    map.setView([55.751244, 37.6184], 12);
  }

  return { map, markers };
}

export function addMarker(map, point) {
  const icon = L.divIcon({
    className: `map-marker ${point.cls || ''}`,
    html: `<span>${point.label || ''}</span>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  });
  return L.marker([point.lat, point.lng], { icon }).addTo(map);
}

export function drawRoute(map, from, to) {
  return L.polyline([[from.lat, from.lng], [to.lat, to.lng]], {
    color: '#ea580c',
    weight: 3,
    dashArray: '6 8',
  }).addTo(map);
}
