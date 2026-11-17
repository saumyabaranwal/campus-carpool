const app = document.getElementById("app");
const toastBox = document.getElementById("toast");
let user = JSON.parse(localStorage.getItem("carpoolUser") || "null");
let tab = "";

const icons = {
  car: `<svg viewBox="0 0 120 56" aria-hidden="true"><path fill="currentColor" d="M6 40v-6c0-4 3-6 7-7l14-3 12-13c2-2 4-3 7-3h28c3 0 5 1 7 3l11 13 15 3c4 1 6 3 6 7v6z"/><path fill="#fff" opacity=".85" d="M33 24l8-10h14v10zM60 24V14h14c2 0 3 1 4 2l8 8z"/><circle cx="32" cy="42" r="8" fill="#17301f"/><circle cx="88" cy="42" r="8" fill="#17301f"/><circle cx="32" cy="42" r="3" fill="#fff"/><circle cx="88" cy="42" r="3" fill="#fff"/></svg>`,
  bike: `<svg viewBox="0 0 120 56" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"><circle cx="28" cy="40" r="11"/><circle cx="92" cy="40" r="11"/><path d="M28 40l16-18h26l22 18M44 22l12 18h20M70 22l-4-8h10"/></g><path fill="currentColor" d="M48 18h24l-4 8H46z"/></svg>`,
};
const places = ["Noida City Centre", "Connaught Place", "Sector 18", "Anand Vihar", "Greater Noida"];

const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

