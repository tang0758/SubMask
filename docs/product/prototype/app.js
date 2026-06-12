const state = {
  orientation: "portrait",
  layout: "",
  locked: false,
  tileActive: false,
  values: {
    opacity: 90,
    width: 89,
    height: 4,
    x: 50,
    bottom: 57
  }
};

const layoutPresets = {
  tiktok: {
    orientation: "portrait",
    width: 92,
    height: 14,
    x: 50,
    bottom: 8
  },
  youtube: {
    orientation: "portrait",
    width: 88,
    height: 10,
    x: 50,
    bottom: 52
  },
  landscape: {
    orientation: "landscape",
    width: 86,
    height: 12,
    x: 50,
    bottom: 8
  }
};

const screens = document.querySelectorAll("[data-screen]");
const viewTabs = document.querySelectorAll("[data-view]");
const preview = document.querySelector("[data-preview]");
const mask = document.querySelector("[data-mask]");
const floatingMask = document.querySelector("[data-floating-mask]");
const tile = document.querySelector("[data-action='tile']");

function setScreen(name) {
  screens.forEach((screen) => {
    screen.classList.toggle("is-active", screen.dataset.screen === name);
  });

  viewTabs.forEach((button) => {
    button.classList.toggle("is-active", button.dataset.view === name);
  });
}

function syncOutputs() {
  Object.entries(state.values).forEach(([key, value]) => {
    document.querySelectorAll(`[data-output="${key}"]`).forEach((output) => {
      output.value = `${value}%`;
      output.textContent = `${value}%`;
    });

    document.querySelectorAll(`[data-control="${key}"]`).forEach((control) => {
      control.value = value;
    });
  });

  document.querySelectorAll("[data-output='runtime-opacity']").forEach((output) => {
    output.value = `${state.values.opacity}%`;
    output.textContent = `${state.values.opacity}%`;
  });

  document.querySelectorAll("[data-control='runtime-opacity']").forEach((control) => {
    control.value = state.values.opacity;
  });
}

function syncOrientation() {
  document.querySelectorAll("[data-orientation]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.orientation === state.orientation);
  });

  preview.classList.toggle("portrait", state.orientation === "portrait");
  preview.classList.toggle("landscape", state.orientation === "landscape");
}

function syncLayoutChips() {
  document.querySelectorAll("[data-layout]").forEach((button) => {
    button.classList.toggle("is-selected", button.dataset.layout === state.layout);
  });
}

function syncMask() {
  const width = state.values.width;
  const height = state.values.height;
  const left = Math.max(0, Math.min(100 - width, state.values.x - width / 2));

  mask.style.width = `${width}%`;
  mask.style.height = `${height}%`;
  mask.style.left = `${left}%`;
  mask.style.bottom = `${state.values.bottom}%`;

  document.documentElement.style.setProperty("--mask-alpha", state.values.opacity / 100);
}

function syncFloatingMask() {
  floatingMask.classList.toggle("is-locked", state.locked);
  floatingMask.classList.toggle("is-unlocked", !state.locked);
  floatingMask.querySelector("[data-action='lock']").textContent = state.locked ? "🔒" : "🔓";
}

function syncTile() {
  tile.classList.toggle("is-active", state.tileActive);
  document.querySelector("[data-tile-state]").textContent = state.tileActive ? "active" : "inactive";
  document.querySelector("[data-tile-note]").textContent = state.tileActive
    ? "再次点击磁贴会停止遮挡，并同步为 inactive。"
    : "权限完整时，点击磁贴可启动遮挡。";
}

function render() {
  syncOutputs();
  syncOrientation();
  syncLayoutChips();
  syncMask();
  syncFloatingMask();
  syncTile();
}

function applyLayout(layout) {
  const preset = layoutPresets[layout];
  state.layout = layout;
  state.orientation = preset.orientation;
  state.values.width = preset.width;
  state.values.height = preset.height;
  state.values.x = preset.x;
  state.values.bottom = preset.bottom;
  render();
}

viewTabs.forEach((button) => {
  button.addEventListener("click", () => setScreen(button.dataset.view));
});

document.querySelector("[data-action='menu']").addEventListener("click", () => {
  const menu = document.querySelector("[data-menu]");
  menu.hidden = !menu.hidden;
});

document.querySelector("[data-action='start']").addEventListener("click", () => {
  state.tileActive = true;
  setScreen("overlay");
  render();
});

document.querySelectorAll("[data-action='home']").forEach((button) => {
  button.addEventListener("click", () => setScreen("home"));
});

document.querySelector("[data-action='lock']").addEventListener("click", () => {
  state.locked = !state.locked;
  render();
});

tile.addEventListener("click", () => {
  state.tileActive = !state.tileActive;
  setScreen(state.tileActive ? "overlay" : "tile");
  render();
});

document.querySelectorAll("[data-orientation]").forEach((button) => {
  button.addEventListener("click", () => {
    state.orientation = button.dataset.orientation;
    state.layout = "";
    render();
  });
});

document.querySelectorAll("[data-layout]").forEach((button) => {
  button.addEventListener("click", () => applyLayout(button.dataset.layout));
});

document.querySelectorAll("[data-control]").forEach((input) => {
  input.addEventListener("input", () => {
    const control = input.dataset.control;
    const value = Number(input.value);
    if (control === "runtime-opacity") {
      state.values.opacity = value;
    } else {
      state.values[control] = value;
      if (control !== "opacity") {
        state.layout = "";
      }
    }
    render();
  });
});

render();
