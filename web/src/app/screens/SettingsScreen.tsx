// Settings — tabbed: Account / Network / Parental / Appearance / Backup.

import { useEffect, useRef, useState } from "react";
import { settingsRepo } from "@data/db/repositories";
import { useParentalStore } from "@app/stores/parental";
import { useUiStore } from "@app/stores/ui";
import { useSessionStore } from "@app/stores/session";
import { useI18n, availableLocales } from "@app/i18n";
import { exportBackup, importBackup } from "@data/manager/BackupManager";
import { setProxyUrl, getDefaultProxy } from "@data/net/proxy";
import { syncFirebasePlaylist } from "@data/sync/firebaseSync";

type Tab = "account" | "network" | "parental" | "appearance" | "backup";

export function SettingsScreen() {
  const { code, logout } = useSessionStore();
  const [tab, setTab] = useState<Tab>("account");
  const t = useI18n((s) => s.t);
  const lang = useI18n((s) => s.lang); void lang;

  const [syncing, setSyncing] = useState(false);

  // Network
  const [proxyUrl, setProxyUrlInput] = useState("");
  const [proxySaved, setProxySaved] = useState<string | null>(null);
  const defaultProxy = getDefaultProxy();

  // Parental
  const hasPin = useParentalStore((s) => s.hasPin);
  const setPin = useParentalStore((s) => s.setPin);
  const clearPin = useParentalStore((s) => s.clearPin);
  const hideLocked = useParentalStore((s) => s.hideLocked);
  const setHideLocked = useParentalStore((s) => s.setHideLocked);
  const [newPin, setNewPin] = useState("");

  // Appearance
  const setLang = useI18n((s) => s.setLang);
  const [theme, setTheme] = useState<"dark" | "light">("dark");
  const sidebarCollapsed = useUiStore((s) => s.sidebarCollapsed);
  const toggleSidebar = useUiStore((s) => s.toggleSidebar);

  // Backup
  const fileRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    void settingsRepo.get<string>("net.proxyUrl").then((v) => { if (v) { setProxyUrlInput(v); setProxySaved(v); } });
    void settingsRepo.get<"dark" | "light">("ui.theme").then((v) => { if (v) { setTheme(v); document.documentElement.dataset.theme = v; } });
  }, []);

  const saveProxy = async () => {
    const trimmed = proxyUrl.trim() || null;
    await setProxyUrl(trimmed);
    setProxySaved(trimmed);
  };

  const resetProxy = async () => {
    await setProxyUrl(null);
    setProxyUrlInput("");
    setProxySaved(null);
  };

  const applyTheme = async (themeToApply: "dark" | "light") => {
    setTheme(themeToApply);
    document.documentElement.dataset.theme = themeToApply;
    await settingsRepo.set("ui.theme", themeToApply);
  };

  const downloadBackup = async () => {
    const blob = await exportBackup();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `ultratv-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const restoreBackup = async (file: File | null | undefined) => {
    if (!file) return;
    await importBackup(file);
    location.reload();
  };

  const doSync = async () => {
    setSyncing(true);
    await syncFirebasePlaylist();
    setSyncing(false);
  };

  return (
    <div>
      <div className="tabs">
        {(["account", "network", "parental", "appearance", "backup"] as Tab[]).map((tk) => (
          <button key={tk} className={tab === tk ? "active" : ""} onClick={() => setTab(tk)}>
            {t(`settings.tab.${tk}`) !== `settings.tab.${tk}` ? t(`settings.tab.${tk}`) : tk.charAt(0).toUpperCase() + tk.slice(1)}
          </button>
        ))}
      </div>

      {tab === "account" && (
        <section>
          <h2>Account</h2>
          <div className="banner" style={{ marginBottom: 16 }}>
            Logged in with code: <strong>{code}</strong>
          </div>
          <div style={{ display: "flex", gap: 12 }}>
            <button disabled={syncing} onClick={doSync}>{syncing ? "Syncing..." : "Sync Playlist"}</button>
            <button onClick={logout} style={{ color: "#ff4d4d", borderColor: "#ff4d4d" }}>Logout</button>
          </div>
        </section>
      )}

      {tab === "network" && (
        <section>
          <h2>Network proxy</h2>
          <p style={{ color: "var(--fg-muted)" }}>
            Proxy that bypasses CORS and forwards User-Agent / Referer.
            Required for most IPTV providers in the browser.
          </p>

          <div className="banner" style={{ marginBottom: 12 }}>
            <div><strong>Currently active:</strong> {proxySaved ?? defaultProxy ?? "(none — direct fetch)"}</div>
            <div style={{ fontSize: 12, color: "var(--fg-muted)", marginTop: 4 }}>
              {proxySaved
                ? "Custom override saved below."
                : "Using the built-in default (baked in at build time)."}
            </div>
            <div style={{ fontSize: 12, color: "var(--fg-muted)" }}>
              <strong>Built-in default:</strong> {defaultProxy ?? "—"}
            </div>
          </div>

          <div className="form-row">
            <label>Custom proxy URL (override the default)</label>
            <div style={{ display: "flex", gap: 8, maxWidth: 720 }}>
              <input style={{ flex: 1 }} value={proxyUrl} onChange={(e) => setProxyUrlInput(e.target.value)} placeholder={defaultProxy ?? "https://your-proxy.example.com"} />
              <button onClick={saveProxy}>Save</button>
              {proxySaved && <button onClick={resetProxy} title="Use the built-in default">Reset</button>}
            </div>
          </div>
        </section>
      )}

      {tab === "parental" && (
        <section>
          <h2>Parental controls</h2>
          <p style={{ color: "var(--fg-muted)" }}>{hasPin ? "PIN is set." : "No PIN configured."}</p>
          <div style={{ display: "flex", gap: 8 }}>
            <input type="password" inputMode="numeric" placeholder="New PIN" value={newPin} onChange={(e) => setNewPin(e.target.value)} />
            <button onClick={async () => { if (newPin) { await setPin(newPin); setNewPin(""); } }}>Set PIN</button>
            {hasPin && <button onClick={() => clearPin()}>Clear PIN</button>}
          </div>
          <label style={{ display: "block", marginTop: 12 }}>
            <input type="checkbox" checked={hideLocked} onChange={(e) => setHideLocked(e.target.checked)} />
            {" "}Hide locked content from browsing
          </label>
        </section>
      )}

      {tab === "appearance" && (
        <section>
          <h2>Appearance</h2>
          <div className="form-row">
            <label>Language</label>
            <select value={lang} onChange={(e) => setLang(e.target.value)}>
              {availableLocales.map((l) => <option key={l} value={l}>{l}</option>)}
            </select>
          </div>
          <div className="form-row">
            <label>Theme</label>
            <div style={{ display: "flex", gap: 8 }}>
              <button className={theme === "dark" ? "filter-button active" : "filter-button"} onClick={() => applyTheme("dark")}>Dark</button>
              <button className={theme === "light" ? "filter-button active" : "filter-button"} onClick={() => applyTheme("light")}>Light</button>
            </div>
          </div>
          <div className="form-row">
            <label>Sidebar</label>
            <button onClick={() => toggleSidebar()}>{sidebarCollapsed ? "Expand" : "Collapse"} sidebar</button>
          </div>
        </section>
      )}

      {tab === "backup" && (
        <section>
          <h2>Backup</h2>
          <div style={{ display: "flex", gap: 8 }}>
            <button onClick={downloadBackup}>Export backup</button>
            <button onClick={() => fileRef.current?.click()}>Import backup</button>
            <input ref={fileRef} type="file" accept="application/json" style={{ display: "none" }} onChange={(e) => restoreBackup(e.target.files?.[0])} />
          </div>
          <p style={{ color: "var(--fg-muted)", marginTop: 12 }}>
            Backup contains: providers (with credentials), catalog, favorites, history, settings, parental locks, filters.
            Stored in IndexedDB locally. The JSON file can be moved between browsers.
          </p>
        </section>
      )}
    </div>
  );
}