async function api(path, data) {
  const res = await fetch("/api/" + path, {
    method: "POST",
    body: new URLSearchParams({ userId: user ? user.id : "", ...data }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.error);
  return json;
}

function toast(msg) {
  toastBox.textContent = msg;
  toastBox.classList.add("show");
  setTimeout(() => toastBox.classList.remove("show"), 2800);
}

async function attempt(action, success) {
  try {
    await action();
    if (success) toast(success);
  } catch (err) {
    toast(err.message);
  }
}

function formatTime(t) {
  return new Date(t).toLocaleString("en-IN", { weekday: "short", day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });
}

function empty(text) {
  return `<div class="empty">${icons.car}<p>${text}</p></div>`;
}

function rideCard(r, action) {
  const taken = r.capacity - r.seatsLeft;
  const dots = Array.from({ length: r.capacity }, (_, i) => `<i class="${i < taken ? "taken" : ""}"></i>`).join("");
  const status = r.completed ? `<span class="tag off">Completed</span>` : "";
  return `<article class="card ride">
    <div class="vehicle">${icons[r.vehicle]}</div>
    <div>
      <div class="route"><span>${esc(r.source)}</span><b></b><span>${esc(r.destination)}</span></div>
      <p class="meta">${formatTime(r.time)} · ${esc(r.model)} · ${esc(r.number)}</p>
      <p class="meta"><span class="avatar">${esc(r.driver[0])}</span>${esc(r.driver)} · ${esc(r.contact)}</p>
    </div>
    <div class="side">
      <div class="price">₹${r.fare}<small>per seat</small></div>
      <div class="seats">${dots}<span>${r.seatsLeft} left</span></div>
      ${status}${action}
    </div>
  </article>`;
}

function loginView() {
  app.innerHTML = `
    <section class="welcome">
      <div class="hero">
        <h1>Share the ride.<br>Split the fare.</h1>
        <p>Find students heading your way, or offer the empty seats in your car.</p>
        <div class="road">${icons.car}</div>
      </div>
      <form class="card" id="login">
        <h2>Get started</h2>
        <label>Student ID <input name="id" required pattern="\\d{10}" title="10 digit enrollment number" inputmode="numeric"></label>
        <label>Name <input name="name" required></label>
        <label>Mobile number <input name="contact" required pattern="[6-9]\\d{9}" title="10 digit mobile number" inputmode="numeric"></label>
        <div class="seg">
          <label><input type="radio" name="role" value="passenger" checked><span>Find a ride</span></label>
          <label><input type="radio" name="role" value="driver"><span>Offer a ride</span></label>
        </div>
        <button>Continue</button>
        <p class="hint">Try the demo: ID 2501030004 (Isha) books rides, 2501030001 (Aarav) drives. Name and mobile 9810011004 / 9810011001.</p>
      </form>
    </section>`;
  document.getElementById("login").onsubmit = (e) => {
    e.preventDefault();
    attempt(async () => {
      user = await api("login", Object.fromEntries(new FormData(e.target)));
      localStorage.setItem("carpoolUser", JSON.stringify(user));
      tab = user.role === "driver" ? "mine" : "find";
      render();
    });
  };
}

function render() {
  if (!user) return loginView();
  const tabs = user.role === "driver"
    ? [["mine", "My rides"], ["offer", "Offer a ride"]]
    : [["find", "Find a ride"], ["bookings", "My bookings"]];
  app.innerHTML = `
    <div class="top">
      <div class="brand"><div class="logo">${icons.car}</div>Campus Carpool</div>
      <div class="who"><div><strong>${esc(user.name)}</strong><small>${user.role === "driver" ? "Driver" : "Passenger"}</small></div>
        <button class="light" id="logout">Log out</button></div>
    </div>
    <div class="tabs">${tabs.map(([k, label]) => `<button class="${k === tab ? "on" : ""}" data-tab="${k}">${label}</button>`).join("")}</div>
    <section id="view"></section>`;
  document.getElementById("logout").onclick = () => {
    localStorage.removeItem("carpoolUser");
    user = null;
    render();
  };
  app.querySelectorAll("[data-tab]").forEach((b) => (b.onclick = () => { tab = b.dataset.tab; render(); }));
  const views = { offer: offerView, mine: myRidesView, find: findView, bookings: bookingsView };
  views[tab](document.getElementById("view"));
}

function findView(box) {
  box.innerHTML = `
    <div class="card"><label>Where are you headed?
      <input id="dest" placeholder="Search a destination"></label>
      <div class="chips">${places.map((p) => `<button type="button" data-place="${p}">${p}</button>`).join("")}</div></div>
    <div id="results"></div>`;
  const input = document.getElementById("dest");
  const results = document.getElementById("results");
  const load = () => attempt(async () => {
    const rides = await api("rides/search", { destination: input.value });
    results.innerHTML = rides.length
      ? rides.map((r) => rideCard(r, `<button data-join="${r.id}" ${r.seatsLeft ? "" : "disabled"}>${r.seatsLeft ? "Join ride" : "Full"}</button>`)).join("")
      : empty("No rides to that place yet. Try another destination.");
    results.querySelectorAll("[data-join]").forEach((b) => (b.onclick = () =>
      attempt(async () => { await api("book", { rideId: b.dataset.join }); load(); }, "Seat booked")));
  });
  input.oninput = load;
  box.querySelectorAll("[data-place]").forEach((b) => (b.onclick = () => { input.value = b.dataset.place; load(); }));
  load();
}

function bookingsView(box) {
  attempt(async () => {
    const list = await api("bookings", {});
    box.innerHTML = list.length
      ? list.map((b) => rideCard(b.ride, b.cancelled ? `<span class="tag off">Cancelled</span>`
          : b.ride.completed ? "" : `<button class="light" data-cancel="${b.id}">Cancel booking</button>`)).join("")
      : empty("You have not booked any rides yet.");
    box.querySelectorAll("[data-cancel]").forEach((b) => (b.onclick = () => {
      if (!confirm("Cancel this booking?")) return;
      attempt(async () => { await api("cancel", { bookingId: b.dataset.cancel }); bookingsView(box); }, "Booking cancelled");
    }));
  });
}

function offerView(box) {
  const soonest = new Date(Date.now() - new Date().getTimezoneOffset() * 60000 + 31 * 60000).toISOString().slice(0, 16);
  box.innerHTML = `
    <form class="card" id="offer">
      <h2>Offer a ride</h2>
      <div class="row"><label>From <input name="source" value="JIIT Sector 62" required></label>
      <label>To <input name="destination" required></label></div>
      <label>Departure <input type="datetime-local" name="time" min="${soonest}" required></label>
      <div class="row"><label>Vehicle <select name="type" id="type">
        <option value="car">Car</option><option value="bike">Bike</option></select></label>
      <label>Seats <input type="number" name="seats" id="seats" min="1" max="6" value="3"></label></div>
      <div class="row"><label>Model <input name="model" required></label>
      <label>Number plate <input name="number" placeholder="UP16AB1234" required></label></div>
      <label>Total trip cost (₹) <input type="number" name="fare" min="10" max="5000" required></label>
      <p class="hint">The cost is split between everyone in the vehicle, so each passenger pays less than the total.</p>
      <button>Post ride</button>
    </form>`;
  const type = document.getElementById("type");
  const seats = document.getElementById("seats");
  type.onchange = () => { seats.disabled = type.value === "bike"; };
  document.getElementById("offer").onsubmit = (e) => {
    e.preventDefault();
    attempt(async () => {
      await api("rides/create", Object.fromEntries(new FormData(e.target)));
      tab = "mine";
      render();
    }, "Ride posted");
  };
}

function myRidesView(box) {
  attempt(async () => {
    const list = await api("rides/mine", {});
    const filled = list.reduce((n, r) => n + r.capacity - r.seatsLeft, 0);
    const earned = list.reduce((n, r) => n + (r.capacity - r.seatsLeft) * r.fare, 0);
    const stats = `<div class="stats">
      <div class="stat"><b>${list.length}</b><span>Rides posted</span></div>
      <div class="stat"><b>${filled}</b><span>Seats booked</span></div>
      <div class="stat"><b>₹${earned}</b><span>Cost recovered</span></div></div>`;
    box.innerHTML = list.length
      ? stats + list.map((r) => rideCard(r, r.completed ? "" : `<button class="green" data-done="${r.id}">Mark completed</button>`)).join("")
      : empty("You have not posted any rides yet.");
    box.querySelectorAll("[data-done]").forEach((b) => (b.onclick = () =>
      attempt(async () => { await api("rides/complete", { rideId: b.dataset.done }); myRidesView(box); }, "Ride completed")));
  });
}

render();
